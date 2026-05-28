param(
    [string]$ResourceRoot = (Join-Path $PSScriptRoot '..\src\main\resources'),
    [string]$PreviewRoot = (Join-Path $PSScriptRoot '..\reference')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$assetRoot = Join-Path $ResourceRoot 'assets\prisonplanet'
$textureRoot = Join-Path $assetRoot 'textures\entity'
$itemModelRoot = Join-Path $assetRoot 'models\item'
$lootRoot = Join-Path $ResourceRoot 'data\prisonplanet\loot_table\entities'
$langPath = Join-Path $assetRoot 'lang\en_us.json'

@($textureRoot, $itemModelRoot, $lootRoot, $PreviewRoot) |
    ForEach-Object { New-Item -ItemType Directory -Path $_ -Force | Out-Null }

$mobs = @(
    [ordered]@{ Id = 'ash_stalker'; Name = 'Ash Stalker'; Base = '302929'; Accent = '5C4943'; Glow = 'D34E31'; Drop = 'fine_ash'; Pattern = 'claws' },
    [ordered]@{ Id = 'chain_wretch'; Name = 'Chain Wretch'; Base = '26292B'; Accent = '776961'; Glow = 'A9AAA6'; Drop = 'ferric_scrap'; Pattern = 'chain' },
    [ordered]@{ Id = 'frost_wraith'; Name = 'Frost Wraith'; Base = '82959D'; Accent = 'C8DBDF'; Glow = '61CFE4'; Drop = 'glacial_shard'; Pattern = 'frost' },
    [ordered]@{ Id = 'vent_crawler'; Name = 'Vent Crawler'; Base = '242328'; Accent = '504448'; Glow = 'C04723'; Drop = 'powdered_char'; Pattern = 'vents' },
    [ordered]@{ Id = 'cage_phantom'; Name = 'Cage Phantom'; Base = '21242A'; Accent = '7C8386'; Glow = 'B9362C'; Drop = 'ferric_scrap'; Pattern = 'bars' },
    [ordered]@{ Id = 'cinder_hound'; Name = 'Cinder Hound'; Base = '30221D'; Accent = '643329'; Glow = 'F46C28'; Drop = 'ember_crystal'; Pattern = 'ember' },
    [ordered]@{ Id = 'rust_sentinel'; Name = 'Rust Sentinel'; Base = '37383A'; Accent = '804A38'; Glow = 'E03227'; Drop = 'ferric_scrap'; Pattern = 'visor' },
    [ordered]@{ Id = 'slag_brute'; Name = 'Slag Brute'; Base = '282326'; Accent = '55433A'; Glow = 'CD6630'; Drop = 'slag_gravel'; Pattern = 'slag' },
    [ordered]@{ Id = 'furnace_warden'; Name = 'Furnace Warden'; Base = '17191C'; Accent = '4E3530'; Glow = 'FF8731'; Drop = 'ember_crystal'; Pattern = 'furnace' },
    [ordered]@{ Id = 'ash_grazer'; Name = 'Ash Grazer'; Base = '595452'; Accent = '93887E'; Glow = 'D0B08B'; Drop = 'fine_ash'; Pattern = 'filter' },
    [ordered]@{ Id = 'glacial_drifter'; Name = 'Glacial Drifter'; Base = 'AECBD0'; Accent = 'D9E8E9'; Glow = '4FBCD4'; Drop = 'glacial_shard'; Pattern = 'wings' },
    [ordered]@{ Id = 'salvage_porter'; Name = 'Salvage Porter'; Base = '49352C'; Accent = '8E5336'; Glow = 'D0924A'; Drop = 'ferric_scrap'; Pattern = 'plates' }
)

function Write-Text([string]$Path, [string]$Text) {
    $directory = Split-Path $Path -Parent
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    [System.IO.File]::WriteAllText($Path, $Text + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
}

function Write-Json([string]$Path, [object]$Data) {
    Write-Text $Path ($Data | ConvertTo-Json -Depth 20)
}

function Hex([string]$Value, [int]$Alpha = 255) {
    return [System.Drawing.Color]::FromArgb(
        $Alpha,
        [Convert]::ToInt32($Value.Substring(0, 2), 16),
        [Convert]::ToInt32($Value.Substring(2, 2), 16),
        [Convert]::ToInt32($Value.Substring(4, 2), 16))
}

function Shade([System.Drawing.Color]$Color, [int]$Delta) {
    return [System.Drawing.Color]::FromArgb(
        $Color.A,
        [Math]::Max(0, [Math]::Min(255, $Color.R + $Delta)),
        [Math]::Max(0, [Math]::Min(255, $Color.G + $Delta)),
        [Math]::Max(0, [Math]::Min(255, $Color.B + $Delta)))
}

function Stable-Hash([string]$Text) {
    $value = 23
    foreach ($character in $Text.ToCharArray()) {
        $value = (($value * 37) + [int]$character) % 2147483629
    }
    return [Math]::Abs($value)
}

function PixelLine([System.Drawing.Graphics]$Graphics, [System.Drawing.Color]$Color, [int]$X1, [int]$Y1, [int]$X2, [int]$Y2, [int]$Width = 2) {
    $pen = [System.Drawing.Pen]::new($Color, $Width)
    $Graphics.DrawLine($pen, $X1, $Y1, $X2, $Y2)
    $pen.Dispose()
}

function Fill([System.Drawing.Graphics]$Graphics, [System.Drawing.Color]$Color, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $brush = [System.Drawing.SolidBrush]::new($Color)
    $Graphics.FillRectangle($brush, $X, $Y, $W, $H)
    $brush.Dispose()
}

function New-MobTexture([object]$Mob) {
    $bitmap = [System.Drawing.Bitmap]::new(64, 64)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $base = Hex $Mob.Base
    $accent = Hex $Mob.Accent
    $glow = Hex $Mob.Glow
    $seed = Stable-Hash $Mob.Id

    for ($y = 0; $y -lt 64; $y += 4) {
        for ($x = 0; $x -lt 64; $x += 4) {
            $shift = ((($seed + $x * 13 + $y * 19) % 5) - 2) * 6
            Fill $graphics (Shade $base $shift) $x $y 4 4
        }
    }

    # Broad plates give cube faces readable tonal separation at game scale.
    Fill $graphics (Shade $accent -8) 0 16 32 15
    Fill $graphics $accent 32 16 32 15
    Fill $graphics (Shade $base 16) 0 32 64 16
    Fill $graphics (Shade $accent -16) 0 48 64 16

    switch ($Mob.Pattern) {
        'claws' {
            PixelLine $graphics $glow 7 7 12 11 2; PixelLine $graphics $glow 18 7 13 11 2
            PixelLine $graphics (Shade $accent 25) 42 35 36 46 2; PixelLine $graphics (Shade $accent 25) 50 35 44 46 2
        }
        'chain' {
            for ($y = 17; $y -le 55; $y += 8) {
                PixelLine $graphics $glow 29 $y 34 ($y + 4) 2
                PixelLine $graphics $glow 34 ($y + 4) 29 ($y + 8) 2
            }
        }
        'frost' {
            PixelLine $graphics $glow 8 4 14 22 2; PixelLine $graphics $glow 14 22 20 10 2
            PixelLine $graphics (Shade $glow 30) 38 18 52 42 2; PixelLine $graphics (Shade $glow 30) 52 20 39 44 2
        }
        'vents' {
            for ($y = 20; $y -le 44; $y += 6) { PixelLine $graphics $glow 7 $y 26 $y 2 }
            Fill $graphics $glow 8 7 4 3; Fill $graphics $glow 19 7 4 3
        }
        'bars' {
            for ($x = 6; $x -le 58; $x += 9) { PixelLine $graphics $accent $x 16 $x 62 2 }
            PixelLine $graphics $glow 4 28 60 28 2; PixelLine $graphics $glow 4 48 60 48 2
        }
        'ember' {
            PixelLine $graphics $glow 5 27 15 34 2; PixelLine $graphics $glow 15 34 10 43 2
            PixelLine $graphics $glow 28 18 37 30 2; PixelLine $graphics $glow 37 30 47 23 2
            PixelLine $graphics $glow 48 45 58 55 2
        }
        'visor' {
            Fill $graphics $glow 4 7 24 3
            for ($y = 22; $y -le 54; $y += 13) { PixelLine $graphics (Shade $accent 20) 36 $y 59 $y 2 }
        }
        'slag' {
            for ($i = 0; $i -lt 7; $i++) {
                $x = ($seed + $i * 17) % 57
                $y = 16 + (($seed + $i * 11) % 40)
                Fill $graphics $glow $x $y 5 4
            }
        }
        'furnace' {
            Fill $graphics $glow 7 20 20 17
            Fill $graphics (Shade $base -8) 10 23 14 11
            PixelLine $graphics (Shade $glow 35) 12 30 22 30 2
            PixelLine $graphics $glow 38 34 53 55 3; PixelLine $graphics $glow 55 20 47 38 2
        }
        'filter' {
            Fill $graphics $glow 8 7 15 6
            for ($x = 10; $x -le 20; $x += 5) { PixelLine $graphics (Shade $base -20) $x 8 $x 12 1 }
            PixelLine $graphics (Shade $accent 22) 5 39 29 43 2
        }
        'wings' {
            PixelLine $graphics $glow 34 20 60 8 2; PixelLine $graphics $glow 34 21 60 34 2
            PixelLine $graphics (Shade $glow 32) 34 21 57 21 2
            PixelLine $graphics $glow 5 52 29 40 2; PixelLine $graphics $glow 5 62 29 41 2
        }
        'plates' {
            for ($x = 5; $x -lt 62; $x += 13) {
                Fill $graphics $accent $x 20 10 8
                Fill $graphics $glow ($x + 2) 22 2 2
            }
            PixelLine $graphics (Shade $accent 30) 8 48 56 48 2
        }
    }

    $path = Join-Path $textureRoot ($Mob.Id + '.png')
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

foreach ($mob in $mobs) {
    New-MobTexture $mob
    Write-Json (Join-Path $itemModelRoot ($mob.Id + '_spawn_egg.json')) ([ordered]@{
        parent = 'minecraft:item/template_spawn_egg'
    })
    Write-Json (Join-Path $lootRoot ($mob.Id + '.json')) ([ordered]@{
        type = 'minecraft:entity'
        pools = @(
            [ordered]@{
                rolls = 1
                entries = @(
                    [ordered]@{
                        type = 'minecraft:item'
                        name = ('prisonplanet:' + $mob.Drop)
                    }
                )
                conditions = @(
                    [ordered]@{
                        condition = 'minecraft:killed_by_player'
                    }
                )
            }
        )
    })
}

$lang = [ordered]@{}
(Get-Content $langPath -Raw | ConvertFrom-Json).PSObject.Properties |
    ForEach-Object { $lang[$_.Name] = $_.Value }
foreach ($mob in $mobs) {
    $lang['entity.prisonplanet.' + $mob.Id] = $mob.Name
    $lang['item.prisonplanet.' + $mob.Id + '_spawn_egg'] = $mob.Name + ' Spawn Egg'
}
Write-Json $langPath $lang

$scale = 3
$columns = 4
$rows = 3
$sheet = [System.Drawing.Bitmap]::new($columns * 64 * $scale, $rows * 64 * $scale)
$graphics = [System.Drawing.Graphics]::FromImage($sheet)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($index = 0; $index -lt $mobs.Count; $index++) {
    $source = [System.Drawing.Bitmap]::FromFile((Join-Path $textureRoot ($mobs[$index].Id + '.png')))
    $destination = [System.Drawing.Rectangle]::new(
        ($index % $columns) * 64 * $scale,
        [Math]::Floor($index / $columns) * 64 * $scale,
        64 * $scale,
        64 * $scale)
    $graphics.DrawImage($source, $destination)
    $source.Dispose()
}
$previewPath = Join-Path $PreviewRoot 'condemned_mob_textures_0.4.0.png'
$sheet.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$sheet.Dispose()

Write-Output ('Generated {0} entity texture atlases, spawn eggs, and loot tables.' -f $mobs.Count)
Write-Output ('Preview: ' + $previewPath)
