# ==============================================================================
# Script de Inicio Seguro: Agendamiento de Citas Backend
# 1. Libera automáticamente el puerto 8080 si quedó bloqueado por un proceso anterior
# 2. Inicia Spring Boot sin conflictos de puerto
# ==============================================================================

Write-Host "Verificando estado del puerto 8080..." -ForegroundColor Cyan

$conns = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conns) {
    $pids = $conns.OwningProcess | Select-Object -Unique | Where-Object { $_ -gt 0 }
    foreach ($p in $pids) {
        Write-Host "Liberando puerto 8080 (finalizando proceso anterior PID $p)..." -ForegroundColor Yellow
        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Milliseconds 800
}

Write-Host "Puerto 8080 disponible. Iniciando Spring Boot..." -ForegroundColor Green
& ".\mvnw.cmd" spring-boot:run
