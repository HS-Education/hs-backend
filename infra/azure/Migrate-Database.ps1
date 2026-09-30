param(
    [Parameter(Mandatory)][string] $ResourceGroup,
    [Parameter(Mandatory)][string] $PostgresServer,
    [Parameter(Mandatory)][string] $KeyVault,
    [Parameter(Mandatory)][string] $Jar
)
. "$PSScriptRoot/Students.Common.ps1"
$subscription = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
$null = Get-StudentsAccount -SubscriptionId $subscription
if ($ResourceGroup -ne 'rg-hs-thesis-azure' -or $PostgresServer -notmatch '^hs-thesis-pg-[a-z0-9]+$' -or
    $KeyVault -notmatch '^hskv-[a-z0-9]+$') { throw 'Unexpected infrastructure names.' }
$hostName = & az postgres flexible-server show --resource-group $ResourceGroup --name $PostgresServer --subscription $subscription --query fullyQualifiedDomainName --output tsv --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Cannot read the expected PostgreSQL server.' }
$address = (Invoke-RestMethod -Uri 'https://api.ipify.org').Trim()
$ip = [System.Net.IPAddress]::Parse($address)
if ($ip.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork) { throw 'An IPv4 runner address is required.' }
$rule = 'cd-' + [guid]::NewGuid().ToString('N')
try {
    & az postgres flexible-server firewall-rule create --resource-group $ResourceGroup --name $PostgresServer --rule-name $rule `
        --start-ip-address $address --end-ip-address $address --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Temporary runner firewall rule could not be created.' }
    $owner = & az keyvault secret show --vault-name $KeyVault --name postgres-password --subscription $subscription --query value --output tsv --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Database owner credential read failed.' }
    $app = & az keyvault secret show --vault-name $KeyVault --name postgres-app-password --subscription $subscription --query value --output tsv --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Database application credential read failed.' }
    if ($env:GITHUB_ACTIONS -eq 'true') {
        Write-Host "::add-mask::$owner"
        Write-Host "::add-mask::$app"
    }
    $env:MIGRATION_DATABASE_URL = "jdbc:postgresql://$hostName`:5432/hs_thesis?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"
    $env:MIGRATION_DATABASE_USER = 'thesis_owner'
    $env:MIGRATION_DATABASE_PASSWORD = $owner
    $env:MIGRATION_APP_PASSWORD = $app
    & java '-Dloader.main=com.hs.hstesis.shared.infrastructure.azure.DatabaseMigrationMain' -cp $Jar org.springframework.boot.loader.launch.PropertiesLauncher
    if ($LASTEXITCODE -ne 0) { throw 'Database migration failed; application deployment must not continue.' }
}
finally {
    foreach ($name in @('MIGRATION_DATABASE_URL', 'MIGRATION_DATABASE_USER', 'MIGRATION_DATABASE_PASSWORD', 'MIGRATION_APP_PASSWORD')) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $owner = $null; $app = $null
    & az postgres flexible-server firewall-rule delete --resource-group $ResourceGroup --name $PostgresServer --rule-name $rule `
        --subscription $subscription --yes --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { Write-Error "Temporary firewall rule cleanup failed: $rule. Remove it before continuing." }
}
