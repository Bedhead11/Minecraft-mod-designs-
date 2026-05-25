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

### Portal

- **Frame material**: Netherite Blocks
- **Activation**: Flint and Steel (same mechanic as Nether portal)
- **Shape**: Rectangular frame, same construction rules as a Nether portal
- **Entry**: Stepping into the lit portal transports the player to `the_condemned`
- **Exit**: Player-built only — players must construct an exit portal inside the dimension using Netherite Blocks and Flint and Steel. Some generated structures may contain pre-built exit portals.

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
- **The server phase clock is authoritative.** There is no per-player phase state. All players in the dimension experience the same phase at the same time. Clients receive the current phase via sync packet on dimension entry and on each phase transition.
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
- Applies to **all** flammable blocks with sky exposure, including those placed by players. Open-air construction using flammable materials will burn during the Day phase.

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
- **Snow persists between visits.** Accumulated snow is saved with the chunk and does not reset on load. A chunk that was buried at the end of Night will still be buried when next loaded. Snow only removes during an active Sunrise phase.

**Ice → Water Reversion**
- `minecraft:packed_ice` and `minecraft:ice` blocks (those created by the Night system) revert to `CondemnedWaterBlock` on the start of Sunrise.
- Tracked via a custom block tag `prisonplanet:night_created_ice`. Any ice not in this tag (e.g. placed by players) is NOT automatically reverted.

---

### 4.5 Cross-Phase Notes

- All block effect systems are **skipped for unloaded chunks**. Chunks load and immediately check current phase, running a catch-up pass (max 256 blocks processed in that first tick to avoid stutter).
- The `CondemnedStoneSurface` block's catch-up logic: on `ChunkWatchEvent` (chunk becomes loaded), all exposed surface blocks schedule a `randomTick` within the next 200 ticks. This prevents popping transitions.
- **Server performance target**: All tick-based handlers are guarded by per-chunk cooldowns and batch limits. Total additional server tick cost should stay under 0.5ms on a loaded vanilla-scale server.

---

## 5. World Generation & Structures

### 5.1 Core Philosophy

`the_condemned` is not a landscape with structures in it. **The world IS the structure.** There is no natural terrain, no sky-open wilderness, no horizon of grass and trees. Every block above the deep catacombs is part of a single continuous megastructure — a prison complex of incomprehensible scale that extends to the world border in all directions and has no visible terminus.

The aesthetic target: Doom's demon-industrial architecture crossed with Mad God's grotesque machinery and Crematoria's overwhelming sense of inescapable scale. The player should feel they are deep inside something vast, not exploring something built.

**What the world contains:**
- A near-solid mass of prison stone from the surface down to the catacomb threshold
- Carved out from within: rooms, corridors, silos, vaults, shafts — all negative space inside the mass
- The "surface" is the broken, partially exposed top of the megastructure — not open land
- The deep layer (below Y=0) is where the structure gives way to the natural Catacomb Depths

**What the world does not contain:**
- Open landscapes
- Natural terrain features (mountains, valleys, rivers)
- Sky visible from ground level except through deliberate openings (silo tops, courtyards, collapsed sections)

---

### 5.2 Vertical Layer System

The Y axis is divided into functional strata. These are generation zones, not biomes.

| Stratum | Y Range | Character |
|---|---|---|
| **Surface** | Y=100 – Y=130 | The broken top of the megastructure. Irregular, partially open to sky. Exposed courtyard sections, silo openings, collapsed roofing. Solar and freeze hazards fully active in open sections. |
| **Upper Decks** | Y=50 – Y=100 | Dense multi-floor prison structure. Most named structures exist here. Mix of roofed (safe) and silo-open (hazardous above Y=50) sections. |
| **Lower Decks** | Y=0 – Y=50 | Deeper, older, more damaged sections. Heavier industrial character. Hazards do not penetrate here (below Y=50 threshold). Dimly lit. |
| **Catacomb Depths** | Y=-64 – Y=0 | The structure gives way to natural carved rock. Underground biome. Immune to all surface hazards. Glacial Shards ore generates here. |

