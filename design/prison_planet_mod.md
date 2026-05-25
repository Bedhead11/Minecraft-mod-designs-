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
| Has precipitation | No |
| Ultrawarm | No (custom hazard system handles heat) |
| Natural spawning | Yes, time-gated |

---

## 3. Extended Day/Night Cycle

Total cycle length: **96,000 ticks** (4× standard Minecraft day)

Each phase is exactly 24,000 ticks (one standard Minecraft day).

| Phase | Ticks | Sky Analog | Hazard |
|---|---|---|---|
| **Day** | 0 – 23,999 | Dawn → Noon | Burning (fire damage) |
| **Sunset** | 24,000 – 47,999 | Noon → Dusk | None (safe window) |
| **Night** | 48,000 – 71,999 | Dusk → Midnight | Freezing (frost damage) |
| **Sunrise** | 72,000 – 95,999 | Midnight → Dawn | None (safe window) |

### Implementation Approach
- `PrisonPlanetSavedData` (extends `SavedData`) stores a `long cycleTick` per world save.
- A `ServerTickEvent` increments `cycleTick` and calls `ServerLevel.setDayTime(cycleTick / 4)` to drive the vanilla sky renderer at ¼ speed.
- Phase detection: `phase = cycleTick / 24000` (integer division, mod 4).
- Sky color overrides and fog are handled on the client via a custom `DimensionSpecialEffects` subclass.

---

## 4. Environmental Hazards

### 4.1 Day — Solar Burning
- **Trigger**: Player is in `the_condemned`, phase == Day, is exposed to sky (sky light level ≥ 10).
- **Effect**: `player.setSecondsOnFire(3)` every 40 ticks.
- **Exemptions**: Fire Resistance potion, specific enchantment (Ash Walker), being inside a structure with a roof.
- **Thematic note**: The sun here is a weapon. Shade is survival.

### 4.2 Night — Deep Freeze
- **Trigger**: Player is in `the_condemned`, phase == Night.
- **Effect**: Increment `player.setTicksFrozen()` by 10 per tick beyond 0, dealing powder-snow-style damage.  Applies slowness as freeze increases.
- **Exemptions**: Frost Ward enchantment, specific armor set bonus, staying near lit fires/torches.
- **Thematic note**: The void sucks all heat away. Light is survival.

### 4.3 Interaction Edge Cases
- Sunrise and Sunset phases are safe — neither hazard is active.
- Transitioning into Day or Night gives a 200-tick (10 second) grace period so players aren't blindsided mid-fight.
- Hazards apply to all living players in the dimension, checked server-side per player tick via `LivingEntityTickEvent`.

---

## 5. Structures

All structures use Jigsaw-based generation where possible for modular reuse of pieces.

| Structure | Size | Rarity | Description |
|---|---|---|---|
| **The Warden's Citadel** | Massive | Very rare | Central fortress dominating the biome. Multi-floor dungeon, boss room, unique loot vault. |
| **Condemned Prison Complex** | Large | Rare | Multi-wing cell block, guard rooms, execution hall. Primary source of enchanted books. |
| **Disposal Pit** | Medium | Uncommon | Open crater with processing machinery, mob spawners, salvage loot. |
| **Guard Tower** | Small | Common | Lone towers scattered across the landscape. Contains a jailer villager and basic loot. |
| **Underground Catacomb** | Medium | Uncommon | Subterranean network of tunnels, buried beneath the surface. Safe during day. |
| **Execution Grounds** | Medium | Rare | Open arena, pillory structures, scattered bones. Mob arena-style encounter. |
| **Condemned's Hovel** | Tiny | Common | Survivor shanty shelters. Safe haven structures with minor loot and maybe a prisoner villager. |
| **Ritual Site** | Small | Uncommon | Cultist circle, altar, strange runes. Source of ritual-themed loot. |

Each structure has its own loot table defining which enchanted books, profession-traded items, and mob spawners appear inside.

---

## 6. Custom Enchantments

All enchantments are obtainable **exclusively** as enchanted books found in the dimension's structure loot tables. They are not available from enchanting tables or librarian villagers outside the dimension.

