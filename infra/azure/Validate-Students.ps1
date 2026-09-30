param([string] $SubscriptionId = '86d9e5e6-b9bf-44b4-915a-106207e0bc02', [string] $Region = '', [switch] $Offline)
. "$PSScriptRoot/Students.Common.ps1"
& "$PSScriptRoot/Test-StudentsGuard.ps1"
& az bicep build --file "$PSScriptRoot/main.bicep" --stdout --only-show-errors | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Infrastructure compilation failed.' }
if (!$Offline) {
    $account = Get-StudentsAccount -SubscriptionId $SubscriptionId
    Write-Host "Verified $($account.displayName): spending limit remains On. No resources have been created."
    $regions = @(Get-StudentsAllowedRegions -SubscriptionId $SubscriptionId)
    Write-Host "Students allowed regions: $($regions -join ', ')."
    if ($Region) { Assert-StudentsRegion -Region $Region -AllowedRegions $regions }
    & az group list --subscription $SubscriptionId --query '[].name' --output json --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Resource inventory read failed.' }
}
