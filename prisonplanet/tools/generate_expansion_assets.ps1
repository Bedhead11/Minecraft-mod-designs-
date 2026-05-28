param(
    [string]$ResourceRoot = (Join-Path $PSScriptRoot '..\src\main\resources'),
    [string]$PreviewRoot = (Join-Path $PSScriptRoot '..\reference')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$assetRoot = Join-Path $ResourceRoot 'assets\prisonplanet'
$dataRoot = Join-Path $ResourceRoot 'data'
$textureRoot = Join-Path $assetRoot 'textures\block'
$itemTextureRoot = Join-Path $assetRoot 'textures\item'
$modelRoot = Join-Path $assetRoot 'models\block'
$itemModelRoot = Join-Path $assetRoot 'models\item'
$stateRoot = Join-Path $assetRoot 'blockstates'
$lootRoot = Join-Path $dataRoot 'prisonplanet\loot_table\blocks'
$recipeRoot = Join-Path $dataRoot 'prisonplanet\recipe'

@($textureRoot, $itemTextureRoot, $modelRoot, $itemModelRoot, $stateRoot, $lootRoot, $recipeRoot, $PreviewRoot) |
    ForEach-Object { New-Item -ItemType Directory -Path $_ -Force | Out-Null }

$rocks = @(
    'ashen_shale', 'charred_basalt', 'brimstone', 'vitrified_rock',
    'sulfur_crust', 'frostbitten_stone', 'obsidian_slag', 'compacted_cinder'
)
$ores = @(
    'glacial_shard_ore', 'deepslate_glacial_shard_ore', 'sulfur_ore', 'deepslate_sulfur_ore',
    'ferric_scrap_ore', 'deepslate_ferric_scrap_ore', 'ember_crystal_ore', 'deepslate_ember_crystal_ore'
)
$masonry = @(
    'etched_condemned_bricks', 'riveted_condemned_bricks', 'soot_stained_bricks',
    'frozen_condemned_bricks', 'condemned_mosaic', 'condemned_runed_tile',
    'prison_wall_panel', 'cracked_wall_panel', 'reinforced_bulkhead',
    'rusted_bulkhead', 'blackened_concrete', 'ash_concrete', 'blood_rust_plate',
    'warning_stripe_plate', 'drainage_tile', 'riveted_plate', 'vented_plate',
    'cage_floor', 'warden_insignia_block', 'ritual_carved_stone'
)
$pillars = @('scorched_pillar', 'cracked_support_column', 'pipe_bundle', 'coolant_pipe')
$slabs = @(
    'scorched_rock_slab', 'condemned_brick_slab', 'reinforced_slab',
    'ashen_shale_slab', 'blackened_concrete_slab', 'condemned_tile_slab'
)
$stairs = @(
    'scorched_rock_stairs', 'condemned_brick_stairs', 'reinforced_stairs',
    'ashen_shale_stairs', 'blackened_concrete_stairs', 'condemned_tile_stairs'
)
$walls = @(
    'scorched_rock_wall', 'condemned_brick_wall', 'reinforced_wall',
    'ashen_shale_wall', 'blackened_concrete_wall', 'condemned_tile_wall'
)
$panes = @('rusted_bars', 'reinforced_bars', 'frost_caked_bars', 'hazard_bars', 'grated_pane', 'coolant_pane')
$fences = @('prison_fence', 'rusted_fence', 'reinforced_fence', 'cable_fence')
$lights = @(
    'ember_lamp', 'red_warning_lamp', 'furnace_lamp', 'frozen_lantern',
    'soul_beacon_lamp', 'emergency_strip_light', 'industrial_ceiling_light', 'ritual_brazier'
)
$plants = @('ash_thorn', 'cinder_bloom', 'frost_reed', 'prison_moss', 'ember_fungus', 'pale_root', 'glassweed', 'bloodfern')
$falling = @('fine_ash', 'red_ash', 'cinder_sand', 'sulfur_sand', 'black_glass_sand', 'frost_ash', 'slag_gravel', 'powdered_char')
$materialItems = @('sulfur_cluster', 'ferric_scrap', 'ember_crystal')
$allBlocks = @($rocks + $ores + $masonry + $pillars + $slabs + $stairs + $walls + $panes + $fences + $lights + $plants + $falling)

function Write-Text([string]$Path, [string]$Text) {
    $directory = Split-Path $Path -Parent
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    [System.IO.File]::WriteAllText($Path, $Text + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
}

function Write-Json([string]$Path, [object]$Data) {
    Write-Text $Path ($Data | ConvertTo-Json -Depth 16)
}

function Stable-Hash([string]$Text) {
    $value = 17
    foreach ($character in $Text.ToCharArray()) {
        $value = (($value * 31) + [int]$character) % 2147483629
    }
    return [Math]::Abs($value)
}

function Paint([System.Drawing.Bitmap]$Bitmap, [int]$X, [int]$Y, [System.Drawing.Color]$Color) {
    if ($X -ge 0 -and $Y -ge 0 -and $X -lt 16 -and $Y -lt 16) {
        $Bitmap.SetPixel($X, $Y, $Color)
    }
}

function Shade([System.Drawing.Color]$Color, [int]$Offset, [int]$Alpha = -1) {
    $a = if ($Alpha -ge 0) { $Alpha } else { $Color.A }
    return [System.Drawing.Color]::FromArgb(
        $a,
        [Math]::Max(0, [Math]::Min(255, $Color.R + $Offset)),
        [Math]::Max(0, [Math]::Min(255, $Color.G + $Offset)),
        [Math]::Max(0, [Math]::Min(255, $Color.B + $Offset)))
}

function Hex([string]$Value, [int]$Alpha = 255) {
    $value = $Value.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        $Alpha,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16))
}

