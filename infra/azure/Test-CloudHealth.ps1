param([Parameter(Mandatory)][string] $BackendUrl, [Parameter(Mandatory)][string] $WorkerUrl)
$ErrorActionPreference = 'Stop'
foreach ($url in @($BackendUrl, $WorkerUrl)) {
    $uri = [uri] $url
    if ($uri.Scheme -ne 'https' -or !$uri.Host.EndsWith('.azurewebsites.net') -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
        throw 'Expected App Service HTTPS URL.'
    }
}
foreach ($url in @("$BackendUrl/actuator/health", "$WorkerUrl/livez", "$BackendUrl/sign-in")) {
    $success = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $url -TimeoutSec 15
            if ($response.StatusCode -eq 200) { $success = $true; break }
        } catch { Write-Host 'Waiting for the deployed application health check.' }
        Start-Sleep -Seconds 5
    }
    if (!$success) { throw 'Cloud health or SPA deep-link check did not pass.' }
}
$config = Invoke-RestMethod -Uri "$BackendUrl/runtime-config.json"
if ($config.apiBaseUrl -ne '/api/v1') { throw 'Cloud API configuration is not same-origin.' }
Write-Host 'HTTP health, SPA navigation and public runtime configuration passed. Login, streaming and document ingestion require the cloud browser smoke test.'
