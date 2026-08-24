# Builds the portable Nitro 1.8.9 player bundle (no Gradle on player PCs)
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$rewriteScript = Join-Path $PSScriptRoot "rewrite-bundled-launch.ps1"

$bundleSrc = Join-Path $repoRoot "build\player-bundle"
$bundleDest = Join-Path $PSScriptRoot "..\game"

Write-Host "Building player bundle from $repoRoot ..."
Push-Location $repoRoot
try {
    & .\gradlew.bat writePlayerBundle --no-daemon -q
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle writePlayerBundle failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $bundleSrc)) {
    throw "Player bundle was not created at $bundleSrc"
}

Write-Host "Copying bundle to $bundleDest ..."
if (Test-Path $bundleDest) {
    Remove-Item $bundleDest -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $bundleDest | Out-Null

$robocopy = Start-Process -FilePath "robocopy.exe" -ArgumentList @(
    $bundleSrc,
    $bundleDest,
    "/E", "/NFL", "/NDL", "/NJH", "/NJS", "/nc", "/ns", "/np"
) -Wait -PassThru -NoNewWindow

if ($robocopy.ExitCode -ge 8) {
    throw "Failed to copy player bundle (robocopy exit $($robocopy.ExitCode))"
}

& powershell.exe -ExecutionPolicy Bypass -File $rewriteScript -BundleDir $bundleDest
Write-Host "Player bundle ready."
