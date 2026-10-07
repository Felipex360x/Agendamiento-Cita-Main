@echo off
rem ==============================================================================
rem Script de Inicio Seguro (CMD): Agendamiento de Citas Backend
rem Libera automáticamente el puerto 8080 y arranca el servidor
rem ==============================================================================

echo Verificando estado del puerto 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Liberando puerto 8080 (finalizando proceso PID %%a)...
    taskkill /F /PID %%a >nul 2>&1
)

echo Puerto 8080 disponible. Iniciando Spring Boot...
call mvnw.cmd spring-boot:run
