@echo off
cd /d "%~dp0"
echo Rebuilding Nitro Client - this takes a few minutes...
powershell -ExecutionPolicy Bypass -File "%~dp0install-nitro-client.ps1"
echo.
echo Done. Use the "Nitro Client" shortcut or exe on your Desktop.
pause
