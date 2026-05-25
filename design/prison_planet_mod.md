# Prison Planet — Custom Dimension Mod Design
**Target**: Minecraft 1.21.1 + NeoForge  
**Scope**: Large (10+ enchantments, 5+ villager professions)

---

## 1. Concept & Theme

A dimension used as a cosmic disposal site — a place where things are sent to be destroyed. Inspired by:
- **Mad God**: hellish industrial machinery, chains, grotesque condemned souls
- **Doom's demon dimension**: ancient evil, flesh-metal architecture, demonic runes, fortresses of torment
- **Crematoria (Riddick)**: scorching lethal sun, freezing lethal night, underground survival, predators tied to time of day

The planet itself is the prison. The environment is the jailer. Players must understand, adapt to, and ultimately master the 4-phase cycle to survive and extract the dimension's unique rewards.

---

## 2. Dimension Properties

| Property | Value |
|---|---|
| Dimension ID | `prisonplanet:the_condemned` |
| Sky | Custom — ash-red during day, deep violet at dusk, void-black at night, pale blue-white at dawn |
| Ceiling | No bedrock ceiling (open sky) |
| Ambient light | Low (0.05) — always dark inside structures |
| Has skylight | Yes |
| Has precipitation | No (snow is handled manually by the hazard system, not vanilla weather) |
| Ultrawarm | No (custom hazard system handles heat and cold) |
| Natural spawning | Yes, time-gated |

---

## 3. Extended Day/Night Cycle

Total cycle length: **192,000 ticks** (8× standard Minecraft day)

Each phase is exactly **48,000 ticks** (2 standard Minecraft days).

| Phase | Tick Range | Sky Analog | Player Hazard | Block Effects |
|---|---|---|---|---|
| **Day** | 0 – 47,999 | Dawn → Noon | Solar burn (if sky-exposed) | Stone→lava, water evaporates, flammables ignite |
| **Sunset** | 48,000 – 95,999 | Noon → Dusk | None | Lava solidifies back to stone |
| **Night** | 96,000 – 143,999 | Dusk → Midnight | Freeze (if not near heat) | Snowfall (heavy, uncapped), water→ice |
| **Sunrise** | 144,000 – 191,999 | Midnight → Dawn | None | Snow melts |

### Implementation Approach
- `PrisonPlanetSavedData` (extends `SavedData`) stores a `long cycleTick` (0–191,999) per world save.
- A `ServerTickEvent` increments `cycleTick` and calls `ServerLevel.setDayTime(cycleTick / 8)` to drive the vanilla sky renderer at ⅛ speed (one visual sky rotation = 192,000 ticks).
- Phase detection: `phase = cycleTick / 48000` (integer division, mod 4) → 0=DAY, 1=SUNSET, 2=NIGHT, 3=SUNRISE.
- Phase transitions are detected by comparing `prevPhase != currentPhase` — triggers the `PhaseTransitionEvent` (custom event) which initialises batch block updates.
- Sky color overrides, fog color, and cloud rendering handled client-side via a custom `DimensionSpecialEffects` subclass.

---

## 4. Environmental Conditions

### 4.0 Applicability Rules (applies to ALL conditions below)

Both player hazards and block-modification effects are **gated by two conditions**:

1. **Y-level threshold**: The block or player must be at or above **Y = 50**. Below this level, the planet's crust provides insulation — neither heat nor cold penetrates. This makes underground areas and the Catacomb Depths biome naturally safe.
2. **Sky exposure**: The block or player position must have a sky light level **≥ 7** (roughly equivalent to being able to "see" the sky). Roofed structures block both conditions entirely. A single-layer roof is sufficient to provide shelter.

If either condition is not met, no player hazard or block effect applies at that position.

---

### 4.1 Day Phase — The Burning Sun

#### Player Hazard
- **Trigger**: Player in `the_condemned`, phase == Day, position passes both applicability rules.
- **Effect**: `player.setSecondsOnFire(3)` applied every 40 ticks (~2 seconds). Damage scales with sky light level: sky light 15 = full damage, sky light 7 = ~half damage (partial shade reduces but does not eliminate burn).
- **Exemptions**: Fire Resistance potion effect; Ash Walker enchantment (boots); full shadow (sky light < 7); underground (Y < 50).
- **Transition grace period**: 200 ticks (10 seconds) after entering Day from Sunrise before burn begins, so players aren't caught off-guard mid-travel.

