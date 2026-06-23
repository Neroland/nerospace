# Alien Villagers & Structures — Design Document

Status: **Design / pre-implementation**. Anchor planet: **Greenxertz** (template out to Cindara, Glacira afterward).
Target: Minecraft 26.1.2 · NeoForge · Java 25 · mod id `nerospace` · package root `za.co.neroland.nerospace`.

This document is the blueprint for the feature. It is grounded in the existing codebase (4 dimensions, 7 biomes, 10 entities, 46 blocks, 106 items, datagen + Blockbench/texture pipeline) and is written so each system maps to concrete files and a build order.

---

## 1. Design pillars

1. **Earn your welcome.** Villagers start wary. The player builds a relationship (gifts, quests, defense) and is rewarded with trades, teaching rights, and access to the village's deeper structures.
2. **A village you grow.** The player teaches buildings; the village constructs them over real time and they become *functional* — passive engines that produce trade goods, unlock professions, defend, and research blueprints.
3. **Worlds worth exploring.** Generation spans tiny outposts to sprawling derelict megastructures: living mega-cities, ancient ruins/dungeons, lore/relic sites, and underground complexes — procedural but rule-bound for coherence.
4. **Trades that matter everywhere.** Goods advance nerospace tech *and* are useful in any other mod (universal materials, exclusive gear, rare/automation drops).
5. **A species for every place.** Villagers look distinct per planet and per biome via a layered + procedural-seed appearance system — recognizable families, near-infinite individuals.

The loop: *discover a settlement → earn trust → unlock trades → teach buildings → village grows into an engine → deeper/larger structures open → repeat on the next planet.*

---

## 2. Alien species & appearance system

### 2.1 Approach — layered render + procedural seed

One entity type, `alien_villager`, with a data-driven **render-layer stack**. This is the procedural-skin idea you liked, made art-directable and debuggable by constraining randomness to palettes and a curated parts library rather than raw pixels.

Render layers (bottom to top):

1. **Base body** — the species silhouette for the planet (see 2.2). Greenbench Blockbench model under `art/blockbench/entity/alien_villager_greenxertz.bbmodel`, synced to Java via `tools/model_sync.py` exactly like the existing `GreenxertzMobModel` family.
2. **Skin palette layer** — a grayscale base texture tinted at render time from a **per-individual color seed** that is *clamped to the planet+biome palette range*. Greenxertz = green/steel; Cindara = ember/red; Glacira = pale/frost. Within range, hue/saturation/value jitter per individual.
3. **Biome accessory layer** — clothing/markings chosen from a curated set keyed to biome (e.g. Greenxertz base vs `terraformed_meadow` vs `terraformed_savanna`). Gives "every biome looks different" without new entity types.
4. **Profession overlay** — tool belt / robe / insignia for the villager's job (Section 6).
5. **Status overlay** — subtle glow eyes (reuse `GlowEyesLayer`) and a reputation/mood tint so the player can read disposition at a glance.

Why not raw runtime-generated textures: they're impossible to art-direct, hard to cache, and a debugging nightmare. The seed-clamped palette approach gives the *feel* of procedural skins with deterministic, reviewable output. The seed is stored on the entity so a given villager always looks the same.

### 2.2 Per-planet species families

| Planet | Biome(s) | Silhouette concept | Palette (per CLAUDE.md families) |
| --- | --- | --- | --- |
| **Greenxertz** | `greenxertz`, `terraformed_meadow` | Tall, slender, crystalline-quartz growths on shoulders; calm | Green / steel (nerosteel + xertz quartz) |
| **Cindara** | `cindara`, `terraformed_savanna` | Stocky, ember-veined skin, ash-cloaked; heat-hardened | Red / volcanic (cindrite) |
| **Glacira** | `glacira`, `terraformed_tundra` | Wrapped in layered furs, frost-rimed; slow, deliberate | Pale blue / white (glacite) |

Disposition by species (your "earn trust" choice, applied uniformly for v1): all start **wary-neutral**, not hostile. Cindara's can be tuned more skittish/proud later if you want per-species personality, but v1 keeps one disposition model to avoid tripling the work.

