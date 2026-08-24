# Generates cosmetic textures (64x64) matched to the procedural model UV layouts.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\nitro\textures\cosmetics'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function New-Bitmap { New-Object System.Drawing.Bitmap 64, 64 }

function Save-Bitmap($bmp, $name) {
    $path = Join-Path $outDir "$name.png"
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Generated $path"
}

function Lerp-Color($c1, $c2, $t) {
    $r = [int]($c1[0] + ($c2[0] - $c1[0]) * $t)
    $g = [int]($c1[1] + ($c2[1] - $c1[1]) * $t)
    $b = [int]($c1[2] + ($c2[2] - $c1[2]) * $t)
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

# Vertical gradient with per-column feather separation lines and noise.
function Draw-FeatherSheet($base, $tip, $edge, $seed) {
    $bmp = New-Bitmap
    $rng = New-Object System.Random $seed
    for ($y = 0; $y -lt 64; $y++) {
        $t = $y / 63.0
        for ($x = 0; $x -lt 64; $x++) {
            $c = Lerp-Color $base $tip $t
            # feather shaft lines every 8 px
            if (($x % 8) -eq 0 -or ($x % 8) -eq 7) {
                $c = Lerp-Color @($c.R, $c.G, $c.B) $edge 0.45
            }
            # subtle barb noise
            $n = ($rng.Next(-8, 9)) / 255.0
            $r = [Math]::Max(0, [Math]::Min(255, $c.R + $n * 40))
            $g = [Math]::Max(0, [Math]::Min(255, $c.G + $n * 40))
            $b = [Math]::Max(0, [Math]::Min(255, $c.B + $n * 40))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
        }
    }
    return $bmp
}

# Membrane: veiny gradient, top rows darker (bone region uv y>=48 painted bone color).
function Draw-MembraneSheet($base, $tip, $vein, $bone, $seed) {
    $bmp = New-Bitmap
    $rng = New-Object System.Random $seed
    for ($y = 0; $y -lt 64; $y++) {
        $t = $y / 63.0
        for ($x = 0; $x -lt 64; $x++) {
            if ($y -ge 48) {
                # bone region
                $shade = ($rng.Next(-10, 11)) / 255.0
                $r = [Math]::Max(0, [Math]::Min(255, $bone[0] + $shade * 30))
                $g = [Math]::Max(0, [Math]::Min(255, $bone[1] + $shade * 30))
                $b = [Math]::Max(0, [Math]::Min(255, $bone[2] + $shade * 30))
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
                continue
            }
            $c = Lerp-Color $base $tip $t
            # vein lines every 10 px, wavy
            $wave = [int](3 * [Math]::Sin($y * 0.35 + $x * 0.1))
            if ((($x + $wave) % 10) -eq 0) {
                $c = Lerp-Color @($c.R, $c.G, $c.B) $vein 0.5
            }
            $n = ($rng.Next(-6, 7)) / 255.0
            $r = [Math]::Max(0, [Math]::Min(255, $c.R + $n * 30))
            $g = [Math]::Max(0, [Math]::Min(255, $c.G + $n * 30))
            $b = [Math]::Max(0, [Math]::Min(255, $c.B + $n * 30))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
        }
    }
    return $bmp
}

# Energy: bright core with glow falloff horizontal bands.
function Draw-EnergySheet($core, $glow, $seed) {
    $bmp = New-Bitmap
    $rng = New-Object System.Random $seed
    for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            $band = [Math]::Abs((($x % 10) - 4.5)) / 4.5
            $c = Lerp-Color $core $glow $band
            $flicker = ($rng.Next(-12, 13)) / 255.0
            $r = [Math]::Max(0, [Math]::Min(255, $c.R + $flicker * 50))
            $g = [Math]::Max(0, [Math]::Min(255, $c.G + $flicker * 50))
            $b = [Math]::Max(0, [Math]::Min(255, $c.B + $flicker * 50))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
        }
    }
    return $bmp
}

# Solid material with vertical sheen + noise (metal / velvet).
function Draw-MaterialSheet($base, $sheen, $strength, $seed) {
    $bmp = New-Bitmap
    $rng = New-Object System.Random $seed
    for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            $s = [Math]::Sin(($x + $y * 0.5) * 0.25) * 0.5 + 0.5
            $c = Lerp-Color $base $sheen ($s * $strength)
            $n = ($rng.Next(-7, 8)) / 255.0
            $r = [Math]::Max(0, [Math]::Min(255, $c.R + $n * 25))
            $g = [Math]::Max(0, [Math]::Min(255, $c.G + $n * 25))
            $b = [Math]::Max(0, [Math]::Min(255, $c.B + $n * 25))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
        }
    }
    return $bmp
}