#### Block Effects — Solar Heat

**Stone → Lava**
- Exposed `CondemnedStoneSurface` blocks (custom surface block replacing standard stone in the dimension) with sky light ≥ 7 and Y ≥ 50 gradually melt during Day.
- Conversion is not instant — uses `randomTick()` on the block with a high probability, so full melting of a large exposed area takes ~2–5 minutes into the Day phase. This gives players a window at the start of Day to move before the ground becomes deadly.
- Melted state behaves as a lava source: emits light level 15, deals fire damage on contact, spreads fire to adjacent flammable blocks.
- On chunk load during Day: block checks current phase and sky light, schedules conversion if exposed.
- Tracking: melted blocks are stored as a `MOLTEN` blockstate boolean on `CondemnedStoneSurface`. No separate lava block is placed — the same block changes state — so Sunset reversion is clean without needing to track which lava blocks were created by the system vs. placed naturally.

**Water Evaporation**
- Custom `CondemnedWaterBlock` (fluid block specific to the dimension — replaces vanilla water in worldgen) evaporates during Day when sky light ≥ 7 and Y ≥ 50.
- On random tick: if exposed during Day, convert to air. Source blocks evaporate first; flowing blocks dissipate naturally afterward.
- If a player attempts to place vanilla water in exposed Day conditions, a `BlockPlaceEvent` handler immediately schedules evaporation within 5 ticks (gives a brief visual puff then disappears).
- **Design note**: Cauldrons, underground pools, and covered containers are the only reliable water sources during Day. This makes water a strategic resource.

**Flammable Block Ignition**
- Flammable blocks (matching `minecraft:igniteable_by_lava` tag or similar) with sky light ≥ 7 and Y ≥ 50 have a chance to spontaneously ignite during Day.
- Implemented via `RandomTickEvent`: on tick, roll chance (configurable, default ~5% per random tick). If pass, place a fire block on top of or adjacent to the flammable block.
- This does NOT use vanilla fire spread — only direct solar ignition. Once lit, vanilla fire spread handles the rest.
- Structures built from flammable materials that are roofed are safe. Open-air wood structures will burn.

---

### 4.2 Sunset Phase — The Cooling

#### Player Hazard
- **None.** This is a safe transition window. Players can move freely.

#### Block Effects — Heat Dissipation

**Lava → Stone Reversion**
- All `CondemnedStoneSurface` blocks in the `MOLTEN` state revert to their solid `STONE` state during Sunset.
- Reversion uses the same `randomTick()` approach as melting — gradual over ~2–5 minutes from the start of Sunset.
- On `PhaseTransitionEvent` (Day → Sunset): a batch scan of all loaded chunks within 3 chunk-radii of each player is triggered to catch any blocks that won't receive a random tick soon. Processed in batches of 64 blocks per server tick to avoid lag spikes.
- Any vanilla fire that was lit by the ignition system continues burning — Sunset does not extinguish it.

---

### 4.3 Night Phase — The Killing Cold

#### Player Hazard
- **Trigger**: Player in `the_condemned`, phase == Night, position passes both applicability rules, AND no heat source is within 5 blocks.
- **Effect**: Increment `player.setTicksFrozen()` by 8 per tick. At max freeze (140 ticks), player takes 1 heart of damage every 2 seconds (identical to powder snow behavior). Slowness I applied at 50% freeze. Slowness II applied at 100% freeze.
- **Heat Source Check** (performed every 10 ticks per player): scan a 5-block sphere around the player for any block matching the `prisonplanet:heat_sources` tag. Tag includes:
  - `minecraft:fire`, `minecraft:soul_fire`
  - `minecraft:campfire` (lit state only), `minecraft:soul_campfire` (lit)
  - `minecraft:furnace`, `minecraft:smoker`, `minecraft:blast_furnace` (all lit-state only)
  - `minecraft:lava` (and `CondemnedStoneSurface` in MOLTEN state — but note: stone is solid by Night, so this only applies to naturally placed lava)
  - `prisonplanet:phase_lantern` (custom block — primary player-placed heat source)
