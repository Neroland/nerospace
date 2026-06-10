# Deeper Terraforming Design (RELEASE_CHECKLIST §1: Deeper terraforming)

Status: **SIGNED OFF (2026-06-10, §15) — slice 1 IMPLEMENTED.** Verified: `runData` + `build` green,
`ecjCheck` 0 errors, gametest suite **36/36** (new: `hydration_module_feeds_terraformer`,
`terraform_water_table_fill`, `terraform_stage_progression`, `terraform_legacy_save_compat`,
`terraform_creature_breeding`, `terraform_monitor_readout`). Sign-off deviation honoured: the
Hydration Module requires TOUCHING the Terraformer (§15 answer 3). Open for runClient: water-fill
look, mature palettes + rain/snow, livestock models, the two new GUIs.

Shaped by the design Q&A with Dario (2026-06-10):
**all four axes** (staged maturation + water cycle + fauna + per-planet outcomes) in scope and
**all in slice 1** (Rich); glacite is a **consumed input** fed through a new **Hydration Module**
block; stage engine is **hybrid** (machine-driven frontiers + passive cosmetic drift); fauna =
**per-planet livestock species** (breedable); mature land gets **real weather**; plus a
**Terraform Monitor** readout block.

Hard constraint from the checklist: **existing Terraformer saves must keep working** — no breaking
migration. §9 is the dedicated compatibility contract.

## 1. Concept — the planet comes alive in stages

Today the Terraformer is a single-pass machine: one expanding ring that recolours the ground neon
emerald, flags it breathable, and is done. "Deeper" turns that one moment into an **arc**: the same
machine now drives a sequence of visible ecological stages that sweep outward behind one another,
so a terraformed world is always a gradient — raw chemistry at the frontier, a living ecosystem at
the centre.

| Stage | Name | What the player sees | Driven by |
|---|---|---|---|
| 0 | Dead | untouched planet | — |
| 1 | **Rooted** | today's conversion: grass/dirt, neon-emerald biome, breathable, sparse plants | energy (unchanged) |
| 2 | **Hydrated** | basins fill with water to a shoreline; on Glacira the surface freezes over | energy + **glacite** (Hydration Module) |
| 3 | **Living** | mature **per-planet biome** (natural palette), grown trees, livestock herds, **rain/snow** | energy (×4/column) |

- Stage 1 **is** the existing conversion, byte-for-byte — old saves are simply "stage 1 everywhere
  the machine has reached" and the new trailing frontiers sweep over that land like any other.
- Stages are **spatial**, not global: each stage has its own trailing radius, so a long-running
  machine shows all three zones at once. The end-game payoff Dario asked for — *watching* a world
  come alive — falls out of the geometry.
