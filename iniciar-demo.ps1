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

try {
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
    }

    $cancelHandler = [ConsoleCancelEventHandler]{ param($sender, $eventArgs) $eventArgs.Cancel = $true; $script:stopRequested = $true }
    [Console]::add_CancelKeyPress($cancelHandler)

    Write-Host 'Iniciando Spring Boot, React y el tunel publico...'
    $backend = Start-Process -FilePath $env:ComSpec -ArgumentList @('/k', 'call mvnw.cmd spring-boot:run') -WorkingDirectory $projectRoot -PassThru
    $launched.Add($backend)
    $frontend = Start-Process -FilePath $env:ComSpec -ArgumentList @('/k', 'call npm.cmd run dev -- --host 127.0.0.1 --port 5173') -WorkingDirectory $frontendRoot -PassThru
    $launched.Add($frontend)

    Start-Sleep -Seconds 12
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