- If inside a heat source radius, freeze accumulation stops and existing freeze ticks deplete at 4/tick.
- **Exemptions**: Frost Ward enchantment reduces freeze accumulation (level 1: −50%, level 2: −80%, level 3: immunity); underground (Y < 50); indoors (sky light < 7).
- **Transition grace period**: 200 ticks (10 seconds) after Night begins before freeze starts building.

#### Block Effects — Deep Freeze

**Snowfall (Heavy, Uncapped)**
- Server-side `SnowAccumulationHandler` runs on `ServerTickEvent`. Every 20 ticks, for each loaded chunk in `the_condemned`, scan the topmost exposed block in each column (sky light ≥ 7, Y ≥ 50).
- Place a snow layer on top (or increment `LAYERS` if already a `SnowLayerBlock`).
- When a `SnowLayerBlock` reaches maximum vanilla layers (8), it converts to a full `minecraft:snow_block`. Additional snow is then placed as new `SnowLayerBlock` on top of the snow block. This continues indefinitely — snow stacks upward as full blocks, each one capped with a partial layer.
- Accumulation rate (ticks between snowfall in a column): scales over the Night phase.
  - Early Night (96,000–112,000): 1 layer per ~60 seconds (slow start)
  - Mid Night (112,000–128,000): 1 layer per ~20 seconds (accelerating)
  - Deep Night (128,000–143,999): 1 layer per ~8 seconds (blizzard)
- By the end of a full Night phase, open areas can accumulate 3–6 full blocks of snow depth.
- Structures with complete roofs are fully protected. Structures with gaps will have snow pouring in through openings.
- The `SnowAccumulationHandler` checks per-column once; it does not process every block every tick.

**Water → Ice**
- `CondemnedWaterBlock` with sky light ≥ 7 and Y ≥ 50 freezes during Night.
- On `randomTick()` during Night: source blocks convert to `minecraft:packed_ice` (not regular ice — regular ice would re-melt immediately on Sunrise; packed ice persists until actively broken or melted by a nearby heat source). Flowing water converts to `minecraft:ice`.
- Underground and covered water is unaffected.

---

### 4.4 Sunrise Phase — The Thaw

#### Player Hazard
- **None.** Safe transition window. Freeze ticks deplete naturally (4/tick) even without heat sources.

#### Block Effects — Melt

**Snow Melts**
- `SnowMeltHandler` runs on `ServerTickEvent`. Every 20 ticks, for each loaded chunk, scan columns for snow layers and snow blocks at the top of the snow stack.
- Remove one snow layer (or decrement `LAYERS` by 1) per column per scan. Snow melts from the top down.
- Rate: ~1 layer per 8 seconds at start of Sunrise, accelerating to 1 layer per 3 seconds by mid-Sunrise. The goal is that all accumulated snow is fully gone before Day begins.
- **Balance check**: If the dimension hasn't been visited in multiple cycles, a large amount of snow may have accumulated. A bulk-clear pass fires on `PhaseTransitionEvent` (Sunrise begin) for chunks far from players.

**Ice → Water Reversion**
- `minecraft:packed_ice` and `minecraft:ice` blocks (those created by the Night system) revert to `CondemnedWaterBlock` on the start of Sunrise.
- Tracked via a custom block tag `prisonplanet:night_created_ice`. Any ice not in this tag (e.g. placed by players) is NOT automatically reverted.

---

### 4.5 Cross-Phase Notes

- All block effect systems are **skipped for unloaded chunks**. Chunks load and immediately check current phase, running a catch-up pass (max 256 blocks processed in that first tick to avoid stutter).
- The `CondemnedStoneSurface` block's catch-up logic: on `ChunkWatchEvent` (chunk becomes loaded), all exposed surface blocks schedule a `randomTick` within the next 200 ticks. This prevents popping transitions.
- **Server performance target**: All tick-based handlers are guarded by per-chunk cooldowns and batch limits. Total additional server tick cost should stay under 0.5ms on a loaded vanilla-scale server.

---

## 5. Structures

All structures use Jigsaw-based generation where possible for modular reuse of pieces.