function Palette([string]$Id, [string]$Kind) {
    if ($Id -match 'sulfur|brimstone') {
        return @((Hex '#39332A'), (Hex '#655338'), (Hex '#C89C38'))
    }
    if ($Id -match 'frost|glacial|coolant') {
        return @((Hex '#283239'), (Hex '#445F6A'), (Hex '#9BE4EC'))
    }
    if ($Id -match 'ember|warning|furnace') {
        return @((Hex '#30282A'), (Hex '#644036'), (Hex '#FF8B36'))
    }
    if ($Id -match 'blood|red_ash|red_warning') {
        return @((Hex '#31262B'), (Hex '#5A3037'), (Hex '#C84237'))
    }
    if ($Id -match 'obsidian|black_glass|powdered_char') {
        return @((Hex '#171820'), (Hex '#292A35'), (Hex '#4E465E'))
    }
    $sets = switch ($Kind) {
        'ore' { @(@('#25252A', '#37333A', '#90DDF0'), @('#272529', '#3E3433', '#FFC64C'), @('#24252A', '#443139', '#E34B2F')) }
        'metal' { @(@('#24272B', '#444B50', '#A65338'), @('#272931', '#59616A', '#C77A3B'), @('#24262B', '#495363', '#78AEB7')) }
        'light' { @(@('#2A2426', '#604139', '#FF9340'), @('#24252D', '#474E5A', '#95E9F1'), @('#2A2225', '#5E2F34', '#F04131')) }
        'plant' { @(@('#433332', '#86624E', '#D95A37'), @('#2D3338', '#708693', '#C9E9E9'), @('#36282B', '#73404B', '#DF3542')) }
        'falling' { @(@('#312E32', '#484248', '#62595B'), @('#443237', '#654348', '#855452'), @('#38302B', '#6B593A', '#A08646')) }
        default { @(@('#27272B', '#36343A', '#51464A'), @('#29292D', '#40383B', '#66453C'), @('#292D32', '#3C444A', '#59666E')) }
    }
    $choice = $sets[(Stable-Hash $Id) % $sets.Count]
    return @((Hex $choice[0]), (Hex $choice[1]), (Hex $choice[2]))
}

