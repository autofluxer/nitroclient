@echo off
cd /d "%~dp0"
echo Fixing Nitro launch (2-3 minutes)...
powershell -ExecutionPolicy Bypass -File "%~dp0fix-launch-config.ps1"
echo.
pause