| Structure | Size | Rarity | Description |
|---|---|---|---|
| **The Warden's Citadel** | Massive | Very rare | Central fortress dominating the biome. Multi-floor dungeon, boss room, unique loot vault. Fully roofed — safe from all hazards inside. |
| **Condemned Prison Complex** | Large | Rare | Multi-wing cell block, guard rooms, execution hall. Primary source of enchanted books. Partially roofed. |
| **Disposal Pit** | Medium | Uncommon | Open crater with processing machinery, mob spawners, salvage loot. Mostly exposed — dangerous during Day/Night. |
| **Guard Tower** | Small | Common | Lone towers scattered across the landscape. Stone construction — roof provides safety. Contains a Jailer villager. |
| **Underground Catacomb** | Medium | Uncommon | Entirely below Y=50 — completely immune to all hazards. Subterranean network with buried loot. |
| **Execution Grounds** | Medium | Rare | Open arena, pillory structures, scattered bones. Entirely exposed — hazardous, high-reward. |
| **Condemned's Hovel** | Tiny | Common | Survivor shanty shelters. Roofed, modest loot, sometimes a Prisoner villager. Intended as emergency refuge. |
| **Ritual Site** | Small | Uncommon | Cultist circle, altar, strange runes. Open-air but the ritual altar itself is shaded. |

Structure design principle: **roof = safety**. Structures should clearly communicate whether they offer shelter through their visual design.

---

## 6. Custom Enchantments

All enchantments are obtainable **exclusively** as enchanted books found in the dimension's structure loot tables. Not available from enchanting tables or librarian villagers outside the dimension.

Enchantments are fully data-driven (1.21 JSON format under `data/prisonplanet/enchantment/`).

| # | Name | Slot | Effect |
|---|---|---|---|
| 1 | **Ash Walker** | Boots | Full immunity to solar burn hazard. Passive Fire Resistance in the dimension. |
| 2 | **Frost Ward** | Chestplate | Levels 1–3 reduce freeze accumulation (−50% / −80% / immune). Worn near any heat source: freeze depletes 2× faster. |
| 3 | **Condemned's Resolve** | Chestplate | At <30% HP, triggers Resistance II for 5 seconds. 30s cooldown. |
| 4 | **Shackle Break** | Boots | Movement speed scales inversely with HP (max +50% speed at 1 heart). |
| 5 | **Warden's Wrath** | Sword | Bonus damage vs. all mobs native to `the_condemned` (+2/+4/+6 dmg by level). |
| 6 | **Soulchain** | Sword | On hit, applies Slowness I for (2 × level) seconds. Visually represented as spectral chains. |
| 7 | **Infernal Temper** | Sword | Melee hits deal additional fire damage (+2/+4/+6). Incompatible with Tundra's Embrace. |
| 8 | **Tundra's Embrace** | Sword | Melee hits inflict frost slow: Slowness I for (3 × level) seconds. Incompatible with Infernal Temper. |
| 9 | **Void Sight** | Helmet | Permanent Night Vision while in the dimension. |
| 10 | **Scavenger's Eye** | Helmet | Fortune-equivalent bonus on loot chest rolls in the dimension. |
| 11 | **Deathless** | Chestplate | On lethal damage, triggers 3-second invincibility instead of death. 10-minute cooldown. One active save at a time. |
| 12 | **Overseer's Dominion** | Any armor piece | Legendary. Full immunity to both solar burn and deep freeze. Cannot coexist with Ash Walker or Frost Ward on the same armor set. Extremely rare. |

---

## 7. Villager Types & Professions

### Villager Types (visual variants)
New `VillagerType` registrations affecting skin/texture.

| Type ID | Description | Home Biome |
|---|---|---|
| `condemned` | Gaunt, ragged — long-term prisoners | Ash Flats |
| `warden_caste` | Armored, imposing — dimensional guards | Void Cliffs (Citadel) |
| `scavenger` | Patchwork armor, salvaged gear | Scorch Wastes |
| `cultist` | Robed, marked — worshippers of the old order | Condemned's Reach |

### Villager Professions
New `VillagerProfession` registrations with custom job site blocks.

