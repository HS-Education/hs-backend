param([string] $Region = 'mexicocentral')
. "$PSScriptRoot/Students.Common.ps1"
$subscription = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
$null = Get-StudentsAccount -SubscriptionId $subscription
Assert-StudentsRegion -Region $Region -AllowedRegions @(Get-StudentsAllowedRegions -SubscriptionId $subscription)
$generated = Join-Path $PSScriptRoot '.generated'
$null = New-Item -ItemType Directory -Path $generated -Force
$file = Join-Path $generated ('capacity-' + [guid]::NewGuid().ToString('N') + '.json')
try {
    @{ name = 'hs-thesis-linux'; location = $Region; type = 'ServerFarm'
        properties = @{ skuName = 'B2'; capacity = 1; needLinuxWorkers = $true } } |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $file -Encoding utf8
    # Validation POST is non-provisioning. The generated body contains no credentials.
    $response = & az rest --method post --url "https://management.azure.com/subscriptions/$subscription/resourceGroups/rg-hs-thesis-azure/providers/Microsoft.Web/validate?api-version=2025-05-01" `
        --headers Content-Type=application/json --body "@$file" --subscription $subscription --only-show-errors --output json
    if ($LASTEXITCODE -ne 0) { throw 'App Service validation could not complete; capacity remains unverified.' }
    $validation = $response | ConvertFrom-Json
    Assert-AppServiceCapacity -Validation $validation
    Write-Host "App Service Linux B2, one instance: validation passed in $Region. Capacity can change before creation."
}
finally { Remove-Item -LiteralPath $file -ErrorAction SilentlyContinue }
