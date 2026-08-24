param(
    [Parameter(Mandatory = $true)]
    [string]$BundleDir
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BundleDir)) {
    throw "Bundle directory not found: $BundleDir"
}

$dest = (Resolve-Path $BundleDir).Path
$configPath = Join-Path $dest ".nitro-launch.json"
if (-not (Test-Path $configPath)) {
    throw "Missing .nitro-launch.json in $dest"
}

$json = Get-Content $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
$libsDir = Join-Path $dest "libs"
$allJars = @(Get-ChildItem $libsDir -Filter *.jar -ErrorAction SilentlyContinue)
if ($allJars.Count -eq 0) {
    throw "No JAR files found in $libsDir"
}

$ordered = [System.Collections.Generic.List[string]]::new()
$seen = @{}

function Add-JarPath([string]$jarPath) {
    if (-not $jarPath) { return }
    $resolved = (Resolve-Path $jarPath -ErrorAction SilentlyContinue)?.Path
    if (-not $resolved) { return }
    if ($seen.ContainsKey($resolved)) { return }
    $ordered.Add($resolved) | Out-Null
    $seen[$resolved] = $true
}

$orderFile = Join-Path $dest "classpath-order.txt"
if (Test-Path $orderFile) {
    foreach ($name in (Get-Content $orderFile -Encoding UTF8)) {
        $name = $name.Trim()
        if (-not $name) { continue }
        Add-JarPath (Join-Path $libsDir $name)
    }
} else {
    foreach ($entry in ($json.classpath -split ';')) {
        if (-not $entry) { continue }
        Add-JarPath (Join-Path $libsDir ([IO.Path]::GetFileName($entry)))
    }
}

$clientJar = Join-Path $libsDir "nitro-client.jar"
if (Test-Path $clientJar) {
    if ($ordered.Contains((Resolve-Path $clientJar).Path)) {
        $ordered.Remove((Resolve-Path $clientJar).Path) | Out-Null
    }
    $ordered.Insert(0, (Resolve-Path $clientJar).Path)
}

foreach ($jar in $allJars) {
    Add-JarPath $jar.FullName
}

$natives = Join-Path $dest "natives"
$assets = Join-Path $dest "assets"
$remap = Join-Path $dest "remapClasspath.txt"
$log4j = Join-Path $dest "log4j2.xml"
$launchCfg = Join-Path $dest "launch.cfg"

$json.classpath = ($ordered -join ";")
$json.bundleDir = $dest.Replace("\", "/")
$json.workingDir = $dest.Replace("\", "/")
$json.jvmArgs = @($json.jvmArgs | ForEach-Object {
    if ($_ -match "^-Djava\.library\.path=") { "-Djava.library.path=$($natives.Replace('\','/'))" }
    elseif ($_ -match "^-Dorg\.lwjgl\.librarypath=") { "-Dorg.lwjgl.librarypath=$($natives.Replace('\','/'))" }
    elseif ($_ -match "^-Dloader\.remapClasspathFile=") { "-Dloader.remapClasspathFile=$($remap.Replace('\','/'))" }
    elseif ($_ -match "^-Dlog4j\.configurationFile=") { "-Dlog4j.configurationFile=$($log4j.Replace('\','/'))" }
    elseif ($_ -match "^-Dfabric\.dli\.config=" -and (Test-Path $launchCfg)) {
        "-Dfabric.dli.config=$($launchCfg.Replace('\','/'))"
    }
    else { $_ }
})

if (-not ($json.jvmArgs -contains '-Dmixin.service=io.github.solclient.wrapper.WrapperMixinService')) {
    $json.jvmArgs += '-Dmixin.service=io.github.solclient.wrapper.WrapperMixinService'
}

for ($i = 0; $i -lt $json.args.Count; $i++) {
    if ($json.args[$i] -eq "--assetsDir" -and ($i + 1) -lt $json.args.Count) {
        $json.args[$i + 1] = $assets.Replace("\", "/")
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($configPath, ($json | ConvertTo-Json -Depth 12), $utf8NoBom)
Write-Host "Rewrote bundled launch config -> $dest ($($ordered.Count) jars)"
