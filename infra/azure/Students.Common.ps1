Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-StudentsAccount {
    param([Parameter(Mandatory)] $Account, [Parameter(Mandatory)][string] $ExpectedSubscriptionId)
    if ($ExpectedSubscriptionId -ne '86d9e5e6-b9bf-44b4-915a-106207e0bc02' -or
        $Account.subscriptionId -ne $ExpectedSubscriptionId -or $Account.state -ne 'Enabled' -or
        $Account.tenantId -ne '0e0cb060-09ad-49f5-a005-68b9b49aa1f6' -or
        $Account.subscriptionPolicies.quotaId -notlike 'AzureForStudents_*' -or
        $Account.subscriptionPolicies.spendingLimit -ne 'On') {
        throw 'Refusing deployment: the expected Azure for Students subscription and spending limit are required.'
    }
}

function Get-StudentsAccount {
    param([string] $SubscriptionId = '86d9e5e6-b9bf-44b4-915a-106207e0bc02')
    $response = & az rest --method get --url "https://management.azure.com/subscriptions/$SubscriptionId`?api-version=2022-12-01" --subscription $SubscriptionId --only-show-errors --output json
    if ($LASTEXITCODE -ne 0) { throw 'Azure authentication or subscription read failed. Renew the Students login.' }
    $account = $response | ConvertFrom-Json
    Assert-StudentsAccount -Account $account -ExpectedSubscriptionId $SubscriptionId
    return $account
}

function Assert-StudentsRegion {
    param([Parameter(Mandatory)][string] $Region, [Parameter(Mandatory)][string[]] $AllowedRegions)
    if ($Region -cnotmatch '^[a-z0-9]+$' -or $AllowedRegions.Count -eq 0 -or $Region -notin $AllowedRegions) {
        throw "Region '$Region' is not permitted by the Students subscription policy. Review the region before provisioning."
    }
}

function Get-StudentsAllowedRegions {
    param([string] $SubscriptionId = '86d9e5e6-b9bf-44b4-915a-106207e0bc02')
    $response = & az policy assignment list --subscription $SubscriptionId --output json --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Cannot verify the Students region policy; provisioning must not continue.' }
    $allowed = $null
    foreach ($assignment in @($response | ConvertFrom-Json)) {
        if (!$assignment.parameters) { continue }
        $parameter = $assignment.parameters.PSObject.Properties['listOfAllowedLocations']
        if (!$parameter) { continue }
        $regions = @($parameter.Value.value)
        if ($null -eq $allowed) { $allowed = $regions }
        else { $allowed = @($allowed | Where-Object { $_ -in $regions }) }
    }
    if ($null -eq $allowed -or @($allowed).Count -eq 0) {
        throw 'The effective allowed-region list could not be established. Review Azure Policy explicitly.'
    }
    return $allowed
}

function Assert-PrivateParameters {
    param([Parameter(Mandatory)] $Parameters)
    $expected = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
    if ($Parameters.parameters.studentsSubscriptionId.value -ne $expected) { throw 'Wrong subscription in parameters.' }
    foreach ($name in @('postgresPassword', 'postgresAppPassword', 'jwtSecret', 'workerApiKey', 'openrouterApiKey', 'bootstrapAdminPassword')) {
        $value = $Parameters.parameters.$name.value
        if (!$value -or $value.StartsWith('REPLACE_') -or $value.StartsWith('@Microsoft.KeyVault(') -or $value.Length -lt 16) {
            throw "A resolved strong value for $name must be supplied privately."
        }
    }
    if ($Parameters.parameters.postgresPassword.value -eq $Parameters.parameters.postgresAppPassword.value) {
        throw 'Database owner and application passwords must be different.'
    }
    if ($Parameters.parameters.jwtSecret.value.Length -lt 32) { throw 'JWT signing needs at least 32 random characters.' }
    if ($Parameters.parameters.frontendHosting.value -notin @('app-service', 'swa')) { throw 'Unsupported frontend hosting mode.' }
    if ($Parameters.parameters.frontendHosting.value -eq 'swa' -and
        $Parameters.parameters.frontendOrigin.value -notmatch '^https://[^/?#@]+$') {
        throw 'SWA requires an explicit trusted frontend HTTPS origin and a validated cookie/domain strategy.'
    }
}

function Assert-ReleaseTag {
    param([Parameter(Mandatory)][string] $Tag)
    if ($Tag -cnotmatch '^v[0-9]+\.[0-9]+\.[0-9]+$') { throw 'An explicit vMAJOR.MINOR.PATCH release tag is required.' }
    & git fetch --no-tags origin main
    if ($LASTEXITCODE -ne 0) { throw 'Cannot verify the main branch.' }
    & git merge-base --is-ancestor "$Tag^{commit}" origin/main
    if ($LASTEXITCODE -ne 0) { throw 'The release tag must point to a commit integrated into main.' }
}
