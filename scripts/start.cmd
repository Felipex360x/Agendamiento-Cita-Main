@echo off
rem Inicia solo el backend (Spring Boot). La logica esta en start.ps1:
rem si el puerto 8080 esta ocupado informa quien lo usa y termina, sin detener procesos ajenos.
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
exit /b %errorlevel%