function New-Texture([string]$Id, [string]$Kind, [string]$OutputName = $Id) {
    $seed = Stable-Hash $Id
    $palette = Palette $Id $Kind
    $base = $palette[0]
    $mid = $palette[1]
    $accent = $palette[2]
    $bitmap = [System.Drawing.Bitmap]::new(16, 16)

    if ($Kind -in @('plant', 'pane')) {
        $bitmap.MakeTransparent()
    } else {
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $roll = ($seed + $x * 37 + $y * 59 + $x * $y) % 17
                $color = if ($roll -lt 3) { Shade $base -7 } elseif ($roll -gt 13) { $mid } else { $base }
                Paint $bitmap $x $y $color
            }
        }
    }

    switch ($Kind) {
        'rock' {
            for ($i = 0; $i -lt 4; $i++) {
                $x = ($seed + $i * 5) % 15
                $y = (($seed / 7) + $i * 4) % 15
                Paint $bitmap $x $y $mid
                Paint $bitmap ($x + 1) $y (Shade $mid 7)
            }
        }
        'ore' {
            for ($i = 0; $i -lt 5; $i++) {
                $x = 2 + (($seed + $i * 7) % 12)
                $y = 2 + (([int]($seed / 9) + $i * 5) % 12)
                Paint $bitmap $x $y $accent
                Paint $bitmap ($x + 1) $y (Shade $accent 24)
                Paint $bitmap $x ($y + 1) (Shade $accent -30)
            }
        }
        'masonry' {
            for ($y = 3; $y -lt 16; $y += 4) {
                for ($x = 0; $x -lt 16; $x++) { Paint $bitmap $x $y (Shade $base -16) }
                $offset = if ((($y + $seed) % 8) -eq 0) { 4 } else { 11 }
                for ($x = $offset; $x -lt 16; $x += 8) { Paint $bitmap $x ($y - 2) (Shade $base -18) }
            }
            if ($Id -match 'riveted|bulkhead|plate|panel|cage|warning') {
                foreach ($corner in @(@(1,1), @(14,1), @(1,14), @(14,14))) { Paint $bitmap $corner[0] $corner[1] $accent }
            }
            switch -Regex ($Id) {
                '^warning_stripe_plate$' {
                    for ($x = -14; $x -lt 16; $x += 6) {
                        for ($n = 0; $n -lt 3; $n++) {
                            for ($y = 0; $y -lt 16; $y++) { Paint $bitmap ($x + $y + $n) $y (Hex '#D88A2E') }
                        }
                    }
                }
                '^cage_floor$' {
                    foreach ($line in @(3, 8, 13)) {
                        for ($n = 0; $n -lt 16; $n++) {
                            Paint $bitmap $line $n (Shade $mid 22)
                            Paint $bitmap $n $line (Shade $mid 22)
                        }
                    }
                }
                '^vented_plate$' {
                    foreach ($y in @(4, 7, 10)) {
                        for ($x = 3; $x -lt 13; $x++) { Paint $bitmap $x $y (Hex '#13151A') }
                    }
                }
                '^drainage_tile$' {
                    foreach ($x in @(4, 7, 10)) {
                        for ($y = 2; $y -lt 14; $y++) { Paint $bitmap $x $y (Hex '#16181B') }
                    }
                }
                '^warden_insignia_block$' {
                    for ($y = 3; $y -lt 12; $y++) {
                        Paint $bitmap 7 $y $accent
                        Paint $bitmap 8 $y $accent
                    }
                    for ($n = 0; $n -lt 5; $n++) {
                        Paint $bitmap (5 + $n) (4 + $n) $accent
                        Paint $bitmap (10 - $n) (4 + $n) $accent
                    }
                }
                'runed|ritual' {
                    for ($n = 3; $n -lt 13; $n++) {
                        Paint $bitmap $n $n $accent
                        Paint $bitmap (15 - $n) $n (Shade $accent 18)
                    }
                    for ($x = 5; $x -lt 11; $x++) { Paint $bitmap $x 8 $accent }
                }
                '^condemned_mosaic$' {
                    for ($y = 2; $y -lt 14; $y += 4) {
                        for ($x = 2; $x -lt 14; $x += 4) {
                            Paint $bitmap $x $y $accent
                            Paint $bitmap ($x + 1) $y $accent
                            Paint $bitmap $x ($y + 1) $accent
                        }
                    }
                }
                '^blood_rust_plate$' {
                    foreach ($x in @(3, 9, 12)) {
                        for ($y = 2; $y -lt (7 + ($x % 5)); $y++) { Paint $bitmap $x $y $accent }
                    }
                }
            }
        }
        'metal' {
            for ($x = 1; $x -lt 16; $x += 7) {
                for ($y = 0; $y -lt 16; $y++) { Paint $bitmap $x $y (Shade $mid -15) }
            }
            foreach ($p in @(@(2,2), @(13,2), @(2,13), @(13,13))) { Paint $bitmap $p[0] $p[1] $accent }
        }
        'pillar_side' {
            for ($x = 0; $x -lt 16; $x++) {
                if (($x % 5) -eq 0) {
                    for ($y = 0; $y -lt 16; $y++) { Paint $bitmap $x $y $mid }
                }
            }
        }
        'pillar_top' {
            for ($r = 1; $r -lt 7; $r += 2) {
                for ($x = $r; $x -lt 16 - $r; $x++) {
                    Paint $bitmap $x $r $mid
                    Paint $bitmap $x (15 - $r) $mid
                }
                for ($y = $r; $y -lt 16 - $r; $y++) {
                    Paint $bitmap $r $y $mid
                    Paint $bitmap (15 - $r) $y $mid
                }
            }
        }
        'pane' {
            $clear = [System.Drawing.Color]::Transparent
            for ($x = 0; $x -lt 16; $x++) {
                for ($y = 0; $y -lt 16; $y++) { Paint $bitmap $x $y $clear }
            }
            foreach ($x in @(1, 2, 7, 8, 13, 14)) {
                for ($y = 0; $y -lt 16; $y++) { Paint $bitmap $x $y $(if (($x + $y) % 4 -eq 0) { $accent } else { $mid }) }
            }
            foreach ($y in @(2, 13)) {
                for ($x = 0; $x -lt 16; $x++) { Paint $bitmap $x $y $mid }
            }
        }
        'light' {
            for ($x = 2; $x -lt 14; $x++) {
                for ($y = 2; $y -lt 14; $y++) {
                    $edge = $x -eq 2 -or $x -eq 13 -or $y -eq 2 -or $y -eq 13
                    Paint $bitmap $x $y $(if ($edge) { $mid } else { $accent })
                }
            }
            for ($x = 4; $x -lt 13; $x += 4) {
                for ($y = 3; $y -lt 13; $y++) { Paint $bitmap $x $y (Shade $accent -40) }
            }
        }
        'plant' {
            for ($y = 5; $y -lt 16; $y++) {
                Paint $bitmap 7 $y $mid
                Paint $bitmap 8 $y (Shade $mid 12)
            }
            foreach ($leaf in @(@(5,10), @(4,9), @(10,8), @(11,7), @(6,6), @(9,4))) {
                Paint $bitmap $leaf[0] $leaf[1] $accent
                Paint $bitmap ($leaf[0] + 1) $leaf[1] (Shade $accent 12)
            }
        }
        'falling' {
            for ($i = 0; $i -lt 15; $i++) {
                $x = ($seed + $i * 11) % 16
                $y = (([int]($seed / 13) + $i * 7) % 16)
                Paint $bitmap $x $y $(if (($i % 3) -eq 0) { $accent } else { $mid })
            }
        }
        'item' {
            for ($y = 3; $y -lt 13; $y++) {
                for ($x = 3; $x -lt 13; $x++) {
                    if (($x + $y) -gt 7 -and ($x + $y) -lt 22) { Paint $bitmap $x $y $accent }
                }
            }
            Paint $bitmap 5 5 (Shade $accent 35)
        }
    }

    $path = if ($Kind -eq 'item') { Join-Path $itemTextureRoot ($OutputName + '.png') } else { Join-Path $textureRoot ($OutputName + '.png') }
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Write-Loot([string]$Id, [string]$Drop = $Id) {
    Write-Json (Join-Path $lootRoot ($Id + '.json')) ([ordered]@{
        type = 'minecraft:block'
        pools = @([ordered]@{
            rolls = 1
            entries = @([ordered]@{ type = 'minecraft:item'; name = ('prisonplanet:' + $Drop) })
            conditions = @([ordered]@{ condition = 'minecraft:survives_explosion' })
        })
    })
}