### 2.3 Data model

A `Variant` record stored via a **data component** (`ModDataComponents`, the 26.1 component system you already use) and synced to clients:

```text
AlienVillagerVariant(
  planet:    enum GREENXERTZ | CINDARA | GLACIRA,
  biomeTag:  ResourceKey<Biome>,   // selects accessory set
  colorSeed: long,                 // clamped palette jitter
  profession:Profession,
  reputationTier: int              // 0..5, drives mood overlay
)
```

Determined on spawn from the biome the villager is placed in; persisted in NBT.

---

## 3. Trust & reputation progression

A per-player, per-village reputation score (0–100) mapped to **6 tiers** (0 Stranger → 5 Kin). Stored on a village-controller block entity (Section 4.1), keyed by player UUID.

How reputation rises:

- **Gifts** — give a villager an item it values (palette-appropriate materials, food). Diminishing returns per day.
- **Quests** — villagers post tasks (Section 7.3): gather X, escort, clear a nearby ruin, defend during a raid. Largest reputation source.
- **Defense** — killing hostile mobs near the village during attacks; healing/curing afflicted villagers.
- **Trade volume** — every completed trade nudges reputation up slightly (vanilla-like).

What tiers unlock:

- T0 Stranger: villagers flee/avoid; no trades.
- T1 Acquainted: basic trades open; can accept your first quest.
- T2 Trusted: can be taught **Tier-1 buildings** (blueprints accepted); mid trades.
- T3 Allied: **Tier-2 buildings**; profession specialists appear; rare trades.
- T4 Honored: **Tier-3 buildings**; access to the village's restricted structures (vaults, deep complexes).
- T5 Kin: best trades, exclusive gear, and the village will auto-expand toward a **mega-city** end state.

> **Logging/compliance note:** reputation is keyed by Minecraft player UUID only. Do **not** send player names, IPs, or interaction logs to the Sentry/telemetry pipeline. Keep any aggregate metrics anonymized and opt-in to stay POPIA/GDPR-compliant. See Section 11.

---

## 4. Village building progression (the "teach them to build" loop)

This unifies all four mechanics you wanted — supply+blueprint, reputation tiers, foundation+grow, and quests — into one coherent loop instead of four competing systems:

