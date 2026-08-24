# Rebuild Nitro 1.21 mod + portable launcher, deploy mod, update Desktop exe.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$javaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
if (-not (Test-Path $javaHome)) {
    $javaHome = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-21*' } | Select-Object -First 1).FullName
}
if (-not $javaHome) {
    Write-Host 'Java 21 not found. Install Eclipse Temurin 21 JDK first.'
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;" + $env:PATH

Write-Host 'Building Nitro 1.21 mod...'
Push-Location (Join-Path $root 'nitro-1.21')
& .\gradlew.bat build --no-daemon
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

$jar = Get-ChildItem (Join-Path $root 'nitro-1.21\build\libs') -Filter 'nitro-client-121-*.jar' |
    Where-Object { $_.Name -notlike '*-sources.jar' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) {
    Write-Host 'Mod jar not found after build.'
    exit 1
}

$modTargets = @(
    (Join-Path $root 'nitro-launcher\game-121\mods'),
    (Join-Path $root 'nitro-launcher-asar\game-121\mods'),
    (Join-Path $env:APPDATA 'nitroclient\nitroclient\nitro-1.21.11\mods')
)
foreach ($dir in $modTargets) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item -Force $jar.FullName (Join-Path $dir 'nitro-client-121.jar')
    Write-Host "Copied mod -> $dir"
}

$fabricCache = Join-Path $env:APPDATA 'nitroclient\nitroclient\nitro-1.21.11\.fabric'
if (Test-Path $fabricCache) {
    try {
        Remove-Item -Recurse -Force $fabricCache -ErrorAction Stop
        Write-Host 'Cleared Fabric cache'
    } catch {
        Write-Host 'Fabric cache in use (close Minecraft/Nitro Client first). Continuing rebuild...'
    }
}

Write-Host 'Building Nitro Client launcher exe...'
Push-Location (Join-Path $root 'nitro-launcher-asar')
& npm run dist
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

$portable = Join-Path $root 'nitro-launcher-asar\dist-exe\Nitro Client.exe'
if (-not (Test-Path $portable)) {
    Write-Host 'Portable launcher not found after build.'
    exit 1
}

$desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Nitro Client.exe'
$desktopFallback = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Nitro Client-updated.exe'
try {
    Copy-Item -Force $portable $desktop -ErrorAction Stop
    if (Test-Path $desktopFallback) { Remove-Item -Force $desktopFallback -ErrorAction SilentlyContinue }
    $desktopPath = $desktop
} catch {
    Copy-Item -Force $portable $desktopFallback
    Write-Host ''
    Write-Host 'Desktop Nitro Client.exe is in use. Close the launcher, then:'
    Write-Host "  1. Delete or rename: $desktop"
    Write-Host "  2. Rename Nitro Client-updated.exe -> Nitro Client.exe"
    $desktopPath = $desktopFallback
}

$desktopItem = Get-Item $desktopPath
$modItem = Get-Item (Join-Path $env:APPDATA 'nitroclient\nitroclient\nitro-1.21.11\mods\nitro-client-121.jar')

Write-Host ''
Write-Host '=== Nitro Client rebuilt ==='
Write-Host "Desktop exe : $($desktopItem.FullName)"
Write-Host "             $($desktopItem.Length) bytes, $($desktopItem.LastWriteTime)"
Write-Host "Mod jar     : $($modItem.Length) bytes, $($modItem.LastWriteTime)"
Write-Host ''
Write-Host 'Launch Nitro Client.exe from your Desktop.'