function Write-SlabLoot([string]$Id) {
    Write-Json (Join-Path $lootRoot ($Id + '.json')) ([ordered]@{
        type = 'minecraft:block'
        pools = @([ordered]@{
            rolls = 1
            entries = @([ordered]@{
                type = 'minecraft:item'
                name = ('prisonplanet:' + $Id)
                functions = @([ordered]@{
                    function = 'minecraft:set_count'
                    count = 2
                    conditions = @([ordered]@{
                        condition = 'minecraft:block_state_property'
                        block = ('prisonplanet:' + $Id)
                        properties = [ordered]@{ type = 'double' }
                    })
                })
            })
            conditions = @([ordered]@{ condition = 'minecraft:survives_explosion' })
        })
    })
}

function Write-Stonecutting([string]$Id, [string]$Ingredient, [int]$Count = 1) {
    Write-Json (Join-Path $recipeRoot ('stonecutting_' + $Id + '.json')) ([ordered]@{
        type = 'minecraft:stonecutting'
        ingredient = [ordered]@{ item = ('prisonplanet:' + $Ingredient) }
        result = [ordered]@{ id = ('prisonplanet:' + $Id); count = $Count }
    })
}

function Write-LightRecipe([string]$Id, [string]$Catalyst) {
    Write-Json (Join-Path $recipeRoot ($Id + '.json')) ([ordered]@{
        type = 'minecraft:crafting_shaped'
        category = 'building'
        pattern = @('MGM', 'GCG', 'MGM')
        key = [ordered]@{
            M = [ordered]@{ item = 'prisonplanet:condemned_metal_plate' }
            G = [ordered]@{ item = 'minecraft:glass_pane' }
            C = [ordered]@{ item = ('prisonplanet:' + $Catalyst) }
        }
        result = [ordered]@{ id = ('prisonplanet:' + $Id); count = 1 }
    })
}

function Write-Cube([string]$Id, [string]$Kind) {
    New-Texture $Id $Kind
    Write-Json (Join-Path $stateRoot ($Id + '.json')) ([ordered]@{ variants = [ordered]@{ '' = [ordered]@{ model = ('prisonplanet:block/' + $Id) } } })
    Write-Json (Join-Path $modelRoot ($Id + '.json')) ([ordered]@{
        parent = 'minecraft:block/cube_all'
        textures = [ordered]@{ all = ('prisonplanet:block/' + $Id) }
    })
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id) })
    Write-Loot $Id
}

function Write-Pillar([string]$Id) {
    New-Texture $Id 'pillar_side'
    New-Texture $Id 'pillar_side' ($Id + '_side')
    New-Texture $Id 'pillar_top' ($Id + '_top')
    Write-Json (Join-Path $stateRoot ($Id + '.json')) ([ordered]@{
        variants = [ordered]@{
            'axis=y' = [ordered]@{ model = ('prisonplanet:block/' + $Id) }
            'axis=z' = [ordered]@{ model = ('prisonplanet:block/' + $Id + '_horizontal'); x = 90 }
            'axis=x' = [ordered]@{ model = ('prisonplanet:block/' + $Id + '_horizontal'); x = 90; y = 90 }
        }
    })
    foreach ($name in @($Id, ($Id + '_horizontal'))) {
        Write-Json (Join-Path $modelRoot ($name + '.json')) ([ordered]@{
            parent = 'minecraft:block/cube_column'
            textures = [ordered]@{ side = ('prisonplanet:block/' + $Id + '_side'); end = ('prisonplanet:block/' + $Id + '_top') }
        })
    }
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id) })
    Write-Loot $Id
}

