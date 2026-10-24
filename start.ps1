# Libera el puerto 8080 e inicia el servidor

Write-Host "Verificando estado del puerto 8080..." -ForegroundColor Cyan

$conns = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conns) {
    $pids = $conns.OwningProcess | Select-Object -Unique | Where-Object { $_ -gt 0 }
    foreach ($p in $pids) {
        Write-Host "Liberando puerto 8080 (deteniendo proceso previo en puerto  $p)..." -ForegroundColor Yellow
        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Milliseconds 800
}

Write-Host "Puerto 8080 disponible. Iniciando Spring Boot..." -ForegroundColor Green
& ".\mvnw.cmd" clean spring-boot:run
