. "$PSScriptRoot/Students.Common.ps1"
$expected = '86d9e5e6-b9bf-44b4-915a-106207e0bc02'
function Account([string] $id = $expected, [string] $offer = 'AzureForStudents_2018-01-01', [string] $limit = 'On', [string] $state = 'Enabled') {
    return [pscustomobject]@{ subscriptionId = $id; tenantId = '0e0cb060-09ad-49f5-a005-68b9b49aa1f6'; state = $state
        subscriptionPolicies = [pscustomobject]@{ quotaId = $offer; spendingLimit = $limit } }
}
Assert-StudentsAccount -Account (Account) -ExpectedSubscriptionId $expected
$rejected = 0
foreach ($account in @((Account -id '00000000-0000-0000-0000-000000000000'),
    (Account -offer 'PayAsYouGo_2014-09-01'), (Account -offer 'AzureForStudentsStarter_2018-01-01'),
    (Account -limit 'Off'), (Account -state 'Disabled'))) {
    try { Assert-StudentsAccount -Account $account -ExpectedSubscriptionId $expected }
    catch { $rejected++; continue }
    throw 'The guard accepted an invalid subscription.'
}
if ($rejected -ne 5) { throw 'Not all negative guard cases were tested.' }
Write-Host 'Students guard: 1 valid and 5 rejected cases passed; no Azure calls were made.'

$allowed = @('westus', 'canadacentral', 'westus3', 'mexicocentral', 'northcentralus')
Assert-StudentsRegion -Region mexicocentral -AllowedRegions $allowed
$regionRejected = 0
foreach ($region in @('eastus', 'eastus2', 'East US', 'https://example.invalid')) {
    try { Assert-StudentsRegion -Region $region -AllowedRegions $allowed }
    catch { $regionRejected++; continue }
    throw 'The region guard accepted a blocked or invalid location.'
}
if ($regionRejected -ne 4) { throw 'Not all region guard cases were tested.' }
Write-Host 'Region guard: 1 valid and 4 rejected cases passed; no Azure calls were made.'

function FixtureParameters {
    $fixture = Get-Content "$PSScriptRoot/parameters.example.json" -Raw | ConvertFrom-Json
    foreach ($name in @('postgresPassword', 'postgresAppPassword', 'jwtSecret', 'workerApiKey', 'openrouterApiKey', 'bootstrapAdminPassword')) {
        $fixture.parameters.$name.value = "fixture-only-$name-0000000000000000000000000000"
    }
    return $fixture
}
Assert-PrivateParameters -Parameters (FixtureParameters)
$parameterRejected = 0
foreach ($case in @('placeholder', 'same-passwords', 'short-jwt', 'unresolved-reference', 'wrong-subscription', 'swa-origin', 'unknown-hosting')) {
    $fixture = FixtureParameters
    switch ($case) {
        'placeholder' { $fixture.parameters.openrouterApiKey.value = 'REPLACE_LOCALLY_NEVER_COMMIT' }
        'same-passwords' { $fixture.parameters.postgresAppPassword.value = $fixture.parameters.postgresPassword.value }
        'short-jwt' { $fixture.parameters.jwtSecret.value = 'sixteen-char-key' }
        'unresolved-reference' { $fixture.parameters.workerApiKey.value = '@Microsoft.KeyVault(SecretUri=https://fixture.invalid/)' }
        'wrong-subscription' { $fixture.parameters.studentsSubscriptionId.value = '00000000-0000-0000-0000-000000000000' }
        'swa-origin' { $fixture.parameters.frontendHosting.value = 'swa'; $fixture.parameters.frontendOrigin.value = 'https://user:password@example.invalid' }
        'unknown-hosting' { $fixture.parameters.frontendHosting.value = 'unknown' }
    }
    try { Assert-PrivateParameters -Parameters $fixture }
    catch { $parameterRejected++; continue }
    throw "Private parameters guard accepted invalid fixture: $case"
}
if ($parameterRejected -ne 7) { throw 'Not all private parameter cases were tested.' }
Write-Host 'Private parameters: 1 valid and 7 rejected cases passed; no real credentials were used.'