function Write-Slab([string]$Id) {
    New-Texture $Id 'masonry'
    Write-Json (Join-Path $stateRoot ($Id + '.json')) ([ordered]@{
        variants = [ordered]@{
            'type=bottom' = [ordered]@{ model = ('prisonplanet:block/' + $Id) }
            'type=top' = [ordered]@{ model = ('prisonplanet:block/' + $Id + '_top') }
            'type=double' = [ordered]@{ model = ('prisonplanet:block/' + $Id + '_double') }
        }
    })
    Write-Json (Join-Path $modelRoot ($Id + '.json')) ([ordered]@{ parent = 'minecraft:block/slab'; textures = [ordered]@{ bottom = ('prisonplanet:block/' + $Id); top = ('prisonplanet:block/' + $Id); side = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $modelRoot ($Id + '_top.json')) ([ordered]@{ parent = 'minecraft:block/slab_top'; textures = [ordered]@{ bottom = ('prisonplanet:block/' + $Id); top = ('prisonplanet:block/' + $Id); side = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $modelRoot ($Id + '_double.json')) ([ordered]@{ parent = 'minecraft:block/cube_all'; textures = [ordered]@{ all = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id) })
    Write-SlabLoot $Id
}

function Write-Stairs([string]$Id) {
    New-Texture $Id 'masonry'
    $base = 'prisonplanet:block/' + $Id
    Write-Text (Join-Path $stateRoot ($Id + '.json')) @"
{"variants":{"facing=east,half=bottom,shape=straight":{"model":"$base"},"facing=west,half=bottom,shape=straight":{"model":"$base","y":180,"uvlock":true},"facing=south,half=bottom,shape=straight":{"model":"$base","y":90,"uvlock":true},"facing=north,half=bottom,shape=straight":{"model":"$base","y":270,"uvlock":true},"facing=east,half=bottom,shape=outer_right":{"model":"${base}_outer"},"facing=west,half=bottom,shape=outer_right":{"model":"${base}_outer","y":180,"uvlock":true},"facing=south,half=bottom,shape=outer_right":{"model":"${base}_outer","y":90,"uvlock":true},"facing=north,half=bottom,shape=outer_right":{"model":"${base}_outer","y":270,"uvlock":true},"facing=east,half=bottom,shape=outer_left":{"model":"${base}_outer","y":270,"uvlock":true},"facing=west,half=bottom,shape=outer_left":{"model":"${base}_outer","y":90,"uvlock":true},"facing=south,half=bottom,shape=outer_left":{"model":"${base}_outer"},"facing=north,half=bottom,shape=outer_left":{"model":"${base}_outer","y":180,"uvlock":true},"facing=east,half=bottom,shape=inner_right":{"model":"${base}_inner"},"facing=west,half=bottom,shape=inner_right":{"model":"${base}_inner","y":180,"uvlock":true},"facing=south,half=bottom,shape=inner_right":{"model":"${base}_inner","y":90,"uvlock":true},"facing=north,half=bottom,shape=inner_right":{"model":"${base}_inner","y":270,"uvlock":true},"facing=east,half=bottom,shape=inner_left":{"model":"${base}_inner","y":270,"uvlock":true},"facing=west,half=bottom,shape=inner_left":{"model":"${base}_inner","y":90,"uvlock":true},"facing=south,half=bottom,shape=inner_left":{"model":"${base}_inner"},"facing=north,half=bottom,shape=inner_left":{"model":"${base}_inner","y":180,"uvlock":true},"facing=east,half=top,shape=straight":{"model":"$base","x":180,"uvlock":true},"facing=west,half=top,shape=straight":{"model":"$base","x":180,"y":180,"uvlock":true},"facing=south,half=top,shape=straight":{"model":"$base","x":180,"y":90,"uvlock":true},"facing=north,half=top,shape=straight":{"model":"$base","x":180,"y":270,"uvlock":true},"facing=east,half=top,shape=outer_right":{"model":"${base}_outer","x":180,"y":90,"uvlock":true},"facing=west,half=top,shape=outer_right":{"model":"${base}_outer","x":180,"y":270,"uvlock":true},"facing=south,half=top,shape=outer_right":{"model":"${base}_outer","x":180,"y":180,"uvlock":true},"facing=north,half=top,shape=outer_right":{"model":"${base}_outer","x":180,"uvlock":true},"facing=east,half=top,shape=outer_left":{"model":"${base}_outer","x":180,"uvlock":true},"facing=west,half=top,shape=outer_left":{"model":"${base}_outer","x":180,"y":180,"uvlock":true},"facing=south,half=top,shape=outer_left":{"model":"${base}_outer","x":180,"y":90,"uvlock":true},"facing=north,half=top,shape=outer_left":{"model":"${base}_outer","x":180,"y":270,"uvlock":true},"facing=east,half=top,shape=inner_right":{"model":"${base}_inner","x":180,"y":90,"uvlock":true},"facing=west,half=top,shape=inner_right":{"model":"${base}_inner","x":180,"y":270,"uvlock":true},"facing=south,half=top,shape=inner_right":{"model":"${base}_inner","x":180,"y":180,"uvlock":true},"facing=north,half=top,shape=inner_right":{"model":"${base}_inner","x":180,"uvlock":true},"facing=east,half=top,shape=inner_left":{"model":"${base}_inner","x":180,"uvlock":true},"facing=west,half=top,shape=inner_left":{"model":"${base}_inner","x":180,"y":180,"uvlock":true},"facing=south,half=top,shape=inner_left":{"model":"${base}_inner","x":180,"y":90,"uvlock":true},"facing=north,half=top,shape=inner_left":{"model":"${base}_inner","x":180,"y":270,"uvlock":true}}}
"@
    Write-Json (Join-Path $modelRoot ($Id + '.json')) ([ordered]@{ parent = 'minecraft:block/stairs'; textures = [ordered]@{ bottom = ('prisonplanet:block/' + $Id); top = ('prisonplanet:block/' + $Id); side = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $modelRoot ($Id + '_inner.json')) ([ordered]@{ parent = 'minecraft:block/inner_stairs'; textures = [ordered]@{ bottom = ('prisonplanet:block/' + $Id); top = ('prisonplanet:block/' + $Id); side = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $modelRoot ($Id + '_outer.json')) ([ordered]@{ parent = 'minecraft:block/outer_stairs'; textures = [ordered]@{ bottom = ('prisonplanet:block/' + $Id); top = ('prisonplanet:block/' + $Id); side = ('prisonplanet:block/' + $Id) } })
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id) })
    Write-Loot $Id
}

function Write-Wall([string]$Id) {
    New-Texture $Id 'masonry'
    $base = 'prisonplanet:block/' + $Id
    Write-Text (Join-Path $stateRoot ($Id + '.json')) @"
{"multipart":[{"when":{"up":"true"},"apply":{"model":"${base}_post"}},{"when":{"north":"low"},"apply":{"model":"${base}_side","uvlock":true}},{"when":{"east":"low"},"apply":{"model":"${base}_side","y":90,"uvlock":true}},{"when":{"south":"low"},"apply":{"model":"${base}_side","y":180,"uvlock":true}},{"when":{"west":"low"},"apply":{"model":"${base}_side","y":270,"uvlock":true}},{"when":{"north":"tall"},"apply":{"model":"${base}_side_tall","uvlock":true}},{"when":{"east":"tall"},"apply":{"model":"${base}_side_tall","y":90,"uvlock":true}},{"when":{"south":"tall"},"apply":{"model":"${base}_side_tall","y":180,"uvlock":true}},{"when":{"west":"tall"},"apply":{"model":"${base}_side_tall","y":270,"uvlock":true}}]}
"@
    foreach ($part in @(@('post', 'template_wall_post'), @('side', 'template_wall_side'), @('side_tall', 'template_wall_side_tall'), @('inventory', 'wall_inventory'))) {
        Write-Json (Join-Path $modelRoot ($Id + '_' + $part[0] + '.json')) ([ordered]@{ parent = ('minecraft:block/' + $part[1]); textures = [ordered]@{ wall = ('prisonplanet:block/' + $Id) } })
    }
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id + '_inventory') })
    Write-Loot $Id
}

function Write-Pane([string]$Id) {
    New-Texture $Id 'pane'
    $base = 'prisonplanet:block/' + $Id
    Write-Text (Join-Path $stateRoot ($Id + '.json')) @"
{"multipart":[{"apply":{"model":"${base}_post"}},{"when":{"north":"true"},"apply":{"model":"${base}_side"}},{"when":{"east":"true"},"apply":{"model":"${base}_side","y":90}},{"when":{"south":"true"},"apply":{"model":"${base}_side_alt"}},{"when":{"west":"true"},"apply":{"model":"${base}_side_alt","y":90}}]}
"@
    foreach ($part in @('post', 'side', 'side_alt')) {
        Write-Json (Join-Path $modelRoot ($Id + '_' + $part + '.json')) ([ordered]@{
            parent = ('minecraft:block/iron_bars_' + $part)
            render_type = 'minecraft:cutout'
            textures = [ordered]@{ bars = ('prisonplanet:block/' + $Id); particle = ('prisonplanet:block/' + $Id) }
        })
    }
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id + '_post') })
    Write-Loot $Id
}