1. **Reputation gates** *what* you may teach (tier unlocks building catalogs — Section 3).
2. **Blueprint** is *what* you teach: a `village_blueprint` item (one per building, obtained from trades, ruins, or research). Using it on the village controller "proposes" that building.
3. **Foundation + grow** is *how* it appears: the village surveyor picks a valid plot (Section 5.4 placement rules), lays a **foundation marker**, and the building rises **block-by-block over real time** as villagers haul materials.
4. **Supply** is the *cost*: the proposed building has a materials manifest. The player (and the village's own production buildings) deposit materials into a **construction stockpile**; build progress consumes from it. No materials = construction stalls.
5. **Quests** are the *pacing/side-content*: villagers ask for help that feeds the above (gather the manifest, escort a specialist, clear the plot of mobs).

### 4.1 Core blocks/block-entities for the system

Following your one-block-entity-per-content-type convention (`ModBlockEntities`, `machine/`-style logic packages):

- **Village Core** (`village_core` block + `VillageCoreBlockEntity`) — the controller. Stores reputation map, owned plots, build queue, construction stockpile, and ticks construction. One per village; placed by worldgen, claimable by the player.
- **Foundation Marker** (`foundation_marker`) — transient block placed by the surveyor at a chosen plot; shows a ghost/preview of the queued building and a progress bar (reuse a HUD/overlay pattern like `OxygenHudLayer`).
- **Construction Stockpile** (`construction_stockpile` + BE) — input inventory for build materials; a small `ItemStore` variant.

### 4.2 How a building "grows" technically

The building is authored as a **jigsaw structure template** (Section 5). Construction is a **timed, staged placement** of that template: the `VillageCoreBlockEntity` reveals the template's blocks in layers (bottom-up, with scaffolding particles), consuming stockpile materials per layer and pausing if starved. This reuses the structure-template system for both natural worldgen *and* player-driven growth — one authoring path, two triggers.

---

## 5. Structures & world generation

### 5.1 Tech approach — hybrid (chosen for you)

Quick orientation on the three options you weren't sure about:

- **Jigsaw / template pools** (vanilla's villages/bastions tech): you author building "pieces" and rules in JSON datagen; the game snaps them together with connection points ("jigsaw blocks"). Great for modular small/medium buildings and natural variety, fully datapack-tweakable, low code. Weak at guaranteeing one *coherent giant* layout.
- **Custom Java `StructurePiece`**: you write generators in code. Total control over scale and rules; needed for "absolutely massive" coherent megastructures. More code, less data-tweakable.
- **Hand-built NBT**: you build pieces in-game with structure blocks, save `.nbt`, assemble via jigsaw. Best art control, least procedural variety.

**Decision: hybrid.** Jigsaw template pools for villages and small/medium buildings (and player-grown buildings, per 4.2). Custom Java `StructurePiece` generators for the four mega-structure classes, which also *embed* jigsaw sub-pieces so big sites still feel varied. Hand-built NBT is used to author individual building pieces where art matters (signature halls, boss rooms).

### 5.2 Structure catalog — small to massive

| Scale | Structure | Tech | Notes |
| --- | --- | --- | --- |
| Tiny | Lone outpost / shrine / trade post | Jigsaw (1–3 pieces) | Frequent; first contact; a villager or two. |
| Small | Hamlet | Jigsaw pool | A Village Core + a few starter buildings; the player's "starter" village to grow. |
| Medium | Established village | Jigsaw pool | Denser, walls, multiple professions, a quest board. |
| **Massive** | **Living mega-city** | Custom Java frame + jigsaw infill | The grown/auto-expanded end state (T5) **and** rare natural spawns. Districts, tiers, hundreds of blocks across. |
| **Massive** | **Ancient ruin / dungeon** | Custom Java | Derelict alien megastructure: multi-level, trapped, mob-guarded, loot vaults, a boss/objective. The "explore for hours" content. |
| **Massive** | **Lore / relic site** | Custom Java + NBT set pieces | Crashed ships, observatories, monoliths; gate keys/blueprints/villager unlocks; puzzle & discovery. |
| **Massive** | **Underground complex** | Custom Java, hooks existing cave carvers | Mines, hatcheries, vaults beneath villages; vertical exploration tied to your existing Greenxertz cave carvers. |

### 5.3 Per-planet structure skinning

Each structure class has a **block-palette swap** per planet (nerosteel/quartz on Greenxertz, cindrite/basalt on Cindara, glacite/ice on Glacira), exactly mirroring how biomes already differ. One generator, three material palettes — the structures look native to each world without three separate generators.

### 5.4 Procedural rules / restrictions (the "guidelines" you asked for)

To keep procedural output coherent and fair:

- **Plot validity:** buildings only place on terrain within a slope tolerance; auto-terraform a foundation pad (clamp cut/fill) rather than floating or burying.
- **District zoning:** mega-cities grow in concentric tiers (core → residential → industrial → walls); building types are restricted to their zone.
- **Spacing & biome rules:** structure sets define min/max separation (vanilla `structure_set` spacing) so worlds aren't carpeted; each structure declares allowed biomes/dimensions via tags (you already use this pattern in `ModBiomeModifiers`).
- **Connectivity:** jigsaw depth caps and mandatory connectors guarantee paths exist (no sealed rooms); dungeons validate a traversable route to the objective.
- **Loot budgets:** loot scales with depth/scale and is tier-gated, generated through loot-table datagen (`ModBlockLootSubProvider` pattern extended with structure loot).
- **Reachability for growth:** the surveyor rejects plots that would overlap protected blocks or player builds.

### 5.5 Files this introduces

New worldgen data under `src/main/resources/data/nerospace/worldgen/` (none exist today):
`structure/`, `template_pool/`, `structure_set/`, plus `processor_list/` for palette/age processors. New Java under a `world/structure/` package for custom `Structure`/`StructurePiece` classes and the registry wiring (`ModStructures`, `ModStructurePieceTypes`, `ModStructureSets`), bootstrapped through the existing `DataGenerators` `RegistrySetBuilder`. Hand-authored pieces as `.nbt` under `data/nerospace/structure/`.

---

## 6. Trade economy & professions

All four trade categories you selected, distributed across professions so each profession has a distinct, useful niche. Trades are tier-gated by reputation and by which buildings the village has.

| Profession | Building required | Trades (examples) | Category |
| --- | --- | --- | --- |
| **Quartz Trader** (base) | Trade post | raw materials, gems, food, enchanted books | Universal materials |
| **Forgewright** | Workshop (T2) | nerosium/nerosteel ingots, rocket parts, fuel canisters | Nerospace progression |
| **Artificer** | Lab (T3) | **exclusive alien tools/gear** with special abilities (see 6.1) | Exclusive gear |
| **Xeno-Botanist** | Farm/greenhouse (T2) | rare seeds, terraform catalysts, livestock eggs, food | Universal + automation feeders |
| **Relic Keeper** | Archive (T4) | blueprints, destination keys, rare enchants, spawn eggs, automation-grade drops | Nerospace progression + rare/automation |
| **Cartographer** | Watchtower (T3) | maps to nearby ruins/relic sites, destination compasses | Exploration enablers |

Cross-mod usefulness is deliberate: ingots, gems, raw ores, enchanted books, seeds, and spawn eggs are all **vanilla-tagged** items any tech/magic mod can consume; the exclusive gear and automation-grade drops give the "must trade here" pull. When Mekanism-class mods port to 26.1, the Relic Keeper's automation drops are the natural integration point (kept tag-based per your standalone-for-now scope).

### 6.1 Exclusive gear (trade-only, with abilities)

A small line of alien tools/armor only obtainable from the Artificer — strong incentives and good "useful in other mods" items:

- **Xertz Resonator** — handheld; pings nearby ores/structures (cross-mod ore-finder).
- **Grav Striders** (boots) — reduced fall damage + step assist on low-gravity planets.
- **Ember Ward / Frost Ward charms** — environmental protection on Cindara/Glacira.
- **Surveyor's Lens** — reveals structure layouts / hidden vault doors.

These register in `ModItems` with behaviors in a new `item/` or `gear/` package; abilities use NeoForge capabilities/data components so they're portable.

---

## 7. Villager traits & abilities (so they "actually do things")

### 7.1 Functional building behaviors (passive engine)

Once built, buildings make the village a resource engine (your "functional" choice):

- **Farm/Greenhouse** → periodically deposits crops/food into the village stockpile (feeds construction + trades).
- **Workshop** → converts raw materials in the stockpile into ingots/parts; unlocks Forgewright trades.
- **Lab** → researches **blueprints** over time (consumes alien fragments/tech scrap you already have as items), unlocking new teachable buildings.
- **Watchtower** → villagers spot and warn of raids; boosts defense; reveals nearby structures on the map.
- **Archive** → slowly generates lore pages and relic-site coordinates (drives exploration).

These run on the `VillageCoreBlockEntity` tick aggregating per-building block entities — the same machine/tick patterns you use in `machine/` and `storage/`.

### 7.2 Individual villager traits

- **Professions** with leveling (more trades unlock as a villager works its building).
- **Schedules/AI** (custom `Brain` goals): work at their building by day, shelter at night, flee/regroup during raids.
- **Mood/disposition** reading off reputation (overlay tint in 2.1) — a wary village visibly warms up.
- **Specialists** (T3+) wander in and can be recruited via quests, adding rare professions.

### 7.3 Quests

A **Quest Board** block at the village core posts 1–3 active tasks: gather a manifest, escort a specialist to a plot, clear a nearby ruin, survive/repel a raid. Completion grants reputation, materials, and occasionally blueprints. Quests are generated from a weighted table constrained by the village's current tier and nearby structures (rule-bound, like the structures).

### 7.4 Threats (stakes)

Periodic raids by the planet's hostile mobs (`xertz_stalker`, `cinder_stalker`, `frost_strider`) give defense its purpose and make watchtowers/walls matter. Reputation rises sharply for helping defend.

---

## 8. Decoration blocks

A decoration set per planet palette, used by structures, player building, and village growth. Authored through your existing **additive** texture/model pipeline (`gen_textures.py` → `gen_bbmodels.py` → `runData`), one `gen_<id>()` + datagen entry each, no hand-painting.

Per-planet families (Greenxertz first, then palette-swapped):

- **Structural:** alien bricks, polished/cut variants, pillars, tiles, paneling, reinforced glass.
- **Light:** bio-luminescent lamps/strips/lanterns (glow per palette), brazier (Cindara), frost-lamp (Glacira).
- **Furnishing:** banners/tapestries, rugs, alien tables/benches/shelves, crystal growths, planters.
- **Civic:** market stalls, signposts, totems/monoliths, fountains, the Quest Board.
- **Mechanical-look:** vents, conduits, cracked/derelict variants for ruins (an "aged" processor can auto-derelict ruin pieces).

Roughly 18–24 base decoration blocks for Greenxertz, reused via palette swap for the other planets — multiplying visual variety without multiplying art work.

---

## 9. Technical architecture map

How each system lands in your existing structure:

| System | New code (package `za.co.neroland.nerospace.…`) | Registries touched | Data/assets |
| --- | --- | --- | --- |
| Alien villager entity | `entity/AlienVillager.java`, `entity/AlienVillagerBrain.java` | `ModEntities` | spawn eggs, lang |
| Appearance | `client/AlienVillagerModel.java`, `client/AlienVillagerRenderer.java`, palette/accessory layers; `art/blockbench/entity/alien_villager_*.bbmodel` | renderer reg in `NerospaceClient` | layered textures under `assets/.../entity/alien_villager/` |
| Variant data | `registry/ModDataComponents` (variant component) | `ModDataComponents` | — |
| Trading/professions | `village/` package (`Profession`, `TradeOffers`) | possibly `ModVillagerProfessions`, `ModItems` (gear) | lang, recipes for gear |
| Reputation & growth | `village/VillageCoreBlockEntity.java`, `village/ConstructionManager.java`, marker/stockpile blocks | `ModBlocks`, `ModBlockEntities`, `ModMenuTypes` | block models/textures, loot, lang |
| Buildings (functional) | `village/building/*BlockEntity.java` | `ModBlockEntities` | — |
| Quests | `village/quest/` + `QuestBoardBlockEntity` | `ModBlockEntities` | lang |
| Structures (small/med) | jigsaw via datagen only | `ModStructures`/sets (datagen) | `data/.../worldgen/{structure,template_pool,structure_set,processor_list}` |
| Mega-structures | `world/structure/*.java` (`Structure`, `StructurePiece`) | `ModStructures`, `ModStructurePieceTypes`, `ModStructureSets` | NBT set pieces, loot tables |
| Decoration blocks | `ModBlocks` entries | `ModBlocks`, tags | `gen_textures.py`/`gen_bbmodels.py` + `ModModelProvider` |
| Gear items | `gear/` or `item/` | `ModItems`, `ModDataComponents` | textures, recipes, lang |

Datagen additions flow through the existing providers (`ModModelProvider`, `ModLanguageProvider`, `ModRecipeProvider`, `ModBlockLootSubProvider`, `ModEntityLootSubProvider`, `ModBlockTagProvider`, `ModItemTagProvider`, `ModAdvancements`) and the `DataGenerators` `RegistrySetBuilder` — plus new structure/template-pool/structure-set bootstraps.

Build verification per CLAUDE.md: every content add → datagen entry + texture (`gen_textures.py`) + bbmodel id + `python3 tools/gen_textures.py && python3 tools/gen_bbmodels.py` → `runData` → `build`; verify through the **gradle MCP** (`mcp__gradle__gradle_build` / `gradle_run_data`, poll `gradle_status`, read `gradle_log`) since the sandbox can't decompile.

---

## 10. Phased implementation plan (Greenxertz first)

Each phase ends at a buildable, testable state. Build through the gradle MCP and confirm BUILD SUCCESSFUL before moving on.

**Phase 0 — Scaffolding & spike (smallest playable).**
Register `alien_villager` entity (reuse `GreenxertzMobModel` pattern), one Greenxertz Blockbench model via `model_sync`, basic renderer, spawn egg, and natural spawn in the `greenxertz` biome. Goal: a wandering alien you can see in-world.

**Phase 1 — Appearance system.**
Variant data component; palette tint layer (seed-clamped to Greenxertz green/steel); biome accessory layer; glow-eyes/mood overlay. Goal: individuals look distinct, families look consistent.

**Phase 2 — Trading & reputation core.**
`village/` package: professions, tier-gated trade offers, reputation store on a `VillageCoreBlockEntity`, gift/trade reputation gains, mood overlay reacts. Trade categories wired (universal + nerospace progression first). Goal: you can trade and watch trust grow.

**Phase 3 — Small structures via jigsaw.**
First worldgen data: outpost + hamlet template pools, structure + structure_set with spacing, Greenxertz palette. A naturally spawned hamlet containing a Village Core. Goal: find a starter village in the world.

**Phase 4 — Teach-and-grow loop.**
Blueprint item, foundation marker + preview, construction stockpile, staged timed placement of jigsaw building templates, reputation gating of catalogs, materials manifests. Goal: teach a building and watch it rise over time.

**Phase 5 — Functional buildings & quests.**
Building block entities (farm/workshop/lab/watchtower/archive) producing into the stockpile and unlocking professions/blueprints; Quest Board + quest table; raids by existing hostile mobs. Goal: the village becomes a self-sustaining engine with things to do.

**Phase 6 — Exclusive gear & decoration set.**
Artificer gear line (abilities via capabilities/components) and the Greenxertz decoration block family through the asset pipeline. Goal: high-value trades and a richer-looking village.

**Phase 7 — Mega-structures.**
Custom Java generators for the four massive classes (mega-city frame + jigsaw infill, ruin/dungeon with boss & loot vaults, lore/relic site, underground complex hooking cave carvers), with procedural rules from 5.4 and structure loot tables. Goal: hours-long exploration; T5 auto-expansion to a mega-city.

**Phase 8 — Template out to Cindara & Glacira.**
Palette-swap structures and decoration; Cindara/Glacira species (silhouette + palette + accessories); per-planet disposition tuning if desired. Goal: feature parity across planets.

A reasonable first shippable milestone is **Phases 0–4** (a discoverable village you can befriend and grow on Greenxertz); Phases 5–8 layer depth on top.

---

## 11. Logging & compliance (POPIA / GDPR)

The mod ships with Sentry/telemetry. For this feature specifically:

- Reputation, quests, and trades are keyed by **Minecraft UUID only** — never player names, emails, or IPs.
- Do not transmit interaction logs (who traded what, chat, coordinates tied to a person) to telemetry.
- Any gameplay metrics must be **aggregate, anonymized, and opt-in**, with a clear toggle in `Config`.
- Persisted save data (reputation maps in block entities) stays local to the world save; it is not exfiltrated.

This keeps the feature POPIA- and GDPR-compliant by data-minimization and purpose-limitation.

---

## 12. Open questions to revisit before/while building

- Should reputation be **per-village** or **per-species-per-planet** (shared standing)? (Plan assumes per-village.)
- Do you want a **cure/conversion** mechanic (rescue afflicted villagers from ruins for big reputation)?
- Mega-city **auto-expansion** at T5: fully automatic, or player-directed zoning?
- Boss design for ancient-ruin dungeons — reuse/upgrade an existing mob, or a new entity?
- How aggressive should **raids** be (opt-out config for peaceful players)?

## Answer to open questions

1. Per-village, to keep the loop tight and local.
2. No cure/conversion in v1, to keep the loop focused on the village and avoid a new mechanic.
3. Auto-expansion, to keep the loop focused on teaching and growth rather than zoning micromanagement.
4. New entity for the boss, to give a unique challenge and reward for dungeon exploration.
5. Moderate raids with an opt-out config, to provide stakes without overwhelming peaceful players.
