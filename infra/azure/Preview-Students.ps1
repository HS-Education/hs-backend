param([string] $Region = 'mexicocentral', [switch] $Detailed)
. "$PSScriptRoot/Students.Common.ps1"
$subscription = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
$null = Get-StudentsAccount -SubscriptionId $subscription
Assert-StudentsRegion -Region $Region -AllowedRegions @(Get-StudentsAllowedRegions -SubscriptionId $subscription)

# Non-production canaries make a resource preview possible without loading any real key.
# This file is generated under an ignored directory, never used for a Create operation.
$fixture = Get-Content "$PSScriptRoot/parameters.example.json" -Raw | ConvertFrom-Json
$fixture.parameters.location.value = $Region
foreach ($name in @('postgresPassword', 'postgresAppPassword', 'jwtSecret', 'workerApiKey', 'openrouterApiKey', 'bootstrapAdminPassword')) {
    $fixture.parameters.$name.value = "PreviewOnly-$name-" + [guid]::NewGuid().ToString('N') + '!'
}
Assert-PrivateParameters -Parameters $fixture
$generated = Join-Path $PSScriptRoot '.generated'
$null = New-Item -ItemType Directory -Path $generated -Force
$file = Join-Path $generated ('preview-' + [guid]::NewGuid().ToString('N') + '.json')
try {
    $fixture | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $file -Encoding utf8
    $response = & az deployment sub what-if --name hs-students-preview --location $Region --subscription $subscription `
        --template-file "$PSScriptRoot/main.bicep" --parameters "@$file" --no-pretty-print --only-show-errors --output json
    if ($LASTEXITCODE -ne 0) { throw 'Azure what-if failed; no resources were created. Resolve the diagnostics before provisioning.' }
    $preview = $response | ConvertFrom-Json
    if ($preview.status -ne 'Succeeded') { throw 'Azure what-if did not report success.' }
    $changes = @($preview.changes)
    if (@($changes | Where-Object changeType -eq 'Delete').Count) {
        throw 'The preview proposes a deletion; it must be reviewed before any real deployment.'
    }
    $summary = [ordered]@{
        status = $preview.status
        region = $Region
        createdResources = $false
        changes = @($changes | Group-Object changeType | ForEach-Object { @{ type = $_.Name; count = $_.Count } })
        deferred = @($changes | Where-Object { $_.changeType -in @('Ignore', 'Unsupported') } | Select-Object resourceId, changeType, unsupportedReason)
    }
    $summary | ConvertTo-Json -Depth 8
    if ($Detailed) { $response }
}
finally {
    # The exact temporary file, not its directory, is removed; all values were fake canaries.
    Remove-Item -LiteralPath $file -ErrorAction SilentlyContinue
}