function Write-Fence([string]$Id) {
    New-Texture $Id 'metal'
    $base = 'prisonplanet:block/' + $Id
    Write-Text (Join-Path $stateRoot ($Id + '.json')) @"
{"multipart":[{"apply":{"model":"${base}_post"}},{"when":{"north":"true"},"apply":{"model":"${base}_side","uvlock":true}},{"when":{"east":"true"},"apply":{"model":"${base}_side","y":90,"uvlock":true}},{"when":{"south":"true"},"apply":{"model":"${base}_side","y":180,"uvlock":true}},{"when":{"west":"true"},"apply":{"model":"${base}_side","y":270,"uvlock":true}}]}
"@
    foreach ($part in @(@('post', 'fence_post'), @('side', 'fence_side'), @('inventory', 'fence_inventory'))) {
        Write-Json (Join-Path $modelRoot ($Id + '_' + $part[0] + '.json')) ([ordered]@{ parent = ('minecraft:block/' + $part[1]); textures = [ordered]@{ texture = ('prisonplanet:block/' + $Id) } })
    }
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = ('prisonplanet:block/' + $Id + '_inventory') })
    Write-Loot $Id
}

function Write-Plant([string]$Id) {
    New-Texture $Id 'plant'
    Write-Json (Join-Path $stateRoot ($Id + '.json')) ([ordered]@{ variants = [ordered]@{ '' = [ordered]@{ model = ('prisonplanet:block/' + $Id) } } })
    Write-Json (Join-Path $modelRoot ($Id + '.json')) ([ordered]@{
        parent = 'minecraft:block/cross'
        render_type = 'minecraft:cutout'
        textures = [ordered]@{ cross = ('prisonplanet:block/' + $Id) }
    })
    Write-Json (Join-Path $itemModelRoot ($Id + '.json')) ([ordered]@{ parent = 'minecraft:item/generated'; textures = [ordered]@{ layer0 = ('prisonplanet:block/' + $Id) } })
    Write-Loot $Id
}