Enchantments are fully data-driven (1.21 JSON format under `data/prisonplanet/enchantment/`).

| # | Name | Applicable To | Effect |
|---|---|---|---|
| 1 | **Ash Walker** | Boots | Immunity to solar burn hazard. Also grants fire resistance passively. |
| 2 | **Frost Ward** | Chestplate | Reduces and eventually negates freeze buildup during Night phase. |
| 3 | **Condemned's Resolve** | Chestplate | When health drops below 30%, grants a burst of Resistance II for 5 seconds. 30s cooldown. |
| 4 | **Shackle Break** | Boots | Movement speed increases as health decreases (max +50% at 1 heart). |
| 5 | **Warden's Wrath** | Sword | Bonus damage against all mobs that naturally spawn in the dimension. |
| 6 | **Soulchain** | Sword | On hit, applies a tether effect — slows the target as if chained (+2s slowness per level). |
| 7 | **Infernal Temper** | Sword | Melee attacks deal additional fire damage. Scales with level (2/4/6 dmg). |
| 8 | **Tundra's Embrace** | Sword | Melee attacks inflict a frost slow (Slowness I for 3s per level). Cannot combine with Infernal Temper. |
| 9 | **Void Sight** | Helmet | Permanent Night Vision while in the dimension. |
| 10 | **Scavenger's Eye** | Helmet | Increases loot rolls from containers in the dimension (Fortune-style bonus on chests). |
| 11 | **Deathless** | Chestplate | On lethal damage, triggers a 3-second invincibility window instead of death. 10-min cooldown. One-time save. |
| 12 | **Overseer's Dominion** | Any | Legendary/unique. Grants immunity to BOTH hazards. Extremely rare drop. Cannot be combined with other exclusive enchants. |

---

## 7. Villager Types & Professions

### Villager Types (visual variants tied to biomes)
These are new `VillagerType` registrations — they affect villager skin/texture.

| Type ID | Description | Home Biome |
|---|---|---|
| `condemned` | Gaunt, ragged — long-term prisoners | Ash Flats biome |
| `warden_caste` | Armored, imposing — dimensional guards | Citadel biome |
| `scavenger` | Patchwork armor, salvaged gear | Disposal biome |
| `cultist` | Robed, marked — worshippers of the old order | Ritual biome |

### Villager Professions
These are new `VillagerProfession` registrations with unique job site blocks.

| Profession | Job Site Block | Sells | Buys |
|---|---|---|---|
| **Jailer** | Shackle Post (custom block) | Chains, restraint tools, dimension-specific armor | Rare mob drops |
| **Warden** | Warden's Desk (custom block) | High-tier weapons, keys, dimensional access items | Gold, rare resources |
| **Black Market Dealer** | Contraband Crate (custom block) | Enchanted books (dimension-exclusive), forbidden potions | Emeralds + rare mats |
| **Condemned Prisoner** | Carved Stone (custom block) | Food, survival supplies, maps to nearby structures | Food, basic materials |
| **Cultist Broker** | Ritual Altar (custom block) | Potions, ritual items, buff scrolls | Rare creature drops |
| **Scavenger** | Salvage Pile (custom block) | Salvaged materials, dimension-specific crafting components | Junk items / mob drops |
| **Overseer** | Command Throne (custom block) | Tier-3 equipment, unique upgrade items, Overseer's Dominion book | Many emeralds + boss drops |

---

## 8. Time-Gated Mob Spawning

Mob spawn pools are switched by checking the current phase from `PrisonPlanetSavedData` inside a `SpawnPlacementRegisterEvent` override and a custom `MobSpawnEvent.SpawnPlacementCheck` handler.

### Day (0–23,999 ticks)
Heat-adapted surface creatures. Players must stay in shade.

| Mob | Type | Notes |
|---|---|---|
| Ash Crawler | Arthropod | Pack hunter, spawns in groups, avoids shade |
| Slag Golem | Construct | Slow but high damage, fire immune |
| Sun Scorpion | Arthropod | Fast, venomous — injects wither/poison |
| Condemned Shade | Undead | Weakened form during day, flees sunlight inconsistently |