- The neon-emerald `nerospace:terraformed` biome becomes the *intermediate* look ("raw terraforming
  chemistry"); the **mature stage-3 biome settles into a natural per-planet palette**. The colour
  shift from alien neon to believable meadow/savanna/tundra is itself the maturation cue.
- Progression pull: stage 2 consumes **glacite**, so deepening a world requires the T4/Glacira trip
  — exactly the forward hook NEW_DESTINATION_DESIGN.md §3 flagged. Glacira stays "the edge of the
  map before the terraform finale".

## 2. Stage engine — trailing frontiers (hybrid)

### 2.1 Three radii, one proven mechanism

`TerraformerBlockEntity` already runs an uncapped ring frontier off three ints
(`radius`, `cursor`, `tier`). Deeper terraforming adds **two trailing frontiers using the identical
ring/cursor machinery**:

```
persisted per machine:  radius/cursor          (stage 1 — existing fields, untouched)
                        hydrationRadius/cursor (stage 2 — new, default 0)
                        lifeRadius/cursor      (stage 3 — new, default 0)
invariant:              lifeRadius <= hydrationRadius <= radius
```

Each work cycle spends the existing column budget across the stages, outermost first (stage 1 keeps
priority so breathable ground never waits on water). A trailing frontier only advances while it
stays strictly inside its predecessor's radius, and each stage has its own gate:

- **Stage 2 column:** costs `2 × terraformEnergyPerBlock` plus **1 hydration unit per water source
  placed** (§3). No hydration units buffered → stage 2 stalls (the GUI/Monitor says why).
- **Stage 3 column:** costs `4 × terraformEnergyPerBlock`. Pure energy — the expensive finale.

Idempotency is preserved per stage (water already present / biome already mature = no-op, no
cost), so persistence stays "radii + cursors" and the chunk-load **catch-up** path extends
naturally: `TerraformManager` persists two extra radii per machine and `onChunkLoaded` replays
whichever stages reach the chunk. Its codec gains the new lists via `optionalFieldOf(…, empty)` so
pre-existing SavedData parses unchanged.

### 2.2 Per-chunk stage flag

A new **additive** chunk attachment `TERRAFORM_STAGE` (int, default 0) records the highest stage
any column in the chunk has completed — it is what makes catch-up skips and the Monitor/criteria
O(1). The existing `TERRAFORMED` boolean is **kept untouched as the breathability contract**
(GreenxertzAtmosphere does not change its lookup). Effective stage everywhere is:

```java
int effectiveStage = Math.max(chunk.getData(TERRAFORM_STAGE),
        Boolean.TRUE.equals(chunk.getData(TERRAFORMED)) ? 1 : 0);
```

so legacy chunks (flag true, no stage data) read as stage 1 with zero migration.

### 2.3 The passive lane (hybrid)

The machine drives every *expensive* transition. On top, a deliberately tiny **cosmetic drift**
pass makes settled land keep softening even while the machine idles: a server-tick handler (every
20 ticks, config-gated, default budget **4 placements/s per level**) picks random loaded columns
inside any machine's stage≥1 radius near players and sprinkles ground cover (short grass, flowers,
on stage≥3 the occasional extra sapling). No per-chunk tick storage, no energy bookkeeping — it is
pure garnish and can be switched off without gameplay loss (`terraformDriftEnabled`).

## 3. Water cycle — Hydration Module + water table

### 3.1 Hydration Module (new block + BE)

A machine on the standard chassis (`MachineItemHandler`, `ContainerData`, themed
`TexturedContainerScreen` GUI) that must sit **within 4 blocks (Chebyshev) of a Terraformer**:

- One input slot accepting `nerospace:hydration_input` (item tag): **Glacite = 16 hydration
  units**, **Block of Glacite = 144** (9×, matching the storage-block ratio). Hopper/pipe feedable
  via the item capability like every machine.
- Melts each item into the **Terraformer's hydration buffer** (cap ~1 024 units, ContainerData-
  synced; the Terraformer owns the buffer so removing the module pauses supply but loses nothing).
- No energy buffer of its own — melting is part of the Terraformer's stage-2 column cost. One new
  GUI: slot + buffer gauge + link status ("No Terraformer in range").
- Recipe proposal (shaped): 4× Glacite + 4× Nerosteel Ingot around a Fluid Tank.

The tag is **glacite-only by default**. Vanilla ice is deliberately excluded (silk-touched
overworld ice would bypass the Glacira gate) but the tag lets modpacks add it — tags + datagen per
CLAUDE.md.

### 3.2 Water-table fill (finally pays off the dead `terraformWaterEnabled` config key)

Each machine persists a **water table**: `waterTableY = surfaceY(centre) − 1`, computed once when
stage 2 first runs (old saves: computed lazily the same way — the machine column's surface is
stable). Stage-2 conversion of a column:

```
scan down from waterTableY while the cell is air/replaceable and above solid ground
place water sources bottom-up to the table   (depth-capped: terraformWaterMaxDepth, default 8)
cost: 1 hydration unit + stage-2 energy per source placed; nothing to fill = biome-only, cheap
```