foreach ($id in $rocks) { Write-Cube $id 'rock' }
foreach ($id in $ores) { Write-Cube $id 'ore' }
foreach ($id in $masonry) { Write-Cube $id 'masonry' }
foreach ($id in $pillars) { Write-Pillar $id }
foreach ($id in $slabs) { Write-Slab $id }
foreach ($id in $stairs) { Write-Stairs $id }
foreach ($id in $walls) { Write-Wall $id }
foreach ($id in $panes) { Write-Pane $id }
foreach ($id in $fences) { Write-Fence $id }
foreach ($id in $lights) { Write-Cube $id 'light' }
foreach ($id in $plants) { Write-Plant $id }
foreach ($id in $falling) { Write-Cube $id 'falling' }

$oreDrops = [ordered]@{
    glacial_shard_ore = 'glacial_shard'; deepslate_glacial_shard_ore = 'glacial_shard'
    sulfur_ore = 'sulfur_cluster'; deepslate_sulfur_ore = 'sulfur_cluster'
    ferric_scrap_ore = 'ferric_scrap'; deepslate_ferric_scrap_ore = 'ferric_scrap'
    ember_crystal_ore = 'ember_crystal'; deepslate_ember_crystal_ore = 'ember_crystal'
}
$oreDrops.GetEnumerator() | ForEach-Object { Write-Loot $_.Key $_.Value }

foreach ($id in $materialItems) {
    New-Texture $id 'item'
    Write-Json (Join-Path $itemModelRoot ($id + '.json')) ([ordered]@{
        parent = 'minecraft:item/generated'
        textures = [ordered]@{ layer0 = ('prisonplanet:item/' + $id) }
    })
}

# Building and lighting acquisition paths for survival construction.
foreach ($id in $masonry) { Write-Stonecutting $id 'condemned_bricks' }
Write-Stonecutting 'scorched_pillar' 'scorched_rock'
Write-Stonecutting 'cracked_support_column' 'condemned_bricks'
Write-Stonecutting 'pipe_bundle' 'condemned_metal_plate'
Write-Stonecutting 'coolant_pipe' 'condemned_metal_plate'
foreach ($id in @('scorched_rock_slab', 'scorched_rock_stairs', 'scorched_rock_wall')) { Write-Stonecutting $id 'scorched_rock' }
foreach ($id in @('condemned_brick_slab', 'condemned_brick_stairs', 'condemned_brick_wall')) { Write-Stonecutting $id 'condemned_bricks' }
foreach ($id in @('reinforced_slab', 'reinforced_stairs', 'reinforced_wall')) { Write-Stonecutting $id 'condemned_reinforced_block' }
foreach ($id in @('ashen_shale_slab', 'ashen_shale_stairs', 'ashen_shale_wall')) { Write-Stonecutting $id 'ashen_shale' }
foreach ($id in @('blackened_concrete_slab', 'blackened_concrete_stairs', 'blackened_concrete_wall')) { Write-Stonecutting $id 'blackened_concrete' }
foreach ($id in @('condemned_tile_slab', 'condemned_tile_stairs', 'condemned_tile_wall')) { Write-Stonecutting $id 'condemned_tile' }
foreach ($id in $panes + $fences) { Write-Stonecutting $id 'condemned_metal_plate' }
Write-LightRecipe 'ember_lamp' 'ember_crystal'
Write-LightRecipe 'red_warning_lamp' 'ferric_scrap'
Write-LightRecipe 'furnace_lamp' 'sulfur_cluster'
Write-LightRecipe 'frozen_lantern' 'glacial_shard'
Write-LightRecipe 'soul_beacon_lamp' 'glacial_shard'
Write-LightRecipe 'emergency_strip_light' 'ferric_scrap'
Write-LightRecipe 'industrial_ceiling_light' 'sulfur_cluster'
Write-LightRecipe 'ritual_brazier' 'ember_crystal'

