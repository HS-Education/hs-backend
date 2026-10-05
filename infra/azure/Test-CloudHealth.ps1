param(
    [Parameter(Mandatory)][string] $BackendUrl,
    [Parameter(Mandatory)][string] $WorkerUrl,
    [ValidateRange(1, 60)][int] $MaxAttempts = 60,
    [ValidateRange(1, 600)][int] $MaxWaitSeconds = 600,
    [ValidateRange(0, 10)][int] $RetryDelaySeconds = 10
)
$ErrorActionPreference = 'Stop'
foreach ($url in @($BackendUrl, $WorkerUrl)) {
    $uri = [uri] $url
    if (!$uri.IsAbsoluteUri -or $uri.Scheme -ne 'https' -or !$uri.Host.EndsWith('.azurewebsites.net') -or
        !$uri.IsDefaultPort -or $uri.AbsolutePath -ne '/' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
        throw 'Expected App Service HTTPS root URL.'
    }
}
$BackendUrl = $BackendUrl.TrimEnd('/')
$WorkerUrl = $WorkerUrl.TrimEnd('/')

function Test-CloudResponse($Response, [string] $Kind) {
    if ($Response.StatusCode -ne 200) { return $false }
    $mediaType = ([string] $Response.Headers['Content-Type']).Split(';')[0].Trim()
    $content = if ($Response.Content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($Response.Content) } else { [string] $Response.Content }
    if ($Kind -eq 'SPA') {
        return $mediaType -eq 'text/html' -and $content -match '<app-root(?:\s|>)' -and $content -match '<script(?:\s|>)'
    }
    if ($mediaType -notmatch '^application/(?:json|[a-z0-9.-]+\+json)$') { return $false }
    try { $value = ConvertFrom-Json -InputObject $content -AsHashtable -NoEnumerate -ErrorAction Stop } catch { return $false }
    if ($value -isnot [System.Collections.IDictionary]) { return $false }
    switch ($Kind) {
        'Backend' { return $value['status'] -is [string] -and $value['status'] -ceq 'UP' }
        'Worker' { return $value['status'] -is [string] -and $value['status'] -ceq 'ok' }
        'Config' { return $value.Count -eq 1 -and @($value.Keys)[0] -ceq 'apiBaseUrl' -and $value['apiBaseUrl'] -is [string] -and $value['apiBaseUrl'] -ceq '/api/v1' }
        default { return $false }
    }
}

$checks = @(
    @{ Uri = "$BackendUrl/actuator/health"; Kind = 'Backend' },
    @{ Uri = "$WorkerUrl/livez"; Kind = 'Worker' },
    @{ Uri = "$BackendUrl/sign-in"; Kind = 'SPA' },
    @{ Uri = "$BackendUrl/runtime-config.json"; Kind = 'Config' }
)
$clock = [Diagnostics.Stopwatch]::StartNew()
$lastPending = 'Backend'
for ($attempt = 1; $attempt -le $MaxAttempts -and $clock.Elapsed.TotalSeconds -lt $MaxWaitSeconds; $attempt++) {
    $ready = $true
    foreach ($check in $checks) {
        $lastPending = $check.Kind
        $remaining = $MaxWaitSeconds - $clock.Elapsed.TotalSeconds
        if ($remaining -le 0) { $ready = $false; break }
        $lastStatus = 'unavailable'
        $lastMediaType = 'unavailable'
        try {
            $response = Invoke-WebRequest -Uri $check.Uri -TimeoutSec ([int][Math]::Ceiling([Math]::Min(10, $remaining))) -MaximumRedirection 0
            $lastStatus = [string][int]$response.StatusCode
            # Only report a bounded, sanitized media type, never arbitrary response headers.
            $type = (([string]$response.Headers['Content-Type']).Split(';')[0]).Trim().ToLowerInvariant()
            if ($type -match '^[a-z0-9.+-]+/[a-z0-9.+-]+$' -and $type.Length -le 80) { $lastMediaType = $type }
            $valid = Test-CloudResponse -Response $response -Kind $check.Kind
        } catch {
            $valid = $false
            if ($_.Exception.Response) { $lastStatus = [string][int]$_.Exception.Response.StatusCode }
        }
        if (!$valid) { $ready = $false; break }
    }
    if ($ready -and $clock.Elapsed.TotalSeconds -lt $MaxWaitSeconds) {
        Write-Host 'HTTP health, SPA navigation and public runtime configuration passed. Login, streaming and document ingestion require the cloud browser smoke test.'
        return
    }
    # Never log response bodies or exception messages: a proxy may return sensitive data.
    Write-Host "Waiting for valid cloud readiness: $lastPending (attempt $attempt/$MaxAttempts; HTTP $lastStatus; media $lastMediaType)."
    $remaining = $MaxWaitSeconds - $clock.Elapsed.TotalSeconds
    if ($attempt -lt $MaxAttempts -and $remaining -gt 0 -and $RetryDelaySeconds -gt 0) {
        Start-Sleep -Milliseconds ([int][Math]::Min($RetryDelaySeconds * 1000, $remaining * 1000))
    }
}
throw "Cloud readiness did not pass within the retry/time budget ($lastPending). HTTP 200 alone is not sufficient."