Basins below the table fill to one flat shoreline — reads as real lakes, no fluid simulation,
fully idempotent. Columns above the table stay dry hills, which is correct. Fluid flow at basin
edges is bounded by the per-cycle column budget and the depth cap; in practice each column's
placement back-fills against already-placed neighbours, so sustained cascades can't happen.

On Glacira the mature tundra biome is cold (§4), so vanilla random ticks freeze still surface
water into ice organically — the ice moon refreezes its own meltwater lakes. Free thematic win.

## 4. Mature biomes & weather (per-planet outcomes)

Stage 3 writes a **per-planet mature biome** (dimension-keyed table, default = meadow for the
overworld/unknown dimensions, where the Terraformer already works today):

| Dimension | Mature biome | Palette direction | Temp | Trees (vanilla features) | Weather |
|---|---|---|---|---|---|
| Greenxertz | `terraformed_meadow` | natural lush green (neon settles down) | 0.8 | oak + birch, sparse | rain |
| Cindara | `terraformed_savanna` | warm gold-green, scorched-earth memory | 1.2 | acacia, very sparse | rain (rare feel) |
| Glacira | `terraformed_tundra` | cold sage green, frosted edges | −0.3 | spruce, sparse | **snow** (accumulates) |

- Three new datapack biomes via the existing `ModBiomes.bootstrap` pattern; runtime biome writes
  reuse `TerraformConversion.writeTerraformedBiome` (parameterised by target) and the existing
  `ClientboundChunksBiomesPacket` resync batching.
- **`hasPrecipitation(true)`** on all three — the visible "this planet has an atmosphere now"
  payoff. Honest limitation: non-overworld levels share the overworld's weather clock
  (`DerivedLevelData`), so *when* it rains follows the overworld cycle; *where* it rains is only
  ever mature terraformed ground (vanilla per-biome precipitation rendering). Acceptable for 1.0;
  independent per-planet weather is deferred. Verify rain/snowfall renders under the END-starfield
  `SPACE` dimension type in runClient (flagged as an open check).
- Stage-3 trees: place vanilla `TreeFeatures` configured features at runtime (sparse, ~3% of
  columns, only on grass with clearance) — grown trees, not saplings, so "Living" land arrives
  with a canopy. Exact 26.1 `ConfiguredFeature#place` signature: javap-probe before coding.
- Mob spawn settings on each mature biome list that planet's livestock species (§5), so natural
  repopulation works wherever the biome exists.

## 5. Fauna — three livestock species (per-planet, breedable)

The mod's first `Animal` subclasses (existing passives are `PathfinderMob`). One shared
`TerraformLivestock` base class (food-driven breeding via vanilla `Animal` machinery, baby
scaling, panic/follow/breed/graze goal set) + three thin species:

| Planet | Species (proposal) | Role analogue | Breeds with | Drop (new item) |
|---|---|---|---|---|
| Greenxertz | **Meadow Loper** | cow — placid bulk grazer | wheat | Loper Haunch (food, ~8 hunger) |
| Cindara | **Ember Strutter** | chicken — skittish ground bird | seeds | Strutter Drumstick (food, ~6) |
| Glacira | **Woolly Drift** | sheep — shaggy cold-coat | wheat | Drift Fleece (→ crafts 4 string) |

- Breed foods are vanilla crops on purpose: terraformed grass drops seeds and supports wheat, so
  the ranching loop closes **on the planet** without overworld imports.
- **Arrival:** natural spawning alone is too slow post-worldgen (the CREATURE mob cap fills
  rarely), so stage-3 conversion actively seeds a starter herd — ~2% of Living columns spawn a
  pair, capped by a nearby-entity count (≤ 8 of the species within 48 blocks). Biome spawn
  settings remain as the long-term backstop.
