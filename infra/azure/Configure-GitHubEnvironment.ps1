param(
    [ValidateSet('HS-Education/hs-backend', 'HS-Education/hs-front')][Parameter(Mandatory)][string] $Repository,
    [string[]] $Reviewers = @(),
    [ValidateSet('Inspect', 'Configure', 'Checks')][string] $Mode = 'Inspect',
    [string] $Reference = 'feature/azure-students-deployment',
    [switch] $ApproveGitHubChanges
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Mode -eq 'Configure' -and !$ApproveGitHubChanges) { throw 'GitHub changes require explicit approval.' }

# Credentials stay in memory; never write them to a file, URL or command output.
$token = $env:GH_TOKEN
if (!$token) {
    $credentialLines = "protocol=https`nhost=github.com`n`n" | & git credential fill
    if ($LASTEXITCODE -ne 0) { throw 'GitHub authentication is unavailable.' }
    foreach ($line in $credentialLines) {
        if ($line.StartsWith('password=')) { $token = $line.Substring(9) }
    }
    $credentialLines = $null
}
if (!$token) { throw 'No GitHub credential is available.' }
$headers = @{ Authorization = "Bearer $token"; Accept = 'application/vnd.github+json'; 'X-GitHub-Api-Version' = '2022-11-28' }
function GitHubRequest([string] $Method, [string] $Path, $Body = $null) {
    $arguments = @{ Method = $Method; Uri = "https://api.github.com/$Path"; Headers = $headers }
    if ($null -ne $Body) { $arguments.ContentType = 'application/json'; $arguments.Body = $Body | ConvertTo-Json -Depth 15 -Compress }
    try { return Invoke-RestMethod @arguments }
    catch {
        $status = if ($_.Exception.Response) { [int] $_.Exception.Response.StatusCode } else { 0 }
        if ($Method -eq 'GET' -and $status -eq 404) { return $null }
        throw "GitHub $Method request failed (HTTP $status); no credential values are logged."
    }
}
try {
    $user = GitHubRequest GET 'user'
    $repo = GitHubRequest GET "repos/$Repository"
    if (!$repo) { throw 'Repository is inaccessible.' }
    if ($Mode -eq 'Checks') {
        $encodedRef = [uri]::EscapeDataString($Reference)
        $commit = GitHubRequest GET "repos/$Repository/commits/$encodedRef"
        if (!$commit) { throw 'Published reference is not available.' }
        $checks = GitHubRequest GET "repos/$Repository/commits/$($commit.sha)/check-runs?per_page=100"
        $runs = GitHubRequest GET "repos/$Repository/actions/runs?branch=$encodedRef&per_page=10"
        [ordered]@{
            repository = $Repository; reference = $Reference; commit = $commit.sha
            checks = @($checks.check_runs | Select-Object name, status, conclusion, html_url)
            runs = @($runs.workflow_runs | Where-Object head_sha -eq $commit.sha | Select-Object name, status, conclusion, html_url)
        } | ConvertTo-Json -Depth 6
        return
    }
    $environment = GitHubRequest GET "repos/$Repository/environments/azure-students"
    $variable = GitHubRequest GET "repos/$Repository/actions/variables/AZURE_CD_ENABLED"
    Write-Host "Repository: $Repository; authenticated actor: $($user.login); admin: $($repo.permissions.admin)."
    Write-Host "Environment exists: $($null -ne $environment); CD enabled: $($null -ne $variable -and $variable.value -eq 'true')."
    if ($environment) {
        $approvals = @($environment.protection_rules | Where-Object type -eq 'required_reviewers')
        if ($approvals.Count -eq 1) {
            Write-Host "Required reviewers: $(@($approvals[0].reviewers | ForEach-Object { $_.reviewer.login }) -join ', '); prevent self-review: $($approvals[0].prevent_self_review)."
        }
        $existingRestrictions = GitHubRequest GET "repos/$Repository/environments/azure-students/deployment-branch-policies"
        Write-Host "Deployment refs: $(@($existingRestrictions.branch_policies | ForEach-Object { $_.type + ':' + $_.name }) -join ', '); admin bypass: $($environment.can_admins_bypass)."
    }
    if ($Mode -eq 'Inspect') { return }
    if (!$repo.permissions.admin) { throw 'Repository admin permission is required to configure an environment.' }
    if ($Reviewers.Count -eq 0) { throw 'At least one explicit reviewer is required.' }
    $reviewerIds = @()
    foreach ($login in $Reviewers) {
        if ($login -notmatch '^[A-Za-z0-9-]+$') { throw 'Invalid reviewer login.' }
        $reviewer = GitHubRequest GET "users/$login"
        if (!$reviewer) { throw 'Reviewer not found.' }
        $permission = GitHubRequest GET "repos/$Repository/collaborators/$login/permission"
        if (!$permission -or $permission.permission -notin @('write', 'maintain', 'admin')) { throw 'Reviewer needs repository write access.' }
        $reviewerIds += @{ type = 'User'; id = $reviewer.id }
    }
    if ($environment) {
        # Do not silently replace existing reviewers, restrictions or bypass policy.
        $approval = @($environment.protection_rules | Where-Object type -eq 'required_reviewers')
        if ($approval.Count -ne 1 -or !$environment.deployment_branch_policy.custom_branch_policies) {
            throw 'Existing environment needs human review; its protection settings were preserved.'
        }
    } else {
        $null = GitHubRequest PUT "repos/$Repository/environments/azure-students" @{
            wait_timer = 0; reviewers = $reviewerIds; prevent_self_review = $true; can_admins_bypass = $false
            deployment_branch_policy = @{ protected_branches = $false; custom_branch_policies = $true }
        }
    }
    $policies = GitHubRequest GET "repos/$Repository/environments/azure-students/deployment-branch-policies"
    $existingPolicies = @($policies.branch_policies)
    if (@($existingPolicies | Where-Object { $_.type -ne 'tag' -or $_.name -ne 'v*' }).Count -gt 0) {
        throw 'Existing deployment policies allow other refs; review them manually. No policy was removed.'
    }
    if ($existingPolicies.Count -eq 0) {
        $null = GitHubRequest POST "repos/$Repository/environments/azure-students/deployment-branch-policies" @{ name = 'v*'; type = 'tag' }
    }
    # Keep paid deployments disabled until infrastructure, OIDC, release tags and smoke fixtures exist.
    if ($variable -and $variable.value -eq 'true') { throw 'CD is already enabled; its value was preserved for human review.' }
    $verb = if ($variable) { 'PATCH' } else { 'POST' }
    $path = if ($variable) { "repos/$Repository/actions/variables/AZURE_CD_ENABLED" } else { "repos/$Repository/actions/variables" }
    $null = GitHubRequest $verb $path @{ name = 'AZURE_CD_ENABLED'; value = 'false' }
    Write-Host 'Protected release-tag environment configured; manual CD remains disabled pending readiness.'
}
finally {
    $headers = $null; $token = $null
}
