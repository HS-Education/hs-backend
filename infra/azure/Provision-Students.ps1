param(
    [ValidateSet('WhatIf', 'Create')][string] $Mode = 'WhatIf',
    [Parameter(Mandatory)][string] $ParametersFile,
    [switch] $ApprovePaidResources
)
. "$PSScriptRoot/Students.Common.ps1"
$subscriptionId = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
$null = Get-StudentsAccount -SubscriptionId $subscriptionId
$resolved = (Resolve-Path -LiteralPath $ParametersFile).Path
$parameters = Get-Content -LiteralPath $resolved -Raw | ConvertFrom-Json
Assert-PrivateParameters -Parameters $parameters
$allowedRegions = @(Get-StudentsAllowedRegions -SubscriptionId $subscriptionId)
Assert-StudentsRegion -Region $parameters.parameters.location.value -AllowedRegions $allowedRegions
if ($Mode -eq 'Create' -and !$ApprovePaidResources) { throw 'Creating paid resources requires -ApprovePaidResources after cost review.' }
if ($Mode -eq 'Create') {
    foreach ($name in @('postgresPassword', 'postgresAppPassword', 'jwtSecret', 'workerApiKey', 'openrouterApiKey', 'bootstrapAdminPassword')) {
        if ($parameters.parameters.$name.value.StartsWith('PreviewOnly-') -or $parameters.parameters.$name.value.StartsWith('fixture-only-')) {
            throw 'Preview/test canaries cannot be used to create the real environment.'
        }
    }
}
# What-if does not register providers or provision a resource group. Registration is an explicit future prerequisite.
$verb = if ($Mode -eq 'Create') { 'create' } else { 'what-if' }
& az deployment sub $verb --name hs-students-infrastructure --location $parameters.parameters.location.value --subscription $subscriptionId `
    --template-file "$PSScriptRoot/main.bicep" --parameters "@$resolved" --only-show-errors --output none
if ($LASTEXITCODE -ne 0) { throw 'Azure infrastructure operation failed; do not retry in another subscription.' }
Write-Host "Students infrastructure $Mode completed. Secret outputs are suppressed."