**Floor height**: Each "deck" within the Upper and Lower strata is approximately 5–8 blocks tall (ceiling height varies by room type). Decks are not rigidly aligned — offset floors, partial mezzanines, and collapsing ceilings are intentional.

---

### 5.3 Technical Generation Approach

**Substrate**: The chunk generator fills the world with solid prison stone (the dimension's primary block) as base terrain. This is a near-uniform fill with minor noise variation — the structure's mass, not natural landscape. Noise variation creates slight density irregularities (small natural fissures, bulging walls) that prevent the fill from looking algorithmic.

**Carving via Jigsaw structures**: Rooms, corridors, and shafts are placed as Jigsaw structures inside the solid substrate. They carve out (replace the fill with air and structure blocks) rather than building upward. Because the substrate is solid, Jigsaw pieces that fail to connect simply remain walled off — dead ends are a feature.

**Structure density**: Jigsaw placement is frequent — structures are packed close enough that the carved space forms a mostly-continuous network. Gaps of uncarved substrate between structure pieces represent collapsed or sealed sections.

**Multiple root structures**: Rather than a single Jigsaw tree generating from one root, multiple independent root structures are seeded at different XZ positions and Y-levels across each region. This creates overlapping networks that feel organically stacked rather than generated from a single point.

---

### 5.4 Tunnel Catalog

Tunnels are the connective tissue of the megastructure. They vary in cross-section and function. All dimensions are interior clear space (wall blocks are additional).

| Tunnel Type | Cross-section (W×H) | Character | Use |
|---|---|---|---|
| **Crawl Passage** | 1×1.5 (stepped) | Maintenance access, barely passable | Connecting walls between areas, hidden routes |
| **Standard Corridor** | 2×3 | Primary pedestrian passage | Cell block access, room-to-room connections |
| **Wide Corridor** | 4×4 | Main thoroughfares | High-traffic routes between districts |
| **Industrial Tunnel** | 6×6 | Heavy machinery clearance | Processing areas, between major chambers |
| **Grand Tunnel** | 10×20 | Massive — spans multiple floors | Primary arteries of the structure; overwhelmingly large; can feel like an underground road |

Grand Tunnels run both horizontally and (occasionally) diagonally through the structure. Seeing one unexpectedly from a side passage is intended to create a sense of scale disorientation. The 20-block height of a Grand Tunnel spans approximately 2.5 deck heights — rooms and corridors from adjacent decks may open directly into the tunnel wall via grated openings or collapsed sections.

Tunnel walls use a consistent structural block palette (prison stone bricks, reinforced variants, iron bars as grates). Variation in wear/damage level is applied per-section — some tunnels are intact, some have partial collapses with debris.

---

### 5.5 Silo System

Silos are vertical shafts that penetrate multiple decks. They are the primary means of vertical movement through the structure and one of the most visually distinctive features.

| Silo Type | Diameter | Depth | Notes |
|---|---|---|---|
| **Utility Shaft** | 3×3 | 1–2 decks | Cramped vertical access, may have iron bar ladder frames |
| **Standard Silo** | 6×6 | 2–4 decks | Common vertical connector; ring walkways at each deck level |
| **Processing Silo** | 10×10 | 3–5 decks | Larger industrial shafts; machinery remnants on walls |
| **Deep Drop Silo** | 5–15 wide | Full stratum height | Rare; plunges from near-surface to Lower Decks or deeper; viewing one from the top is vertigo-inducing |

Silo interiors are not empty — they have:
- Ring walkways at each deck intersection (partial, often broken)
- Wall-mounted machinery, piping stubs, grated openings from adjacent corridors
- Lava vents or ice deposits depending on the silo's age and condition
- Occasional debris at the bottom

Deep Drop Silos are open to the sky at the top (if they reach the Surface stratum) — this means they are active hazard zones during Day (solar) and Night (snow accumulation into the shaft).

---

### 5.6 Room Catalog

Rooms are terminus or junction nodes in the Jigsaw network. They vary in function and size. Each room type has multiple Jigsaw piece variants to prevent repetition.

| Room Type | Interior Size (W×D×H) | Description |
|---|---|---|
| **Cell Row** | 12×4×3 | A single row of prison cells along one wall, standard-ceiling corridor facing them. Multiple variants: occupied, destroyed, open. |
| **Cell Block** | 20×20×5 | Multi-row cell arrangement, open central floor, guard walkway above. |
| **Bunker Room** | 5×5×3 to 8×8×4 | Small enclosed rooms. Intended as defensive shelters or storage. Heavy door frames, reinforced walls. |
| **Guard Station** | 4×4×3 | Fortified booth at corridor junctions. Window openings (iron bars) overlooking the passage. |
| **Processing Chamber** | 12×12×6 | Mid-scale industrial room. Machinery (decorative), drain channels, ceiling hooks, chains. |
| **Holding Bay** | 8×16×8 | Long, high-ceilinged room. Rows of wall brackets suggest mass containment. Loot and spawner potential. |
| **Engine Room** | 16×16×10 | Large machinery chamber. Multi-tier catwalks, large central structure (non-functional machinery block arrangement). |
| **Vault Chamber** | 8×8×6 | Reinforced door frame (iron doors), sealed feel. Loot concentration. Rare. |
| **Collapsed Section** | Varies | Deliberately ruined — ceiling partially caved in, debris piles, broken wall openings into adjacent spaces. |
| **Courtyard** | 10×10 to 20×20, open top | Enclosed on all sides by walls, no roof. Open to sky. Solar/freeze hazard active. May contain remains of structures within. |

---

### 5.7 Surface Generation

The Surface stratum (Y=100–130) is where the megastructure's roof would be — but it is broken, irregular, and partially collapsed. It is not a flat skyline.

**What the surface looks like:**
- Uneven tops of walls and roofs at different heights — no consistent "ground level"
- Open silo tops breaking through, revealed as dark shafts going down
- Courtyard sections where the roof is entirely absent (hazard zones)
- Collapsed sections where the roof has fallen into the floor below, creating rubble-filled ramps down
- Intact roofed sections where players can walk on top of the structure (solid, protected from solar hazard by the structure below their feet — but the top surface itself is exposed)
- Occasional tall remnants (broken towers, wall stubs) rising above the average roof height

The surface is the most hazardous area (full solar burn during Day, full freeze and snow burial during Night) but is the entry point from portals and the location of above-ground landmarks.

---

### 5.8 Jigsaw Piece Architecture

The Jigsaw system assembles the megastructure from categorised piece pools. Connectors between pieces are typed to enforce size compatibility.

**Connector types (Jigsaw "block name" values):**
- `condemned:horizontal/crawl` — 1×1.5 passage connection
- `condemned:horizontal/standard` — 2×3 corridor connection
- `condemned:horizontal/wide` — 4×4 corridor connection
- `condemned:horizontal/industrial` — 6×6 tunnel connection
- `condemned:horizontal/grand` — 10×20 tunnel connection
- `condemned:vertical/shaft_up` — silo upward connection
- `condemned:vertical/shaft_down` — silo downward connection
- `condemned:room/entry` — room doorway connection (bidirectional)

**Piece pools:**
| Pool | Contains |
|---|---|
| `condemned:surface_roots` | Surface-level root structures — silo openings, courtyard rims, rooftop sections |
| `condemned:upper_rooms` | All room types valid for Upper Decks |
| `condemned:lower_rooms` | Room types for Lower Decks (heavier damage, older character) |
| `condemned:corridors_standard` | Standard and wide corridor pieces with junction variants |
| `condemned:corridors_grand` | Grand Tunnel sections, junctions, and branch openings |
| `condemned:silos` | Silo shaft sections, ring walkway variants, top-cap and bottom-cap pieces |
| `condemned:transitions` | Pieces that bridge between Jigsaw connector types (e.g. standard corridor widening to industrial) |
| `condemned:terminators` | Dead-end caps — collapsed walls, sealed doors, rubble fills |

---

### 5.9 Named Landmark Structures

Within the procedural megastructure, landmark structures exist as large hand-designed Jigsaw roots that override the standard generation in their region. They are placed by a separate `StructureSet` with controlled spacing.

These are districtlevel features — the procedural generation fills in around and between them, with corridors connecting into their Jigsaw entry points.

| Structure | Stratum | Description |
|---|---|---|
| **The Warden's Citadel** | Surface + Upper Decks | The largest landmark. A hand-designed multi-floor fortress with a distinct silhouette visible at the surface. Fully roofed interior. Boss room in the deepest level. |
| **Condemned Prison Complex** | Upper Decks | Multi-wing cell block district. Largest concentration of cells. Primary enchanted book loot source. Partially roofed — some wings exposed. |
| **Disposal Pit** | Surface + Upper Decks | An open-top processing area. Silos leading to machinery below. Hazardous (exposed). |
| **Guard Tower** | Surface | Surface-level towers. Structural remnants rising above the roof. Stone construction, roofed at the top. |
| **Underground Catacomb** | Catacomb Depths | Entirely below Y=0. A natural-feeling cavern network embedded in the base of the structure. Hazard-immune. |
| **Execution Grounds** | Surface | A large open-top courtyard with constructed features inside (pillory, arena markers). Fully exposed. |
| **Condemned's Hovel** | Surface / Upper Decks | Small refuge structures in collapsed surface sections — built into rubble rather than the prison proper. |
| **Ritual Site** | Upper Decks | Mid-structure location. A carved chamber with ritual markings. Not an open-top structure. |

Structure design principle: **roof = safety**. Structures must clearly communicate shelter status through visual design. Pre-built exit portals may be included in select landmarks — a per-structure decision made during NBT design.

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
| 13 | **Inbuilt Afterburner** | Elytra only | While gliding (`isFallFlying()`), pressing sprint applies a directional velocity burst. Level 1: ~50% firework power, 5s cooldown. Level 2: ~75% power, 3s cooldown. Level 3: ~100% power, 1.5s cooldown. No item consumed, no explosion — purely velocity. Works in all dimensions. Cooldown tracked per-player via `DataAttachment`. Compatible with Mending and Unbreaking. Does not prevent simultaneous firework use. |

---

## 7. Custom Armor

### Permafrost Armor Set

**Concept**: Armor forged from Glacial Shards — a crystalline mineral that forms exclusively in the Catacomb Depths biome below Y=50. The sustained cold of Night phases, trapped and compressed underground over geological time, has created mineral ice that doesn't melt even at the surface. Paradoxically, the coldness it stores is so extreme that it insulates completely against external heat.

**Material — Glacial Shard**:
- Ore vein in the Catacomb Depths biome, below Y=50
- Requires diamond-tier pickaxe or better
- Cannot be smelted or heated — heat causes it to shatter into powder (invalid for crafting)
- Drops 1–3 shards per ore block; Fortune applies

**Set Pieces**: Helmet, Chestplate, Leggings, Boots (standard 4-piece set)

**Defense**: Comparable to iron armor in base armor value — the specialization is in its unique properties, not raw defense.

**Incremental Effects** (cumulative by pieces worn):

| Pieces | Effect |
|---|---|
| 1 piece | Permanent Fire Resistance I while the piece is equipped |
| 2 pieces | Solar burn hazard damage reduced by 50% |
| 3 pieces | Solar burn hazard fully negated (sun immunity) |
| 4 pieces (full set) | Lava contact deals 0 HP damage. Instead, lava deals durability damage to armor pieces at the rates below. |

**Durability Damage Triggers** (apply regardless of how many pieces are worn):

| Source | Durability Lost |
|---|---|
| Submerged in lava | 2 per second per submerged piece |
| `CondemnedStoneSurface` in MOLTEN state (contact) | 2 per second per contacting piece |
| Standing in block fire | 1 per 2 seconds per piece |
| Flaming arrow / fire charge hit | 5 per hit to the piece in that hit slot |
| Fire Aspect melee hit | 3 per hit to the piece in that hit slot |

**Design Intent**: Permafrost Armor is heat-resistance only. The full set counters the Day phase hazard entirely but offers zero protection against Night freeze — intentional asymmetry. A player wearing the full set can walk through the sun unharmed but will still freeze to death if they don't find heat at Night. It also creates a resource-pressure mechanic: players who use the armor aggressively near molten stone will need to repair it regularly. The Mending Flame block is the primary repair mechanism for this armor.

**Repairability**: Via Mending Flame (primary), or anvil with Glacial Shards. Standard Mending enchantment also works.

---

## 8. Villager Types & Professions

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

## 9. Time-Gated Mob Spawning

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

## 10. Biomes

| Biome | Description | Primary Structure | Hazard Modifier |
|---|---|---|---|
| **Ash Flats** | Flat grey wasteland, ash blocks, dead trees | Prison Complex, Guard Towers | Full hazards |
| **Scorch Wastes** | Cracked terrain, lava seeps, magma blocks | Disposal Pit | Day burn 50% stronger; stone melts faster |
| **Void Cliffs** | Tall stone spires over dark ravines | Warden's Citadel | Normal hazards; cliff faces offer shade |
| **Catacomb Depths** | Underground cavern biome (generates below Y=50 only) | Catacomb, Ritual Sites | Fully immune — no sky exposure possible |
| **Condemned's Reach** | Ruined city remnants, semi-habitable | Hovels, Execution Grounds | Ruins provide partial sky blocking; hazards reduced |

---

## 11. Key Custom Blocks

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

### Utility Blocks

#### Mending Flame

A supernatural repair forge. Visually: a blue-white flame (closer to soul fire in color) burning from a stone basin. Works anywhere — once crafted from dimension materials, it is portable and usable in any dimension.

**GUI**: Three slots — Item Input, Fuel Input, Output — plus a toggle button: **Purify Curses** (off by default).

**Fuel → Mending Points (MP) conversion** (1 MP = 1 durability restored):

| Fuel Item | MP Provided |
|---|---|
| Charcoal / Coal | 50 MP |
| Wood log | 20 MP |
| Blaze Rod | 200 MP |
| Condemned Fuel (dimension item) | 500 MP |
| Lava Bucket | 1,000 MP (returns empty bucket) |

**Repair rate**: 1 durability per 2 ticks (~10 HP/second). Not instant for large repairs — encourages leaving items in the block and returning.

**Anvil repair**: Accepts Chipped Anvil and Damaged Anvil as items in the Input slot.
- Damaged Anvil → Chipped Anvil: 500 MP
- Chipped Anvil → Anvil: 750 MP

**Curse removal** (when Purify toggle is ON):
- No extra fuel cost beyond what is needed to repair the item's durability
- All enchantments matching `minecraft:curse` tag are stripped on completion
- The player must manually enable Purify — curses are never removed without consent
- **Open question**: If an item is already at full durability, purification would cost 0 fuel. Decide whether a minimum fuel cost applies in this case.

**Interaction with Permafrost Armor**: The primary intended repair method for Permafrost Armor. Glacial Shards can also be used as a repair material in the fuel slot (treated as 300 MP per shard) specifically for armor — this is a secondary use of the shard material beyond crafting.

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

## 12. Technical Architecture

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
│   ├── PhaseLanternBlock.java             — Heat aura block; light level 12
│   └── MendingFlameBlock.java            — Container block; opens MendingFlameMenu on use
│
├── item/
│   ├── armor/
│   │   ├── PermafrostArmorMaterial.java   — ArmorMaterial: tier stats, repair ingredient (GlacialShard)
│   │   └── PermafrostArmorItem.java       — ArmorItem subclass; tracks durability-from-lava/fire logic
│   └── GlacialShardItem.java             — Raw material; also used as Mending Flame fuel (300 MP/shard)
│
├── menu/
│   ├── MendingFlameMenu.java             — AbstractContainerMenu: 3 slots + purify toggle state
│   └── MendingFlameScreen.java           — Screen (client): renders slots + toggle button
│
├── enchantment/
│   └── AfterburnerEnchantment.java       — Stores level; actual boost applied in SprintFlyHandler
│
├── entity/
│   ├── mobs/                              — 12 custom mob classes
│   └── villager/
│       └── ModVillagerTrades.java
│
├── structures/
│   ├── ModStructureTypes.java
│   └── pieces/                               — Landmark structure piece classes (if non-Jigsaw)
│
├── worldgen/
│   ├── ModWorldGenProvider.java              — DatapackBuiltinEntriesProvider bootstrap
│   ├── PrisonChunkGenerator.java            — Custom ChunkGenerator: fills substrate, no natural terrain
│   └── PrisonCarverHelper.java              — Utility: substrate fill noise + minor irregularity variation
│
└── events/
    ├── SpawnControlHandler.java
    ├── SprintFlyHandler.java             — LivingEntityTickEvent: Afterburner boost + DataAttachment cooldown
    ├── PermafrostArmorTickHandler.java   — LivingEntityTickEvent: lava/fire durability damage to armor pieces
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
│   ├── biome/ [biome JSONs — count TBD]
│   ├── noise_settings/
│   │   └── the_condemned.json               — Near-solid fill settings
│   ├── structure/ [landmark structure JSONs]
│   ├── structure_set/
│   │   ├── condemned_landmarks.json         — Landmark spacing/placement
│   │   └── condemned_procedural.json        — Procedural Jigsaw root density
│   └── template_pool/
│       ├── surface_roots/
│       ├── upper_rooms/
│       ├── lower_rooms/
│       ├── corridors_standard/
│       ├── corridors_grand/
│       ├── silos/
│       ├── transitions/
│       └── terminators/
└── tags/
    ├── blocks/
    │   ├── heat_sources.json              — Blocks that prevent Night freeze
    │   ├── solar_reactive.json            — Blocks affected by Day sun (custom stone)
    │   └── night_created_ice.json         — Ice blocks created by Night system (for targeted melt)
    └── entity_types/
        └── prison_planet_spawns_[day|sunset|night|sunrise].json
```

---

## 13. Implementation Phases

### Phase 1 — Core Dimension (Foundation)
- [ ] Mod scaffolding (NeoForge 1.21.1 gradle setup, main class, mods.toml)
- [ ] Dimension type + dimension JSON registration
- [ ] `PrisonChunkGenerator` — solid substrate fill with minor noise variation (placeholder block)
- [ ] Basic stratum boundaries (Surface / Upper / Lower / Catacomb Y ranges enforced)
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

### Phase 4 — World Generation (Procedural)
- [ ] Custom blocks (prison stone family, all structural variants)
- [ ] Jigsaw connector types defined (`condemned:horizontal/*`, `condemned:vertical/*`, `condemned:room/entry`)
- [ ] **Terminators pool**: dead-end cap pieces (collapsed wall, sealed door, rubble)
- [ ] **Standard corridors pool**: 2×3 and 4×4 corridor pieces with junction variants
- [ ] **Standard rooms pool**: Bunker Room, Cell Row, Guard Station pieces (multiple variants each)
- [ ] First playable pass: solid world carved by standard corridors + bunker rooms; verify connectivity feel
- [ ] **Silo pool**: utility shaft and standard silo shaft sections, ring walkway variants, top/bottom caps
- [ ] **Grand Tunnel pool**: 10×20 sections, junctions, branch openings into standard corridors
- [ ] **Large rooms pool**: Cell Block, Processing Chamber, Holding Bay, Engine Room, Vault Chamber
- [ ] **Surface roots pool**: silo openings, courtyard rims, rooftop surface pieces, collapsed sections
- [ ] **Transitions pool**: pieces bridging connector size differences
- [ ] Multi-root seeding: separate root structures at different XZ positions and Y-levels per region
- [ ] All 7 villager job site blocks

### Phase 5 — Landmark Structures
- [ ] Guard Tower NBT + Jigsaw entry points
- [ ] Condemned's Hovel NBT
- [ ] Disposal Pit NBT
- [ ] Condemned Prison Complex NBT (modular wings via Jigsaw sub-pool)
- [ ] Ritual Site NBT
- [ ] Execution Grounds NBT
- [ ] Underground Catacomb NBT (Catacomb Depths stratum)
- [ ] The Warden's Citadel NBT (largest; designed last)
- [ ] Loot tables for all landmark structures
- [ ] `condemned_landmarks` StructureSet with spacing/exclusion zone config

### Phase 6 — Permafrost Armor & Mending Flame
- [ ] `GlacialShardItem` + ore generation in Catacomb Depths biome (below Y=50)
- [ ] `PermafrostArmorMaterial` + `PermafrostArmorItem` (4-piece set)
- [ ] `PermafrostArmorTickHandler` — per-tick durability damage from lava/fire/molten stone
- [ ] Incremental set bonus logic (1 piece = Fire Res, 2 = partial sun, 3 = full sun, 4 = lava HP immunity)
- [ ] `MendingFlameBlock` + `MendingFlameMenu` + `MendingFlameScreen`
- [ ] Fuel → MP conversion table, repair-rate tick logic
- [ ] Anvil repair (Damaged → Chipped → normal) in menu
- [ ] Purify Curses toggle — fuel cost multiplier + curse stripping on completion

### Phase 7 — Enchantments
- [ ] All 13 enchantment JSON definitions
- [ ] Custom enchantment effect components for: Condemned's Resolve, Shackle Break, Deathless, Overseer's Dominion
- [ ] Inbuilt Afterburner: `AfterburnerEnchantment` registration + `SprintFlyHandler` + player `DataAttachment` for cooldown
- [ ] Loot table integration (books distributed across structure chests by rarity)

### Phase 8 — Villagers & Mobs
- [ ] 4 villager types (textures + registration)
- [ ] 7 villager professions + job site blocks
- [ ] Trade definitions per profession + leveling
- [ ] 12 custom mob entity classes + AI goals + placeholder textures

### Phase 9 — Mob Spawning Schedule
- [ ] Entity type tags per phase
- [ ] `SpawnControlHandler` hooked into `MobSpawnEvent`
- [ ] Biome-specific spawn weight tuning per phase

### Phase 10 — Polish & Balance
- [ ] All 5 biomes with unique terrain
- [ ] Sound events per phase transition (ambient, tension build)
- [ ] Particle effects (ash during Day, snow/frost during Night, steam during Sunset)
- [ ] Full balance pass: hazard damage, accumulation rates, enchantment power, mob difficulty
- [ ] Advancements tree for the dimension

---

## 14. Open Design Questions

### Resolved
| # | Question | Decision |
|---|---|---|
| 1 | Dimension access method | Netherite Block frame, lit with Flint and Steel (Nether portal mechanic) |
| 2 | Dimension exit | Player-built portals; some generated structures may contain pre-built exits |
| 5 | Multiplayer phase sync | Server clock is authoritative; no per-player desync anywhere |
| 6 | Snow persistence | Snow persists in world save; only removed during active Sunrise phase |
| 7 | Flammable block scope | All flammable blocks, including player-placed, are subject to solar ignition |
| 8 | Permafrost Armor — Night | Heat resistance only is intentional; no cold protection |
| 9 | Mending Flame — curse removal cost | No extra cost; removed at same fuel cost as normal repair |
| 10 | Afterburner — dimension scope | Works in all dimensions; bound to the Elytra item |

### Deferred (no planning yet)
- **Mob design** (The Overseer and all custom mobs)
- **Biome distribution** (Warden's Citadel placement, biome layouts)

### Still Open
1. **Mending Flame — zero-cost edge case**: If an item is already at full durability, enabling Purify would cost 0 fuel. Should there be a minimum fuel requirement for curse removal on a non-damaged item?