$names = [ordered]@{}
foreach ($id in $allBlocks) {
    $title = (Get-Culture).TextInfo.ToTitleCase(($id -replace '_', ' '))
    $names['block.prisonplanet.' + $id] = $title
}
$names['item.prisonplanet.sulfur_cluster'] = 'Sulfur Cluster'
$names['item.prisonplanet.ferric_scrap'] = 'Ferric Scrap'
$names['item.prisonplanet.ember_crystal'] = 'Ember Crystal'
$names['itemGroup.prisonplanet.the_condemned'] = 'The Condemned'
$langPath = Join-Path $assetRoot 'lang\en_us.json'
$lang = [ordered]@{}
(Get-Content $langPath -Raw | ConvertFrom-Json).PSObject.Properties |
    ForEach-Object { $lang[$_.Name] = $_.Value }
$names.GetEnumerator() | ForEach-Object { $lang[$_.Key] = $_.Value }
Write-Json $langPath $lang

function Update-ValuesTag([string]$Path, [string[]]$Values) {
    if (Test-Path $Path) {
        $existing = Get-Content $Path -Raw | ConvertFrom-Json
        $tag = [ordered]@{ replace = [bool]$existing.replace; values = @($existing.values) }
    } else {
        $tag = [ordered]@{ replace = $false; values = @() }
    }
    $tag.values = @($tag.values + $Values | Sort-Object -Unique)
    Write-Json $Path $tag
}

$pickaxe = @($rocks + $ores + $masonry + $pillars + $slabs + $stairs + $walls + $panes + $fences + $lights)
$shovel = @($falling)
Update-ValuesTag (Join-Path $dataRoot 'minecraft\tags\block\mineable\pickaxe.json') ($pickaxe | ForEach-Object { 'prisonplanet:' + $_ })
Update-ValuesTag (Join-Path $dataRoot 'minecraft\tags\block\mineable\shovel.json') ($shovel | ForEach-Object { 'prisonplanet:' + $_ })
Update-ValuesTag (Join-Path $dataRoot 'minecraft\tags\block\walls.json') ($walls | ForEach-Object { 'prisonplanet:' + $_ })
Update-ValuesTag (Join-Path $dataRoot 'minecraft\tags\block\fences.json') ($fences | ForEach-Object { 'prisonplanet:' + $_ })
Update-ValuesTag (Join-Path $dataRoot 'prisonplanet\tags\block\heat_sources.json') @(
    'prisonplanet:ember_lamp', 'prisonplanet:furnace_lamp', 'prisonplanet:ritual_brazier'
)
Update-ValuesTag (Join-Path $dataRoot 'minecraft\tags\block\needs_iron_tool.json') @(
    'prisonplanet:glacial_shard_ore', 'prisonplanet:deepslate_glacial_shard_ore',
    'prisonplanet:ember_crystal_ore', 'prisonplanet:deepslate_ember_crystal_ore'
)

# Catalogue sheet for checking the family as a single palette before shipping.
$scale = 4
$columns = 12
$rows = [Math]::Ceiling($allBlocks.Count / $columns)
$sheet = [System.Drawing.Bitmap]::new($columns * 16 * $scale, $rows * 16 * $scale)
$graphics = [System.Drawing.Graphics]::FromImage($sheet)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($index = 0; $index -lt $allBlocks.Count; $index++) {
    $source = [System.Drawing.Bitmap]::FromFile((Join-Path $textureRoot ($allBlocks[$index] + '.png')))
    $destination = [System.Drawing.Rectangle]::new(
        ($index % $columns) * 16 * $scale,
        [Math]::Floor($index / $columns) * 16 * $scale,
        16 * $scale,
        16 * $scale)
    $graphics.DrawImage($source, $destination)
    $source.Dispose()
}
$previewPath = Join-Path $PreviewRoot 'condemned_block_catalog_0.3.0.png'
$sheet.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$sheet.Dispose()

Write-Output ("Generated {0} block asset families and {1} mined item textures." -f $allBlocks.Count, $materialItems.Count)
Write-Output ("Preview: " + $previewPath)
