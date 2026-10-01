"""Exercise real PowerShell cloud readiness with offline HTTP responses only."""
from pathlib import Path
import shutil
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'infra/azure/Test-CloudHealth.ps1'
HARNESS = r'''
$ErrorActionPreference='Stop'
$global:scenario='SCENARIO'
$global:attempt=0; $global:requests=0
$global:testClock=[pscustomobject]@{Elapsed=[pscustomobject]@{TotalSeconds=0.0}}
function Start-Sleep { throw 'Offline regression must not retry with real sleeps.' }
function Invoke-WebRequest {
    param([string]$Uri,[int]$TimeoutSec,[int]$MaximumRedirection)
    if ($Uri -notmatch '^https://hs-(api|worker)-offline\.azurewebsites\.net/(actuator/health|livez|sign-in|runtime-config\.json)$') { throw 'Unexpected network target.' }
    if ($MaximumRedirection -ne 0 -or $TimeoutSec -lt 1 -or $TimeoutSec -gt 10) { throw 'HTTP bounds were not preserved.' }
    $global:requests++
    if ($Uri.EndsWith('/actuator/health')) { $global:attempt++ }
    $type='application/json'; $body='{"status":"UP"}'; $status=200
    if ($Uri.EndsWith('/livez')) { $body='{"status":"ok"}' }
    if ($Uri.EndsWith('/sign-in')) { $type='text/html; charset=UTF-8'; $body='<html><app-root></app-root><script src="main.js"></script></html>' }
    if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apiBaseUrl":"/api/v1"}' }
    switch ($global:scenario) {
        'boot-html' { if ($global:attempt -eq 1) { $type='text/html'; $body='<html>Azure is starting</html>' } }
        'config-html' {
            if ($Uri.EndsWith('/runtime-config.json') -and $global:attempt -eq 1) { $type='text/html'; $body='<html>Azure is starting</html>' }
        }
        'http-error' { if ($global:attempt -eq 1) { throw 'PRIVATE_HTTP_ERROR_MARKER' } }
        'always-html' { $type='text/html'; $body='<html>PRIVATE_BODY_MARKER</html>' }
        'wrong-config' { if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apiBaseUrl":"https://api.example.org/api/v1"}' } }
        'extra-config' { if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apiBaseUrl":"/api/v1","secret":"PRIVATE_BODY_MARKER"}' } }
        'wrong-key' { if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apibaseurl":"/api/v1"}' } }
        'array-base' { if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apiBaseUrl":["/api/v1"]}' } }
        'array-status' { if ($Uri.EndsWith('/actuator/health')) { $body='{"status":["UP"]}' } }
        'array-worker' { if ($Uri.EndsWith('/livez')) { $body='{"status":["ok"]}' } }
        'down' { if ($Uri.EndsWith('/actuator/health')) { $body='{"status":"DOWN"}' } }
        'bad-worker' { if ($Uri.EndsWith('/livez')) { $body='{"status":"error"}' } }
        'default-spa' { if ($Uri.EndsWith('/sign-in')) { $body='<html>Azure landing page</html>' } }
        'invalid-json' { if ($Uri.EndsWith('/runtime-config.json')) { $body='{"apiBaseUrl":' } }
        'wrong-media' { if ($Uri.EndsWith('/runtime-config.json')) { $type='text/plain' } }
        'array-json' { if ($Uri.EndsWith('/runtime-config.json')) { $body='[{"apiBaseUrl":"/api/v1"}]' } }
        'redirect' { if ($Uri.EndsWith('/runtime-config.json')) { $status=302 } }
        'actuator-media' { if ($Uri.EndsWith('/actuator/health')) { $type='application/vnd.spring-boot.actuator.v3+json' } }
        'deadline' { $global:testClock.Elapsed.TotalSeconds=2.0 }
        'late-config' { if ($Uri.EndsWith('/runtime-config.json')) { $global:testClock.Elapsed.TotalSeconds=2.0 } }
    }
    if ($global:scenario -eq 'bytes') { $body=[Text.Encoding]::UTF8.GetBytes($body) }
    return [pscustomobject]@{StatusCode=$status; Headers=@{'Content-Type'=$type}; Content=$body}
}
$caught=$null
# Replace only the clock construction, not readiness control flow or validators.
# Host load and PowerShell module autoload must not consume the virtual test budget.
$scriptSource=Get-Content -LiteralPath 'SCRIPT_PATH' -Raw -Encoding UTF8
$clockStatement='$clock = [Diagnostics.Stopwatch]::StartNew()'
if ([regex]::Matches($scriptSource,[regex]::Escape($clockStatement)).Count -ne 1) {
    throw 'The production monotonic clock contract changed; update the test explicitly.'
}
$subject=[scriptblock]::Create($scriptSource.Replace($clockStatement,'$clock = $global:testClock'))
try {
    & $subject -BackendUrl 'BACKEND_URL' -WorkerUrl 'https://hs-worker-offline.azurewebsites.net/' -MaxAttempts 2 -MaxWaitSeconds WAIT_SECONDS -RetryDelaySeconds 0
} catch { $caught=$_.Exception.Message }
if ('EXPECTED_ERROR' -eq '') {
    if ($caught) { throw "Unexpected script failure: $caught" }
} elseif (!$caught -or !$caught.Contains('EXPECTED_ERROR')) { throw "Missing expected safe failure: $caught" }
if ($global:attempt -ne EXPECTED_ATTEMPTS -or $global:requests -ne EXPECTED_REQUESTS) {
    throw "Unexpected HTTP lifecycle: $global:attempt / $global:requests"
}
Write-Host 'Offline readiness lifecycle verified.'
'''