| Profession | Job Site Block | Sells | Buys |
|---|---|---|---|
| **Jailer** | Shackle Post | Chains, restraint tools, dimension-specific armor | Rare mob drops |
| **Warden** | Warden's Desk | High-tier weapons, keys, dimensional access items | Gold, rare resources |
| **Black Market Dealer** | Contraband Crate | Enchanted books (dimension-exclusive), forbidden potions | Emeralds + rare mats |
| **Condemned Prisoner** | Carved Stone | Food, survival supplies, maps to nearby structures | Food, basic materials |
| **Cultist Broker** | Ritual Altar | Potions, ritual items, buff scrolls | Rare creature drops |
| **Scavenger** | Salvage Pile | Salvaged materials, dimension-specific crafting components | Junk / mob drops |
| **Overseer** | Command Throne | Tier-3 equipment, unique upgrade items, Overseer's Dominion book | Many emeralds + boss drops |

---

## 8. Time-Gated Mob Spawning

Phase checked via `PrisonPlanetSavedData` inside `MobSpawnEvent.SpawnPlacementCheck` handler.

### Day (0 – 47,999 ticks)
Heat-adapted surface creatures. Players seek shade.

| Mob | Type | Notes |
|---|---|---|
| Ash Crawler | Arthropod | Pack hunter; spawns in groups of 3–6; avoids shaded areas |
| Slag Golem | Construct | Slow, very high damage; fire immune; patrols molten stone areas |
| Sun Scorpion | Arthropod | Fast, venomous; injects Wither + Poison II |
| Condemned Shade | Undead | Weakened form during day; erratic AI; seeks shade like the player |

### Sunset (48,000 – 95,999 ticks)
Most dangerous phase — all factions become active, mob variety peaks.

| Mob | Type | Notes |
|---|---|---|
| Shadow Stalker | Illager-variant | Stealthy; short-range teleport; strikes from behind |
| Warden Construct | Construct | High HP; patrols structures; aggressive toward trespassers |
| Spectral Prisoner | Ghost/Undead | Partially phases through blocks; chain-pull attack |
| Ritual Cultist | Humanoid | Casts debuffs; summons lesser mobs; targeted first in a fight |

### Night (96,000 – 143,999 ticks)
Cold-adapted predators. Players seek heat sources.

| Mob | Type | Notes |
|---|---|---|
| Frost Wraith | Undead | Inflicts additional freeze on hit; immune to freeze hazard; ignores heat sources |
| Night Crawler | Arthropod | Extremely fast; ambushes at sprint speed; swarms |
| Condemned Horde | Undead | Zombie-like; creates noise that draws more Condemned toward the sound |
| The Overseer (rare) | Boss-tier | Rare elite patrol mob; drops Overseer's Dominion fragment on death |

### Sunrise (144,000 – 191,999 ticks)
Weakened stragglers. Safest surface travel window.

| Mob | Type | Notes |
|---|---|---|
| Wandering Condemned | Undead | Passive unless attacked; shuffles aimlessly |
| Lesser Shade | Undead | Weaker Condemned Shade; low HP, minimal damage |
| Dying Crawler | Arthropod | Half-speed; drops components; non-aggressive |
| Injured Cultist | Humanoid | Minimal combat; may offer one-time trade if not attacked |

---

## 9. Biomes

| Biome | Description | Primary Structure | Hazard Modifier |
|---|---|---|---|
| **Ash Flats** | Flat grey wasteland, ash blocks, dead trees | Prison Complex, Guard Towers | Full hazards |
| **Scorch Wastes** | Cracked terrain, lava seeps, magma blocks | Disposal Pit | Day burn 50% stronger; stone melts faster |
| **Void Cliffs** | Tall stone spires over dark ravines | Warden's Citadel | Normal hazards; cliff faces offer shade |
| **Catacomb Depths** | Underground cavern biome (generates below Y=50 only) | Catacomb, Ritual Sites | Fully immune — no sky exposure possible |
| **Condemned's Reach** | Ruined city remnants, semi-habitable | Hovels, Execution Grounds | Ruins provide partial sky blocking; hazards reduced |

---

## 10. Key Custom Blocks

### Structural Blocks
| Block | Purpose |
|---|---|
| Ash Stone / Ash Stone Bricks | Primary building block; non-reactive to solar system |
| Condemned Stone Bricks | Decorative variant; used in prison structures |
| Shackle Chain (decorative) | Hanging/wall decoration |

