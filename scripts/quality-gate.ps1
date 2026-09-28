param([switch]$SkipSmoke)

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$frontRoot = (Resolve-Path (Join-Path $backendRoot '..\hs-tesis-front')).Path
$python = Join-Path $backendRoot 'ai-service\.venv\Scripts\python.exe'
$composeFile = Join-Path $PSScriptRoot 'compose.quality-gate.yaml'
$mavenRepository = Join-Path $env:USERPROFILE '.m2\repository'

function Assert-Success([string]$stage) {
    if ($LASTEXITCODE -ne 0) { throw "$stage failed with exit code $LASTEXITCODE" }
}

function Wait-Http([string]$url, [System.Diagnostics.Process]$process) {
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($process.HasExited) { throw "Process exited before $url became ready" }
        try {
            $response = Invoke-WebRequest -Uri $url -TimeoutSec 2 -UseBasicParsing
            if ($response.StatusCode -lt 500) { return }
        } catch {
            if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401) { return }
        }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for $url"
}

function Assert-PortFree([int]$port) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
    try { $listener.Start() } catch {
        $socketError = $_.Exception.InnerException
        if ($socketError -is [System.Net.Sockets.SocketException]) {
            if ($socketError.SocketErrorCode -eq [System.Net.Sockets.SocketError]::AddressAlreadyInUse) {
                throw "Port $port is already in use; stop that service before the isolated smoke test."
            }
            if ($socketError.SocketErrorCode -eq [System.Net.Sockets.SocketError]::AccessDenied) {
                throw "Windows denied binding to TCP port $port. No listener was confirmed; check local network/security restrictions or excluded port ranges."
            }
            throw "Could not check TCP port ${port}: $($socketError.SocketErrorCode) - $($socketError.Message)"
        }
        throw "Could not check TCP port ${port}: $($_.Exception.Message)"
    }
    finally { $listener.Stop() }
}

if (-not (Test-Path -LiteralPath $python)) {
    throw 'Create ai-service/.venv and install ai-service/requirements.txt before running the gate.'
}

Push-Location $backendRoot
try {
    & mvn "-Dmaven.repo.local=$mavenRepository" '-Plocal-gate' verify
    Assert-Success 'Java unit and PostgreSQL integration tests'
} finally { Pop-Location }

Push-Location (Join-Path $backendRoot 'ai-service')
try {
    & $python -m unittest discover -s tests -p 'test_*.py' -v
    Assert-Success 'OpenRouter offline contract tests'
} finally { Pop-Location }

Push-Location $frontRoot
try {
    & (Join-Path $frontRoot 'node_modules\.bin\vitest.cmd') run --configLoader runner
    Assert-Success 'Angular unit tests'
} finally { Pop-Location }

if ($SkipSmoke) { return }

