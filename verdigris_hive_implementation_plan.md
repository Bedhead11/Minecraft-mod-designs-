# The Verdigris Hive — Full Technical Implementation Plan
### NeoForge 1.21.1 · Java 21 · Minecraft 1.21.1

---

## TABLE OF CONTENTS

1. [Project Setup & Architecture](#1-project-setup--architecture)
2. [Registry & Resource Namespacing](#2-registry--resource-namespacing)
3. [Core Mod Constants & Configuration](#3-core-mod-constants--configuration)
4. [Blocks — Full Specification](#4-blocks--full-specification)
5. [Items — Full Specification](#5-items--full-specification)
6. [Fluids — Full Specification](#6-fluids--full-specification)
7. [World Generation — The Verdigris Hive Dimension](#7-world-generation--the-verdigris-hive-dimension)
8. [World Generation — Solarpunk Overworld Biome](#8-world-generation--solarpunk-overworld-biome)
9. [Structures — Jigsaw & Template Pools](#9-structures--jigsaw--template-pools)
10. [Entities & Mobs — Full Specification](#10-entities--mobs--full-specification)
11. [Temperature Mechanic](#11-temperature-mechanic)
12. [Corruption Spread System](#12-corruption-spread-system)
13. [Day/Night Cycle Extremes](#13-daynight-cycle-extremes)
14. [Create Mod Integration Layer](#14-create-mod-integration-layer)
15. [Gameplay Progression & Loot](#15-gameplay-progression--loot)
16. [Sounds & Ambience](#16-sounds--ambience)
17. [Visual & Rendering Specs](#17-visual--rendering-specs)
18. [Data Generation](#18-data-generation)
19. [Performance Constraints & Budgets](#19-performance-constraints--budgets)
20. [Mandatory Goals & Acceptance Criteria](#20-mandatory-goals--acceptance-criteria)
21. [Dependency Declarations](#21-dependency-declarations)
22. [File & Package Structure](#22-file--package-structure)

---

## 1. PROJECT SETUP & ARCHITECTURE

### 1.1 Toolchain

| Property | Value |
|---|---|
| Minecraft Version | 1.21.1 |
| NeoForge Version | 21.1.x (latest stable at time of build) |
| Java Version | Java 21 (Eclipse Temurin recommended) |
| Build System | Gradle 8.x (via NeoForge MDK wrapper) |
| Mod ID | `verdigrishive` |
| Maven Group | `com.verdigrishive` |
| Mod Name | The Verdigris Hive |
| License | MIT (open source; contributors welcome) |

### 1.2 MDK Bootstrap

Use the official NeoForge MDK from https://github.com/neoforged/MDK as the base. Clone it and replace all placeholder values.

`gradle.properties` must define:
```properties
org.gradle.jvmargs=-Xmx4G
org.gradle.daemon=false
minecraft_version=1.21.1
neoforge_version=21.1.x
mod_id=verdigrishive
mod_name=The Verdigris Hive
mod_version=0.1.0
mod_group_id=com.verdigrishive
mod_authors=<authors>
mod_description=A biopunk megadimension and solarpunk biome with Create mod integration.
```

### 1.3 Architectural Principles

**PRINCIPLE 1 — Data-Driven First.** Every value that could change in a balance pass (temperature tick rates, corruption spread chance, loot table weights, structure spawn rates) MUST be defined in JSON data files or the mod config, NOT hardcoded. Hardcoded magic numbers are a blocking defect.

**PRINCIPLE 2 — Registry Isolation.** Every registry object (blocks, items, entities, biomes, dimensions, features, structure types, sound events, particle types, menu types, recipe types) MUST be registered via its own `DeferredRegister` in a dedicated registration class. No cross-register references in static initializers.

**PRINCIPLE 3 — Side Safety.** All server-side logic (world gen, entity AI, temperature ticks, corruption spread) MUST be guarded against client-side execution. All client-side logic (rendering, particle effects, HUD overlays) MUST use `@OnlyIn(Dist.CLIENT)` or be in a `ClientEvents` class registered only on `FMLClientSetupEvent`. No `Level` casts to `ServerLevel` without an `instanceof` check.

**PRINCIPLE 4 — Event Bus Hygiene.** NeoForge 1.21.1 uses two event buses: `NeoForge.EVENT_BUS` (game events) and `FMLJavaModLoadingContext.get().getModEventBus()` (mod lifecycle). Never register a lifecycle event on the game bus or vice versa. All event listener classes must be registered explicitly in the main mod class.

**PRINCIPLE 5 — Soft Dependencies.** Create mod and all Create addons are SOFT dependencies. Every integration point MUST be wrapped in a `ModList.get().isLoaded("create")` guard, with a non-Create fallback path for all mechanics that reference Create objects. The mod MUST function (with reduced content) without Create installed.

**PRINCIPLE 6 — Extensibility via Datapacks.** Players and pack authors must be able to override biome placements, structure weights, loot tables, temperature values, corruption spread rules, and mob spawn lists via a datapack without touching Java code.

**PRINCIPLE 7 — No Mixin Unless Necessary.** Mixins are only permitted for: (a) the custom day/night cycle length; (b) the temperature HUD bar swap (hunger↔warmth); (c) Create contraption patching if required by the Create integration. All other behavior must use NeoForge events. Every Mixin must have a comment block explaining why no event alternative exists.

### 1.4 Package Layout (High-Level)

```
com.verdigrishive/
├── VerdigrisHive.java              ← Main mod class
├── client/
│   ├── ClientEvents.java
│   ├── hud/
│   │   └── TemperatureHudOverlay.java
│   ├── particle/
│   └── renderer/
│       ├── entity/
│       └── block/
├── common/
│   ├── block/
│   ├── blockentity/
│   ├── capability/
│   │   ├── ITemperatureCapability.java
│   │   └── ICorruptionTrackerCapability.java
│   ├── config/
│   │   └── VHConfig.java
│   ├── data/
│   │   └── VHDataGenerator.java
│   ├── effect/
│   ├── entity/
│   │   ├── ai/
│   │   └── mob/
│   ├── fluid/
│   ├── item/
│   ├── loot/
│   ├── menu/
│   ├── recipe/
│   ├── sound/
│   └── util/
├── integration/
│   └── create/
│       ├── CreateIntegration.java
│       ├── CreateRecipeTypes.java
│       └── contraption/
├── server/
│   └── ServerEvents.java
└── worldgen/
    ├── biome/
    ├── dimension/
    ├── feature/
    ├── noise/
    ├── placement/
    └── structure/
```

---

## 2. REGISTRY & RESOURCE NAMESPACING

### 2.1 Registry Classes

Create one static registry class per object type. Each class holds only its `DeferredRegister` and `RegistryObject`/`DeferredHolder` fields. No logic. Example pattern:

```java
public class VHBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(VerdigrisHive.MOD_ID);

    // ... all block registrations here
    
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
```

Registry classes required:
- `VHBlocks`
- `VHItems`
- `VHFluids`
- `VHFluidTypes`
- `VHEntityTypes`
- `VHBiomes`
- `VHDimensions` (holds the `ResourceKey<Level>` constant, not a DeferredRegister)
- `VHNoiseSettings` (holds `ResourceKey<NoiseGeneratorSettings>`)
- `VHStructureTypes`
- `VHStructureSets`
- `VHPlacedFeatures`
- `VHConfiguredFeatures`
- `VHSoundEvents`
- `VHParticleTypes`
- `VHMobEffects`
- `VHMenuTypes`
- `VHRecipeTypes`
- `VHRecipeSerializers`
- `VHBlockEntityTypes`
- `VHCapabilities` (attachment types for temperature/corruption)

### 2.2 Resource Location Conventions

All resource locations use the format `verdigrishive:<category>/<name>`.

| Category | Prefix example |
|---|---|
| Blocks | `verdigrishive:block/corruption_flesh` |
| Items | `verdigrishive:item/sun_shard` |
| Biomes | `verdigrishive:biome/sunscorch_terraces` |
| Structures | `verdigrishive:structure/bosco_spire` |
| Sounds | `verdigrishive:ambient.fleshfold.pulse` |
| Loot tables | `verdigrishive:chests/andesite_refinery` |

### 2.3 Main Mod Class

```java
@Mod(VerdigrisHive.MOD_ID)
public class VerdigrisHive {
    public static final String MOD_ID = "verdigrishive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VerdigrisHive(IEventBus modEventBus, ModContainer modContainer) {
        VHConfig.register(modContainer);
        VHBlocks.register(modEventBus);
        VHItems.register(modEventBus);
        VHFluids.register(modEventBus);
        VHFluidTypes.register(modEventBus);
        VHEntityTypes.register(modEventBus);
        VHSoundEvents.register(modEventBus);
        VHParticleTypes.register(modEventBus);
        VHMobEffects.register(modEventBus);
        VHMenuTypes.register(modEventBus);
        VHRecipeTypes.register(modEventBus);
        VHRecipeSerializers.register(modEventBus);
        VHBlockEntityTypes.register(modEventBus);
        VHCapabilities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(DataGeneratorEvents::onGatherData);

        NeoForge.EVENT_BUS.register(ServerEvents.class);
        NeoForge.EVENT_BUS.register(TemperatureEvents.class);
        NeoForge.EVENT_BUS.register(CorruptionEvents.class);
        NeoForge.EVENT_BUS.register(DimensionEvents.class);
    }
}
```

---

## 3. CORE MOD CONSTANTS & CONFIGURATION

### 3.1 VHConfig

Use NeoForge's `ModConfigSpec` builder. Config file: `verdigrishive-common.toml`.

All values must have min/max bounds and a comment string.

**Temperature section:**
```toml
[temperature]
  # How many ticks between each heat damage tick in the canopy (daytime, above Y+180). Default 20 (1 second).
  canopy_heat_tick_interval = 20
  # Damage per heat tick (in half-hearts). Default 1.
  canopy_heat_damage = 1.0
  # Y level above which heat damage begins.
  canopy_heat_y_threshold = 180
  # How many ticks between each cold damage tick in the fleshfold (nighttime, below Y-100). Default 40.
  fleshfold_cold_tick_interval = 40
  # Damage per cold tick (in half-hearts). Default 1.
  fleshfold_cold_damage = 1.0
  # Y level below which cold damage begins.
  fleshfold_cold_y_threshold = -100
  # Number of temperature units gained per tick in extreme zones. Range 1-100.
  temp_change_rate = 5
  # Maximum temperature accumulation before damage (0-1000 scale). Default 600.
  temp_max = 1000
  # Danger threshold where HUD turns red. Default 600.
  temp_danger_threshold = 600
```

**Day/Night cycle section:**
```toml
[cycle]
  # Multiplier on vanilla day/night cycle length. Default 8 (makes one day = 8 vanilla days = 160000 ticks).
  day_length_multiplier = 8
  # Fraction of the full day that is "twilight" (safe zone). Default 0.08 (8% of cycle = ~12800 ticks of safety at each transition).
  twilight_fraction = 0.08
```

**Corruption section:**
```toml
[corruption]
  # Chance (1/N) per block per night tick to spread. Default 200.
  spread_chance = 200
  # Light level at or above which corruption cannot spread. Default 8.
  spread_light_block_threshold = 8
  # Maximum number of corruption spread checks per chunk per tick. Default 16. Raises this if spread feels too slow.
  max_checks_per_chunk_tick = 16
  # Whether corruption can spread in the overworld (for the solarpunk biome edge case). Default false.
  spread_in_overworld = false
```

**Create integration section:**
```toml
[create]
  # Whether to enable Create mod integration features. Auto-detected if Create is loaded.
  enable_create_integration = true
  # RPM generated by a Marrow Engine per fuel unit. Default 64.
  marrow_engine_rpm = 64
  # Heat units output per tick from a solar collector.
  solar_collector_heat_output = 15
```

### 3.2 VHConstants

```java
public final class VHConstants {
    // Dimension key
    public static final ResourceKey<Level> VERDIGRIS_HIVE_LEVEL =
        ResourceKey.create(Registries.DIMENSION, new ResourceLocation(VerdigrisHive.MOD_ID, "verdigris_hive"));

    // Noise settings key  
    public static final ResourceKey<NoiseGeneratorSettings> VERDIGRIS_HIVE_NOISE =
        ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation(VerdigrisHive.MOD_ID, "verdigris_hive"));

    // Dimension Y bounds
    public static final int DIM_MIN_Y = -512;
    public static final int DIM_MAX_Y = 384;
    public static final int DIM_HEIGHT = DIM_MAX_Y - DIM_MIN_Y; // 896

    // Zone Y thresholds (must match config defaults; config overrides these at runtime)
    public static final int ZONE_CANOPY_TOP = 384;
    public static final int ZONE_CANOPY_BOTTOM = 200;
    public static final int ZONE_STACK_BOTTOM = 60;
    public static final int ZONE_PLUMBING_BOTTOM = -100;
    public static final int ZONE_FLESHFOLD_BOTTOM = -512;

    // Temperature scale
    public static final int TEMP_NEUTRAL = 500;   // middle of 0-1000 scale
    public static final int TEMP_HOT_DANGER = 600;
    public static final int TEMP_COLD_DANGER = 400;
    public static final int TEMP_HOT_MAX = 1000;
    public static final int TEMP_COLD_MIN = 0;
}
```

---

## 4. BLOCKS — FULL SPECIFICATION

### 4.1 Block Registration Pattern

Every block must:
1. Have a matching `BlockItem` registered in `VHItems` (unless explicitly decoration-only with no item form, e.g., Corruption Growth at stage 3+).
2. Have a `BlockStateProvider`-generated blockstate file.
3. Have a `BlockModelProvider`-generated model.
4. Have `BlockLootSubProvider`-generated loot.
5. Have `BlockTagsProvider` entries for `minecraft:mineable/pickaxe` or `minecraft:mineable/axe` as appropriate.
6. Have a hardness and blast resistance value. Nothing is indestructible (blast resistance ≤ 3600.0f, the obsidian value). Flesh blocks resist axes, not pickaxes.

### 4.2 Canopy & Solarpunk Blocks

| Registry Name | Class Base | Properties | Notes |
|---|---|---|---|
| `solar_glass` | `GlassBlock` | strength(0.3f, 1.5f), noOcclusion | Custom texture: iridescent teal-gold. Used in Bosco Spire windows. |
| `solar_glass_pane` | `IronBarsBlock` | as above | Pane variant. |
| `verdigris_copper` | `Block` | strength(3f,6f), requiresCorrectTool | Oxidized copper aesthetic; unique texture. NOT the vanilla weathered copper. |
| `verdigris_copper_slab` | `SlabBlock` | same | — |
| `verdigris_copper_stairs` | `StairBlock` | same | — |
| `verdigris_copper_pillar` | `RotatedPillarBlock` | same | Pillar variant for Art Nouveau columns. |
| `art_nouveau_iron` | `Block` | strength(4f,8f), requiresCorrectTool | Decorative ironwork. Curvilinear texture. |
| `art_nouveau_iron_bars` | `IronBarsBlock` | same | Used for balcony railings. |
| `wither_vine_block` | `Block` | strength(0.2f, 0.4f), noCollision | Dead/brown vine. Placed by worldgen on canopy surfaces. Falls like gravel if block beneath broken. |
| `wither_vine_plant` | `BushBlock` | noCollision | Bottom plant form. |
| `solar_panel` | `HorizontalDirectionalBlock` | strength(2f,4f) | Produces "solar charge" (see Items). Requires sky access. If Create loaded: produces rotational power when charged. |
| `solar_panel_frame` | `Block` | strength(3f,6f) | The brass-frame support block for solar panels. |
| `living_moss` | `Block` | strength(0.5f,1f) | Spreads to adjacent verdigris_copper when light > 9. Slows movement. |
| `wilted_leaves` | `LeavesBlock` | — | Ochre leaf block. Does not decay. |
| `canopy_soil` | `DirtBlock` subclass | — | Supports vertical farms. Grows crops faster than farmland. |
| `hydroponic_tray` | `BlockEntity` block | strength(1f, 3f) | Grows one crop continuously. Outputs to adjacent hoppers/Create belts. |

### 4.3 Stack / Kowloon Blocks

| Registry Name | Class Base | Properties | Notes |
|---|---|---|---|
| `stacked_concrete` | `Block` | strength(2f, 12f), requiresCorrectTool | Dirty grey concrete variant. Stain texture. |
| `stacked_concrete_slab` | `SlabBlock` | same | — |
| `stacked_concrete_stairs` | `StairBlock` | same | — |
| `corrugated_iron` | `Block` | strength(2f, 6f), requiresCorrectTool | Roofing sheet. Directional model. |
| `hanging_wire` | `Block` | strength(0.1f, 0.5f), noCollision | Decorative. Full block BoundingBox but hollow model. Conducts electricity if Create: New Age loaded. |
| `hanging_laundry` | `Block` | strength(0.1f, 0.1f), noCollision | Purely decorative. 4 random textile textures via variant blockstate. |
| `junction_box` | `BlockEntity` block | strength(1f, 2f) | If Create: New Age loaded: acts as wire junction. Otherwise: decorative, drops scrap metal. |
| `water_tap` | `Block` | strength(1f, 2f) | Right-click fills a Bottle with water. |
| `illegal_stair_block` | `StairBlock` | strength(1f, 3f) | Slightly different texture to vanilla stairs; placed in cramped stairwells. |
| `neon_sign` | `BlockEntity` block | strength(0.5f, 1f), lightLevel=12 | Emissive. Text is baked into 4 preset textures (randomized on placement). |

### 4.4 Plumbing / Industrial Blocks

| Registry Name | Class Base | Properties | Notes |
|---|---|---|---|
| `rusted_andesite_alloy` | `Block` | strength(3f, 6f), requiresCorrectTool | Drops andesite alloy (if Create loaded) or iron nuggets. |
| `rusted_brass` | `Block` | strength(3f, 6f), requiresCorrectTool | Drops brass ingot (if Create) or gold nugget. |
| `sap_pipe` | `BaseEntityBlock` | strength(2f, 4f) | Directional pipe block. Connects to adjacent sap pipes and fluid reservoirs. Does NOT use Create fluid pipes — this is a standalone fluid conduit. |
| `sap_reservoir` | `BaseEntityBlock` | strength(2f, 4f) | 8-bucket fluid tank for Sap. Connects to sap_pipe. |
| `brine_vat` | `BaseEntityBlock` | strength(2f, 4f) | 8-bucket fluid tank for Brine. |
| `broken_millstone` | `Block` | strength(3f, 6f) | Non-functional. Drops andesite alloy + stone. |
| `snapped_belt_housing` | `Block` | strength(2f, 4f) | Non-functional. Drops iron nuggets. |
| `heat_conduit` | `BaseEntityBlock` | strength(2f, 5f) | Carries thermal units from Solar Collectors downward. If Create: New Age loaded: integrates with its heat system. |
| `pressure_valve` | `BaseEntityBlock` | strength(2f, 4f) | Controls flow between sap/brine pipes. Outputs a comparator signal proportional to fluid pressure. |

### 4.5 Fleshfold / Biopunk Blocks

All flesh blocks are tagged `verdigrishive:flesh_blocks`. Harvest tool: axe. Blast resistance: 3f. Do NOT drop silk touch variants — they always drop their base item form. All flesh blocks emit a subtle wet squelch sound on walk. Sound defined in `sounds.json`.

| Registry Name | Class Base | Properties | Notes |
|---|---|---|---|
| `flesh_wall` | `Block` | strength(2f, 3f) | Main structural block of the Fleshfold. Ochre/muscle texture. |
| `flesh_floor` | `SlabBlock` subclass | strength(2f, 3f) | Ribbed underside texture. |
| `bone_pillar` | `RotatedPillarBlock` | strength(3f, 4f) | Load-bearing column with bone texture. |
| `bone_arch` | `Block` | strength(3f, 4f) | Custom model: curved arch shape (non-cube). |
| `ribbed_corridor` | `Block` | strength(2f, 3f) | Esophageal ribbed texture. Horizontal directional. |
| `visceral_moss` | `Block` | strength(0.2f, 0.5f) | Dark olive green. Slow player. Corruption adjacent spreads faster through it. |
| `corruption_block` | `Block` | strength(1f, 1f) | Spreads. Emits corruption gas cloud particles. Causes wither effect level 1 to standing entities. |
| `corruption_growth` | `GrowingPlantBodyBlock` subclass | strength(0.1f,0.1f), noCollision | The spreading organism. Stage 0-3 blockstates. Stage 3: no item drop, must be destroyed with fire or marrow acid. |
| `pulsing_wall` | `BaseEntityBlock` | strength(2f, 3f) | Animates via BlockEntity tick: scales hitbox ±0.1 blocks on a 6-second sinusoidal cycle. Blocks passage when "exhaled" (hitbox at max). |
| `toothy_door` | `DoorBlock` subclass | strength(2f, 4f) | Opens by right-click (like vanilla door) but plays a growl sound. Model is a ring of teeth. |
| `marrow_ore` | `DropExperienceBlock` | strength(3f, 5f), requiresCorrectTool | Drops `raw_marrow` item. Requires iron-tier tool minimum. |
| `ichor_seep` | `LiquidBlock` | — | Source block: ichor fluid. Placed by worldgen. Deals 1 damage/2 ticks on contact without Ichor Resistance effect. |
| `amniotic_fluid_block` | `LiquidBlock` | — | Source: amniotic fluid. Deals no damage but applies Slowness IV while submerged. |
| `bioluminescent_sac` | `Block` | strength(0.5f,1f), lightLevel=11 | Pink/purple light source. Drops `bioluminescent_gel` item. |
| `lung_block` | `BaseEntityBlock` | strength(2f,3f) | Breathing animation via BlockEntity. Generates Corruption Gas particles at night. Companion to `pulsing_wall`. |
| `marrow_sink_floor` | `Block` | strength(2f,4f) | Dark bone-dust floor texture. Makes a crunch sound on walk. |

### 4.6 Portal Block

| Registry Name | Class Base | Properties | Notes |
|---|---|---|---|
| `verdigris_portal` | `Block` | strength(-1f, 3600f), noOcclusion, lightLevel=11 | The dimension portal frame. Custom portal frame block using `VerdigrisPortalShape` helper class (mirrors `NetherPortal` architecture). Active portal block emits particle effect. Non-breakable when active. |
| `verdigris_portal_frame` | `Block` | strength(50f, 1200f), requiresCorrectTool | The ritual frame material. Must be verdigris_copper with one sun_shard embedded. |

---

## 5. ITEMS — FULL SPECIFICATION

### 5.1 Item Registration Pattern

Every item:
1. Is registered via `VHItems.ITEMS` (a `DeferredRegister.Items`).
2. Has an `ItemModelProvider`-generated model.
3. Has a tooltip defined in `en_us.json` lang file under `item.verdigrishive.<name>.tooltip`.
4. Has appropriate `ItemTagsProvider` tags.

### 5.2 Resource Items (Raw Materials)

| Registry Name | Stack Size | Notes |
|---|---|---|
| `sun_shard` | 64 | Dropped by Sunblighters; crafted from solar_panel + glass. Used in portal frame and sun-charging. |
| `raw_marrow` | 64 | Dropped by marrow_ore. Smelts into `refined_marrow`. |
| `refined_marrow` | 64 | Smelting output. Crafting input for Marrow Engine and biopunk tools. |
| `ichor_bucket` | 1 | Filled bucket for Ichor fluid. |
| `amniotic_bucket` | 1 | Filled bucket for Amniotic fluid. |
| `sap_bucket` | 1 | Filled bucket for Sap fluid. |
| `brine_bucket` | 1 | Filled bucket for Brine fluid. |
| `bioluminescent_gel` | 64 | Dropped by bioluminescent_sac. Crafting ingredient for light-items. |
| `corruption_sample` | 16 | Dropped by Corruption Wheezers. Used in Corruption-resistance potions. |
| `brass_photovoltaic` | 64 | Crafted from solar_panel + brass (if Create loaded, brass_ingot; if not, gold_ingot). Powers sun-charging recipes. |
| `marrow_fuel_pellet` | 64 | Crafted from refined_marrow. Used as Marrow Engine fuel. Burn time: 1600 ticks. |
| `scrap_metal` | 64 | Drops from junction_box, broken_millstone, snapped_belt_housing. Used in mid-tier crafting. |
| `ichor_crystal` | 64 | Crystallized ichor. Crafted from ichor_bucket + refined_marrow. Core endgame ingredient. |
| `verdigris_key` | 1 | Boss drop from Verdigris Saint. Opens the Verdigris Throne arena gate. |

### 5.3 Tools & Equipment

All tools use the vanilla `Tier` system. Define one custom Tier:

**`MARROW_TIER`:**
- Level: 3 (iron equivalent)
- Uses: 1500
- Speed: 8.0f
- Damage: 3.0f
- Enchantability: 9
- Repair ingredient: `refined_marrow`
- Tag: `verdigrishive:marrow_tier_materials`

| Registry Name | Class | Notes |
|---|---|---|
| `marrow_sword` | `SwordItem` (MARROW_TIER) | +3 damage, +0.5 attack speed vs flesh_blocks mobs. Deals extra damage to limb-segmented mobs when targeting a limb slot. |
| `marrow_pickaxe` | `PickaxeItem` (MARROW_TIER) | — |
| `marrow_axe` | `AxeItem` (MARROW_TIER) | +50% damage to all flesh_block-type blocks. |
| `marrow_shovel` | `ShovelItem` (MARROW_TIER) | — |
| `brass_parasol` | `Item` (custom) | Equip in offhand. Reduces heat damage by 80% when held. Durability: 256. |
| `thermometer` | `Item` (custom) | Right-click: shows current temperature value in chat + screen title. No durability. |
| `corruption_ward` | `Item` (custom) | Placed item that becomes a block (→ `corruption_ward_block`). Creates a 7-radius zone where corruption cannot spread. Consumes 1 ichor_crystal on placement. |
| `sun_flask` | `Item` (custom) | Bottle that can be "sun-charged" by holding in canopy at noon for 30 seconds. When charged, right-click applies 60 seconds of Fire Resistance + Haste I. |
| `marrow_compass` | `CompassItem` subclass | Points toward nearest Marker Spire. Crafted from refined_marrow + compass. |

### 5.4 Armor

Define one armor material: `VERDIGRIS_ARMOR`. Material stats:
- Defense per slot: 2 / 5 / 6 / 2 (total 15; iron = 15)
- Toughness: 1.0
- Knockback resistance: 0.0
- Repair ingredient: `refined_marrow`
- Enchantability: 9
- Sound: `verdigrishive:item.armor.equip_verdigris`

| Registry Name | Slot | Bonus |
|---|---|---|
| `verdigris_helmet` | HEAD | +15% heat resistance (reduces heat tick damage by 15%) |
| `verdigris_chestplate` | CHEST | — |
| `verdigris_leggings` | LEGS | +10% cold resistance |
| `verdigris_boots` | FEET | Immunity to ichor fluid damage while equipped |

**Full set bonus (server-side tick check when all 4 equipped):** +30% total temperature resistance (additive with individual bonuses), grants Corruption Resistance effect permanently while wearing full set.

### 5.5 Special Items

| Registry Name | Class | Behaviour |
|---|---|---|
| `portal_igniter` | `Item` (custom) | Right-click on verdigris_portal_frame: runs `VerdigrisPortalShape.trySpawnPortal()`. Consumed on success. Crafted from sun_shard + flint_and_steel recipe. |
| `marrow_engine_core` | `Item` | Placed into Marrow Engine multiblock. Not a standalone block item. |
| `guidebook` | `Item` | Right-click: opens Patchouli (soft dep) guidebook. If Patchouli not loaded: opens a simple custom screen with dimension overview text. |

---

## 6. FLUIDS — FULL SPECIFICATION

### 6.1 Fluid Pattern

Each fluid needs: a `FluidType`, a `ForgeFlowingFluid.Source`, a `ForgeFlowingFluid.Flowing`, a source block, a flowing block, a bucket item, and a `FluidType` with textures.

Use NeoForge's `FluidType.Properties` builder for each.

### 6.2 Sap

- **Purpose:** Produced by Vertical Farm Ziggurats. Fuel for mid-tier crafting. If Create loaded: valid fluid for Create pipes.
- **Color (FluidType tint):** Amber, `0xFFBB6600`
- **Viscosity:** 2000 (slightly thick, like honey-lite)
- **Density:** 1100 (slightly denser than water = 1000)
- **Temperature:** 300 (room temp in Kelvin-coded units)
- **Damage on contact:** None
- **Create integration:** Valid in Create fluid pipes. Has a `ProcessingRecipe` for Sap → Sugar + Wax.
- **Blocks:** `sap_fluid_source`, `sap_fluid_flowing`
- **Bucket:** `sap_bucket`

### 6.3 Brine

- **Purpose:** Byproduct of Sap processing. Used in Marrow Engine as coolant. Crafting ingredient for anti-corruption potion.
- **Color:** Dark grey-green, `0xFF4A6B4A`
- **Viscosity:** 800
- **Density:** 1200
- **Damage on contact:** None
- **Blocks:** `brine_fluid_source`, `brine_fluid_flowing`
- **Bucket:** `brine_bucket`

### 6.4 Ichor

- **Purpose:** Primary biopunk fluid. Damages non-resistant players. Used in endgame crafting and the Marrow Engine as a high-output fuel when combined with solar charge.
- **Color:** Deep red-black, `0xCC880000`
- **Viscosity:** 3000 (very thick)
- **Density:** 1400
- **Damage on contact:** 1 HP per 2 ticks, type = `DamageTypes.IN_FIRE` equivalent (create custom `DamageType`: `verdigrishive:ichor_burn`)
- **Blocks:** `ichor_seep` (source), `ichor_flowing`
- **Bucket:** `ichor_bucket`

### 6.5 Amniotic Fluid

- **Purpose:** Found in the Marrow Sink biome. Applies Slowness IV while submerged. Contains bioluminescent organisms → emits faint light (FluidType lightLevel = 3).
- **Color:** Translucent pink-purple, `0x88AA44CC`
- **Viscosity:** 1500
- **Density:** 1050
- **Damage on contact:** None
- **Effects on immersion:** Slowness IV (20 ticks/tick check), Glowing (20 ticks), Blindness I if submerged > 5 seconds
- **Blocks:** `amniotic_fluid_source`, `amniotic_fluid_flowing`
- **Bucket:** `amniotic_bucket`

---

## 7. WORLD GENERATION — THE VERDIGRIS HIVE DIMENSION

### 7.1 Dimension JSON Architecture

The dimension is fully data-driven. All files live in:
`src/main/resources/data/verdigrishive/`

Required files:
```
data/verdigrishive/
├── dimension/
│   └── verdigris_hive.json          ← The dimension definition
├── dimension_type/
│   └── verdigris_hive.json          ← The dimension type (Y bounds, skylight, etc.)
└── worldgen/
    ├── noise_settings/
    │   └── verdigris_hive.json      ← Noise generator settings
    ├── biome/
    │   ├── sunscorch_terraces.json
    │   ├── mossheart_gardens.json
    │   ├── cabling_wards.json
    │   ├── andesite_plumbing.json
    │   ├── fleshfold.json
    │   └── marrow_sink.json
    ├── multi_noise_biome_source_parameter_list/
    │   └── verdigris_hive.json      ← MultiNoise biome placement
    └── noise/
        └── rot_axis.json            ← Custom noise parameter for rot axis
```

### 7.2 Dimension Type JSON

```json
{
  "ultrawarm": false,
  "natural": false,
  "coordinate_scale": 1.0,
  "has_skylight": false,
  "has_ceiling": true,
  "ambient_light": 0.1,
  "fixed_time": null,
  "monster_spawn_light_level": { "type": "minecraft:uniform", "value": { "min_inclusive": 0, "max_inclusive": 7 }},
  "monster_spawn_block_light_limit": 0,
  "piglin_safe": false,
  "bed_works": false,
  "respawn_anchor_works": false,
  "has_raids": false,
  "min_y": -512,
  "height": 896,
  "logical_height": 896,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "verdigrishive:verdigris_hive",
  "create_dragon_fight": false
}
```

`"has_skylight": false` and `"has_ceiling": true` are critical. Skylightless = no vanilla day/night sky rendering. The mod provides its own sky renderer (see §17).

### 7.3 Noise Settings JSON

Key parameters:
```json
{
  "sea_level": -200,
  "disable_mob_generation": false,
  "aquifers_enabled": false,
  "ore_veins_enabled": false,
  "legacy_random_source": false,
  "default_block": { "Name": "verdigrishive:flesh_wall" },
  "default_fluid": { "Name": "verdigrishive:ichor_fluid_source" },
  "noise": {
    "min_y": -512,
    "height": 896,
    "size_horizontal": 1,
    "size_vertical": 2
  },
  "noise_router": { /* see below */ },
  "surface_rule": { /* see below */ },
  "spawn_target": []
}
```

**`noise_router`:** Use the overworld router as a base but override:
- `barrier`: set to a high constant (>1.0) to make the terrain mostly solid — the megastructure is carved out of solid filler
- `initial_density_without_jaggedness`: constant 0.5 (uniform density)

**Strategy:** Do NOT try to generate the entire city in noise JSON. Noise generates a solid filler block (flesh_wall below Y-100, stacked_concrete above). The megastructure is generated entirely by jigsaw structures and a custom `ChunkGenerator` subclass.

### 7.4 Custom Chunk Generator: `HiveChunkGenerator`

Extend `NoiseBasedChunkGenerator`. Override `fillFromNoise()` to:

1. Below Y −100: fill with `flesh_wall` as the base block.
2. Y −100 to Y +60: fill with `stacked_concrete`.
3. Y +60 to Y +200: fill with `stacked_concrete` fading to `verdigris_copper` via a density noise.
4. Above Y +200: fill with `verdigris_copper` fading to `solar_glass`.
5. Above Y +350: air (open sky zone).
6. Apply a second "void carver" pass that cuts corridors, rooms, and shafts through the solid fill. This is NOT Minecraft's cave carver — it is a custom `VoidCarver extends WorldCarver<CarverConfiguration>` that:
   - Cuts horizontal "floor-plate" rooms every 8–12 blocks of Y
   - Cuts vertical shafts (4×4 to 8×8 wide) connecting floors
   - Cuts irregular "blob" rooms for structure attachment points
   - Uses three noise functions: `room_selector` (which Y bands have rooms), `shaft_density` (where vertical shafts go), `room_size` (radius of blob rooms)

**MANDATORY:** `HiveChunkGenerator` must be registered as a `Codec<HiveChunkGenerator>` via `Registry.register(BuiltInRegistries.CHUNK_GENERATOR, "verdigrishive:hive", HiveChunkGenerator.CODEC)` so it serializes to/from JSON and works in multiplayer.

### 7.5 Biome Source: MultiNoise

The dimension uses `MultiNoiseBiomeSource` with a custom parameter list. Biome placement is driven by two axes:
- **Y (depth):** controls which vertical zone the biome belongs to. Use the `depth` noise parameter.
- **Rot axis (weirdness):** controls how far the solarpunk→biopunk transition has occurred at that X/Z column.

**Biome MultiNoise entries (approximate):**

| Biome | Depth range | Weirdness range | Notes |
|---|---|---|---|
| `sunscorch_terraces` | −1.0 to −0.6 | 0.0 to 1.0 | Top zone, any rot level |
| `mossheart_gardens` | −1.0 to −0.7 | −1.0 to −0.5 | Top zone, low rot (healthy) |
| `cabling_wards` | −0.4 to 0.1 | −0.5 to 0.5 | Mid stack |
| `andesite_plumbing` | 0.1 to 0.5 | −1.0 to 1.0 | Lower mid |
| `fleshfold` | 0.5 to 0.9 | 0.0 to 1.0 | Lower zone, high rot |
| `marrow_sink` | 0.8 to 1.0 | −1.0 to 1.0 | Deepest zone |

**Note:** MultiNoise depth values in 1.21.1 do NOT directly correspond to Y levels. They are interpolated through the noise router. Calibrate empirically in-game and adjust JSON values until zone boundaries match VHConstants Y thresholds within ±16 blocks.

### 7.6 Per-Biome JSON Requirements

Each biome JSON must specify:

**`sunscorch_terraces.json`:**
```json
{
  "precipitation": "none",
  "temperature": 2.0,
  "downfall": 0.0,
  "effects": {
    "fog_color": 16763904,
    "water_color": 4159204,
    "water_fog_color": 329011,
    "sky_color": 16773120,
    "grass_color": 10795828,
    "foliage_color": 10053171,
    "ambient_sound": "verdigrishive:ambient.canopy.wind",
    "additions_sound": { "sound": "verdigrishive:ambient.canopy.glass_creak", "tick_chance": 0.006 },
    "mood_sound": { "sound": "verdigrishive:mood.canopy.distant_windmill", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0 }
  },
  "spawners": {
    "monster": [
      { "type": "verdigrishive:sunblighter", "weight": 100, "minCount": 2, "maxCount": 4 },
      { "type": "verdigrishive:wither_vine_mob", "weight": 80, "minCount": 1, "maxCount": 3 }
    ],
    "creature": []
  },
  "spawn_costs": {},
  "carvers": {},
  "features": [
    [], [], [], [], [], [],
    ["verdigrishive:placed/wither_vine_patch", "verdigrishive:placed/wilted_leaves_canopy"],
    [],
    ["verdigrishive:placed/solar_panel_cluster"]
  ]
}
```

Repeat this level of detail for all 6 biomes. Each biome must have:
- Unique `sky_color`, `fog_color`, `grass_color`, `foliage_color`
- At least one `ambient_sound`
- At least one `mood_sound`
- At least one `additions_sound`
- Appropriate `spawners` entries
- Appropriate `features` at the correct decoration stage index

### 7.7 Custom Sky Renderer

The dimension has `"has_skylight": false`, so vanilla sky rendering does nothing. Implement a custom sky renderer registered via `ClientPayloadHandler` + `DimensionSpecialEffects` subclass:

Class: `VHSkyRenderer implements SkyRenderingPlugin` (NeoForge 1.21.1 rendering hook).

Behaviour:
- Render a procedural sky dome. Not a skybox texture.
- Canopy view (player Y > 100): during daytime — washed-out white-yellow gradient; during twilight — amber/gold gradient; during nighttime — deep indigo with no stars (ceiling blocks sky).
- Below Y 60: render the ceiling of the stack above as a texture (solid dark with light spots from neon_signs).
- Fleshfold (Y < −100): no sky rendering at all. Render only fog.
- Sky dome must respond to the custom day/night tick from `VHDimensionTimeManager`.

---

## 8. WORLD GENERATION — SOLARPUNK OVERWORLD BIOME

### 8.1 Biome: `verdant_arcology`

The overworld biome is registered in `VHBiomes` and injected into the overworld via NeoForge's `BiomeModifier` system.

**BiomeModifier JSON** (`data/verdigrishive/neoforge/biome_modifier/add_verdant_arcology.json`):
```json
{
  "type": "neoforge:add_spawns",
  "biomes": "#verdigrishive:is_verdant_arcology",
  "spawners": [ ... ]
}
```

Actually inject the biome via a custom `BiomeSource` injector registered on `BiomeLoadingEvent`... wait: in 1.21.1 NeoForge the correct approach is the `BiomeModifiers` system with a JSON or via `LevelStem` injection.

**CORRECT APPROACH for 1.21.1:** Add `verdant_arcology` to the overworld biome list via:
1. Register the biome in `VHBiomes` with full `BiomeGenerationSettings` and `MobSpawnSettings`.
2. In `data/verdigrishive/worldgen/multi_noise_biome_source_parameter_list/overworld.json`, use the NeoForge `neoforge:injected_biomes` mechanism OR supply a `BiomeModifier` of type `neoforge:add_biome_to_overworld_surface` (check NeoForge 1.21.1 changelog for exact type string — verify against NeoForge's BiomeModifiers registry).
3. Set noise parameters: temperature ~0.8, humidity ~0.9, continentalness 0.1–0.7, erosion −0.5 to 0.0, weirdness 0.0–0.5. This places it in temperate lowland areas near coasts — analogous to existing meadow/plains placements.

**`verdant_arcology` Biome Properties:**
```json
{
  "precipitation": "rain",
  "temperature": 0.8,
  "downfall": 0.9,
  "effects": {
    "fog_color": 12638463,
    "water_color": 4159204,
    "water_fog_color": 329011,
    "sky_color": 7972607,
    "grass_color": 5159473,
    "foliage_color": 3866368,
    "ambient_sound": "verdigrishive:ambient.arcology.wind",
    "additions_sound": { "sound": "verdigrishive:ambient.arcology.water_fountain", "tick_chance": 0.004 },
    "mood_sound": { "sound": "verdigrishive:mood.arcology.distant_birds", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0 },
    "music": { "sound": "verdigrishive:music.arcology.theme", "min_delay": 12000, "max_delay": 24000, "replace_current_music": false }
  }
}
```

**Visual identity:**
- Grass color: `5159473` (bright emerald)
- Foliage: `3866368` (deep lush green)
- Sky: `7972607` (clear soft blue, brighter than vanilla plains)
- Particle: `verdigrishive:arcology_spore` — slow-floating white motes, spawned at rate 0.025 per tick from tall_grass surfaces

**Terrain generation:**
- Flat to gently rolling (use vanilla erosion target −0.3 to 0.0).
- Large shallow freshwater lakes (configure via `LakeFeature` placed features).
- Tall biome-specific trees: `arcology_tree` (see §8.3).
- No hostile mob spawns during daytime. At night, standard vanilla mob spawning.

**Structures in this biome:**
- `arcology_tower` (the main Bosco-Verticale-style tower; 1 per ~64 chunks)
- `solar_garden` (small automated farm patch; 1 per ~16 chunks)
- `clean_water_cistern` (waterwheel-fed fountain; 1 per ~8 chunks)

### 8.2 Arcology Tower Structure

A jigsaw structure. Template pools:
- `arcology_tower/base` — foundation + ground floor (1 piece, mandatory)
- `arcology_tower/mid_floor` — residential/garden floors (5–8 pieces stacked, random selection)
- `arcology_tower/top` — glass dome crown (1 piece, terminal)
- `arcology_tower/balcony_addon` — addon pieces that attach to the side face of any mid_floor

Tower height: 40–80 blocks. Width: 9×9 footprint.

Each mid_floor piece contains:
- A `hydroponic_tray` block entity
- At least 2 `solar_panel` blocks
- Vines, planters, `wither_vine_block` in healthy (green) variant
- If Create loaded: a functional water wheel or windmill bearing as decoration (non-functional unless player repairs it)

### 8.3 Arcology Tree

Custom `TreeFeature` configured via `TreeConfiguration`. Not a jigsaw structure.
- Trunk: `verdigris_copper_pillar` (the metallic tower aesthetic)
- Leaves: `wither_vine_block` + `living_moss` at the crown (not wilted; this is the healthy overworld)
- Trunk height: 8–14 blocks
- Crown spread: 5–7 radius
- Place 1–3 solar panels at the top of the trunk, facing random horizontal directions

Generate using `TreeFeature` with a `BlobFoliagePlacer` subclass. Register as a `ConfiguredFeature<TreeConfiguration>` and a `PlacedFeature` using `PlacementModifier` list: `[InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING), BiomeFilter.biome()]`.

---

## 9. STRUCTURES — JIGSAW & TEMPLATE POOLS

### 9.1 General Structure Architecture

All structures use the Jigsaw system. Each structure:
1. Has a `Structure` subclass (for custom parameters) or uses `JigsawStructure` directly if no custom parameters needed.
2. Has a `StructureType<?>` registered in `VHStructureTypes`.
3. Has a `StructurePlacement` (use `RandomSpreadStructurePlacement` for most).
4. Has a `StructureSet` (in `data/verdigrishive/worldgen/structure_set/`).
5. Has NBT template files in `data/verdigrishive/structures/`.

**CRITICAL — Vertical Jigsaw Limit Workaround:**
As noted in the brainstorm, vanilla jigsaw has a `max_distance_from_center.vertical` hard limit of ~80 blocks. For the megastructure, do NOT rely on a single jigsaw start. Use the **anchor chain pattern**:
- The main jigsaw structure generates a "hub" piece at a set Y.
- The hub piece contains jigsaw blocks pointing up AND down.
- The upward-pointing block's `target` pool is `anchor_chain_up`, which generates a new `StructureStart` call (via a custom `JigsawAnchorFeature` that fires a second jigsaw generation call at the target Y position, passing context via a tag on the chunk).
- Implement `VHJigsawAnchor` — a `StructureProcessor` subclass that, when placed, schedules a new jigsaw expansion start at its location in the current chunk's level. This is done via a queued `ServerLevel.getChunkSource().getGenerator()` call in `StructurePiece.postProcess()`.

This is the primary technical risk in the project. Allocate 3+ weeks to this system. Fallback: if anchor chaining is not achievable within scope, cap the structure at 80 blocks vertical and repeat the structure start every 80 blocks via a `PlacedFeature` that triggers new structure starts at intervals.

### 9.2 Structure: The Bosco Spire (Dimension)

- **Start pool:** `verdigrishive:structures/bosco_spire/start`
- **Size:** `max_distance_from_center` = 60 (horizontal), 80 (vertical); max 8 pieces
- **Placement:** `RandomSpread`, average separation 24 chunks, salt unique
- **Template pieces (NBT files required):**
  - `bosco_spire/base_1.nbt` — 9×9 ground level, windmill bearing platform
  - `bosco_spire/mid_garden_a.nbt` through `..._d.nbt` — 4 variants of 9×12 garden floor
  - `bosco_spire/top_dome.nbt` — terminal piece with solar glass dome
  - `bosco_spire/balcony_left.nbt`, `bosco_spire/balcony_right.nbt` — side-attached addons
- **Loot tables:** `verdigrishive:chests/bosco_spire_locker` — contains sun_shard, wither_vine_block, solar_panel, and mid-tier items
- **Create integration:** Each mid_garden piece contains one `windmill_bearing` (if Create loaded) or `art_nouveau_iron_bars` frame (if not). The bearing is in a "broken" state — player can wrench-repair it.
- **Mandatory contents:** At least one `solar_panel`, one `hydroponic_tray`, and one `water_tap` per spire.

### 9.3 Structure: Andesite Refinery (Dimension — Plumbing Zone)

- **Start pool:** `verdigrishive:structures/andesite_refinery/start`
- **Size:** horizontal 40, vertical 30; max 12 pieces (wide and squat)
- **Placement:** `RandomSpread` in `andesite_plumbing` biome only
- **Template pieces:**
  - `andesite_refinery/main_hall.nbt` — the central 13×9×13 factory floor
  - `andesite_refinery/boiler_room.nbt` — contains brine_vat + sap_reservoir
  - `andesite_refinery/conveyor_corridor.nbt` — long corridor with decorative belt housing
  - `andesite_refinery/crane_bay.nbt` — high-ceiling room with overhead gantry tracks
  - `andesite_refinery/storage_annex.nbt` — chest room with loot
- **Loot tables:** `verdigrishive:chests/andesite_refinery` — brass, andesite alloy (if Create), scrap_metal, sap_bucket, brine_bucket, marrow_fuel_pellet
- **Create integration (mandatory when loaded):** `main_hall` piece contains: 1 broken mechanical mixer, 1 broken belt sequence (3 depots with missing belt), 1 non-functional water wheel. If Create not loaded: replace with rusted_andesite_alloy decorative blocks.

### 9.4 Structure: The Heliodome (Dimension — Canopy Apex)

- **Type:** Fixed placement. One per dimension at X=0, Z=0, Y=320. NOT jigsaw-spawned.
- **Template:** Single NBT file `heliodome/main.nbt` — 33×33×28 geodesic dome. Must be handcrafted in-game.
- **Required contents:**
  - Central `verdigris_portal_frame` arrangement (inner shrine for a second portal deeper into the Fleshfold — optional late-game portal)
  - 8 chest blocks with `verdigrishive:chests/heliodome` loot table
  - 1 `helio_warden` spawn pad (Entity spawn via `EntitySpawnPlacementRegistry` or direct NBT entity tag in template)
  - Solar glass dome roof, fully enclosed
  - A boss music trigger zone (see §16)
- **Implementation:** Spawn via `ChunkGenerator.createStructures()` override in `HiveChunkGenerator` — check if chunk contains (0,0), place the Heliodome structure start unconditionally.

### 9.5 Structure: The Lung Atrium (Dimension — Fleshfold)

- **Start pool:** `verdigrishive:structures/lung_atrium/start`
- **Size:** horizontal 20, vertical 25; max 5 pieces
- **Template pieces:**
  - `lung_atrium/entry_corridor.nbt`
  - `lung_atrium/atrium_main.nbt` — 15×20×15 central chamber. Contains `pulsing_wall` blocks on all four sides. Contains 2 `lung_block` blocks in the ceiling. Contains the mini-boss spawn pad.
  - `lung_atrium/side_passage_a.nbt`, `..._b.nbt` — optional side chambers
- **Special mechanic:** The `atrium_main` piece uses a `StructureProcessor` (`LungAtriumActivationProcessor`) that, when placed, registers the chunk in a server-side `LungAtriumManager`. The Manager ticks all registered atriums every 120 ticks, cycling `pulsing_wall` blocks between their open/closed blockstate variants using `level.setBlock()`.
- **Boss spawn:** The Lung Warden (see §10) spawns at the center of `atrium_main` on first player entry (triggered by `BlockInteractEvent` equivalent — `PlayerTickEvent` proximity check within 5 blocks of the atrium center blockpos, stored per-chunk in `LungAtriumManager`).

### 9.6 Structure: The Marker Spire (Dimension — Deepest Fleshfold)

- **Type:** Single per dimension. Placed at the lowest accessible Y band (Y ~ −450), random X/Z within 512 blocks of origin.
- **Template:** `marker_spire/main.nbt` — a 7×7×40 obelisk of bone_pillar + corruption_block, topped with a `bioluminescent_sac` beacon cluster.
- **Mechanic:** While the Marker Spire stands, all `corruption_block` spread checks in loaded chunks within 256 blocks of the spire run at 2× the config rate (double spread chance). When the spire is destroyed (all bone_pillar blocks in its core broken), a `StructureDestroyedEvent` (custom, fired from `BlockDestroyEvent` handler in `CorruptionEvents`) removes the spire from the dimension's `SavedData` registry and permanently halves corruption spread in the affected chunks.
- **Implementation:** Use `DimensionDataStorage` (a `SavedData` subclass: `VHDimensionSavedData`) to persist: spire block position, spire destroyed flag, per-chunk corruption state.

### 9.7 Structure: The Verdigris Throne (Dimension — Rot Line)

- **Type:** Single per dimension. Fixed at Y=0, random X/Z.
- **Template:** `verdigris_throne/main.nbt` — 21×21×15 arena where flesh meets glass. Half the blocks are verdigris_copper; half are flesh_wall. Contains a ritual circle of bone_arch and solar_glass.
- **Boss gate:** A `verdigris_key`-locked `toothy_door` blocks the entrance. Right-clicking with a `verdigris_key` in the main hand opens it and consumes the key.

### 9.8 Structure: The Cabling Slums (Dimension — Stack)

- **Type:** Jigsaw. The most common structure. Functions as the primary ambient worldgen of the Stack zone.
- **Start pool:** `verdigrishive:structures/cabling_slums/start`
- **Size:** horizontal 60, vertical 40; max 20 pieces
- **Template pieces (minimum 12 unique pieces):**
  - `cabling_slums/room_small_a` through `..._f` — 6 single-room apartments (5×4×5 each)
  - `cabling_slums/corridor_h.nbt`, `corridor_v.nbt` — horizontal/vertical connectors
  - `cabling_slums/junction.nbt` — 4-way crossing point
  - `cabling_slums/water_tap_alcove.nbt` — the 8-tap communal water point
  - `cabling_slums/rooftop.nbt` — terminal top piece with laundry and neon_sign
  - `cabling_slums/basement.nbt` — terminal bottom piece
- **Mandatory per slum complex:** At least 8 `water_tap` blocks total (referencing the historical KWC 8 water points), at least 3 `neon_sign` blocks, at least 1 `hanging_laundry` block per room piece.
- **Loot:** `verdigrishive:chests/cabling_slums_chest` — food, scrap_metal, water_tap, hanging_laundry, low-tier tools.

---

## 10. ENTITIES & MOBS — FULL SPECIFICATION

### 10.1 Entity Registration Pattern

Each entity:
1. Has a `EntityType<T>` registered in `VHEntityTypes`.
2. Has a `MobRenderer<T>` registered in `EntityRenderers.register()` inside `ClientEvents.onClientSetup()`.
3. Has a `Model` class extending `EntityModel<T>`.
4. Has a texture at `assets/verdigrishive/textures/entity/<name>.png`.
5. Has spawn eggs if it is a naturally-spawned mob (`SpawnEggItem` registered in `VHItems`).
6. Has spawn conditions registered via `SpawnPlacementsRegisterEvent`.
7. Has all AI goals defined in `registerGoals()`.
8. Has loot table at `data/verdigrishive/loot_tables/entities/<name>.json`.

### 10.2 Sunblighter

- **Base class:** `Monster`
- **Size:** 0.6W × 1.8H (humanoid)
- **HP:** 20 (10 hearts)
- **Attack:** 4 damage (melee)
- **Movement speed:** 0.25
- **AI Goals (in priority order):**
  1. `FloatGoal`
  2. `MeleeAttackGoal` (only active if `isSunActive()` returns true — custom method checking dim time)
  3. `WaterAvoidingRandomStrollGoal`
  4. `LookAtPlayerGoal`
  5. `RandomLookAroundGoal`
- **Special:** At night (in dim time), Sunblighters become passive and sit down (play `CELEBRATING` pose). At noon (peak daytime), their attack damage doubles to 8 and they gain +30% speed.
- **Drops:** 0–2 `sun_shard`, 0–1 `brass_photovoltaic` (rare, 10% chance)
- **Sound:** `verdigrishive:entity.sunblighter.ambient`, `.hurt`, `.death`
- **Texture concept:** Humanoid in copper solar-collector headgear, wrapped in ochre fabric. Emissive eyes.

### 10.3 Wither Vine Mob

- **Base class:** `Monster`
- **Size:** 0.5W × 2.5H (tall, vine-like)
- **HP:** 14
- **Attack:** 3 damage + Slowness I for 3 seconds
- **Movement:** Clings to walls and ceilings. Override `checkAndHandleWaterInteraction()` to allow vertical surface pathfinding via `WallClimberMovement` (model after Spider's climb implementation).
- **AI Goals:**
  1. `FloatGoal`
  2. `MeleeAttackGoal`
  3. `ClimbToPlayerGoal` (custom: prioritizes paths via ceiling over floor)
  4. `WaterAvoidingRandomStrollGoal` (ceiling-biased variant)
  5. `LookAtPlayerGoal`
- **Special:** Drops from ceiling on player if within 5 blocks below. Implement via `PlayerTickEvent`: if a Wither Vine Mob is within 5 XZ, 8 Y above a player, and has line of sight, it releases and free-falls onto them (gravity + `Entity.setNoGravity(false)`).
- **Weakness:** Takes 2× damage from Shears. Takes 2× damage when player has water (swimming). Immune to Poison.
- **Drops:** 0–2 `wither_vine_block`, 0–1 `living_moss`
- **Sound:** `verdigrishive:entity.wither_vine.ambient`, `.hurt`, `.death`, `.drop` (the fall sound)

### 10.4 Helio Warden (Boss)

- **Base class:** `Monster` + custom `BossEntity` interface
- **Size:** 1.2W × 3.0H
- **HP:** 300
- **Attack (melee):** 12 damage
- **Attack (ranged):** Fires solar_bolt projectile entity (see §10.12) every 80 ticks when player is >6 blocks away
- **Boss bar:** `BossEvent` registered, visible at 200-block range
- **AI Goals:**
  1. `FloatGoal`
  2. `RangedAttackGoal` (distance 6–20, attack interval 80 ticks)
  3. `MeleeAttackGoal`
  4. `WaterAvoidingRandomStrollGoal`
  5. `LookAtPlayerGoal`
- **Phase 2 trigger:** At 50% HP, gains Speed II permanently and switches to only melee. Emits burst of `solar_bolt` projectiles in all directions as a phase-transition attack.
- **Drops:** `verdigris_key` (guaranteed 1), 2–4 `sun_shard`, 1 `brass_photovoltaic`
- **Sound:** Boss-specific ambient, hurt, death, phase transition roar

### 10.5 Cable Crawler

- **Base class:** `Monster`
- **Size:** 0.6W × 0.5H (small, spider-like)
- **HP:** 8
- **Attack:** 2 damage + disables Create contraptions within 5 blocks for 60 ticks (custom `ContraptiondisruptEffect`)
- **Movement:** Spider-style (`ClimbingMob` interface). Nests in `hanging_wire` blocks — hides inside the block model (scale to 0.3× render size when "nested").
- **AI Goals:**
  1. `FloatGoal`
  2. `MeleeAttackGoal` (only exits nest if player within 4 blocks)
  3. `ReturnToNestGoal` (custom: finds nearest `hanging_wire` block and navigates to it after combat)
  4. `LookAtPlayerGoal`
- **Special:** Spawns in groups of 3–6. Causes `verdigrishive:electrical_shock` effect (no vanilla equivalent — a custom `MobEffect` that: disables Create contraptions in range, prevents item use for 2 seconds). Effect duration 60 ticks, cannot stack.
- **Drops:** 0–1 `scrap_metal`, 0–1 `hanging_wire` (rare)

### 10.6 Squatter

- **Base class:** `AbstractVillager`
- **HP:** 20
- **Trades:** Implemented via `MerchantOffers`. Offers:
  - Level 1: scrap_metal (8) → emerald (1); emerald (2) → water_tap (1)
  - Level 2: emerald (3) → sap_bucket (1); scrap_metal (16) → emerald (2)
  - Level 3: emerald (5) → marrow_fuel_pellet (3); emerald (8) → thermometer (1)
  - Level 4: emerald (12) → corruption_ward (1)
  - Level 5: emerald (20) + ichor_crystal (1) → guidebook (1)
- **Hostility:** Passive unless attacked, then calls nearby Squatter group to attack.
- **Spawning:** Only in `cabling_slums` structure pieces, 1–3 per complex.
- **Sound:** Distinct from vanilla villager sounds. `verdigrishive:entity.squatter.ambient`, `.trade`, `.hurt`

### 10.7 Slasher (Biopunk — Limb-Targeted Combat)

- **Base class:** `Monster`
- **Size:** 0.8W × 2.0H
- **HP (full):** 40 — BUT implemented as a segmented health system (see §10.11)
- **Attack:** 7 damage (blade arm)
- **Movement speed:** 0.3 (fast)
- **AI Goals:**
  1. `FloatGoal`
  2. `ZombieAttackGoal` (custom: always attempts to close distance)
  3. `WaterAvoidingRandomStrollGoal`
  4. `LookAtPlayerGoal`
- **Special:** Without limb targeting: immune to damage from arrows/projectiles to the torso (70% damage reduction to body shots). Arms must be destroyed before torso damage is full.
- **Drops:** 0–2 `refined_marrow`, 0–1 `corruption_sample`
- **Sound:** `verdigrishive:entity.slasher.ambient` (distant gurgling), `.attack` (blade slash), `.hurt`, `.death`, `.arm_destroyed` (bone crack)

### 10.8 Lurker

- **Base class:** `Monster`
- **Size:** 0.5W × 0.7H (small, child-proportioned)
- **HP:** 18
- **Attack:** 4 damage + Wither I for 3 seconds
- **Movement:** Wall-climbing (`ClimbingMob`). Pathing prefers ceiling when available. Very fast (speed 0.45).
- **Sound design (MANDATORY):** The ambient sound for Lurker MUST be a custom audio asset that combines: a high-pitched cooing/babbling sound layer with a low-frequency leopard growl. Two separate audio files mixed in `sounds.json` via separate `additions_sound` and `mood_sound` entries. DO NOT use only one audio type for this entity — the contrast is the design intent.
- **AI Goals:**
  1. `FloatGoal`
  2. `MeleeAttackGoal`
  3. `HideBehindWallGoal` (custom: pathfinds to a position where a wall block is between the Lurker and the player)
  4. `ClimberRandomStrollGoal`
  5. `LookAtPlayerGoal`
- **Special:** Spawns in packs of 4–8. If Lurker is above player by ≥ 3 blocks: drops onto player, dealing 6 fall-damage bonus.
- **Drops:** 0–1 `corruption_sample`, 0–1 `bioluminescent_gel` (rare)

### 10.9 Corruption Wheezer

- **Base class:** `Monster`
- **Size:** 1.5W × 1.5H (blob-shaped)
- **HP:** 30
- **Attack:** No melee. Emits `corruption_cloud` particle cloud in a 5-block radius every 60 ticks.
- **AI Goals:**
  1. `FloatGoal`
  2. `MoveTowardsTargetGoal` (slow approach)
  3. `NearestAttackableTargetGoal<Player>`
  4. `WaterAvoidingRandomStrollGoal` (very slow, speed 0.15)
- **Special mechanic:** The `corruption_cloud` emitted by the Wheezer:
  - Is a custom `ParticleType` (`verdigrishive:corruption_gas`) rendered as a green-black fog sphere.
  - Applies `MobEffects.WITHER` level 1 for 80 ticks to any entity inside.
  - Accelerates nearby `corruption_growth` block spread (doubles spread chance within 5 blocks while Wheezer is alive).
- **Drops:** 1–3 `corruption_sample` (guaranteed), 0–1 `refined_marrow`

### 10.10 Brute (Elite)

- **Base class:** `Monster`
- **Size:** 1.8W × 3.5H (massive)
- **HP:** 120
- **Attack:** 15 damage + knockback
- **Spawn rate:** Only Y < −300. Weight 5 (very rare; Slasher weight for comparison: 80).
- **AI Goals:**
  1. `FloatGoal`
  2. `MeleeAttackGoal`
  3. `ChargeGoal` (custom: if player is > 8 blocks away, charges in a straight line at speed 1.5 for 1.5 seconds, damages everything in path)
  4. `WaterAvoidingRandomStrollGoal`
- **Drops:** 2–4 `refined_marrow`, 1 `ichor_crystal` (50% chance), 0–1 `verdigris_key` (10% chance)

### 10.11 Limb-Targeting Combat System

This is the primary custom combat mechanic. Implementation requirements:

**Architecture:**
- Create interface `ILimbSegmented` implemented by Slasher.
- `ILimbSegmented` exposes: `getLimbHealth(LimbSlot slot)`, `damageLimb(LimbSlot slot, float amount)`, `isLimbDestroyed(LimbSlot slot)`.
- `LimbSlot` enum: `LEFT_ARM`, `RIGHT_ARM`, `LEFT_LEG`, `RIGHT_LEG`, `HEAD`, `TORSO`.
- Limb health stored as a `Map<LimbSlot, Float>` in the entity's `DataAccessor` (synced to client via `EntityDataSerializer`).

**Hitbox determination:**
- Override `Entity.getBoundingBox()` — returns overall AABB as normal.
- On `LivingAttackEvent`: intercept damage to any `ILimbSegmented` entity. Calculate hit position by: `hitPos.y - entity.getY()` relative to entity height. Map Y ratio to limb zone:
  - 0.0–0.25 → legs (LEFT or RIGHT random)
  - 0.25–0.55 → torso
  - 0.55–0.85 → arms (LEFT or RIGHT based on X offset from center)
  - 0.85–1.0 → head
- Apply damage to that limb's health pool.
- Torso damage: 100% only if both arms destroyed; otherwise 30%.
- Head damage: 100% always (weak point).
- Entity dies when torso health reaches 0 OR head health reaches 0.
- When an arm is destroyed: `isLimbDestroyed(ARM)` returns true → attack AI goal deactivates that arm's attack. Slasher loses 1 of its 2 blade attacks.

**Client rendering:**
- Slasher model has separate `ModelPart` for each limb.
- In `SlasherRenderer.render()`: check `ILimbSegmented` data. If arm is destroyed: skip rendering that `ModelPart` (set visible = false). Spawn `verdigrishive:bone_fragment` particles at the destroyed limb's world position.

**Scope note:** Implement limb targeting for Slasher only in the initial version. The system is designed to be extensible via `ILimbSegmented` for future mobs.

### 10.12 The Last Man (NPC)

- **Base class:** `NPC` (or `AbstractVillager` with overrides)
- **Size:** 0.6W × 1.8H
- **HP:** 100. Does NOT respawn. `setInvulnerable(true)` — only takes damage from the player holding `marrow_axe` (checked in `LivingAttackEvent`).
- **Spawning:** One per dimension, placed by the Heliodome structure's NBT template (EntityTag in the template NBT).
- **Trades:**
  - emerald (5) → `marrow_compass` (1)
  - emerald (8) + bone_meal (16) → structure map to nearest Andesite Refinery
  - emerald (12) + bone_meal (32) → structure map to nearest Marker Spire
  - emerald (20) → structure map to Verdigris Throne
- **Dialogue:** Right-clicking opens a custom screen (not the vanilla trade screen). Displays flavour text strings defined in the lang file (`entity.verdigrishive.last_man.dialogue_0` through `..._9`). Cycles on repeated interaction.
- **Sound:** `verdigrishive:entity.last_man.ambient` (muffled speech through a diving bell helmet).

### 10.13 Verdigris Saint (Fusion Mob — Boss)

- **Base class:** `Monster` + `BossEntity`
- **Size:** 0.9W × 2.4H
- **HP:** 200
- **Attack (melee):** 10 damage
- **Attack (ranged):** Fires `verdigris_bolt` alternating between a corruption_cloud debuff bolt and a solar_bolt damage bolt.
- **Boss bar:** Visible at 200-block range.
- **Phase system (3 phases):**
  - Phase 1 (100–66% HP): Primarily ranged. Spawns 2 Lurkers on entry.
  - Phase 2 (66–33% HP): Spawns a Corruption Wheezer. Gains Regeneration I.
  - Phase 3 (< 33% HP): Melee charge mode. Corruption spreads from the boss entity itself (places `corruption_growth` at its feet every 20 ticks).
- **Drops:** `verdigris_key` (guaranteed), 1 `ichor_crystal` (guaranteed), 2–4 `refined_marrow`, 1–2 `sun_shard`
- **Spawning:** ONLY inside Verdigris Throne structure, one per activation.

### 10.14 Projectile Entities

**`SolarBoltEntity` extends `AbstractArrow`:**
- Damage: 6
- Speed: 1.5
- Texture: `textures/entity/solar_bolt.png` (small glowing orb)
- On hit entity: applies `Burning` for 3 seconds
- On hit block: spawns `verdigrishive:solar_impact` particles

**`VerdigrisBoltEntity` extends `ThrowableProjectile`:**
- Two variants controlled by a `DataAccessor` boolean `isCorrupt`:
  - Not corrupt: 8 damage, knockback 1
  - Corrupt: 4 damage + Wither II for 60 ticks + 3-block corruption_growth placement at impact

---

## 11. TEMPERATURE MECHANIC

### 11.1 Architecture

Temperature is stored as a player capability (NeoForge 1.21.1 Attachment system).

**Attachment type registration:**
```java
public class VHCapabilities {
    public static final AttachmentType<PlayerTemperatureData> TEMPERATURE =
        AttachmentType.serializable(PlayerTemperatureData::new).build();
    // registered via DeferredRegister<AttachmentType<?>>
}
```

**`PlayerTemperatureData`:**
```java
public class PlayerTemperatureData {
    private int temperature = VHConstants.TEMP_NEUTRAL; // 500
    private int ticksInExtremeZone = 0;

    // getters/setters, serialize to CompoundTag (temperature int, ticks int)
}
```

### 11.2 Temperature Tick Logic

Located in `TemperatureEvents` class, registered on `NeoForge.EVENT_BUS`.

Listen to `PlayerTickEvent.Post`:
```
On every tick for a player in the Verdigris Hive dimension:
  Get PlayerTemperatureData attachment
  Get current Y level
  Get current dim time (from VHDimensionTimeManager)
  
  If Y > config.canopy_heat_y_threshold AND dim time is DAYTIME:
    increment temperature by config.temp_change_rate
    if temperature > config.temp_danger_threshold:
      schedule damage tick (every config.canopy_heat_tick_interval ticks)
  
  Else if Y < config.fleshfold_cold_y_threshold AND dim time is NIGHTTIME:
    decrement temperature by config.temp_change_rate
    if temperature < (TEMP_MAX - config.temp_danger_threshold):
      schedule cold damage tick (every config.fleshfold_cold_tick_interval ticks)
  
  Else:
    // Return to neutral
    if temperature > TEMP_NEUTRAL: temperature -= config.temp_change_rate / 2
    if temperature < TEMP_NEUTRAL: temperature += config.temp_change_rate / 2
  
  Apply mitigations:
    If player holds brass_parasol in offhand AND in heat zone: reduce heat gain by 80%
    If player wears full verdigris_armor set: reduce all temp change by 30%
    If player has fire_resistance effect AND in heat zone: zero heat gain
    If player has water in inventory (water_bottle, water_bucket): reduce heat gain by 20%
```

**Damage dealing:**
Use a cooldown field `ticksUntilNextDamage` in `PlayerTemperatureData`. Decrement per tick. When ≤ 0 and temperature is in danger zone: apply `player.hurt(VHDamageSources.HEATSTROKE, config.canopy_heat_damage)` or `FROSTBITE`. Reset cooldown to config interval.

### 11.3 Damage Sources

Register two custom `DamageType` resources:
- `data/verdigrishive/damage_type/heatstroke.json` — `"message_id": "heatstroke"`, `"exhaustion": 0.1`, `"scaling": "never"`
- `data/verdigrishive/damage_type/frostbite.json` — `"message_id": "frostbite"`, `"exhaustion": 0.1`, `"scaling": "never"`

Death messages in `en_us.json`:
- `death.attack.heatstroke` = `"%1$s was scorched by the Verdigris sun"`
- `death.attack.frostbite` = `"%1$s froze in the darkness below"`

### 11.4 HUD Overlay (Temperature Bar)

Class: `TemperatureHudOverlay` registered via `RegisterGuiLayersEvent` (NeoForge 1.21.1).

**Behaviour:**
- Renders a horizontal bar in the HUD, positioned directly where the Hunger bar is (same Y coordinate, right side of health bar row).
- When player is in a cold zone (temperature < TEMP_NEUTRAL): the Hunger bar is hidden (`GuiLayerManager.remove("minecraft:food_level")` equivalent or use `RenderGuiOverlayEvent.Pre` to cancel the vanilla food render) AND the Temperature bar is rendered in its place.
- When player is in a hot zone (temperature > TEMP_NEUTRAL): the Temperature bar replaces the Hunger bar.
- When temperature is neutral: show the Hunger bar normally, hide Temperature bar.
- **Bar texture:** 16 icons × 2 states (full/half). Color shifts from blue (cold) to white (neutral) to orange (hot) to red (danger). Use a `GuiComponent.blitRepeating` or individual icon draw calls.
- **Danger state:** Bar pulses (alpha oscillation) when temperature is past the danger threshold.
- **MANDATORY:** This is the primary survival readout. It must be clearly visible and unambiguous.

### 11.5 Twilight Safe Window

`VHDimensionTimeManager.getTimePhase()` returns enum `TimePhase { DAYTIME, TWILIGHT, NIGHTTIME }`.
- TWILIGHT is the period ±(config.twilight_fraction × full_day_length) around each dawn and dusk.
- During TWILIGHT: no temperature ticks in either direction (stay at current value). No environmental damage. Mobs behave normally.
- TWILIGHT is the natural "working window" for players in extreme zones.

---

## 12. CORRUPTION SPREAD SYSTEM

### 12.1 Architecture

The Corruption system runs server-side. State is persisted in `VHDimensionSavedData` (a `SavedData` subclass stored per-dimension via `level.getDataStorage().computeIfAbsent()`).

**`VHDimensionSavedData` stores:**
- Set of `ChunkPos` where corruption is active (`activeCorruptionChunks`)
- Map of `BlockPos` → `CorruptionStage` (stage 0–3) for tracked corruption blocks
- `BlockPos` of Marker Spire (nullable — null once destroyed)
- Whether the Marker Spire is destroyed (boolean)

### 12.2 Spread Logic

Listen to `LevelTickEvent.Post` (server-side only, check `event.getLevel() instanceof ServerLevel`).

Run only when `event.phase == LevelTickEvent.Phase.END`.

**Per-tick logic:**
```
Only execute during NIGHTTIME (check VHDimensionTimeManager).
For each loaded chunk in the dimension that is in activeCorruptionChunks:
  For i in 0..config.max_checks_per_chunk_tick:
    Pick a random BlockPos within the chunk's Y range (-512 to -100)
    If that block is corruption_block or corruption_growth:
      For each of the 6 adjacent blocks:
        Roll 1/config.spread_chance chance
        If succeeds AND adjacent block is:
          - A flesh_block type → set to corruption_growth stage 0
          - Already corruption_growth stage N → set to stage N+1 (max 3)
          - corruption_growth stage 3 → set to corruption_block
        If light level at adjacent block >= config.spread_light_block_threshold → skip (no spread)
```

**Performance guard:** Limit total spread operations per tick to `activeCorruptionChunks.size() × config.max_checks_per_chunk_tick`. If this exceeds 512, halve the max_checks_per_chunk_tick for this tick and log a warning.

### 12.3 Corruption Block Behaviour

`CorruptionBlock.randomTick()`:
- Apply Wither I for 40 ticks to any entity standing on it.
- 1/100 chance to spawn `verdigrishive:corruption_gas` particles upward.

`CorruptionGrowthBlock.randomTick()`:
- Stage advancement: already handled by the spread system above. Do NOT advance stage in randomTick to avoid double-advancing.

### 12.4 Player Corruption Resistance

If the player has the `verdigrishive:corruption_resistance` `MobEffect` active:
- Immune to Wither from corruption_block contact.
- Immune to Wheezer cloud Wither.
- Corruption_growth blocks do not spread onto blocks the player is standing on.

The effect is obtained via potion (`verdigrishive:anti_corruption_potion`) crafted with `corruption_sample` + `brine_bucket` + `awkward_potion` in a brewing recipe.

---

## 13. DAY/NIGHT CYCLE EXTREMES

### 13.1 VHDimensionTimeManager

A server-side singleton (stored in `VHDimensionSavedData`) that tracks the dimension's time.

**Time representation:** A long `dimTime` incrementing every game tick. One full cycle = `20 * 60 * 20 * config.day_length_multiplier` ticks (at multiplier 8, that is 192,000 ticks = 160 real minutes).

**Phase calculation:**
```java
public static TimePhase getPhase(long dimTime, long cycleLength, float twilightFraction) {
    double fraction = (dimTime % cycleLength) / (double) cycleLength;
    // 0.0 = dawn, 0.5 = dusk
    double dawnTwilight = twilightFraction / 2.0;
    double duskStart = 0.5 - (twilightFraction / 2.0);
    double duskEnd = 0.5 + (twilightFraction / 2.0);
    double nightEnd = 1.0 - (twilightFraction / 2.0);
    
    if (fraction < dawnTwilight) return TimePhase.TWILIGHT;         // pre-dawn
    if (fraction < duskStart) return TimePhase.DAYTIME;
    if (fraction < duskEnd) return TimePhase.TWILIGHT;              // dusk
    if (fraction < nightEnd) return TimePhase.NIGHTTIME;
    return TimePhase.TWILIGHT;                                        // pre-dawn
}
```

**Ticking:** Increment `dimTime` every server tick in `LevelTickEvent`. Sync to client via a custom `VHTimeSyncPacket` (custom network packet) every 20 ticks. Client stores the last synced time and interpolates.

### 13.2 Mixin for Day/Night Cycle Length

This is one of the approved Mixin sites (see §1.3 Principle 7).

`MixinClientLevel` targets `ClientLevel.tickTime()` (or the equivalent in 1.21.1 — verify the exact method). Intercept the vanilla time increment in the Verdigris Hive dimension and substitute `VHDimensionTimeManager`'s client-side interpolated time. This ensures:
- The sky renderer gets the correct time value.
- The vanilla `/time query daytime` is overridden in this dimension.

**Mixin justification comment (MANDATORY in code):**
```java
// MIXIN: No NeoForge event exists to intercept per-tick time advancement for a specific
// dimension without replacing the entire tick pipeline. This mixin is minimal and
// only activates when Level.dimension() == VHConstants.VERDIGRIS_HIVE_LEVEL.
```

### 13.3 Mob Behaviour Changes by Phase

Listen to `LivingTickEvent` on server side:

- **DAYTIME:** Sunblighters aggressive and buffed. Wither Vine Mobs active. Biopunk mobs (Slasher, Lurker, Wheezer) take 30% more damage (sun vulnerability — custom attribute modifier applied on phase transition).
- **TWILIGHT:** All mobs resume normal base behaviour. No buffs or debuffs.
- **NIGHTTIME:** Sunblighters become passive (clear attack target). Biopunk mobs gain +20% speed and +10% damage. Corruption spread activates.

Phase transitions are fired as a custom `DimensionPhaseChangeEvent` (extends `Event`) on `NeoForge.EVENT_BUS` whenever `getPhase()` returns a different value than the previous tick. Other systems listen to this event to trigger their phase-specific behaviour, avoiding polling every tick.

### 13.4 Visual Phase Indicators

Client-side, the sky renderer transitions its color gradient over the twilight duration. Additionally:

- A custom `ToastComponent` notification appears at phase transitions ("Dawn breaks over the Hive", "Darkness falls — the Flesh awakens").
- The ambient sound track crossfades between the daytime and nighttime ambient tracks during twilight.

---

## 14. CREATE MOD INTEGRATION LAYER

### 14.1 Integration Class Structure

All Create integration lives in `com.verdigrishive.integration.create`.

Entry point: `CreateIntegration.init()` called from `VerdigrisHive.commonSetup()` inside a `ModList.get().isLoaded("create")` guard. This method registers all Create-specific recipes, fluid interactions, and contraption handlers.

If Create is NOT loaded: all blocks that reference Create (solar_panel, junction_box, heat_conduit) fall back to standalone behaviour defined in the base block class (no Create-specific imports in the base class — use interfaces and service loading).

### 14.2 Rotational Power Generators

**Marrow Engine Multiblock:**
- Structure: 3×3×3 multiblock. Center block: `marrow_engine_core` (a `KineticBlockEntity` subclass from Create's API). Surrounding blocks: `flesh_wall` × 26.
- When the multiblock is complete (detected via `MarrowEngineBlockEntity.onLoad()` checking neighbors): activates as a rotational source.
- RPM output: `config.marrow_engine_rpm` (default 64).
- Fuel: `marrow_fuel_pellet`. Burn time: 1600 ticks. Insert via right-click (opens a 1-slot container screen).
- Sound: `verdigrishive:block.marrow_engine.running` — looping breathing sound, pitch-shifted by RPM.
- **Requires Create API:** `KineticBlockEntity`, `IRotate`, `BlockEntityBehaviour`. These are accessed via the Create API (soft dependency — all references inside `integration.create` package).

**Solar Collector Upgrade:**
- `solar_panel` block, when in Create-loaded context, additionally exports heat units to adjacent `IHeatContainer` blocks (Create: New Age API).
- Output: `config.solar_collector_heat_output` per tick when sky access is available AND `VHDimensionTimeManager.getPhase() == DAYTIME`.
- If Create: New Age NOT loaded: `solar_panel` simply produces `brass_photovoltaic` items when right-clicked (standalone fallback).

**Windmill Bearing Repair:**
- In Bosco Spire worldgen pieces, windmill bearings are placed in a "broken" tag state (`verdigrishive:broken=true`).
- When the player right-clicks with a wrench (Create's Wrench item): `BlockInteractEvent` fires, integration code checks the block tag, removes the broken tag, and activates the bearing.
- Activation restores the bearing to a fully functional Create windmill.

### 14.3 Create Fluid Integration

Sap, Brine, Ichor, and Amniotic Fluid are all `FluidType` instances compatible with Create's fluid pipe system automatically — Create's fluid pipes work with any `FluidStack`.

Additional Create-specific recipes (registered in `CreateIntegration.init()`):
- **Mixing recipe:** Sap + Brine → Nutrient Solution + Ichor (small amount). Requires Heated Mixer.
- **Emptying recipe:** Ichor Crystal → Ichor Fluid (1000 mB) + Refined Marrow (1).
- **Filling recipe:** Ichor Fluid (1000 mB) + Refined Marrow → Ichor Crystal.
- **Pressing recipe:** Raw Marrow × 4 → Marrow Fuel Pellet.
- **Milling recipe (Millstone):** Rusted Andesite Alloy → Andesite Alloy + Sand (byproduct, 50%).
- **Sequenced Assembly:** Verdigris Armor crafting using brass sheets + refined_marrow + verdigris_copper.

**Implementation:** Use Create's `ProcessingRecipeBuilder` API or, if unavailable, write raw JSON recipe files in `data/verdigrishive/recipes/` using Create's recipe type strings (`create:mixing`, `create:pressing`, etc.).

### 14.4 Mechanical Contraptions in Worldgen

Structures that include Create machines (Andesite Refinery, Bosco Spire) use NBT templates with Create machinery pre-placed. The machinery is placed in a "broken" state via:
1. Block tags (custom tag `verdigrishive:broken` set on the block in the NBT).
2. Contraptions are NOT assembled in worldgen — no `ContraptionHandler.assemble()` is called during structure placement. Only static blocks are placed. The player assembles them manually.

### 14.5 Create: Steam 'n' Rails Integration

Guard: `ModList.get().isLoaded("railways")` (Steam 'n' Rails mod ID — verify correct mod ID at build time).

If loaded:
- Rail track blocks are pre-placed in vertical shafts of the megastructure at Y intervals defined by structure piece heights.
- `TrainShaftFeature` (custom `PlacedFeature`) places a straight track run every 80 blocks of Y in designated shaft positions.
- The train shaft positions are fixed offsets from the dimension origin (e.g., X=+16, Z=+16 from any 128-block grid anchor).

If NOT loaded:
- Shaft blocks are generated as empty vertical corridors. No rails placed.

### 14.6 Create: New Age Integration

Guard: `ModList.get().isLoaded("create_new_age")` (verify mod ID).

If loaded:
- `solar_panel` exports heat to New Age's `IHeatContainer`.
- `heat_conduit` acts as a New Age heat pipe (wrap New Age's `HeatPipeBehaviour` inside a `heat_conduit` `BlockEntity`).
- `junction_box` acts as a New Age electrical junction (wrap New Age's `WireBehaviour`).
- Nuclear/steam recipes for Ichor Fluid as a reactor coolant are registered (Ichor as a high-efficiency coolant fluid).

### 14.7 Biomancy + Bio-Factory Integration

Guard: `ModList.get().isLoaded("biomancy")` AND `ModList.get().isLoaded("biofactory")`.

If loaded:
- `flesh_wall` is tagged as `biomancy:flesh_blocks` so Biomancy creatures interact with it.
- Bio-Factory's `NutrientFluid` can flow through `sap_pipe` blocks.
- Bio-Factory's `FleshBlob` device is an accepted input to the `MarrowEngine` (its nutrients supplement the fuel supply, reducing marrow_fuel_pellet consumption by 50%).
- Biomancy's `PrimordialCradle` spawns within the Fleshfold biome as an additional structure (weight 20, rarer than Cabling Slums).
- **Retexture note:** Document in the mod's README that Biomancy's cartoony art style visually conflicts with the Scorn-inspired Fleshfold. Provide an optional resource pack (`verdigrishive/optional/biomancy_retexture.zip`) that applies the Fleshfold palette to Biomancy blocks. The retexture pack MUST be separate from the core mod.

---

## 15. GAMEPLAY PROGRESSION & LOOT

### 15.1 Loot Tables

All loot tables use NeoForge's `LootTableSubProvider` in `VHDataGenerator`. Each chest has a pool with `min`/`max` rolls and weighted item entries.

**`verdigrishive:chests/bosco_spire_locker`:**
- 3–6 rolls
- sun_shard (weight 30, 1–3), wither_vine_block (weight 20, 2–5), solar_panel (weight 5, 1), brass_photovoltaic (weight 10, 1–2), scrap_metal (weight 40, 3–8), sap_bucket (weight 15, 1)

**`verdigrishive:chests/andesite_refinery`:**
- 4–8 rolls
- scrap_metal (weight 50, 4–12), rusted_andesite_alloy (weight 30, 2–5), rusted_brass (weight 20, 1–3), sap_bucket (weight 20, 1–2), brine_bucket (weight 15, 1), marrow_fuel_pellet (weight 10, 1–3), thermometer (weight 5, 1)

**`verdigrishive:chests/heliodome`:**
- 6–10 rolls
- sun_shard (weight 20, 2–5), ichor_crystal (weight 5, 1), brass_photovoltaic (weight 15, 2–4), refined_marrow (weight 20, 2–4), verdigris_helmet (weight 3, 1), verdigris_chestplate (weight 3, 1), guidebook (weight 2, 1)

**`verdigrishive:chests/cabling_slums_chest`:**
- 2–5 rolls
- bread (weight 40, 1–3), scrap_metal (weight 50, 2–6), water_tap (weight 10, 1), hanging_laundry (weight 15, 1–2), iron_nugget (weight 30, 2–8), rope (weight 20, 1–4)

### 15.2 Crafting Recipes

All recipes defined as JSON in `data/verdigrishive/recipes/`. Use `RecipeProvider` subclass in `VHDataGenerator`.

**Portal Igniter:**
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [ "SFS", "F F", "SFS" ],
  "key": { "S": "verdigrishive:sun_shard", "F": "minecraft:flint_and_steel" },
  "result": { "item": "verdigrishive:portal_igniter", "count": 1 }
}
```

**Marrow Sword:**
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [ " R ", " R ", " S " ],
  "key": { "R": "verdigrishive:refined_marrow", "S": "minecraft:stick" },
  "result": { "item": "verdigrishive:marrow_sword" }
}
```

All armor, tools, and equipment follow standard shaped crafting patterns. Define all 4 armor pieces, all 4 tool types, brass_parasol, thermometer, corruption_ward, sun_flask, marrow_compass, portal_igniter, marrow_engine_core.

**Smelting recipes:**
- raw_marrow → refined_marrow (200 ticks, 0.7 XP)
- rusted_andesite_alloy → andesite_alloy (if Create loaded) or iron_ingot (if not) (200 ticks, 0.5 XP)
- rusted_brass → brass_ingot (if Create) or gold_nugget × 6 (if not) (200 ticks, 0.5 XP)

**Brewing recipes** (implement via `BrewingRecipeRegistry.addRecipe()` in `commonSetup`):
- Awkward Potion + corruption_sample + brine_bucket → anti_corruption_potion

### 15.3 Advancement Tree

Advancements defined in `data/verdigrishive/advancements/`. Required advancements:

```
verdigrishive:root               — "Enter the Verdigris Hive" (trigger: dimension change to verdigris_hive)
├── find_slum                    — "Find a Cabling Slum" (trigger: structure)
├── first_heat                   — "The Sun Burns Here" (trigger: custom: take heatstroke damage)
│   └── craft_parasol            — "Brass Shade" (trigger: item crafted)
├── repair_windmill              — "The Gears Still Turn" (trigger: interact with windmill bearing)
│   └── first_train              — "Vertical Transit" (trigger: ride a Create train in dimension)
├── descend_fleshfold            — "Where the Flesh Lives" (trigger: Y < -100)
│   ├── first_slasher            — "Strategic Dismemberment" (trigger: destroy arm limb on Slasher)
│   │   └── kill_slasher         — "Complete Deconstruction" (trigger: kill entity)
│   ├── find_marker_spire        — "The Signal" (trigger: within 20 blocks of structure)
│   │   └── destroy_spire        — "Silenced" (trigger: custom: spire destroyed event)
│   └── first_lurker             — "Something Above You" (trigger: hit by Lurker drop attack)
├── find_heliodome               — "The Glass Crown" (trigger: structure)
│   └── kill_helio_warden        — "Sunbeaten" (trigger: kill entity)
└── kill_verdigris_saint         — "The Fusion Answered" (trigger: kill entity) [GOAL advancement, framed]
```

---

## 16. SOUNDS & AMBIENCE

### 16.1 sounds.json Structure

```json
{
  "ambient.canopy.wind": { "sounds": [{ "name": "verdigrishive:ambient/canopy/wind_loop", "stream": true }], "subtitle": "subtitles.verdigrishive.ambient.wind" },
  "ambient.canopy.glass_creak": { "sounds": [
    "verdigrishive:ambient/canopy/glass_creak_1",
    "verdigrishive:ambient/canopy/glass_creak_2",
    "verdigrishive:ambient/canopy/glass_creak_3"
  ]},
  "ambient.fleshfold.pulse": { "sounds": [{ "name": "verdigrishive:ambient/fleshfold/pulse_loop", "stream": true }]},
  "ambient.arcology.wind": { "sounds": [{ "name": "verdigrishive:ambient/arcology/wind_loop", "stream": true }]},
  "entity.lurker.ambient": { "sounds": [
    { "name": "verdigrishive:entity/lurker/coo_1", "weight": 3 },
    { "name": "verdigrishive:entity/lurker/coo_2", "weight": 3 },
    { "name": "verdigrishive:entity/lurker/growl_1", "weight": 1 }
  ]},
  "entity.lurker.hurt": { "sounds": ["verdigrishive:entity/lurker/hurt_1", "verdigrishive:entity/lurker/hurt_2"] },
  "entity.lurker.death": { "sounds": ["verdigrishive:entity/lurker/death"] },
  ...
}
```

### 16.2 Required Sound Assets (Full List)

The following sound files must be provided as `.ogg` format at the listed paths under `assets/verdigrishive/sounds/`:

**Ambient (looping, streamed):**
- `ambient/canopy/wind_loop.ogg` — steady warm wind
- `ambient/canopy/glass_creak_1-3.ogg` — glass structure creak
- `ambient/canopy/distant_windmill.ogg` — faraway windmill groan
- `ambient/stack/indoors_hum.ogg` — indoor electrical hum, distant voices
- `ambient/plumbing/drip_loop.ogg` — water dripping, distant steam
- `ambient/fleshfold/pulse_loop.ogg` — slow heartbeat + fluid movement
- `ambient/fleshfold/creak_loop.ogg` — wet structural creaking
- `ambient/marrow_sink/fluid_loop.ogg` — thick fluid movement
- `ambient/arcology/wind_loop.ogg` — clean outdoor breeze
- `ambient/arcology/water_fountain.ogg` — fountain water

**Entity sounds:**
Every entity listed in §10 requires: `.ambient` (2+ variants), `.hurt` (2+ variants), `.death` (1+), `.step` (optional but preferred).
Additionally for special entities:
- Slasher: `.arm_destroyed`
- Wither Vine: `.drop`
- Marrow Engine: `.running` (looping mechanical breath)
- Lurker: `.coo_1`, `.coo_2`, `.growl_1` (as specified in §10.8)
- Last Man: `.ambient` (muffled diving bell speech)
- Helio Warden boss: `.phase_transition`, `.roar`
- Verdigris Saint boss: `.phase_2_trigger`, `.phase_3_trigger`

**Block sounds:**
- Flesh blocks: walk sound distinct from vanilla dirt (wet squelch)
- Pulsing wall: `block.pulsing_wall.exhale`, `block.pulsing_wall.inhale`
- Toothy door: `block.toothy_door.open` (growl)
- Marrow engine: (covered above)

**Music tracks:**
- `music/arcology/theme.ogg` — hopeful, clean, acoustic + electronic (overworld biome music)
- `music/hive/daytime.ogg` — tense industrial ambient
- `music/hive/nighttime.ogg` — horror ambient, low bass, irregular rhythm
- `music/hive/boss/helio_warden.ogg` — intense solar-themed boss music
- `music/hive/boss/verdigris_saint.ogg` — fusion theme: industrial + organic

**Note to implementer:** Placeholder `.ogg` files (1-second silence) MUST be placed for every entry in sounds.json during development so the mod does not crash on missing sound events. Replace with final audio assets before release.

### 16.3 Dimension Background Music

Use `BiomeSpecialEffects.Builder.backgroundMusic()` in each biome's Java builder (in `VHBiomes`) to assign the daytime/nighttime music. Music switches via `VHTimeSyncPacket` triggering a `SoundManager.play()` call client-side with a crossfade of 40 ticks.

---

## 17. VISUAL & RENDERING SPECS

### 17.1 Texture Requirements

All textures are 32×32 pixels (2× vanilla resolution). This is a deliberate design decision for richness without requiring full PBR shaders.

**Block textures:** Every block defined in §4 requires:
- A 32×32 `.png` at `assets/verdigrishive/textures/block/<name>.png`
- Animated textures (pulsing_wall, corruption_block, bioluminescent_sac, ichor_seep) require a matching `.png.mcmeta` file defining the animation frame count and speed.

**Entity textures:** Every mob requires a 64×32 or 64×64 `.png` at `assets/verdigrishive/textures/entity/<name>.png`.

**Emissive textures:** For blocks and entities with glow (bioluminescent_sac, neon_sign, corruption_block, ichor, solar_panel active state): implement emissive rendering via NeoForge's `ForgeRenderTypes.ENTITY_CUTOUT_NO_CULL_Z_OFFSET` on the model layer. Mark the emissive texture at `textures/<path>_e.png`. Register the emissive model layer in the entity/block renderer.

**Color palette (strict):**

| Zone | Primary | Secondary | Accent |
|---|---|---|---|
| Solar Canopy | `#D4A54A` (warm gold) | `#7AB67A` (wilted green) | `#8EC4C4` (glass blue) |
| Stack | `#7A7A7A` (stained concrete) | `#2A2A2A` (shadow) | `#CC6600` (neon orange) |
| Plumbing | `#6B7A6B` (verdigris) | `#8B4513` (rust) | `#B8A000` (dull brass) |
| Fleshfold | `#8B6B3A` (ochre flesh) | `#F5F0DC` (bone white) | `#9B4B2A` (deep viscera) |
| Marrow Sink | `#4A3A5A` (deep purple) | `#8A44AA` (bioluminescent) | `#2A1A3A` (near black) |
| Arcology | `#3A8A3A` (bright green) | `#88CCFF` (clean sky blue) | `#FFD700` (solar gold) |

### 17.2 Particle Effects

Register all particles via `VHParticleTypes` (`DeferredRegister<ParticleType<?>>`). Each particle requires a client-side `ParticleProvider` registered in `ClientEvents`.

| Particle ID | Description | Used by |
|---|---|---|
| `corruption_gas` | Slow-rising dark green fog cloud, radius 0.5–1.0, fades over 40 ticks | Wheezer, corruption_block, lung_block |
| `solar_impact` | Bright yellow starburst, 8 rays, 10-tick lifetime | solar_bolt hit, solar_panel activate |
| `bone_fragment` | Small white irregular shards, gravity-affected | Slasher arm destruction |
| `ichor_drip` | Dark red thick drops, gravity | ichor_seep edges |
| `amniotic_bubble` | Pink sphere, slow upward float, bioluminescent | amniotic fluid surface |
| `arcology_spore` | White mote, very slow drift, 60-tick lifetime | Arcology biome surface |
| `verdigris_spark` | Teal-green electric spark | junction_box, cable_crawler attack |
| `heat_shimmer` | Distortion ripple (use a translucent heat shimmer quad) | Canopy at noon |

### 17.3 Custom Sky Rendering

Implement `VHSkyRenderer` registered via NeoForge 1.21.1's `RegisterDimensionSpecialEffectsEvent` (verify exact event name in 1.21.1 changelog) or the equivalent dimension effects registry hook.

Sky rendering targets:
- Register `VHDimensionEffects extends DimensionSpecialEffects` with `ResourceLocation("verdigrishive:verdigris_hive")`.
- Override `getSunriseColor()` to return amber (dawn), white-yellow (noon), null (night).
- Override `renderSky()` (or equivalent hook) to render the custom sky dome shader.
- The sky dome is a simple gradient quad rendered at the far clip plane. No actual dome geometry — just a gradient background.
- No stars rendered (ceiling dimension).

### 17.4 Block Entity Renderers

`pulsing_wall` requires a `BlockEntityRenderer<PulsingWallBlockEntity>` that:
- Each tick interpolates the block's visual hitbox scale between min (0.9) and max (1.1) using `Mth.lerp()` applied to a sinusoidal `Math.sin(ticker / 120.0 * Math.PI * 2)` value.
- Renders the extra-extended face as a semi-transparent overlay quad when at max extension.

`hydroponic_tray` requires a renderer that shows the current crop growth stage above the tray.

---

## 18. DATA GENERATION

### 18.1 VHDataGenerator

All data files that can be generated programmatically MUST be generated via `DataGenerator`. This includes:

- Block models and blockstates (`ModelProvider` subclass)
- Item models (`ItemModelProvider` subclass)
- Block loot tables (`BlockLootSubProvider` subclass)
- Entity loot tables (`EntityLootSubProvider` subclass)
- Chest loot tables (`ChestLootSubProvider`)
- Crafting recipes (`RecipeProvider` subclass)
- Tags: blocks, items, entity types, biomes, fluids, damage types
- Advancements (`AdvancementProvider` subclass)
- Language file (`LanguageProvider` — `en_us` only; other languages added manually)

Register via:
```java
@SubscribeEvent
public static void onGatherData(GatherDataEvent event) {
    DataGenerator gen = event.getGenerator();
    PackOutput output = gen.getPackOutput();
    CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

    gen.addProvider(event.includeServer(), new VHRecipeProvider(output, lookupProvider));
    gen.addProvider(event.includeServer(), new VHBlockTagsProvider(output, lookupProvider, event.getExistingFileHelper()));
    gen.addProvider(event.includeServer(), new VHItemTagsProvider(output, lookupProvider, event.getExistingFileHelper()));
    gen.addProvider(event.includeServer(), new VHBlockLootProvider(output, lookupProvider));
    gen.addProvider(event.includeServer(), new VHAdvancementsProvider(output, lookupProvider));
    gen.addProvider(event.includeClient(), new VHBlockStateProvider(output, event.getExistingFileHelper()));
    gen.addProvider(event.includeClient(), new VHItemModelProvider(output, event.getExistingFileHelper()));
    gen.addProvider(event.includeClient(), new VHLanguageProvider(output));
}
```

### 18.2 World Gen Data

Biome JSONs, dimension JSONs, noise settings, structure JSONs, placed features, and configured features MUST be written manually (they are not generatable via the standard DataProvider system in 1.21.1). These live in `src/main/resources/data/verdigrishive/worldgen/`.

However: use `BootstrapContext` + `DatapackBuiltinEntriesProvider` for all worldgen registries that support it (biomes, configured features, placed features, noise settings). This allows worldgen objects to be built in Java and output as JSON by the datagen run.

---

## 19. PERFORMANCE CONSTRAINTS & BUDGETS

### 19.1 Mandatory Performance Targets

These are non-negotiable. Test on a mid-range machine (Intel i7-8700, 8GB RAM allocated to JVM, no OptiFine/Sodium, 12-chunk render distance).

| Metric | Target | Measurement method |
|---|---|---|
| Server TPS in dimension with 1 player | ≥ 18 TPS | `/forge tps` |
| Server TPS in dimension with 4 players | ≥ 16 TPS | `/forge tps` |
| Client FPS in canopy zone | ≥ 40 FPS | F3 screen, no shaders |
| Client FPS in Fleshfold | ≥ 35 FPS | F3 screen |
| Chunk generation time (Hive dim) | ≤ 500ms per chunk | Server log timing |
| Corruption spread TPS impact | ≤ 0.5 TPS loss | Measure with/without corruption active |

### 19.2 Performance Rules

**Rule 1 — No per-tick entity loops.** Never loop over `level.getEntities()` every tick. Use targeted lookups (`level.getEntitiesOfClass()` with an AABB limit) or event-driven detection.

**Rule 2 — Corruption spread is capped.** The cap defined in §12.2 (512 total operations per tick) is a hard limit, not a guideline. Exceed it and log a warning at DEBUG level.

**Rule 3 — Structure generation is async.** `HiveChunkGenerator.fillFromNoise()` must not call `level.getBlockState()` outside the chunk being generated. No cross-chunk reads during gen.

**Rule 4 — `VHDimensionSavedData` save is throttled.** Only call `setDirty()` on the SavedData when a meaningful state change occurs (corruption spread event, spire destroyed, etc.). Do NOT call `setDirty()` every tick.

**Rule 5 — Client particle budget.** `corruption_gas` particles are limited to 32 active at once per player (capped in `CorruptionGasParticle.tick()` via a static atomic counter). `heat_shimmer` particles: max 8 active at once.

**Rule 6 — Temperature checks batched.** The temperature tick in §11.2 runs every 5 ticks, not every tick. Multiply the damage calculation accordingly.

**Rule 7 — Sound streaming.** All looping ambient sounds MUST have `"stream": true` in `sounds.json`. Non-streaming ambient sounds of >2 seconds are a blocking defect.

**Rule 8 — Block entity tick scoping.** BlockEntities that need to run every tick (`pulsing_wall`, `lung_block`, `marrow_engine`) must override `getUpdateTag()` and only sync state when it changes, not every tick. Use `level.sendBlockUpdated()` only on state transitions.

**Rule 9 — Jigsaw budget.** No single structure generation call (including anchor chains) may take more than 200ms. If anchor chaining exceeds this, switch to the interval-placement fallback (§9.1).

**Rule 10 — Memory.** The mod must not hold strong references to `Level` objects in static fields. All level references in event listeners are local or passed as parameters.

---

## 20. MANDATORY GOALS & ACCEPTANCE CRITERIA

The following features must be complete before the mod is considered shippable (version 1.0.0). Each item is binary: pass or fail.

### Phase 1 MVP (required for any release)

- [ ] **G1:** Player can craft a `portal_igniter`, build a `verdigris_portal_frame`, and ignite a portal that teleports them to the Verdigris Hive dimension.
- [ ] **G2:** The Verdigris Hive dimension loads without crashing and generates terrain that is visually distinct from the Overworld and Nether (custom blocks, no vanilla dirt visible in the main body of the structure).
- [ ] **G3:** At least 3 of the 6 biomes generate correctly and are visually distinct from each other.
- [ ] **G4:** The temperature mechanic functions: player takes heatstroke damage above Y+180 during daytime. The HUD bar appears and updates correctly. Death message displays.
- [ ] **G5:** The cold mechanic functions: player takes frostbite damage below Y-100 during nighttime. HUD bar appears.
- [ ] **G6:** Day/night cycle runs at 8× the vanilla length. TimePhase enum is correct at all cycle points.
- [ ] **G7:** Sunblighter, Wither Vine, Slasher, and Lurker spawn, have functional AI, have drop tables, and play sounds.
- [ ] **G8:** Cabling Slums structure generates in the Stack zone. Contains all mandatory elements (8 water_tap blocks, 3 neon_signs, laundry).
- [ ] **G9:** Andesite Refinery generates in Plumbing zone. Loot chest contains correct items.
- [ ] **G10:** Bosco Spire generates in Canopy zone. Contains solar_panel and hydroponic_tray.
- [ ] **G11:** Corruption spread activates at night. Placing a light source stops spread. Removing the Marker Spire halves spread rate.
- [ ] **G12:** Full verdigris armor set provides the stat bonuses described in §5.4.
- [ ] **G13:** The Verdant Arcology overworld biome generates in at least 1 out of every 200 chunks explored. Contains at least one Arcology Tower structure.
- [ ] **G14:** All blocks have textures (no missing texture magenta/black). All items have textures. All entities have textures.
- [ ] **G15:** Mod loads cleanly without Create installed. Temperature, corruption, and base dimension features all work. No `ClassNotFoundException` for Create classes.
- [ ] **G16:** Mod loads cleanly WITH Create installed. At least the Marrow Engine and Windmill Repair features function.
- [ ] **G17:** Performance targets in §19.1 are met (1-player TPS ≥ 18).
- [ ] **G18:** No `NullPointerException` or `ClassCastException` in normal gameplay in the first 60 minutes of play.
- [ ] **G19:** All advancement triggers fire correctly. Advancement tree is completable.
- [ ] **G20:** `VHConfig` values are respected at runtime. Changing `canopy_heat_damage` to 0.0 in the config stops heat damage without requiring a restart (config is hot-reloadable via `ModConfigEvent.Reloading`).

### Phase 2 (required for 1.0.0 feature-complete)

- [ ] **G21:** Heliodome structure generates at dimension origin. Helio Warden boss spawns and uses both combat phases. Drops `verdigris_key`.
- [ ] **G22:** Lung Atrium generates in Fleshfold. Pulsing walls cycle correctly. Lung Warden spawns on proximity.
- [ ] **G23:** Marker Spire generates. Player can destroy it. `VHDimensionSavedData` persists the destroyed state across server restarts.
- [ ] **G24:** Verdigris Throne generates. The `verdigris_key` opens the gate. Verdigris Saint boss spawns with all 3 phases.
- [ ] **G25:** Slasher limb targeting system functions. Destroying both arms prevents Slasher melee attacks. Torso damage is reduced while arms are intact.
- [ ] **G26:** The Last Man NPC spawns in the Heliodome, is non-hostile, offers all 4 trade tiers, and dialogue text displays correctly.
- [ ] **G27:** Squatter NPC spawns in Cabling Slums, offers all 5 trade tiers.
- [ ] **G28:** Sun Flask sun-charging mechanic works. Held in canopy at noon for 30 seconds → charged. Right-click applies Fire Resistance + Haste I.
- [ ] **G29:** Full Create integration layer active when Create is loaded. Marrow Engine multiblock generates rotational power. Windmill repair works. All Create-specific recipes are craftable.
- [ ] **G30:** Patchouli guidebook (if Patchouli loaded) OR custom screen (if not) opens from `guidebook` item and displays at least 5 pages of content explaining the dimension mechanics.

---

## 21. DEPENDENCY DECLARATIONS

### 21.1 `neoforge.mods.toml` Dependencies

```toml
[[dependencies.verdigrishive]]
    modId = "neoforge"
    type = "required"
    versionRange = "[21.1,22)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "minecraft"
    type = "required"
    versionRange = "[1.21.1,1.22)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "create"
    type = "optional"
    versionRange = "[0.5,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "railways"
    type = "optional"
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "create_new_age"
    type = "optional"
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "biomancy"
    type = "optional"
    versionRange = "[2.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "biofactory"
    type = "optional"
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "patchouli"
    type = "optional"
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.verdigrishive]]
    modId = "tough_as_nails"
    type = "optional"
    versionRange = "[4.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

### 21.2 Dependency Integration Notes

| Dependency | If loaded | If absent |
|---|---|---|
| Create | Full integration (§14) | Base blocks, standalone fluid pipes, no rotational power |
| Steam 'n' Rails | Rail shafts generated | Empty corridors generated |
| Create: New Age | Solar→heat pipeline | solar_panel produces items only |
| Biomancy + Bio-Factory | Extra Fleshfold structure, fluid bridges | No change to core gameplay |
| Patchouli | Full illustrated guidebook | Custom simple screen |
| Tough As Nails | Hook into TAN's temperature system | Use VH's own temperature implementation |

**Tough As Nails integration note:** If TAN is loaded, VH's temperature system should DISABLE its own temperature bar and damage logic, and instead register its zones with TAN's `TemperatureChangeEvent` system, letting TAN handle the HUD and damage. This prevents double-damage. Guard: `ModList.get().isLoaded("toughasnails")`.

---

## 22. FILE & PACKAGE STRUCTURE

### 22.1 Complete Source Tree

```
src/
├── main/
│   ├── java/com/verdigrishive/
│   │   ├── VerdigrisHive.java
│   │   ├── client/
│   │   │   ├── ClientEvents.java
│   │   │   ├── hud/
│   │   │   │   └── TemperatureHudOverlay.java
│   │   │   ├── particle/
│   │   │   │   ├── CorruptionGasParticle.java
│   │   │   │   ├── SolarImpactParticle.java
│   │   │   │   ├── BoneFragmentParticle.java
│   │   │   │   ├── IchorDripParticle.java
│   │   │   │   ├── AmnioticBubbleParticle.java
│   │   │   │   ├── ArcologySporeParticle.java
│   │   │   │   ├── VerdigrisSparkParticle.java
│   │   │   │   └── HeatShimmerParticle.java
│   │   │   └── renderer/
│   │   │       ├── entity/
│   │   │       │   ├── SunblighterRenderer.java
│   │   │       │   ├── WitherVineRenderer.java
│   │   │       │   ├── HelioWardenRenderer.java
│   │   │       │   ├── CableCrawlerRenderer.java
│   │   │       │   ├── SquatterRenderer.java
│   │   │       │   ├── SlasherRenderer.java
│   │   │       │   ├── LurkerRenderer.java
│   │   │       │   ├── CorruptionWheezerRenderer.java
│   │   │       │   ├── BruteRenderer.java
│   │   │       │   ├── LastManRenderer.java
│   │   │       │   ├── VerdigrisSaintRenderer.java
│   │   │       │   ├── SolarBoltRenderer.java
│   │   │       │   └── VerdigrisBoltRenderer.java
│   │   │       └── block/
│   │   │           ├── PulsingWallRenderer.java
│   │   │           └── HydroponicTrayRenderer.java
│   │   ├── common/
│   │   │   ├── block/
│   │   │   │   ├── VHBlocks.java
│   │   │   │   ├── canopy/
│   │   │   │   │   ├── SolarPanelBlock.java
│   │   │   │   │   ├── HydroponicTrayBlock.java
│   │   │   │   │   └── WitherVinePlantBlock.java
│   │   │   │   ├── stack/
│   │   │   │   │   ├── NeonSignBlock.java
│   │   │   │   │   ├── JunctionBoxBlock.java
│   │   │   │   │   └── HangingWireBlock.java
│   │   │   │   ├── plumbing/
│   │   │   │   │   ├── SapPipeBlock.java
│   │   │   │   │   ├── SapReservoirBlock.java
│   │   │   │   │   ├── BrineVatBlock.java
│   │   │   │   │   ├── HeatConduitBlock.java
│   │   │   │   │   └── PressureValveBlock.java
│   │   │   │   ├── flesh/
│   │   │   │   │   ├── PulsingWallBlock.java
│   │   │   │   │   ├── LungBlock.java
│   │   │   │   │   ├── ToothyDoorBlock.java
│   │   │   │   │   ├── CorruptionBlock.java
│   │   │   │   │   └── CorruptionGrowthBlock.java
│   │   │   │   └── portal/
│   │   │   │       └── VerdigrisPortalBlock.java
│   │   │   ├── blockentity/
│   │   │   │   ├── VHBlockEntityTypes.java
│   │   │   │   ├── HydroponicTrayBlockEntity.java
│   │   │   │   ├── SapReservoirBlockEntity.java
│   │   │   │   ├── BrineVatBlockEntity.java
│   │   │   │   ├── PressureValveBlockEntity.java
│   │   │   │   ├── PulsingWallBlockEntity.java
│   │   │   │   ├── LungBlockEntity.java
│   │   │   │   ├── NeonSignBlockEntity.java
│   │   │   │   ├── JunctionBoxBlockEntity.java
│   │   │   │   └── HeatConduitBlockEntity.java
│   │   │   ├── capability/
│   │   │   │   ├── VHCapabilities.java
│   │   │   │   ├── PlayerTemperatureData.java
│   │   │   │   └── VHDimensionSavedData.java
│   │   │   ├── config/
│   │   │   │   └── VHConfig.java
│   │   │   ├── data/
│   │   │   │   └── VHDataGenerator.java
│   │   │   ├── effect/
│   │   │   │   ├── VHMobEffects.java
│   │   │   │   ├── CorruptionResistanceEffect.java
│   │   │   │   └── ElectricalShockEffect.java
│   │   │   ├── entity/
│   │   │   │   ├── VHEntityTypes.java
│   │   │   │   ├── ILimbSegmented.java
│   │   │   │   ├── LimbSlot.java
│   │   │   │   ├── BossEntity.java
│   │   │   │   ├── ai/
│   │   │   │   │   ├── ClimbToPlayerGoal.java
│   │   │   │   │   ├── HideBehindWallGoal.java
│   │   │   │   │   ├── ReturnToNestGoal.java
│   │   │   │   │   ├── ChargeGoal.java
│   │   │   │   │   └── ClimberRandomStrollGoal.java
│   │   │   │   └── mob/
│   │   │   │       ├── SunblighterEntity.java
│   │   │   │       ├── WitherVineMobEntity.java
│   │   │   │       ├── HelioWardenEntity.java
│   │   │   │       ├── CableCrawlerEntity.java
│   │   │   │       ├── SquatterEntity.java
│   │   │   │       ├── SlasherEntity.java
│   │   │   │       ├── LurkerEntity.java
│   │   │   │       ├── CorruptionWheezerEntity.java
│   │   │   │       ├── BruteEntity.java
│   │   │   │       ├── LastManEntity.java
│   │   │   │       ├── VerdigrisSaintEntity.java
│   │   │   │       ├── SolarBoltEntity.java
│   │   │   │       └── VerdigrisBoltEntity.java
│   │   │   ├── fluid/
│   │   │   │   ├── VHFluids.java
│   │   │   │   ├── VHFluidTypes.java
│   │   │   │   ├── SapFluid.java
│   │   │   │   ├── BrineFluid.java
│   │   │   │   ├── IchorFluid.java
│   │   │   │   └── AmnioticFluid.java
│   │   │   ├── item/
│   │   │   │   ├── VHItems.java
│   │   │   │   ├── PortalIgniterItem.java
│   │   │   │   ├── SunFlaskItem.java
│   │   │   │   ├── CorruptionWardItem.java
│   │   │   │   ├── ThermometerItem.java
│   │   │   │   ├── MarrowCompassItem.java
│   │   │   │   ├── GuidebookItem.java
│   │   │   │   └── BrassParasolItem.java
│   │   │   ├── loot/
│   │   │   │   └── VHLootModifiers.java
│   │   │   ├── network/
│   │   │   │   ├── VHNetwork.java
│   │   │   │   └── VHTimeSyncPacket.java
│   │   │   ├── portal/
│   │   │   │   └── VerdigrisPortalShape.java
│   │   │   ├── recipe/
│   │   │   │   ├── VHRecipeTypes.java
│   │   │   │   └── VHRecipeSerializers.java
│   │   │   ├── sound/
│   │   │   │   └── VHSoundEvents.java
│   │   │   └── util/
│   │   │       ├── VHConstants.java
│   │   │       └── VHDimensionTimeManager.java
│   │   ├── integration/
│   │   │   └── create/
│   │   │       ├── CreateIntegration.java
│   │   │       ├── CreateRecipeTypes.java
│   │   │       ├── MarrowEngineBlockEntity.java
│   │   │       └── contraption/
│   │   │           └── HiveContraptionHandler.java
│   │   ├── mixin/
│   │   │   └── MixinClientLevel.java
│   │   ├── server/
│   │   │   ├── ServerEvents.java
│   │   │   ├── TemperatureEvents.java
│   │   │   ├── CorruptionEvents.java
│   │   │   └── DimensionEvents.java
│   │   └── worldgen/
│   │       ├── biome/
│   │       │   └── VHBiomes.java
│   │       ├── dimension/
│   │       │   └── HiveChunkGenerator.java
│   │       ├── feature/
│   │       │   ├── VHConfiguredFeatures.java
│   │       │   ├── VHPlacedFeatures.java
│   │       │   ├── VoidCarver.java
│   │       │   ├── ArcologyTreeFeature.java
│   │       │   └── TrainShaftFeature.java
│   │       ├── noise/
│   │       │   └── VHNoiseSettings.java
│   │       └── structure/
│   │           ├── VHStructureTypes.java
│   │           ├── VHStructureSets.java
│   │           ├── LungAtriumManager.java
│   │           ├── LungAtriumActivationProcessor.java
│   │           ├── VHJigsawAnchor.java
│   │           └── processor/
│   │               ├── BrokenContraptionProcessor.java
│   │               └── RotVariantProcessor.java
│   └── resources/
│       ├── META-INF/
│       │   └── neoforge.mods.toml
│       ├── assets/verdigrishive/
│       │   ├── blockstates/         ← generated
│       │   ├── lang/
│       │   │   └── en_us.json
│       │   ├── models/
│       │   │   ├── block/           ← generated
│       │   │   └── item/            ← generated
│       │   ├── particles/
│       │   ├── sounds.json
│       │   └── textures/
│       │       ├── block/           ← 32×32 .png files
│       │       ├── entity/          ← 64×32 or 64×64 .png files
│       │       ├── gui/
│       │       │   └── temperature_bar.png
│       │       ├── item/
│       │       └── particle/
│       └── data/verdigrishive/
│           ├── advancements/        ← generated
│           ├── damage_type/
│           │   ├── heatstroke.json
│           │   └── frostbite.json
│           ├── dimension/
│           │   └── verdigris_hive.json
│           ├── dimension_type/
│           │   └── verdigris_hive.json
│           ├── loot_tables/         ← generated
│           ├── neoforge/
│           │   └── biome_modifier/
│           │       └── add_verdant_arcology.json
│           ├── recipes/             ← generated
│           ├── structures/
│           │   ├── bosco_spire/     ← hand-built NBT
│           │   ├── andesite_refinery/
│           │   ├── heliodome/
│           │   ├── lung_atrium/
│           │   ├── marker_spire/
│           │   ├── verdigris_throne/
│           │   ├── cabling_slums/
│           │   └── arcology_tower/
│           ├── tags/                ← generated
│           └── worldgen/
│               ├── biome/
│               ├── configured_feature/
│               ├── multi_noise_biome_source_parameter_list/
│               ├── noise/
│               ├── noise_settings/
│               ├── placed_feature/
│               ├── structure/
│               ├── structure_set/
│               └── template_pool/
```

---

## APPENDIX A — KNOWN TECHNICAL RISKS

| Risk | Severity | Mitigation |
|---|---|---|
| Jigsaw vertical generation limit | HIGH | Anchor chain system (§9.1); fallback to interval placement |
| Create API changes between versions | MEDIUM | Wrap all Create calls behind interface; only access via `integration.create` package |
| MultiNoise biome calibration | MEDIUM | Use debug worldgen visualization mode; iterate JSON values empirically |
| Pulsing wall hitbox collision | LOW | Cap scale change to ±0.1 blocks; test with EntityPushedByEntityEvent |
| Limb targeting synchronization | MEDIUM | Use EntityDataSerializer for all limb health data; never infer state client-side |
| Mixin compatibility | MEDIUM | Target only one method; add `require = 1` to fail fast if method signature changes |
| SavedData corruption on crash | LOW | Always write `setDirty()` before any risky operation; use try/catch in spread logic |
| NeoForge API changes in 1.21.1 patch | LOW | Pin NeoForge version in `gradle.properties`; do not use `+` wildcard versioning |

## APPENDIX B — IMPLEMENTATION ORDER RECOMMENDATION

Build in this order to de-risk early and enable playtesting at each stage:

1. Mod scaffolding (§1, §2, §3) — no gameplay, just structure
2. Core blocks + items (§4, §5) — can test in creative
3. Dimension + basic world gen (§7.1–7.5) — entering the dimension
4. Temperature + day/night (§11, §13) — core survival loop
5. Biomes (§7.6, §8) — visual identity
6. Two structures: Cabling Slums + Andesite Refinery (§9) — exploration
7. Three mobs: Sunblighter, Slasher, Lurker (§10) — combat
8. Fluids + corruption (§6, §12) — resource loop
9. Remaining structures + mobs (§9, §10) — content density
10. Create integration (§14) — automation layer
11. Overworld biome (§8) — counterpart content
12. Boss encounters (§10.4, §10.13) — endgame
13. Data gen + loot + advancements (§15, §18) — polish
14. Performance pass (§19) — mandatory before release
15. Sound assets replacement (§16) — final polish