### Environmental System Blocks
| Block | Purpose |
|---|---|
| `CondemnedStoneSurface` | Surface stone replacement. Has `MOLTEN` blockstate. Transitions stone↔lava during Day/Sunset. Used in worldgen as the dimension's top layer. |
| `CondemnedWaterBlock` | Dimension-specific water fluid. Evaporates (Day, exposed) or freezes (Night, exposed). Behaves as normal water otherwise. |
| `DeepSnow` | Extended snow accumulation. Stacks as: SnowLayerBlock (layers 1–8) → full snow block → new SnowLayerBlock on top. Handled by SnowAccumulationHandler. |
| Phase Lantern | Craftable heat source. Emits heat aura (5-block radius) protecting against Night freeze. Light level 12. Craft from dimension-specific materials. |

### Villager Job Site Blocks
| Block | Profession |
|---|---|
| Shackle Post | Jailer |
| Warden's Desk | Warden |
| Contraband Crate | Black Market Dealer |
| Carved Condemned Stone | Condemned Prisoner |
| Ritual Altar | Cultist Broker |
| Salvage Pile | Scavenger |
| Command Throne | Overseer |

---

## 11. Technical Architecture

```
prisonplanet/
├── PrisonPlanetMod.java
│
├── core/
│   ├── ModBlocks.java
│   ├── ModItems.java
│   ├── ModEntityTypes.java
│   ├── ModVillagerTypes.java
│   ├── ModVillagerProfessions.java
│   ├── ModEnchantments.java
│   ├── ModStructures.java
│   └── ModBiomes.java
│
├── dimension/
│   ├── PrisonPlanetSavedData.java        — Stores cycleTick (0–191,999), dirty-chunk queues
│   ├── CyclePhase.java                   — Enum DAY/SUNSET/NIGHT/SUNRISE; phase(cycleTick) helper
│   ├── PhaseTransitionEvent.java         — Custom NeoForge event fired on phase change
│   ├── CycleTickHandler.java             — ServerTickEvent: tick+sync; fires PhaseTransitionEvent
│   │
│   ├── hazard/
│   │   ├── EnvironmentalHazardHandler.java — LivingEntityTickEvent: burn/freeze per player
│   │   └── HeatSourceChecker.java          — Utility: scans 5-block sphere for heat_sources tag
│   │
│   └── blockeffects/
│       ├── BlockExposureHelper.java        — Utility: checks Y≥50 AND skyLight≥7
│       ├── PhaseBlockUpdateHandler.java    — PhaseTransitionEvent: batch-scans loaded chunks
│       ├── SnowAccumulationHandler.java    — ServerTickEvent: Night snowfall per column
│       └── SnowMeltHandler.java            — ServerTickEvent: Sunrise snow removal per column
│
├── block/
│   ├── CondemnedStoneSurfaceBlock.java    — MOLTEN blockstate; randomTick for melt/solidify
│   ├── CondemnedWaterBlock.java           — Fluid block; evaporate/freeze logic in randomTick
│   └── PhaseLanternBlock.java             — Heat aura block; light level 12
│
├── entity/
│   ├── mobs/                              — 12 custom mob classes
│   └── villager/
│       └── ModVillagerTrades.java
│
├── structures/
│   ├── ModStructureTypes.java
│   └── pieces/
│
├── worldgen/
│   └── ModWorldGenProvider.java
│
└── events/
    ├── SpawnControlHandler.java
    └── client/
        └── ClientDimensionEffects.java    — Sky color, fog, custom rendering (client-only)
```

### Data Resources Layout
```
resources/data/prisonplanet/
├── dimension/
│   └── the_condemned.json
├── dimension_type/
│   └── the_condemned_type.json
├── enchantment/
│   └── [12 enchantment JSON files]
├── loot_table/
│   └── chests/ [per-structure loot tables]
├── worldgen/
│   ├── biome/ [5 biome JSONs]
│   ├── noise_settings/
│   │   └── the_condemned.json
│   ├── structure/ [8 structure JSONs]
│   ├── structure_set/
│   │   └── the_condemned_structures.json
│   └── template_pool/ [jigsaw pool JSONs]
└── tags/
    ├── blocks/
    │   ├── heat_sources.json              — Blocks that prevent Night freeze
    │   ├── solar_reactive.json            — Blocks affected by Day sun (custom stone)
    │   └── night_created_ice.json         — Ice blocks created by Night system (for targeted melt)
    └── entity_types/
        └── prison_planet_spawns_[day|sunset|night|sunrise].json
```

