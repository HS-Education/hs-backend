param([switch]$SkipSmoke, [switch]$RunLoad, [switch]$RunCapacity, [switch]$RunMalware, [switch]$RunDast, [switch]$DockerBackend, [switch]$SkipDependencyAudit)

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$frontRoot = (Resolve-Path (Join-Path $backendRoot '..\hs-tesis-front')).Path
$python = Join-Path $backendRoot 'ai-service\.venv\Scripts\python.exe'
$composeFile = Join-Path $PSScriptRoot 'compose.quality-gate.yaml'
$mavenRepository = Join-Path $env:USERPROFILE '.m2\repository'
if ($SkipSmoke -and $RunLoad) { throw '-RunLoad requires the isolated smoke environment.' }
if ($SkipSmoke -and $RunCapacity) { throw '-RunCapacity requires the isolated smoke environment.' }
if ($SkipSmoke -and $RunMalware) { throw '-RunMalware requires the isolated smoke environment.' }
if ($SkipSmoke -and $RunDast) { throw '-RunDast requires the isolated smoke environment.' }

function Assert-Success([string]$stage) {
    if ($LASTEXITCODE -ne 0) { throw "$stage failed with exit code $LASTEXITCODE" }
}

function Wait-Http([string]$url, [System.Diagnostics.Process]$process, [string]$containerName) {
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($process -and $process.HasExited) { throw "Process exited before $url became ready" }
        if ($containerName) {
            $running = & docker inspect --format '{{.State.Running}}' $containerName 2>$null
            if ($LASTEXITCODE -ne 0 -or $running -ne 'true') {
                throw "Container $containerName exited before $url became ready"
            }
        }
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
    & node (Join-Path $frontRoot 'scripts\check-i18n.cjs')
    Assert-Success 'Translation key check'
    & (Join-Path $frontRoot 'node_modules\.bin\ng.cmd') build
    Assert-Success 'Angular production build'
} finally { Pop-Location }

if (-not $SkipDependencyAudit) {
    & docker run --rm --mount "type=bind,source=$backendRoot,target=/src,readonly" 'ghcr.io/google/osv-scanner@sha256:afd838850ac1a0fcc15ff4a041dc9ba11123c3f0d2666217a5f0fcf9222b55fa' scan source --lockfile=/src/pom.xml --format=table --verbosity=error
    Assert-Success 'Java dependency OSV audit'

    & $python -m pip_audit -r (Join-Path $backendRoot 'ai-service\requirements.txt')
    Assert-Success 'Python dependency audit'

    Push-Location $frontRoot
    try {
        & pnpm audit --prod --audit-level high
        Assert-Success 'Frontend production dependency audit'
    } finally { Pop-Location }
}

if ($SkipSmoke) { return }