- Pipeline per CLAUDE.md: three **distinct** bbmodel geometries (one cube per bone, via
  `model_sync.py`), textures + glow layers from `gen_textures.py` (palettes: loper = Greenxertz
  green/steel, strutter = ember ramp, drift = glacite frost), spawn eggs via
  `NerospaceSpawnEggItem`, vanilla-alias sounds + subtitles (checklist §3), loot tables via
  datagen.
- Deliberately **not** in slice 1: shearing, cooked food variants, milking, custom AI beyond the
  shared goal set (see §12).

## 6. Terraform Monitor (new block + BE)

The readout block, in the machine-GUI house style:

- Placeable anywhere; finds the nearest Terraformer within 32 blocks. GUI shows: stage radii
  (1/2/3), hydration buffer, machine state with **stall reason** ("Needs glacite", "No energy",
  "Frontier in unloaded chunks"), and the local column's effective stage.
- **Comparator output = local effective stage** (0/5/10/15 for stages 0–3) — automatable ("open
  the ranch gates when the land turns Living").
- No inventory, tiny BE (cached link pos, revalidated lazily). Recipe proposal: nerosteel + glass
  + xertz quartz + redstone.

## 7. Reuse of existing systems (extend, don't rewrite)

- **Ring/cursor frontier** — trailing stages are the same `ring()` enumeration with their own
  radius/cursor; no new geometry code.
- **`TerraformConversion`** — stays the single shared conversion path (live frontier + chunk-load
  catch-up call the same per-stage methods), keeping the two paths identical by construction.
- **`TerraformManager`** — same SavedData, codec extended with optional per-stage radii.
- **`TERRAFORMED` flag + `GreenxertzAtmosphere`** — breathability logic untouched; deeper stages
  change *looks and life*, never the air contract.
- **Machine chassis** — `MachineItemHandler` (capability = store, the audited pattern),
  `ContainerData` sync, `TexturedContainerScreen` GUIs, comparator conventions.
- **Biome resync** — the existing `ClientboundChunksBiomesPacket` batching, unchanged.
- **Mob pipeline** — `ModEntities` builder, `model_sync.py`, texture/glow scripts, spawn-egg item.
- **Progression** — `ModCriteria` `PlayerTrigger` pattern (a `LIVING_GROUND` twin of
  `TERRAFORMED_GROUND`, fired from the same throttled atmosphere check), vanilla
  `BredAnimalsTrigger` for breeding, datagen advancements + Star Guide steps + lang.
- **Tuning/Config split** — new base numbers live in `Tuning` and scale through the existing five
  multipliers; only enable-booleans and performance caps become config keys.

## 8. Trade-offs considered and rejected

1. **Passive-only maturation** (chunks ripen on timestamps) — rejected: adds a per-chunk ticking
   surface, decouples progress from energy (the system's one honest throttle), and continues with
   the machine gone. Kept only as the cosmetic drift lane, which carries no gameplay.
2. **Global machine stages** (whole disc flips stage at once) — rejected: no spatial gradient (the
   best visual), an ugly world-wide biome flip in one tick, and an ambiguous catch-up story.
3. **Per-column stage maps** (Long2Byte per chunk, like the oxygen field) — rejected: stage is
   derivable from centre distance vs. radii; one int per chunk suffices. Smallest possible new
   save surface is also the safest compatibility story.
4. **Water as a piped fluid** — rejected: there is no water fluid network in the mod, it would
   orphan the explicitly flagged glacite hook, and item logistics (hopper/pipe into a slot) is the
   established house pattern.
5. **Vanilla ice as hydration fuel** — rejected as a default (overworld ice farms would bypass the
   Glacira gate); the item tag leaves it open to packs.
6. **One universal creature / texture-variant trio** — Dario chose per-planet species; cost is
   contained by the shared `TerraformLivestock` base + the one-cube-per-bone model pipeline.
7. **Per-stage × per-planet biome matrix** (up to 12 biomes) — rejected: three new mature biomes
   plus the existing neon intermediate already give two palette steps; water, trees, herds and
   weather carry the rest of the gradient. Datagen surface stays sane.
8. **Real weather simulation per planet** — rejected for 1.0: `DerivedLevelData` weather sharing
   is a vanilla constraint; biome-level precipitation delivers the payoff at zero simulation cost.

## 9. Save compatibility (hard constraint — the no-break contract)

| Surface | Change | Why old saves keep working |
|---|---|---|
| Terraformer BE NBT | new ints (`HydrationRadius`, `HydrationCursor`, `LifeRadius`, `LifeCursor`, `Hydration`, `WaterTableY`) | read via `getIntOr(…, 0/MIN)` — absent = stage frontiers start at 0, table computed lazily; existing `Tier/Radius/Cursor/Energy/Upgrade` untouched |
| `TerraformManager` SavedData | two extra per-machine int lists | `optionalFieldOf` with empty defaults — old files parse, radii default 0 |
| Chunk attachments | new `TERRAFORM_STAGE` int | additive; `TERRAFORMED` boolean codec untouched; `effectiveStage` rule (§2.2) maps legacy chunks to stage 1 |
| Config | new keys additive; dead `terraformWaterEnabled` becomes live | no key renamed/removed; defaults preserve current behaviour until new blocks are built |
| World content | none removed | an old world looks **identical** until the player crafts the new blocks; existing terraformed land stays breathable neon-emerald stage 1 and upgrades in place |

A dedicated gametest (`terraform_legacy_save_compat`, §12) constructs the legacy state (flag set,
no stage data) and asserts breathability, effective stage 1, and an in-place stage-2 upgrade with
no re-payment of stage-1 work.

## 10. Performance budget

- All three frontiers share the existing `terraformMaxColumnsPerTick` hard cap — deeper stages
  make the machine *slower per area*, never heavier per tick.
- Water: depth cap 8, unit cost per source, placement back-fills against neighbours → bounded
  fluid updates; `terraformWaterEnabled` kills the layer outright.
- Drift: 4 placements/s/level default, loaded-and-near-players only, OFF-able.
- Herd seeding: entity-count capped (≤8/species/48 blocks), CREATURE mob cap applies anyway.
- No new per-player-tick work: breathability stays the O(1) flag test; the stage criterion rides
  the existing throttled check; Monitor reads cached ContainerData.
- Biome rewrites batch through the existing chunk-biome packet exactly as today.

## 11. Progression — Star Guide + advancements

Terraforming chapter grows 2 → 5 steps (tree 28 → 31 nodes), keeping the chapter last:

| Step | Icon | Advancement | Criterion |
|---|---|---|---|
| `terraformer` (existing) | Terraformer | `guide/terraformer` | unchanged |
| `terraformed_ground` (existing) | Terraformer | `guide/terraformed_ground` | unchanged |
| `hydration_module` | Hydration Module | `guide/hydration_module` — "Meltwater" (TASK) | craft/has-item, parent `terraformed_ground` |
| `living_world` | creature spawn egg | `guide/living_world` — "World Awake" (CHALLENGE) | new `LIVING_GROUND` PlayerTrigger: stand on stage-3 ground |
| `new_life` | wheat | `guide/new_life` — "New Life" (GOAL) | `BredAnimalsTrigger` on any of the three species |

Toast flags follow the existing all-nodes-toast convention; lang via `ModLanguageProvider`
(`gui.nerospace.star_guide.step.*`).

## 12. Slice 1 scope (numbered, internally phased)

Implementation lands in four internally-verified phases (gradle MCP green between each), because
this is the largest §1 slice:

**Phase D1 — stage engine + water (the core):**
1. Trailing radii/cursors + per-stage budgets/costs in `TerraformerBlockEntity`; hydration buffer.
2. `TERRAFORM_STAGE` attachment + `effectiveStage`; `TerraformManager` codec extension + staged
   catch-up.
3. Water-table fill in `TerraformConversion` (reads `terraformWaterEnabled` at last); Hydration
   Module block/BE/GUI/recipe/tags (`nerospace:hydration_input`); datagen + textures for the
   module.
4. Gametests: `hydration_module_feeds_terraformer`, `terraform_water_table_fill`,
   `terraform_stage_progression`, `terraform_legacy_save_compat`.

**Phase D2 — mature biomes + weather + trees:**
5. Three mature biomes (datagen registry set) + dimension→biome table; stage-3 conversion writes
   them; runtime tree placement; precipitation on.
6. Gametest: stage-3 biome write + idempotency (extend `terraform_stage_progression`).

**Phase D3 — fauna:**
7. `TerraformLivestock` base + Meadow Loper / Ember Strutter / Woolly Drift: entities, breeding,
   drops (3 new items + fleece→string recipe), loot, spawn eggs, bbmodels via `model_sync.py`,
   textures/glow via the scripts, sound aliases + subtitles, biome spawn settings + herd seeding.
8. Gametest: `terraform_creature_breeding` (+ entity sanity per existing mob-test patterns).

**Phase D4 — Monitor + progression + polish:**
9. Terraform Monitor block/BE/GUI/comparator/recipe/datagen/textures.
10. Star Guide +3 steps, advancements, `LIVING_GROUND` criterion, lang; tree 28→31.
11. Gametests: `terraform_monitor_readout`, extend `star_guide_advancements_resolve`.
12. Full verify: `runData` → `build` → `ecjCheck` → full gametest suite via the gradle MCP; wiki
    pages (Terraformer update + Hydration-Module + Terraform-Monitor + Creatures).

## 13. Deferred (explicitly out of slice 1)

- Shearing/milking/cooked variants and any custom livestock AI beyond the shared goal set.
- Custom flora blocks / alien trees (art-overhaul pass owns bespoke looks; vanilla features for now).
- True ice/snow **surface rules** on Glacira (already deferred to the art pass).
- Independent per-planet weather cycles (vanilla `DerivedLevelData` constraint).
- Additional hydration fuels (vanilla ice etc.) — tag exists, default stays glacite-only.
- Oxygen-field interplay changes (e.g. mature land feeding the field) — the breathability contract
  stays exactly as shipped.
- Stage-aware ambient audio / bespoke creature sounds (checklist §3 post-1.0 audio item).
- JEI/EMI surfacing of hydration costs (checklist §8, blocked on 26.1 ports).

## 14. Sign-off questions

1. **Stage arc:** three stages — Rooted → Hydrated → Living — with the neon-emerald biome as the
   intermediate look and natural palettes only at stage 3. Approve?
2. **Water rule:** water table = machine column surface − 1, depth cap 8, 1 hydration unit per
   source, glacite = 16 units / block = 144. Approve numbers (tunable later via gametested bases)?
3. **Hydration Module range:** within 4 blocks of the Terraformer (not strict adjacency) — close
   enough to read as one installation, loose enough to build around. OK, or require touching?
4. **Creature identities:** names (Meadow Loper / Ember Strutter / Woolly Drift), role analogues,
   vanilla breed foods, and the three new drop items (haunch food / drumstick food / fleece→string)
   — approve or rename/re-spec any of them?
5. **Glacite-only gate:** vanilla ice excluded from `hydration_input` by default — confirm?
6. **Mature palettes:** the §4 table (meadow/savanna/tundra directions, snow on Glacira) — approve
   colour directions (exact hexes tuned in runClient)?
7. **Progression:** +3 Star Guide steps / advancement names ("Meltwater", "World Awake",
   "New Life") — approve?
8. **Phasing:** D1→D4 order (water core first, fauna third) — approve, or pull fauna earlier?


## 15. Sign-off answers

1. Approved as proposed.
2. Approved as proposed.
3. For now, it requires touching; we can relax to 4 blocks later if it feels too tight in testing.
4. Approved as proposed.
5. Confirmed as proposed.
6. Approved as proposed.
7. Approved as proposed.
8. Approved as proposed.