### Sunset (24,000–47,999 ticks)
Transition predators emerge. Most active and varied phase.

| Mob | Type | Notes |
|---|---|---|
| Shadow Stalker | Illager-variant | Stealthy, teleports short distances |
| Warden Construct | Construct | Patrols near structures, high HP |
| Spectral Prisoner | Ghost/Undead | Passes through walls, chain attacks |
| Ritual Cultist | Humanoid | Casts effects, summons lesser mobs |

### Night (48,000–71,999 ticks)
Cold-adapted predators. Players must stay near heat sources.

| Mob | Type | Notes |
|---|---|---|
| Frost Wraith | Undead | Inflicts additional freeze on hit, resists freeze hazard |
| Night Crawler | Arthropod | Extremely fast, attacks in swarms |
| Condemned Horde | Undead | Zombie-like, draws in other condemned toward players |
| The Overseer (rare) | Boss-tier | Rare patrol encounter, drops Overseer's Dominion fragment |

### Sunrise (72,000–95,999 ticks)
Weakened stragglers. Safest time to travel.

| Mob | Type | Notes |
|---|---|---|
| Wandering Condemned | Undead | Passive until attacked |
| Lesser Shade | Undead | Weaker version of Condemned Shade |
| Dying Crawler | Arthropod | Low HP, slower, drops components |
| Injured Cultist | Humanoid | Low combat ability, may offer trades if not attacked |

---

## 9. Biomes

The dimension contains several distinct biomes to distribute structures, mob spawns, and visual variety.

| Biome | Description | Primary Structure | Unique Hazard Modifier |
|---|---|---|---|
| **Ash Flats** | Flat grey wasteland, ash blocks, dead trees | Prison Complex, Guard Towers | Full hazards apply |
| **Scorch Wastes** | Cracked terrain, lava seeps, magma blocks | Disposal Pit | Day burn is stronger (+50%) |
| **Void Cliffs** | Tall stone spires over dark voids, treacherous footing | Warden's Citadel | Normal hazards |
| **Catacomb Depths** | Underground cavern biome only | Catacomb, Ritual Sites | No sky — neither hazard applies |
| **Condemned's Reach** | Ruined city remnants, semi-habitable | Hovels, Execution Grounds | Hazards partially blocked by ruins |

---

## 10. Key Custom Blocks

These are needed to support villager job sites and dimension structures.

| Block | Purpose |
|---|---|
| Ash Stone / Ash Stone Bricks | Primary building block for structures |
| Shackle Post | Jailer job site block |
| Warden's Desk | Warden job site block |
| Contraband Crate | Black Market Dealer job site block |
| Carved Condemned Stone | Prisoner job site block |
| Ritual Altar | Cultist Broker job site block |
| Salvage Pile | Scavenger job site block |
| Command Throne | Overseer job site block |
| Phase Lantern | Decorative/functional — emits heat aura that reduces freeze buildup |
| Shackle Chain (decorative) | Structural decoration |

---

## 11. Technical Architecture

