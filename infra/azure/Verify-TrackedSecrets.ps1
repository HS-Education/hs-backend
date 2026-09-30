param([Parameter(Mandatory)][string] $RepositoryPath)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $RepositoryPath).Path
$files = & git -C $root -c "safe.directory=$($root.Replace('\', '/'))" ls-files --cached --others --exclude-standard
if ($LASTEXITCODE -ne 0) { throw 'Cannot list versionable files.' }
$patterns = @(
    'sk-or-v1-[a-fA-F0-9]{40,}',
    'gh[pousr]_[A-Za-z0-9]{30,}',
    'github_pat_[A-Za-z0-9_]{50,}',
    '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----',
    'AccountKey=[A-Za-z0-9+/]{40,}={0,2}'
)
$matches = @()
foreach ($file in $files) {
    if ($file -match '(^|/)\.env($|\.)' -and $file -notmatch '\.example$') { $matches += "$file (environment file)"; continue }
    $path = Join-Path $root $file
    if (!(Test-Path -LiteralPath $path -PathType Leaf)) { continue }
    if ([IO.Path]::GetExtension($path) -in @('.png', '.jpg', '.jpeg', '.pdf', '.zip', '.woff', '.woff2', '.ttf')) { continue }
    $content = [IO.File]::ReadAllText($path)
    foreach ($pattern in $patterns) {
        if ([regex]::IsMatch($content, $pattern)) { $matches += "$file (credential signature)"; break }
    }
}
if ($matches.Count) {
    # Report paths only; printing the matching content would leak the very credential being checked.
    $matches | ForEach-Object { Write-Host $_ }
    throw 'Potential credentials found in versionable files; resolve before committing.'
}
Write-Host "Credential signatures and environment-file check passed for $($files.Count) versionable files. This is a targeted check, not a complete secret scanner."
