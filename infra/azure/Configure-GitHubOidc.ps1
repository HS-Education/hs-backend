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
$resourceGroup = 'rg-hs-thesis-azure'
$group = "/subscriptions/$subscription/resourceGroups/$resourceGroup"
# Do not leave a partially configured identity when the resource targets do not exist.
foreach ($name in @($BackendApp, $WorkerApp)) {
    & az webapp show --resource-group $resourceGroup --name $name --subscription $subscription --output none --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'The App Service targets must exist before configuring OIDC.' }
}
& az postgres flexible-server show --resource-group $resourceGroup --name $PostgresServer --subscription $subscription --output none --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'The PostgreSQL target must exist before configuring OIDC.' }
& az keyvault show --name $KeyVault --resource-group $resourceGroup --subscription $subscription --output none --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'The Key Vault target must exist before configuring OIDC.' }

function Assert-CdFederations {
    param([AllowEmptyCollection()][object[]] $Federations)
    if ($Federations.Count -gt 1) { throw 'Unexpected extra OIDC federations; existing trust was not modified.' }
    foreach ($federation in $Federations) {
        if ($federation.name -ne 'hs-backend-azure-students' -or
            $federation.issuer -ne 'https://token.actions.githubusercontent.com' -or
            $federation.subject -ne 'repo:HS-Education/hs-backend:environment:azure-students' -or
            @($federation.audiences).Count -ne 1 -or $federation.audiences[0] -ne 'api://AzureADTokenExchange') {
            throw 'Existing OIDC federation does not match the protected environment; it was not modified.'
        }
    }
}

# A user-assigned identity supports GitHub OIDC through ARM. It needs neither
# tenant-wide Graph privileges nor a client secret, VM or self-hosted runner.
$identityName = 'hs-thesis-github-cd'
$identitiesJson = & az identity list --resource-group $resourceGroup --subscription $subscription --output json --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Managed identity lookup failed.' }
$identities = @($identitiesJson | ConvertFrom-Json | Where-Object name -eq $identityName)
if ($identities.Count -gt 1) { throw 'Cannot uniquely identify the CD managed identity.' }
if ($identities.Count -eq 1) {
    if ($identities[0].tenantId -ne '0e0cb060-09ad-49f5-a005-68b9b49aa1f6' -or $identities[0].location -ne 'mexicocentral') {
        throw 'The existing CD managed identity is outside the approved tenant or region.'
    }
    $federationsJson = & az identity federated-credential list --identity-name $identityName --resource-group $resourceGroup --subscription $subscription --output json --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Existing OIDC trust could not be verified.' }
    Assert-CdFederations -Federations @($federationsJson | ConvertFrom-Json)
}
$identityJson = & az deployment group create --name hs-students-github-oidc --resource-group $resourceGroup `
    --template-file "$PSScriptRoot/github-oidc.bicep" --mode Incremental --subscription $subscription `
    --query properties.outputs.identity.value --output json --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Managed identity Bicep deployment failed.' }
$identity = $identityJson | ConvertFrom-Json
if ($identity.tenantId -ne '0e0cb060-09ad-49f5-a005-68b9b49aa1f6' -or
    $identity.id -ne "$group/providers/Microsoft.ManagedIdentity/userAssignedIdentities/$identityName" -or
    !$identity.clientId -or !$identity.principalId) { throw 'The configured CD identity is not in the approved Students scope.' }
$principalId = $identity.principalId
$clientId = $identity.clientId

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
