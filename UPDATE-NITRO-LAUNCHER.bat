@echo off
echo Updating Nitro Client launcher on Desktop...
taskkill /IM "Nitro Client.exe" /F >nul 2>&1
timeout /t 2 /nobreak >nul
copy /Y "%~dp0nitro-launcher\dist-exe\Nitro Client.exe" "%USERPROFILE%\Desktop\Nitro Client.exe"
if errorlevel 1 (
  echo FAILED - close Nitro Client and run this again.
  pause
  exit /b 1
)
for %%F in ("%USERPROFILE%\Desktop\Nitro Client.exe") do echo OK - Updated at %%~tF
echo Launch Nitro Client from Desktop and try a theme chip again.
pause