@unittest.skipUnless(shutil.which('pwsh'), 'PowerShell 7 is required')
class CloudReadinessTest(unittest.TestCase):
    def run_case(self, scenario, attempts, requests, error='', backend='https://hs-api-offline.azurewebsites.net/', wait=30):
        source = HARNESS
        for key, value in {'SCENARIO': scenario, 'SCRIPT_PATH': str(SCRIPT).replace("'", "''"),
                           'BACKEND_URL': backend, 'EXPECTED_ERROR': error, 'WAIT_SECONDS': str(wait),
                           'EXPECTED_ATTEMPTS': str(attempts), 'EXPECTED_REQUESTS': str(requests)}.items():
            source = source.replace(key, value)
        result = subprocess.run(['pwsh', '-NoProfile', '-NonInteractive', '-Command', source],
                                capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertNotIn('PRIVATE_BODY_MARKER', result.stdout + result.stderr)
        self.assertNotIn('PRIVATE_HTTP_ERROR_MARKER', result.stdout + result.stderr)
        return result.stdout

    def test_ready_endpoints_pass_without_retry(self):
        self.run_case('ready', 1, 4)

    def test_initial_azure_html_retries_instead_of_accepting_http_200(self):
        self.run_case('boot-html', 2, 5)

    def test_runtime_config_is_in_the_retry_loop(self):
        self.run_case('config-html', 2, 8)

    def test_transient_http_failure_retries_without_logging_exception(self):
        self.run_case('http-error', 2, 5)

    def test_byte_content_is_decoded(self):
        self.run_case('bytes', 1, 4)

    def test_vendor_actuator_json_media_type_is_valid(self):
        self.run_case('actuator-media', 1, 4)

    def test_deadline_stops_further_http_requests(self):
        for repetition in range(3):
            with self.subTest(repetition=repetition):
                self.run_case('deadline', 1, 1, 'Cloud readiness did not pass', wait=1)

    def test_response_after_deadline_cannot_pass(self):
        for repetition in range(3):
            with self.subTest(repetition=repetition):
                self.run_case('late-config', 1, 4, 'Cloud readiness did not pass', wait=1)

    def test_persistent_invalid_responses_fail_closed_with_bounded_attempts(self):
        for scenario, requests in [('always-html', 2), ('wrong-config', 8), ('extra-config', 8),
                                   ('down', 2), ('bad-worker', 4), ('default-spa', 6),
                                   ('invalid-json', 8), ('wrong-media', 8), ('array-json', 8), ('redirect', 8),
                                   ('wrong-key', 8), ('array-base', 8), ('array-status', 2), ('array-worker', 4)]:
            with self.subTest(scenario=scenario):
                self.run_case(scenario, 2, requests, 'Cloud readiness did not pass')

    def test_unsafe_targets_fail_before_requests(self):
        for backend in ['http://hs-api-offline.azurewebsites.net', 'https://example.org',
                        'https://hs-api-offline.azurewebsites.net/nested',
                        'https://hs-api-offline.azurewebsites.net:8443',
                        'https://user@hs-api-offline.azurewebsites.net',
                        'https://hs-api-offline.azurewebsites.net?secret=test']:
            with self.subTest(backend=backend):
                self.run_case('ready', 0, 0, 'Expected App Service HTTPS root URL', backend)


if __name__ == '__main__':
    unittest.main()