```
prisonplanet/
├── PrisonPlanetMod.java              — @Mod entry point, registration buses
│
├── core/
│   ├── ModBlocks.java                — DeferredRegister<Block>
│   ├── ModItems.java                 — DeferredRegister<Item>
│   ├── ModEntityTypes.java           — DeferredRegister<EntityType<?>>
│   ├── ModVillagerTypes.java         — DeferredRegister<VillagerType>
│   ├── ModVillagerProfessions.java   — DeferredRegister<VillagerProfession>
│   ├── ModEnchantments.java          — DeferredRegister<Enchantment> (effect type registration)
│   ├── ModStructures.java            — DeferredRegister<StructureType<?>>
│   └── ModBiomes.java                — DeferredRegister<Biome>
│
├── dimension/
│   ├── PrisonPlanetSavedData.java    — SavedData: stores cycleTick per level
│   ├── CyclePhase.java               — Enum: DAY, SUNSET, NIGHT, SUNRISE + helper methods
│   ├── EnvironmentalHazardHandler.java  — LivingEntityTickEvent: applies burn/freeze
│   └── CycleTickHandler.java         — ServerTickEvent: increments cycleTick, syncs sky time
│
├── entity/
│   ├── mobs/                         — Custom mob entity classes (12 mobs)
│   └── villager/
│       └── ModVillagerTrades.java    — VillagerTradesEvent handler
│
├── structures/
│   ├── ModStructureTypes.java        — Structure class registrations
│   └── pieces/                       — Structure piece classes (for non-jigsaw structures)
│
├── worldgen/
│   └── ModWorldGenProvider.java      — DatapackBuiltinEntriesProvider (bootstraps dimension,
│                                        biomes, noise settings, configured features)
│
└── events/
    ├── SpawnControlHandler.java      — MobSpawnEvent: phase-gated spawn enable/disable
    └── ClientDimensionEffects.java   — Custom sky color, fog, cloud rendering (client-only)
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
│   ├── biome/ [5 biome JSON files]
│   ├── noise_settings/
│   │   └── the_condemned.json
│   ├── structure/ [8 structure JSON files]
│   ├── structure_set/
│   │   └── the_condemned_structures.json
│   └── template_pool/ [jigsaw pool JSONs]
└── tags/
    ├── blocks/
    └── entity_types/
        └── prison_planet_spawns_[day/sunset/night/sunrise].json
```

---

## 12. Implementation Phases

### Phase 1 — Core Dimension (Foundation)
- [ ] Mod scaffolding (NeoForge 1.21.1 gradle setup, main class)
- [ ] Dimension type + dimension JSON registration
- [ ] Basic noise terrain (ash flats biome, placeholder blocks)
- [ ] `PrisonPlanetSavedData` + `CycleTickHandler` (time system working)
- [ ] Phase detection logic + debug overlay

### Phase 2 — Environmental Hazards
- [ ] `EnvironmentalHazardHandler` (burn + freeze logic)
- [ ] Grace period on phase transition
- [ ] Sky color overrides per phase (client-side `DimensionSpecialEffects`)
- [ ] Phase Lantern block (freeze mitigation)

### Phase 3 — Structures
- [ ] Custom blocks (Ash Stone family, job site blocks)
- [ ] 2–3 starter structures (Guard Tower, Condemned's Hovel, Disposal Pit) with loot tables
- [ ] Remaining 5 structures
- [ ] Jigsaw pools for modular prison complex pieces

### Phase 4 — Enchantments
- [ ] All 12 enchantment JSON definitions
- [ ] Any custom enchantment effect components (Condemned's Resolve, Deathless, Overseer's Dominion need custom triggers)
- [ ] Loot table integration (books in structure chests)

### Phase 5 — Villagers & Mobs
- [ ] 4 villager types (textures + registration)
- [ ] 7 villager professions + job site blocks
- [ ] Trade definitions per profession
- [ ] 12 custom mob entity classes + AI + textures

### Phase 6 — Mob Spawning Schedule
- [ ] Phase-tag entity type lists
- [ ] `SpawnControlHandler` hooked into `MobSpawnEvent`
- [ ] Biome-specific spawn weight tuning

### Phase 7 — Polish & Balance
- [ ] All 5 biomes implemented with unique terrain
- [ ] Sound events per phase transition
- [ ] Particle effects (ash, frost)
- [ ] Balance pass on hazard damage, enchantment power, mob difficulty
- [ ] Advancements tree for the dimension

---

## 13. Open Design Questions

1. **Dimension access**: How does the player enter? (Portal structure, crafted key, specific item + ritual?)
2. **Dimension exit**: Can the player die-to-exit, or is there an explicit exit portal inside the dimension?
3. **Custom boss**: Should The Overseer be a full boss fight with phases, or a rare elite patrol mob?
4. **Armor set**: Should there be a craftable "Condemned Set" found/crafted in the dimension that provides partial hazard protection without relying on enchantments?
5. **Biome distribution**: Should the Warden's Citadel be its own biome, or spawn within Void Cliffs?
6. **Multiplayer sync**: The cycle clock is per-level (same for all players). Should players on servers share the same phase, or is a per-player phase possible (adds complexity)?
