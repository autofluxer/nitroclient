$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$modProject = Join-Path $root 'nitro-1.21'
$launcherDir = Join-Path $root 'nitro-launcher'
$outDir = Join-Path $launcherDir 'game-121\mods'

Push-Location $modProject
try {
  & .\gradlew.bat build --no-daemon
  if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
} finally {
  Pop-Location
}

$built = Get-ChildItem (Join-Path $modProject 'build\libs') -Filter 'nitro-client-121-*.jar' |
  Where-Object { $_.Name -notlike '*-sources.jar' } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

if (-not $built) { throw 'Mod jar not found after build' }

New-Item -ItemType Directory -Force -Path $outDir | Out-Null
Copy-Item -Force $built.FullName (Join-Path $outDir 'nitro-client-121.jar')
Write-Host "Copied $($built.Name) -> $outDir\nitro-client-121.jar"
