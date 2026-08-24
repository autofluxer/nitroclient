# Generates two animated cape styles (8 frames each, 64x32 per frame).
# 1) nitro_cape_*     — black + light-blue fabric, blue/black fire
# 2) jovanstar_cape_* — void black, white glow eyes mark, drifting fog
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\nitro\textures\cape'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$frames = 8
$fw = 64
$fh = 32

function Set-Px($bmp, $x, $y, $r, $g, $b, $a = 255) {
    if ($x -lt 0 -or $y -lt 0 -or $x -ge $bmp.Width -or $y -ge $bmp.Height) { return }
    $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, [int]$r, [int]$g, [int]$b))
}

$N = @(
    '1...1',
    '11..1',
    '1.1.1',
    '1..11',
    '1...1',
    '1...1',
    '1...1'
)

function Save-StyleFrames($prefix, $drawFrame) {
    $strip = New-Object System.Drawing.Bitmap $fw, ($fh * $frames)
    for ($f = 0; $f -lt $frames; $f++) {
        & $drawFrame $strip ($f * $fh) $f
    }
    $stripPath = Join-Path $outDir "$prefix.png"
    $strip.Save($stripPath, [System.Drawing.Imaging.ImageFormat]::Png)

    for ($f = 0; $f -lt $frames; $f++) {
        $frameBmp = New-Object System.Drawing.Bitmap $fw, $fh
        $g = [System.Drawing.Graphics]::FromImage($frameBmp)
        $g.DrawImage($strip, (New-Object System.Drawing.Rectangle 0, 0, $fw, $fh), 0, ($f * $fh), $fw, $fh, [System.Drawing.GraphicsUnit]::Pixel)
        $g.Dispose()
        $frameBmp.Save((Join-Path $outDir ("${prefix}_$f.png")), [System.Drawing.Imaging.ImageFormat]::Png)
        $frameBmp.Dispose()
    }
    $strip.Dispose()

    $mcmeta = @"
{
  "animation": {
    "frametime": 2,
    "interpolate": true,
    "frames": [0, 1, 2, 3, 4, 5, 6, 7]
  }
}
"@
    Set-Content -Path (Join-Path $outDir "$prefix.png.mcmeta") -Value $mcmeta -Encoding UTF8
    Write-Host "Generated $prefix ($frames frames)"
}

# ===================== NITRO: black + light blue + blue/black fire =====================
Save-StyleFrames 'nitro_cape' {
    param($bmp, $oy, $frame)

    for ($y = 0; $y -lt 32; $y++) {
        for ($x = 0; $x -lt 64; $x++) { Set-Px $bmp $x ($oy + $y) 0 0 0 0 }
    }

    $panels = @(@{ U = 1; V = 1 }, @{ U = 12; V = 1 })
    foreach ($p in $panels) {
        $u0 = $p.U; $v0 = $p.V
        for ($ly = 0; $ly -lt 16; $ly++) {
            for ($lx = 0; $lx -lt 10; $lx++) {
                $gx = $u0 + $lx
                $gy = $oy + $v0 + $ly

                # Black base with subtle blue weave
                $weave = (($lx * 3 + $ly * 2) % 4)
                $r = 4 + $weave
                $g = 8 + $weave
                $b = 18 + $weave * 2

                # Light-blue rim
                $edge = [Math]::Min($lx, 9 - $lx)
                if ($edge -eq 0) { $r = 90; $g = 200; $b = 255 }
                elseif ($edge -eq 1) { $r = 30; $g = 90; $b = 160 }

                # Blue / black fire along bottom
                if ($ly -ge 10) {
                    $heat = ($ly - 9) / 6.0
                    $flicker = [Math]::Sin(($lx * 1.9) + ($frame * 1.1) + ($ly * 0.7)) * 0.5 + 0.5
                    $flicker2 = [Math]::Sin(($lx * 2.8) - ($frame * 1.4) + 1.4) * 0.5 + 0.5
                    $intensity = [Math]::Min(1.0, $heat * (0.5 + 0.5 * $flicker) * (0.65 + 0.35 * $flicker2))

                    # Core: electric light-blue -> mid cyan -> deep black-blue tips
                    $fr = [int]((40 + 140 * (1.0 - $heat)) * $intensity)
                    $fg = [int]((120 + 100 * (1.0 - $heat * 0.6)) * $intensity)
                    $fb = [int]((220 + 35 * (1.0 - $heat)) * $intensity)

                    # Dark flame pockets
                    if ($flicker2 -lt 0.35) {
                        $fr = [int]($fr * 0.15)
                        $fg = [int]($fg * 0.2)
                        $fb = [int]($fb * 0.35)
                    }

                    if ($intensity -gt 0.12) {
                        $r = [Math]::Min(255, [int]($r * (1 - $intensity * 0.85) + $fr))
                        $g = [Math]::Min(255, [int]($g * (1 - $intensity * 0.7) + $fg))
                        $b = [Math]::Min(255, [int]($b * (1 - $intensity * 0.4) + $fb))
                    }
                    if (([Math]::Sin($lx * 5.1 + $frame * 2.2 + $ly) * 0.5 + 0.5) -gt 0.92 -and $intensity -gt 0.4) {
                        $r = 200; $g = 245; $b = 255
                    }
                }

                Set-Px $bmp $gx $gy $r $g $b 255
            }
        }

        # Light-blue N
        $nx = $u0 + 2
        $ny = $oy + $v0 + 3
        for ($row = 0; $row -lt 7; $row++) {
            $line = $N[$row]
            for ($col = 0; $col -lt 5; $col++) {
                if ($line[$col] -eq '1') {
                    Set-Px $bmp ($nx + $col) ($ny + $row) 140 220 255 255
                    foreach ($ox in @(-1, 0, 1)) {
                        foreach ($oy2 in @(-1, 0, 1)) {
                            if ($ox -eq 0 -and $oy2 -eq 0) { continue }
                            $hx = $nx + $col + $ox
                            $hy = $ny + $row + $oy2
                            if ($hx -ge $u0 -and $hx -lt ($u0 + 10) -and $hy -ge ($oy + $v0) -and $hy -lt ($oy + $v0 + 16)) {
                                $ex = $bmp.GetPixel($hx, $hy)
                                if ($ex.B -lt 210) { Set-Px $bmp $hx $hy 20 70 140 255 }
                            }
                        }
                    }
                }
            }
        }

        for ($lx = 0; $lx -lt 10; $lx++) {
            Set-Px $bmp ($u0 + $lx) ($oy + $v0) 100 210 255 255
        }
    }

    for ($x = 1; $x -le 10; $x++) {
        Set-Px $bmp $x ($oy + 0) 20 60 120 255
        Set-Px $bmp ($x + 10) ($oy + 0) 8 16 32 255
    }
}

