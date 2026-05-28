param(
    [string]$ResourceRoot = (Join-Path $PSScriptRoot '..\src\main\resources'),
    [string]$PreviewRoot = (Join-Path $PSScriptRoot '..\reference')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$blockTextureRoot = Join-Path $ResourceRoot 'assets\prisonplanet\textures\block'
$itemTextureRoot = Join-Path $ResourceRoot 'assets\prisonplanet\textures\item'
@($blockTextureRoot, $itemTextureRoot, $PreviewRoot) |
    ForEach-Object { New-Item -ItemType Directory -Path $_ -Force | Out-Null }

function Color([string]$hex, [int]$alpha = 255) {
    return [System.Drawing.Color]::FromArgb(
        $alpha,
        [Convert]::ToInt32($hex.Substring(0, 2), 16),
        [Convert]::ToInt32($hex.Substring(2, 2), 16),
        [Convert]::ToInt32($hex.Substring(4, 2), 16))
}

function Pixel([System.Drawing.Bitmap]$bitmap, [int]$x, [int]$y, [System.Drawing.Color]$color) {
    if ($x -ge 0 -and $x -lt $bitmap.Width -and $y -ge 0 -and $y -lt $bitmap.Height) {
        $bitmap.SetPixel($x, $y, $color)
    }
}

$deep = Color '16383D' 224
$body = Color '245A60' 220
$current = Color '317A80' 220
$edge = Color '62BEC0' 235
$glow = Color '9DE5D5' 245
$spark = Color 'C5F38C' 245

function New-FluidSheet([string]$path, [int]$frames, [bool]$flowing) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16 * $frames)
    for ($frame = 0; $frame -lt $frames; $frame++) {
        for ($x = 0; $x -lt 16; $x++) {
            for ($y = 0; $y -lt 16; $y++) {
                $wave = ($x * 5 + $y * 3 + $frame * 4) % 13
                $baseColor = if ($wave -lt 3) { $current } elseif ($wave -eq 12) { $deep } else { $body }
                Pixel $bitmap $x ($frame * 16 + $y) $baseColor
            }
        }
        for ($x = 0; $x -lt 16; $x++) {
            if ((($x + $frame * 2) % 5) -lt 2) {
                Pixel $bitmap $x ($frame * 16 + 14) $edge
                Pixel $bitmap $x ($frame * 16 + 15) $glow
            }
        }
        $streakCount = if ($flowing) { 5 } else { 3 }
        for ($streak = 0; $streak -lt $streakCount; $streak++) {
            $x = ($streak * 5 + $frame * 3 + 2) % 15
            $start = 12 - (($frame + $streak * 2) % 9)
            $length = if ($flowing) { 5 } else { 3 }
            for ($dy = 0; $dy -lt $length; $dy++) {
                Pixel $bitmap $x ($frame * 16 + $start - $dy) $edge
            }
            Pixel $bitmap $x ($frame * 16 + $start - $length) $glow
        }
        Pixel $bitmap ((3 + $frame * 4) % 16) ($frame * 16 + 3) $spark
    }
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

New-FluidSheet (Join-Path $blockTextureRoot 'suspended_brine_still.png') 4 $false
New-FluidSheet (Join-Path $blockTextureRoot 'suspended_brine_flow.png') 8 $true

$mcmeta = "{`n  `"animation`": {`n    `"frametime`": 3,`n    `"interpolate`": true`n  }`n}`n"
[System.IO.File]::WriteAllText(
    (Join-Path $blockTextureRoot 'suspended_brine_still.png.mcmeta'), $mcmeta,
    [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText(
    (Join-Path $blockTextureRoot 'suspended_brine_flow.png.mcmeta'), $mcmeta,
    [System.Text.UTF8Encoding]::new($false))

$bucket = [System.Drawing.Bitmap]::new(16, 16)
for ($x = 4; $x -le 11; $x++) {
    Pixel $bucket $x 4 (Color 'B3B8B5')
    Pixel $bucket $x 12 (Color '545B5D')
}
for ($y = 5; $y -le 11; $y++) {
    Pixel $bucket 3 $y (Color '737B7B')
    Pixel $bucket 12 $y (Color '3A4144')
    for ($x = 4; $x -le 11; $x++) {
        $fillColor = if ($y -le 7) { $glow } else { $body }
        Pixel $bucket $x $y $fillColor
    }
}
for ($x = 5; $x -le 10; $x++) {
    Pixel $bucket $x 5 $edge
}
for ($x = 5; $x -le 10; $x++) {
    Pixel $bucket $x 2 (Color '949B99')
}
Pixel $bucket 4 3 (Color '949B99')
Pixel $bucket 11 3 (Color '949B99')
Pixel $bucket 6 9 $spark
$bucket.Save((Join-Path $itemTextureRoot 'suspended_brine_bucket.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bucket.Dispose()

$preview = [System.Drawing.Bitmap]::new(288, 112)
$graphics = [System.Drawing.Graphics]::FromImage($preview)
$graphics.Clear((Color '10161A'))
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$still = [System.Drawing.Image]::FromFile((Join-Path $blockTextureRoot 'suspended_brine_still.png'))
$flow = [System.Drawing.Image]::FromFile((Join-Path $blockTextureRoot 'suspended_brine_flow.png'))
$icon = [System.Drawing.Image]::FromFile((Join-Path $itemTextureRoot 'suspended_brine_bucket.png'))
$graphics.DrawImage($still, [System.Drawing.Rectangle]::new(14, 14, 80, 80), 0, 0, 16, 16, [System.Drawing.GraphicsUnit]::Pixel)
$graphics.DrawImage($flow, [System.Drawing.Rectangle]::new(104, 14, 80, 80), 0, 0, 16, 16, [System.Drawing.GraphicsUnit]::Pixel)
$graphics.DrawImage($icon, [System.Drawing.Rectangle]::new(194, 14, 80, 80), 0, 0, 16, 16, [System.Drawing.GraphicsUnit]::Pixel)
$still.Dispose()
$flow.Dispose()
$icon.Dispose()
$graphics.Dispose()
$preview.Save((Join-Path $PreviewRoot 'suspended_brine_textures_0.5.0.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
