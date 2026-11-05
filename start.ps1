# Inicia el backend (Spring Boot) en el puerto 8080.
# Nunca detiene procesos ajenos: si el puerto esta ocupado, informa quien lo usa y termina.

$port = 8080
Write-Host "Verificando estado del puerto $port..." -ForegroundColor Cyan

$conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($conns) {
    $pids = $conns.OwningProcess | Select-Object -Unique | Where-Object { $_ -gt 0 }
    foreach ($p in $pids) {
        $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { 'desconocido' }
        Write-Host "El puerto $port ya esta en uso por el proceso '$name' (PID $p)." -ForegroundColor Yellow
    }
    Write-Host 'No se detuvo ningun proceso. Cierra ese programa manualmente (o, si es este mismo backend, ya esta en ejecucion) y vuelve a intentarlo.' -ForegroundColor Red
    exit 1
}

Write-Host "Puerto $port disponible. Iniciando Spring Boot..." -ForegroundColor Green
& ".\mvnw.cmd" spring-boot:run