foreach ($port in @(4200, 8080, 15432, 15673, 19000)) { Assert-PortFree $port }
$project = 'hsquality' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempDir = Join-Path $tempBase ('hs-quality-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null

$backendProcess = $null
$frontProcess = $null
$smokeSucceeded = $false
$env:GATE_DB_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:GATE_RABBIT_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:GATE_MINIO_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:SMOKE_USERNAME = 'gate_student'
$env:SMOKE_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $frontRoot '.playwright-browsers'
$env:POSTGRES_DB_URL = 'jdbc:postgresql://localhost:15432/hs_quality'
$env:POSTGRES_USER = 'hs_quality'
$env:POSTGRES_PASSWORD = $env:GATE_DB_PASSWORD
$env:MINIO_URL = 'http://localhost:19000'
$env:MINIO_ROOT_USER = 'hs_quality'
$env:MINIO_ROOT_PASSWORD = $env:GATE_MINIO_PASSWORD
$env:MINIO_BUCKET_NAME = 'hs-quality'
$env:RABBITMQ_HOST = 'localhost'
$env:RABBITMQ_AMQP_PORT = '15673'
$env:RABBITMQ_USER = 'hs_quality'
$env:RABBITMQ_PASSWORD = $env:GATE_RABBIT_PASSWORD
$env:PYTHON_WORKER_BASE_URL = 'http://localhost:18000'
$env:WORKER_API_KEY = [Guid]::NewGuid().ToString('N')
$env:ADMIN_USERNAME = 'gate_admin'
$env:ADMIN_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:JWT_SECRET = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')

$dotenvLines = @(
    "JWT_SECRET=$env:JWT_SECRET"
    'JWT_EXPIRATION_MINUTES=60'
    'JWT_REFRESH_EXPIRATION_DAYS=7'
    "STUDENT_USERNAME=$env:SMOKE_USERNAME"
    "STUDENT_PASSWORD=$env:SMOKE_PASSWORD"
    "ADMIN_USERNAME=$env:ADMIN_USERNAME"
    "ADMIN_PASSWORD=$env:ADMIN_PASSWORD"
)
[IO.File]::WriteAllLines(
    (Join-Path $tempDir '.env'),
    [string[]]$dotenvLines,
    [System.Text.UTF8Encoding]::new($false))

try {
    & docker compose -p $project -f $composeFile up -d --wait --wait-timeout 120
    Assert-Success 'Isolated dependencies startup'
    $minioReady = $false
    $minioLastError = 'No HTTP response received'
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri 'http://127.0.0.1:19000/minio/health/ready' -TimeoutSec 2 -UseBasicParsing
            if ($response.StatusCode -eq 200) { $minioReady = $true; break }
        } catch {
            $minioLastError = $_.Exception.Message
            Start-Sleep -Seconds 1
        }
    }
    if (-not $minioReady) {
        & docker compose -p $project -f $composeFile logs --no-color minio
        throw "MinIO did not become ready at 127.0.0.1:19000. Last probe error: $minioLastError"
    }

    $jar = Join-Path $backendRoot 'target\hs-tesis-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing backend jar: $jar" }
    $backendProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar', ('"' + $jar + '"'), '--server.port=8080') -WorkingDirectory $tempDir -RedirectStandardOutput (Join-Path $tempDir 'backend.out.log') -RedirectStandardError (Join-Path $tempDir 'backend.err.log') -WindowStyle Hidden -PassThru
    Wait-Http 'http://localhost:8080/api/v1/auth/me' $backendProcess

    $ng = Join-Path $frontRoot 'node_modules\@angular\cli\bin\ng.js'
    $frontProcess = Start-Process -FilePath (Get-Command node).Source -ArgumentList @(('"' + $ng + '"'), 'serve', '--host', '127.0.0.1', '--port', '4200') -WorkingDirectory $frontRoot -RedirectStandardOutput (Join-Path $tempDir 'frontend.out.log') -RedirectStandardError (Join-Path $tempDir 'frontend.err.log') -WindowStyle Hidden -PassThru
    Wait-Http 'http://127.0.0.1:4200/sign-in' $frontProcess

    Push-Location $frontRoot
    try {
        & (Join-Path $frontRoot 'node_modules\.bin\playwright.cmd') install chromium
        Assert-Success 'Chromium installation'
        & (Join-Path $frontRoot 'node_modules\.bin\playwright.cmd') test
        Assert-Success 'Browser login smoke test'
    } finally { Pop-Location }
    $smokeSucceeded = $true
} finally {
    foreach ($process in @($frontProcess, $backendProcess)) {
        if ($process) {
            try {
                $process.Refresh()
                if (-not $process.HasExited) {
                    $taskkill = Join-Path $env:SystemRoot 'System32\taskkill.exe'
                    & $taskkill /PID $process.Id /T /F | Out-Null
                    if ($LASTEXITCODE -ne 0) {
                        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
                    }
                }
                if (-not $process.WaitForExit(15000)) {
                    Write-Warning "Smoke-test process $($process.Id) is still running after shutdown was requested."
                }
            } catch {
                Write-Warning "Could not confirm shutdown of smoke-test process $($process.Id): $($_.Exception.Message)"
            } finally {
                $process.Dispose()
            }
        }
    }
    & docker compose -p $project -f $composeFile down -v --remove-orphans | Out-Null
    $resolvedTemp = [IO.Path]::GetFullPath($tempDir)
    if ($resolvedTemp.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
        [IO.Path]::GetFileName($resolvedTemp).StartsWith('hs-quality-')) {
        if ($smokeSucceeded) {
            $tempRemoved = $false
            for ($attempt = 0; $attempt -lt 5; $attempt++) {
                try {
                    Remove-Item -LiteralPath $resolvedTemp -Recurse -Force -ErrorAction Stop
                    $tempRemoved = $true
                    break
                } catch {
                    if ($attempt -lt 4) { Start-Sleep -Seconds 1 }
                    else { Write-Warning "Smoke tests passed, but temporary logs could not be removed from ${resolvedTemp}: $($_.Exception.Message)" }
                }
            }
        } else {
            Write-Warning "Smoke test failed; diagnostic logs are preserved at $resolvedTemp"
            foreach ($logName in @('backend.out.log', 'backend.err.log', 'frontend.out.log', 'frontend.err.log')) {
                $logPath = Join-Path $resolvedTemp $logName
                if (Test-Path -LiteralPath $logPath) {
                    Write-Host "--- $logName (last 100 lines) ---"
                    Get-Content -LiteralPath $logPath -Tail 100
                }
            }
        }
    }
}