---

## 12. Implementation Phases

### Phase 1 — Core Dimension (Foundation)
- [ ] Mod scaffolding (NeoForge 1.21.1 gradle setup, main class, mods.toml)
- [ ] Dimension type + dimension JSON registration
- [ ] Basic noise terrain (Ash Flats biome, placeholder blocks)
- [ ] `PrisonPlanetSavedData` + `CycleTickHandler` — time system functional
- [ ] `CyclePhase` enum + `PhaseTransitionEvent`
- [ ] Debug HUD overlay (current phase, cycleTick, sky light at foot position)

### Phase 2 — Environmental Hazards
- [ ] `BlockExposureHelper` (Y + sky light check)
- [ ] `EnvironmentalHazardHandler` (burn during Day, freeze during Night)
- [ ] `HeatSourceChecker` + `prisonplanet:heat_sources` tag
- [ ] Grace period logic on phase transitions
- [ ] Sky color + fog overrides per phase (client `DimensionSpecialEffects`)
- [ ] Phase Lantern block

### Phase 3 — Block Modification Systems
- [ ] `CondemnedStoneSurface` block (MOLTEN blockstate, melt/solidify randomTick)
- [ ] `PhaseBlockUpdateHandler` (batch scan on phase transition)
- [ ] `CondemnedWaterBlock` (evaporation during Day, freeze during Night)
- [ ] `SnowAccumulationHandler` (Night snowfall, uncapped stacking)
- [ ] `SnowMeltHandler` (Sunrise snow removal)
- [ ] Chunk catch-up pass on load (blocks sync to current phase)

### Phase 4 — Structures
- [ ] Custom blocks (Ash Stone family, all 7 job site blocks)
- [ ] 2–3 starter structures with loot tables (Guard Tower, Condemned's Hovel, Disposal Pit)
- [ ] Remaining 5 structures
- [ ] Jigsaw pools for modular prison complex pieces

### Phase 5 — Enchantments
- [ ] All 12 enchantment JSON definitions
- [ ] Custom enchantment effect components for: Condemned's Resolve, Shackle Break, Deathless, Overseer's Dominion
- [ ] Loot table integration (books distributed across structure chests by rarity)

### Phase 6 — Villagers & Mobs
- [ ] 4 villager types (textures + registration)
- [ ] 7 villager professions + job site blocks
- [ ] Trade definitions per profession + leveling
- [ ] 12 custom mob entity classes + AI goals + placeholder textures

### Phase 7 — Mob Spawning Schedule
- [ ] Entity type tags per phase
- [ ] `SpawnControlHandler` hooked into `MobSpawnEvent`
- [ ] Biome-specific spawn weight tuning per phase

### Phase 8 — Polish & Balance
- [ ] All 5 biomes with unique terrain
- [ ] Sound events per phase transition (ambient, tension build)
- [ ] Particle effects (ash during Day, snow/frost during Night, steam during Sunset)
- [ ] Full balance pass: hazard damage, accumulation rates, enchantment power, mob difficulty
- [ ] Advancements tree for the dimension

---

## 13. Open Design Questions

1. **Dimension access**: Portal structure, crafted key, specific ritual, or boss drop?
2. **Dimension exit**: Player-built exit portal, or fixed exits inside structures only?
3. **The Overseer as boss**: Full multi-phase boss fight, or rare elite patrol mob?
4. **Condemned Armor Set**: Craftable dimension armor providing partial hazard resistance without enchantments?
5. **Biome distribution**: Should the Warden's Citadel biome be separate from Void Cliffs, or generated within it?
6. **Multiplayer sync**: Cycle clock is per-level (all players share the same phase). Is this the desired behavior for servers?
7. **Snow persistence**: Should accumulated snow persist between visits (world save), or reset at chunk load? Persistence is more immersive but requires careful balance.
8. **Flammable block scope**: Should the solar ignition system affect structures (potentially destroying them over time), or only natural terrain blocks?
