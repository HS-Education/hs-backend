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

function Get-PostgresFirewallCliFlags {
    # CLI releases renamed the server/rule flags. Inspect local capabilities
    # before any firewall or credential operation; never guess from a version.
    $helpLines = & az postgres flexible-server firewall-rule create --help
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine the PostgreSQL firewall CLI syntax.' }
    $helpText = $helpLines -join "`n"
    if ($helpText -match '(?m)^\s*--server-name(?:\s|$)' -and $helpText -match '(?m)^\s*--name(?:\s|$)') {
        return @{ Server = '--server-name'; Rule = '--name' }
    }
    if ($helpText -match '(?m)^\s*--name(?:\s|$)' -and $helpText -match '(?m)^\s*--rule-name(?:\s|$)') {
        return @{ Server = '--name'; Rule = '--rule-name' }
    }
    throw 'Unsupported PostgreSQL firewall CLI syntax; no temporary rule was created.'
}

$firewallFlags = Get-PostgresFirewallCliFlags
$serverArguments = @($firewallFlags.Server, $PostgresServer)
$hostName = & az postgres flexible-server show --resource-group $ResourceGroup --name $PostgresServer --subscription $subscription --query fullyQualifiedDomainName --output tsv --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Cannot read the expected PostgreSQL server.' }
$address = (Invoke-RestMethod -Uri 'https://api.ipify.org').Trim()
$ip = [System.Net.IPAddress]::Parse($address)
if ($ip.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork -or $ip.Equals([System.Net.IPAddress]::Any)) {
    throw 'A nonzero IPv4 runner address is required; broad Azure access is forbidden.'
}
$rule = 'cd-' + [guid]::NewGuid().ToString('N')
$ruleArguments = @($firewallFlags.Rule, $rule)
$ruleId = "/subscriptions/$subscription/resourceGroups/$ResourceGroup/providers/Microsoft.DBforPostgreSQL/flexibleServers/$PostgresServer/firewallRules/$rule"
$migrationFailure = $null
$cleanupFailure = $null
try {
    & az postgres flexible-server firewall-rule create --resource-group $ResourceGroup @serverArguments @ruleArguments `
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
catch {
    $migrationFailure = $_
}
finally {
    foreach ($name in @('MIGRATION_DATABASE_URL', 'MIGRATION_DATABASE_USER', 'MIGRATION_DATABASE_PASSWORD', 'MIGRATION_APP_PASSWORD')) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $owner = $null; $app = $null
    try {
        # The resource ID unambiguously identifies only this invocation's rule
        # on both CLI layouts, even if creation succeeded but its response failed.
        & az postgres flexible-server firewall-rule delete --ids $ruleId `
            --subscription $subscription --yes --output none --only-show-errors
        # A failed delete is harmless only when absence is positively verified.
        $remainingJson = & az postgres flexible-server firewall-rule list --resource-group $ResourceGroup @serverArguments `
            --subscription $subscription --query "[?name=='$rule'].name" --output json --only-show-errors
        if ($LASTEXITCODE -ne 0 -or !$remainingJson) { throw 'Temporary rule absence could not be verified.' }
        $remaining = $remainingJson | ConvertFrom-Json -NoEnumerate -ErrorAction Stop
        if ($remaining -isnot [array] -or $remaining.Count -ne 0) { throw 'The temporary firewall rule is not verified absent.' }
    }
    catch {
        $cleanupFailure = "Temporary firewall rule cleanup could not be confirmed: $ruleId. Check and remove this exact rule before continuing."
    }
}
if ($migrationFailure) {
    if ($cleanupFailure) { throw "$($migrationFailure.Exception.Message) $cleanupFailure" }
    throw $migrationFailure
}
if ($cleanupFailure) { throw $cleanupFailure }
