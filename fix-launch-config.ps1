# Fix Play: rebuild game + launcher, update install
$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$launcherDir = Join-Path $repoRoot "nitro-launcher"
$installDir = Join-Path $env:LOCALAPPDATA "Nitro Client"
$gameDir = Join-Path $installDir "game"

Write-Host "Building game bundle..."
& powershell.exe -ExecutionPolicy Bypass -File (Join-Path $launcherDir "scripts\prepare-player-bundle.ps1")

Write-Host "Copying game to install..."
if (Test-Path $gameDir) { Remove-Item $gameDir -Recurse -Force }
Copy-Item -Path (Join-Path $launcherDir "game") -Destination $gameDir -Recurse -Force
& powershell.exe -ExecutionPolicy Bypass -File (Join-Path $launcherDir "scripts\rewrite-bundled-launch.ps1") -BundleDir $gameDir

Write-Host "Rebuilding launcher..."
Get-Process -Name "Nitro Client" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
Push-Location $launcherDir
try {
    npm run dist:dir
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

$sourceDir = Join-Path $launcherDir "dist-exe\win-unpacked"
Write-Host "Updating installed launcher..."
Copy-Item -Path (Join-Path $sourceDir '*') -Destination $installDir -Recurse -Force

Write-Host ""
Write-Host "Done. Play opens the main menu only."
Write-Host "Use Join Nitro SMP in-game to connect."