$requiredPorts = @(4200, 8080, 15432, 15673, 19000)
if ($RunMalware) { $requiredPorts += 13310 }
foreach ($port in $requiredPorts) { Assert-PortFree $port }
$project = 'hsquality' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempDir = Join-Path $tempBase ('hs-quality-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null

$backendProcess = $null
$backendContainer = $null
$frontProcess = $null
$smokeSucceeded = $false
$env:GATE_DB_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:GATE_RABBIT_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:GATE_MINIO_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:SMOKE_USERNAME = 'gate_student'
$env:SMOKE_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:TEACHER_USERNAME = 'gate_teacher'
$env:TEACHER_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:COORDINATOR_USERNAME = 'gate_coordinator'
$env:COORDINATOR_PASSWORD = [Guid]::NewGuid().ToString('N')
$env:COORDINATOR2_USERNAME = 'gate_coordinator2'
$env:COORDINATOR2_PASSWORD = [Guid]::NewGuid().ToString('N')
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
if ($RunMalware) {
    $env:UPLOADS_MALWARE_SCAN_ENABLED = 'true'
    $env:UPLOADS_MALWARE_SCAN_HOST = '127.0.0.1'
    $env:UPLOADS_MALWARE_SCAN_PORT = '13310'
    $env:RUN_MALWARE_E2E = '1'
}

$dotenvLines = @(
    "JWT_SECRET=$env:JWT_SECRET"
    'JWT_EXPIRATION_MINUTES=60'
    'JWT_REFRESH_EXPIRATION_DAYS=7'
    "STUDENT_USERNAME=$env:SMOKE_USERNAME"
    "STUDENT_PASSWORD=$env:SMOKE_PASSWORD"
    "TEACHER_USERNAME=$env:TEACHER_USERNAME"
    "TEACHER_PASSWORD=$env:TEACHER_PASSWORD"
    "COORDINATOR_USERNAME=$env:COORDINATOR_USERNAME"
    "COORDINATOR_PASSWORD=$env:COORDINATOR_PASSWORD"
    "COORDINATOR2_USERNAME=$env:COORDINATOR2_USERNAME"
    "COORDINATOR2_PASSWORD=$env:COORDINATOR2_PASSWORD"
    "ADMIN_USERNAME=$env:ADMIN_USERNAME"
    "ADMIN_PASSWORD=$env:ADMIN_PASSWORD"
)
[IO.File]::WriteAllLines(
    (Join-Path $tempDir '.env'),
    [string[]]$dotenvLines,
    [System.Text.UTF8Encoding]::new($false))

try {
    $composeArgs = @('compose', '-p', $project, '-f', $composeFile)
    if ($RunMalware) { $composeArgs += @('--profile', 'malware') }
    & docker @composeArgs up -d --wait --wait-timeout 240
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

    if ($RunMalware) {
        Push-Location $backendRoot
        try {
            & mvn "-Dmaven.repo.local=$mavenRepository" '-Dtest=ClamAvMalwareScannerIT' test
            Assert-Success 'ClamAV clean, EICAR and fail-closed integration tests'
        } finally { Pop-Location }
    }

    $backendPom = [xml](Get-Content -LiteralPath (Join-Path $backendRoot 'pom.xml') -Raw)
    $jar = Join-Path $backendRoot ('target\{0}-{1}.jar' -f $backendPom.project.artifactId, $backendPom.project.version)
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing backend jar: $jar" }
    if ($DockerBackend) {
        $containerEnvFile = Join-Path $tempDir 'backend-container.env'
        $containerEnvLines = @(
            'POSTGRES_DB_URL=jdbc:postgresql://postgres:5432/hs_quality'
            'POSTGRES_USER=hs_quality'
            "POSTGRES_PASSWORD=$env:GATE_DB_PASSWORD"
            'MINIO_URL=http://minio:9000'
            'MINIO_ROOT_USER=hs_quality'
            "MINIO_ROOT_PASSWORD=$env:GATE_MINIO_PASSWORD"
            'MINIO_BUCKET_NAME=hs-quality'
            'RABBITMQ_HOST=rabbitmq'
            'RABBITMQ_AMQP_PORT=5672'
            'RABBITMQ_USER=hs_quality'
            "RABBITMQ_PASSWORD=$env:GATE_RABBIT_PASSWORD"
            'PYTHON_WORKER_BASE_URL=http://host.docker.internal:18000'
            "WORKER_API_KEY=$env:WORKER_API_KEY"
            "ADMIN_USERNAME=$env:ADMIN_USERNAME"
            "ADMIN_PASSWORD=$env:ADMIN_PASSWORD"
        )
        if ($RunMalware) {
            $containerEnvLines += @('UPLOADS_MALWARE_SCAN_ENABLED=true',
                'UPLOADS_MALWARE_SCAN_HOST=clamav', 'UPLOADS_MALWARE_SCAN_PORT=3310')
        }
        [IO.File]::WriteAllLines($containerEnvFile, [string[]]$containerEnvLines, [System.Text.UTF8Encoding]::new($false))
        $backendContainer = "${project}-backend"
        & docker run --detach --name $backendContainer --network "${project}_default" -p '127.0.0.1:8080:8080' -p '[::1]:8080:8080' --mount "type=bind,source=$jar,target=/app/app.jar,readonly" --mount "type=bind,source=$(Join-Path $tempDir '.env'),target=/app/.env,readonly" --env-file $containerEnvFile --workdir /app eclipse-temurin:21-jre java -jar /app/app.jar --server.port=8080 | Out-Null
        Assert-Success 'Isolated Java backend container startup'
    } else {
        $backendProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar', ('"' + $jar + '"'), '--server.port=8080') -WorkingDirectory $tempDir -RedirectStandardOutput (Join-Path $tempDir 'backend.out.log') -RedirectStandardError (Join-Path $tempDir 'backend.err.log') -WindowStyle Hidden -PassThru
    }
    Wait-Http 'http://localhost:8080/api/v1/auth/me' $backendProcess $backendContainer

    $ng = Join-Path $frontRoot 'node_modules\@angular\cli\bin\ng.js'
    $frontArgs = @(('"' + $ng + '"'), 'serve', '--host', '127.0.0.1', '--port', '4200')
    # Docker Desktop sends host.docker.internal; allow that header only for opt-in ZAP against this loopback-bound server.
    if ($RunDast) { $frontArgs += '--allowed-hosts' }
    $frontProcess = Start-Process -FilePath (Get-Command node).Source -ArgumentList $frontArgs -WorkingDirectory $frontRoot -RedirectStandardOutput (Join-Path $tempDir 'frontend.out.log') -RedirectStandardError (Join-Path $tempDir 'frontend.err.log') -WindowStyle Hidden -PassThru
    Wait-Http 'http://127.0.0.1:4200/sign-in' $frontProcess $null

    Push-Location $frontRoot
    try {
        $env:PLAYWRIGHT_JUNIT_OUTPUT_FILE = Join-Path $tempDir 'playwright-junit.xml'
        & (Join-Path $frontRoot 'node_modules\.bin\playwright.cmd') install chromium
        Assert-Success 'Chromium installation'
        & (Join-Path $frontRoot 'node_modules\.bin\playwright.cmd') test
        Assert-Success 'Browser critical journey suite'
    } finally { Pop-Location }
    if ($RunLoad) {
        & node (Join-Path $PSScriptRoot 'load-probe.mjs')
        Assert-Success 'Bounded local performance probe'
    }
    if ($RunCapacity) {
        & docker run --rm -e SMOKE_USERNAME -e SMOKE_PASSWORD -e ADMIN_USERNAME -e ADMIN_PASSWORD -e TEACHER_USERNAME -e TEACHER_PASSWORD -e COORDINATOR_USERNAME -e COORDINATOR_PASSWORD -e COORDINATOR2_USERNAME -e COORDINATOR2_PASSWORD --mount "type=bind,source=$PSScriptRoot,target=/scripts,readonly" grafana/k6:1.0.0 run /scripts/k6-capacity.js
        Assert-Success 'Isolated k6 capacity profile'
    }
    if ($RunDast) {
        # Passive baseline only. Do not run an active attack against an unknown target.
        $dastTarget = 'http://host.docker.internal:4200/sign-in'
        $dastStatus = & docker run --rm --entrypoint curl ghcr.io/zaproxy/zaproxy:stable --max-time 10 -sS -o /dev/null -w '%{http_code}' $dastTarget
        Assert-Success 'ZAP target reachability from Docker'
        if ($dastStatus -ne '200') { throw "ZAP target returned HTTP $dastStatus instead of 200" }
        & docker run --rm ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t $dastTarget -m 1 -I
        Assert-Success 'Isolated ZAP passive baseline'
    }
    $smokeSucceeded = $true
} finally {
    if ($backendContainer) {
        try {
            & docker logs $backendContainer 2>&1 | Out-File -LiteralPath (Join-Path $tempDir 'backend.out.log') -Encoding utf8
        } catch {
            Write-Warning "Could not capture logs for smoke-test container ${backendContainer}: $($_.Exception.Message)"
        }
        & docker rm --force $backendContainer | Out-Null
    }
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
    & docker @composeArgs down -v --remove-orphans | Out-Null
    $resolvedTemp = [IO.Path]::GetFullPath($tempDir)
    if ($resolvedTemp.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
        [IO.Path]::GetFileName($resolvedTemp).StartsWith('hs-quality-')) {
        foreach ($secretName in @('.env', 'backend-container.env')) {
            $secretPath = Join-Path $resolvedTemp $secretName
            Remove-Item -LiteralPath $secretPath -Force -ErrorAction SilentlyContinue
        }
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
