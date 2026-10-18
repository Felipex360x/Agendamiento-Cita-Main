@echo off
rem Libera el puerto 8080 e inicia el servidor

echo Verificando estado del puerto 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Liberando puerto 8080 - finalizando proceso PID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

echo Puerto 8080 disponible. Iniciando Spring Boot...
call mvnw.cmd clean spring-boot:run
