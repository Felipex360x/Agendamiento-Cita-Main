$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendRoot = Join-Path $projectRoot 'frontend'
$launched = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$stopRequested = $false

function Stop-LaunchedProcessTree {
    param([System.Diagnostics.Process]$Process)
    if ($null -eq $Process -or $Process.HasExited) { return }
    & taskkill.exe /PID $Process.Id /T /F 2>$null | Out-Null
}

function Wait-Http {
    param([string]$Name, [string]$Url, [int]$TimeoutSeconds = 180, [System.Diagnostics.Process]$Process = $null, [string]$LogPath = $null)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($Process -and $Process.HasExited) {
            $tail = if ($LogPath -and (Test-Path $LogPath)) { (Get-Content $LogPath -Tail 15) -join [Environment]::NewLine } else { '' }
            throw "$Name termino antes de estar listo (codigo $($Process.ExitCode)).$([Environment]::NewLine)$tail"
        }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) { Write-Host "$Name listo ($Url)." -ForegroundColor Green; return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "$Name no respondio en $TimeoutSeconds s ($Url)."
}

try {
    # Defaults for the local MySQL service defined in docker-compose.yml.
    if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) { $env:DB_USERNAME = 'desarrollador' }
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { $env:DB_PASSWORD = 'password_seguro_123' }
    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
        $jwtBytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($jwtBytes) } finally { $rng.Dispose() }
        $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
    }

    if (-not (Test-Path (Join-Path $frontendRoot 'node_modules\vite\bin\vite.js'))) {
        throw 'Faltan dependencias de React. Ejecuta npm install dentro de frontend.'
    }

    $cloudflared = Get-Command cloudflared.exe -ErrorAction SilentlyContinue
    $cloudflaredPath = if ($cloudflared) { $cloudflared.Source } else { Join-Path $projectRoot 'cloudflared.exe' }
    if (-not (Test-Path $cloudflaredPath)) {
        throw 'No se encontro cloudflared.exe. Descargalo desde https://developers.cloudflare.com/tunnel/downloads/ o dejalo junto a este archivo.'
    }

    $dbReady = Test-NetConnection 127.0.0.1 -Port 3306 -InformationLevel Quiet
    if (-not $dbReady) {
        if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) {
            throw 'No hay una base de datos en el puerto 3306 y Docker no esta disponible.'
        }
        Write-Host 'Iniciando MySQL con Docker Compose...'
        Push-Location $projectRoot
        try { & docker.exe compose up -d mysql_usuario; if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar MySQL. Revisa Docker Desktop.' } }
        finally { Pop-Location }

        $dbReady = $false
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            if (Test-NetConnection 127.0.0.1 -Port 3306 -InformationLevel Quiet) {
                $dbReady = $true
                break
            }
            Start-Sleep -Seconds 1
        }
        if (-not $dbReady) { throw 'MySQL no quedo disponible en el puerto 3306. Revisa el estado del contenedor.' }
    }

    $cancelHandler = [ConsoleCancelEventHandler]{ param($sender, $eventArgs) $eventArgs.Cancel = $true; $script:stopRequested = $true }
    [Console]::add_CancelKeyPress($cancelHandler)

    Write-Host 'Iniciando Spring Boot y React...'
    $logs = Join-Path $projectRoot 'logs'
    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    $backendLog = Join-Path $logs 'backend.log'
    $frontendLog = Join-Path $logs 'frontend.log'
    # /c (no /k): el cmd termina cuando termina el proceso real, asi se detecta que Spring o Vite cayeron.
    $backend = Start-Process -FilePath $env:ComSpec -ArgumentList @('/c', "call mvnw.cmd spring-boot:run > `"$backendLog`" 2>&1") -WorkingDirectory $projectRoot -PassThru -WindowStyle Minimized
    $launched.Add($backend)
    $frontend = Start-Process -FilePath $env:ComSpec -ArgumentList @('/c', "call npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort > `"$frontendLog`" 2>&1") -WorkingDirectory $frontendRoot -PassThru -WindowStyle Minimized
    $launched.Add($frontend)

    # Salud real (con timeout) en lugar de una pausa fija: Spring directo, y Spring + frontend por la ruta de Vite.
    Wait-Http -Name 'Spring Boot' -Url 'http://127.0.0.1:8080/actuator/health' -TimeoutSeconds 180 -Process $backend -LogPath $backendLog
    Wait-Http -Name 'Frontend (Vite)' -Url 'http://127.0.0.1:5173/' -TimeoutSeconds 60 -Process $frontend -LogPath $frontendLog
    Wait-Http -Name 'Salud de Spring via Vite' -Url 'http://127.0.0.1:5173/actuator/health' -TimeoutSeconds 30
    if ($env:CLOUDFLARE_TUNNEL_TOKEN) {
        if (-not $env:TUNNEL_HOSTNAME) { throw 'Configuraste CLOUDFLARE_TUNNEL_TOKEN, pero falta TUNNEL_HOSTNAME.' }
        Write-Host "Iniciando tunel para https://$env:TUNNEL_HOSTNAME ..."
        $tunnel = Start-Process -FilePath $cloudflaredPath -ArgumentList @('tunnel', 'run', '--token', $env:CLOUDFLARE_TUNNEL_TOKEN) -PassThru
    } else {
        Write-Host 'Se abrira otra ventana de cloudflared. Comparte la URL https://....trycloudflare.com que aparezca ahi.'
        Write-Host 'La URL temporal deja de funcionar al cerrar el tunel.'
        $tunnel = Start-Process -FilePath $cloudflaredPath -ArgumentList @('tunnel', '--url', 'http://127.0.0.1:5173') -PassThru
    }
    $launched.Add($tunnel)

    Write-Host 'Deja esta ventana abierta mientras tu amigo usa la aplicacion.'
    Write-Host 'Presiona Ctrl+C aqui para cerrar Spring, React y el tunel.'
    $anyProcessExited = $false
    while (-not $stopRequested -and -not $anyProcessExited) {
        Start-Sleep -Milliseconds 500
        foreach ($process in $launched) {
            if ($process.HasExited) { $anyProcessExited = $true; break }
        }
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    if ($cancelHandler) { [Console]::remove_CancelKeyPress($cancelHandler) }
    foreach ($process in $launched) { Stop-LaunchedProcessTree -Process $process }
    if ($launched.Count -gt 0) { Write-Host 'Se cerraron los procesos iniciados por este archivo.' }
}

if ($Host.Name -eq 'ConsoleHost') {
    Write-Host 'Presiona una tecla para cerrar esta ventana.'
    [void][Console]::ReadKey($true)
}
