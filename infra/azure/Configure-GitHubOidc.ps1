param(
    [Parameter(Mandatory)][string] $BackendApp,
    [Parameter(Mandatory)][string] $WorkerApp,
    [Parameter(Mandatory)][string] $PostgresServer,
    [Parameter(Mandatory)][string] $KeyVault,
    [switch] $ApproveIdentityChanges
)
. "$PSScriptRoot/Students.Common.ps1"
$subscription = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
$null = Get-StudentsAccount -SubscriptionId $subscription
if (!$ApproveIdentityChanges) { throw 'Identity and RBAC changes require -ApproveIdentityChanges after infrastructure review.' }
if ($BackendApp -notmatch '^hs-thesis-api-[a-z0-9]+$' -or $WorkerApp -notmatch '^hs-thesis-worker-[a-z0-9]+$' -or
    $PostgresServer -notmatch '^hs-thesis-pg-[a-z0-9]+$' -or $KeyVault -notmatch '^hskv-[a-z0-9]+$') { throw 'Unexpected resource names.' }
$group = "/subscriptions/$subscription/resourceGroups/rg-hs-thesis-azure"
# Do not leave a partially configured Entra identity when the resource targets do not exist.
foreach ($name in @($BackendApp, $WorkerApp)) {
    & az webapp show --resource-group rg-hs-thesis-azure --name $name --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'The App Service targets must exist before configuring OIDC.' }
}
& az postgres flexible-server show --resource-group rg-hs-thesis-azure --name $PostgresServer --subscription $subscription --output none --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'The PostgreSQL target must exist before configuring OIDC.' }
& az keyvault show --name $KeyVault --resource-group rg-hs-thesis-azure --subscription $subscription --output none --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'The Key Vault target must exist before configuring OIDC.' }
$existing = & az ad app list --display-name hs-thesis-github-cd --subscription $subscription --query '[].appId' --output json --only-show-errors | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or @($existing).Count -gt 1) { throw 'Cannot uniquely identify the CD application.' }
if (@($existing).Count -eq 0) {
    $clientId = & az ad app create --display-name hs-thesis-github-cd --subscription $subscription --query appId --output tsv --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Microsoft Entra application creation failed.' }
    & az ad sp create --id $clientId --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'CD service principal creation failed.' }
} else { $clientId = @($existing)[0] }
$principalId = & az ad sp show --id $clientId --subscription $subscription --query id --output tsv --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Cannot read the CD service principal.' }
$generated = Join-Path $PSScriptRoot '.generated'
$null = New-Item -ItemType Directory -Path $generated -Force
$federationFile = Join-Path $generated 'backend-federation.json'
@{ name = 'hs-backend-azure-students'; issuer = 'https://token.actions.githubusercontent.com'
   subject = 'repo:HS-Education/hs-backend:environment:azure-students'; audiences = @('api://AzureADTokenExchange') } |
    ConvertTo-Json | Set-Content -LiteralPath $federationFile -Encoding utf8
$federations = & az ad app federated-credential list --id $clientId --subscription $subscription --output json --only-show-errors | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Cannot read federated identities.' }
if (!(@($federations) | Where-Object name -eq 'hs-backend-azure-students')) {
    & az ad app federated-credential create --id $clientId --parameters "@$federationFile" --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'OIDC federation creation failed.' }
} else {
    $federation = @($federations | Where-Object name -eq 'hs-backend-azure-students')
    if ($federation.Count -ne 1 -or $federation[0].issuer -ne 'https://token.actions.githubusercontent.com' -or
        $federation[0].subject -ne 'repo:HS-Education/hs-backend:environment:azure-students' -or
        @($federation[0].audiences).Count -ne 1 -or $federation[0].audiences[0] -ne 'api://AzureADTokenExchange') {
        throw 'Existing OIDC federation does not match the protected environment; it was not modified.'
    }
}
& az role definition create --role-definition "@$PSScriptRoot/firewall-role.json" --subscription $subscription --output none --only-show-errors
if ($LASTEXITCODE -ne 0) {
    $role = & az role definition list --name 'HS Thesis CD PostgreSQL Firewall Operator' --subscription $subscription --output json --only-show-errors | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or @($role).Count -ne 1) { throw 'Firewall role creation or lookup failed.' }
}
$roles = @(
    @{ role = 'Reader'; scope = "/subscriptions/$subscription" },
    @{ role = 'Website Contributor'; scope = "$group/providers/Microsoft.Web/sites/$BackendApp" },
    @{ role = 'Website Contributor'; scope = "$group/providers/Microsoft.Web/sites/$WorkerApp" },
    @{ role = 'HS Thesis CD PostgreSQL Firewall Operator'; scope = "$group/providers/Microsoft.DBforPostgreSQL/flexibleServers/$PostgresServer" },
    @{ role = 'Key Vault Secrets User'; scope = "$group/providers/Microsoft.KeyVault/vaults/$KeyVault/secrets/postgres-password" },
    @{ role = 'Key Vault Secrets User'; scope = "$group/providers/Microsoft.KeyVault/vaults/$KeyVault/secrets/postgres-app-password" }
)
foreach ($role in $roles) {
    & az role assignment create --assignee-object-id $principalId --assignee-principal-type ServicePrincipal --role $role.role `
        --scope $role.scope --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Scoped CD role assignment failed.' }
}
Write-Host "OIDC client ID: $clientId. No client secret was created. Configure the protected GitHub environment separately."