# ===================== JOVANSTAR: void black + white glow / fog =====================
Save-StyleFrames 'jovanstar_cape' {
    param($bmp, $oy, $frame)

    for ($y = 0; $y -lt 32; $y++) {
        for ($x = 0; $x -lt 64; $x++) { Set-Px $bmp $x ($oy + $y) 0 0 0 0 }
    }

    $panels = @(@{ U = 1; V = 1 }, @{ U = 12; V = 1 })
    foreach ($p in $panels) {
        $u0 = $p.U; $v0 = $p.V
        for ($ly = 0; $ly -lt 16; $ly++) {
            for ($lx = 0; $lx -lt 10; $lx++) {
                $gx = $u0 + $lx
                $gy = $oy + $v0 + $ly

                # Charcoal void with subtle noise
                $noise = (($lx * 17 + $ly * 31 + $frame * 3) % 7)
                $r = 8 + $noise
                $g = 8 + $noise
                $b = 10 + $noise

                # Thin white rim light (like the poster backlight)
                $edge = [Math]::Min([Math]::Min($lx, 9 - $lx), [Math]::Min($ly, 15 - $ly))
                if ($edge -eq 0) {
                    $pulse = [Math]::Sin($frame * 0.5 + $lx * 0.4) * 0.5 + 0.5
                    $r = [int](170 + 70 * $pulse)
                    $g = [int](170 + 70 * $pulse)
                    $b = [int](180 + 70 * $pulse)
                } elseif ($edge -eq 1) {
                    $r = 40; $g = 40; $b = 48
                }

                # Drifting white fog / smoke wisps
                $fog = [Math]::Sin(($lx * 0.9) + ($ly * 0.45) - ($frame * 0.7)) * 0.5 + 0.5
                $fog2 = [Math]::Sin(($lx * 1.4) - ($ly * 0.8) + ($frame * 0.55) + 2.0) * 0.5 + 0.5
                $fogAmt = $fog * $fog2
                if ($fogAmt -gt 0.62 -and $ly -ge 2 -and $ly -le 13) {
                    $a = ($fogAmt - 0.62) / 0.38
                    $r = [Math]::Min(255, [int]($r + 140 * $a))
                    $g = [Math]::Min(255, [int]($g + 140 * $a))
                    $b = [Math]::Min(255, [int]($b + 150 * $a))
                }

                Set-Px $bmp $gx $gy $r $g $b 255
            }
        }

        # Glowing white "eyes" mark — two horizontal bars (JV signature)
        $eyeY = $oy + $v0 + 5
        $eyePulse = [Math]::Sin($frame * 0.6) * 0.5 + 0.5
        $eyeBright = [int](210 + 45 * $eyePulse)
        # left eye
        for ($ex = 2; $ex -le 3; $ex++) {
            for ($ey = 0; $ey -le 1; $ey++) {
                Set-Px $bmp ($u0 + $ex) ($eyeY + $ey) $eyeBright $eyeBright 255 255
            }
        }
        # right eye
        for ($ex = 6; $ex -le 7; $ex++) {
            for ($ey = 0; $ey -le 1; $ey++) {
                Set-Px $bmp ($u0 + $ex) ($eyeY + $ey) $eyeBright $eyeBright 255 255
            }
        }
        # soft glow under eyes
        for ($ex = 2; $ex -le 7; $ex++) {
            Set-Px $bmp ($u0 + $ex) ($eyeY + 2) 60 60 70 255
        }

        # Minimal JV mark at bottom
        Set-Px $bmp ($u0 + 3) ($oy + $v0 + 13) 180 180 190 255
        Set-Px $bmp ($u0 + 4) ($oy + $v0 + 13) 120 120 130 255
        Set-Px $bmp ($u0 + 5) ($oy + $v0 + 13) 180 180 190 255
        Set-Px $bmp ($u0 + 6) ($oy + $v0 + 13) 120 120 130 255
    }

    for ($x = 1; $x -le 10; $x++) {
        Set-Px $bmp $x ($oy + 0) 30 30 36 255
        Set-Px $bmp ($x + 10) ($oy + 0) 12 12 14 255
    }
}

Write-Host 'All cape styles generated.'