# Region-aware sheet: paints different UV regions with their own material so
# each model part (band / spikes / brim / tips) gets a distinct look.
# Regions: array of @{ X1; Y1; X2; Y2; Base; Sheen; Strength }  (first match wins)
function Draw-RegionSheet($regions, $fallbackBase, $fallbackSheen, $fallbackStrength, $seed) {
    $bmp = New-Bitmap
    $rng = New-Object System.Random $seed
    for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            $base = $fallbackBase; $sheen = $fallbackSheen; $strength = $fallbackStrength
            foreach ($reg in $regions) {
                if ($x -ge $reg.X1 -and $x -lt $reg.X2 -and $y -ge $reg.Y1 -and $y -lt $reg.Y2) {
                    $base = $reg.Base; $sheen = $reg.Sheen; $strength = $reg.Strength
                    break
                }
            }
            $s = [Math]::Sin(($x + $y * 0.5) * 0.25) * 0.5 + 0.5
            $c = Lerp-Color $base $sheen ($s * $strength)
            $n = ($rng.Next(-7, 8)) / 255.0
            $r = [Math]::Max(0, [Math]::Min(255, $c.R + $n * 25))
            $g = [Math]::Max(0, [Math]::Min(255, $c.G + $n * 25))
            $b = [Math]::Max(0, [Math]::Min(255, $c.B + $n * 25))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int]$r, [int]$g, [int]$b))
        }
    }
    return $bmp
}

# celestial: white base -> soft gold tips
Save-Bitmap (Draw-FeatherSheet @(250, 248, 240) @(226, 188, 116) @(180, 150, 90) 11) 'celestial_wings'
# obsidian: slate -> near black tips
Save-Bitmap (Draw-FeatherSheet @(92, 98, 112) @(18, 20, 26) @(140, 150, 170) 22) 'obsidian_wings'
# infernal: crimson membrane, black bone
Save-Bitmap (Draw-MembraneSheet @(150, 26, 36) @(60, 8, 14) @(220, 60, 60) @(28, 22, 24) 33) 'infernal_wings'
# verdant: green membrane, dark bone
Save-Bitmap (Draw-MembraneSheet @(58, 150, 74) @(16, 66, 34) @(120, 220, 130) @(30, 40, 30) 44) 'verdant_wings'
# storm: cyan core -> violet glow
Save-Bitmap (Draw-EnergySheet @(210, 250, 255) @(96, 80, 230) 55) 'storm_wings'

# crown: gold band (u<38) + brighter gem spikes with white sparkle (u>=38)
Save-Bitmap (Draw-RegionSheet @(
    @{ X1 = 38; Y1 = 0; X2 = 64; Y2 = 32; Base = @(120, 220, 235); Sheen = @(255, 255, 255); Strength = 0.85 }
) @(212, 164, 52) @(255, 236, 160) 0.7 66) 'aurora_crown'

# horns: charcoal base (seg1 u0-10), ember mid (seg2 u10-20), glowing tips (seg3 u20-30)
Save-Bitmap (Draw-RegionSheet @(
    @{ X1 = 20; Y1 = 0; X2 = 32; Y2 = 16; Base = @(255, 120, 40); Sheen = @(255, 220, 120); Strength = 0.8 },
    @{ X1 = 10; Y1 = 0; X2 = 20; Y2 = 16; Base = @(140, 50, 28); Sheen = @(230, 110, 50); Strength = 0.6 }
) @(46, 24, 22) @(120, 55, 35) 0.4 77) 'ember_horns'

# halo: radiant warm white
Save-Bitmap (Draw-MaterialSheet @(255, 238, 180) @(255, 255, 235) 0.8 88) 'radiant_halo'

# velvet hat: violet brim+crown, gold satin band (band uv starts at u>=29, v 13-24)
Save-Bitmap (Draw-RegionSheet @(
    @{ X1 = 29; Y1 = 13; X2 = 64; Y2 = 24; Base = @(212, 164, 52); Sheen = @(255, 236, 160); Strength = 0.75 }
) @(34, 28, 52) @(88, 74, 130) 0.45 99) 'velvet_hat'

Write-Host 'All cosmetic textures generated.'
