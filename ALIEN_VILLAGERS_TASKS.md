# Alien Villagers & Structures — Task Tracker

Companion to `ALIEN_VILLAGERS_DESIGN.md`. Checked off as work progresses.
Locked decisions: per-village reputation · no cure/conversion in v1 · T5 auto-expansion · new boss entity · moderate raids with opt-out config.

Build rule (CLAUDE.md): after each content add → datagen entry + texture (`gen_textures.py`) + bbmodel id → `python3 tools/gen_textures.py && python3 tools/gen_bbmodels.py` → verify via gradle MCP (`gradle_run_data` then `gradle_build`, poll `gradle_status`, read `gradle_log`). Never tick a code task until BUILD SUCCESSFUL.

---

## Phase 0 — Scaffolding & spike (a wandering alien you can see) ✅ DONE

- [x] `entity/AlienVillager.java` — PathfinderMob, wary-neutral AI (FloatGoal, gentle AvoidEntity, stroll, look), variant via SynchedEntityData
- [x] `registry/ModEntities` — registered `alien_villager` (CREATURE, 0.6×1.95) + attributes via `ModEntityEvents`
- [x] Variant stored as **SynchedEntityData** (planet/homeBiome/colorSeed) on the entity — not an item data component (correct for entity-bound data). Persisted via add/readAdditionalSaveData; assigned lazily on first server tick
- [x] `client/AlienVillagerModel.java` (humanoid: head/body/crystals in marker block + pivoted arms/legs) → `art/blockbench/entity/alien_villager.bbmodel` via `model_sync.py`; added to model_sync REGISTRY
- [x] Renderer via shared `GreenxertzCreatureRenderer` + glow layer; renderer + layer registered in `NerospaceClient`
- [x] Textures `entity/alien_villager.png` + `_glow.png` (green/steel palette painter in `gen_textures.py`)
- [x] Spawn egg item in `ModItems` + flat-item model + lang + creative tab
- [x] Natural spawn in `greenxertz` biome (`ModBiomes` CREATURE weight 6) + `ModEntityEvents` ON_GROUND placement
- [x] datagen: `entity.nerospace.alien_villager`, spawn-egg name + model generated
- [x] **Build-verify: `compileJava` ✅ and `runData` ✅ BUILD SUCCESSFUL (via gradle MCP, `-x syncModels`)**

> ⚠️ Build note: the `syncModels` Gradle task fails under the gradle MCP with "no Python found" — Python isn't on that task's PATH. This is a **pre-existing environment issue** (every build runs `model_sync.py`), not specific to this feature. The `alien_villager.bbmodel` is already generated and committed, so builds were verified with `-x syncModels`. To build normally, set `-PpythonCmd=<path-to-python>` or put Python on PATH. (Also fixed a truncated tail in `tools/model_sync.py` discovered en route.)

## Phase 1 — Appearance system

- [ ] Variant assignment on spawn from biome
- [ ] Palette tint render layer (seed-clamped to Greenxertz green/steel)
- [ ] Biome accessory render layer (greenxertz / terraformed_meadow sets)
- [ ] Glow-eyes + mood/disposition overlay (reuse `GlowEyesLayer`)
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 2 — Trading & reputation core

- [ ] `village/Profession.java` + profession registry
- [ ] Tier-gated trade offers (universal materials + nerospace progression first)
- [ ] `village/VillageCoreBlockEntity.java` — per-village reputation map (player UUID → 0..100, 6 tiers)
- [ ] Gift / trade-volume / defense reputation gains
- [ ] Trading screen + menu (reuse `ModMenuTypes` patterns)
- [ ] Mood overlay reacts to reputation tier
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 3 — Small structures via jigsaw

- [ ] `data/nerospace/worldgen/{structure,template_pool,structure_set,processor_list}` scaffolding
- [ ] Outpost + hamlet template pools (Greenxertz palette)
- [ ] `ModStructures` / `ModStructureSets` bootstrap via `DataGenerators` RegistrySetBuilder
- [ ] Hamlet contains a claimable Village Core
- [ ] Spacing/biome rules (structure_set separation)
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 4 — Teach-and-grow loop

- [ ] `village_blueprint` item (one per building)
- [ ] `foundation_marker` block + build preview/progress
- [ ] `construction_stockpile` block + BE
- [ ] `village/ConstructionManager` — staged timed placement of building templates, material consumption
- [ ] Reputation-gated building catalogs + materials manifests
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 5 — Functional buildings & quests

- [ ] Building BEs: farm/greenhouse, workshop, lab, watchtower, archive (produce into stockpile / unlock professions/blueprints)
- [ ] `QuestBoardBlockEntity` + weighted quest table (tier/structure constrained)
- [ ] Moderate raids by hostile mobs + opt-out `Config` flag
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 6 — Exclusive gear & decoration set

- [ ] Artificer gear line (Xertz Resonator, Grav Striders, Ember/Frost Wards, Surveyor's Lens) — abilities via capabilities/components
- [ ] Greenxertz decoration block family (~18–24 blocks) via asset pipeline
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 7 — Mega-structures

- [ ] `world/structure/` custom Java generators + `ModStructurePieceTypes`
- [ ] Living mega-city (custom frame + jigsaw infill; T5 auto-expansion target)
- [ ] Ancient ruin/dungeon (multi-level, trapped, loot vaults, **new boss entity**)
- [ ] Lore/relic site (crashed ships, observatories, monoliths; NBT set pieces)
- [ ] Underground complex (hooks existing cave carvers)
- [ ] Procedural rules (plot validity, zoning, connectivity, loot budgets) + structure loot tables
- [ ] **Build-verify → BUILD SUCCESSFUL**

## Phase 8 — Template out to Cindara & Glacira

- [ ] Palette-swap structures + decoration per planet
- [ ] Cindara & Glacira species (silhouette + palette + accessories)
- [ ] Per-planet disposition tuning (optional)
- [ ] **Build-verify → BUILD SUCCESSFUL**

---

### Progress log
- 2026-06-16: Design doc + task tracker created. Open questions resolved. Starting Phase 0.
- 2026-06-16: **Phase 0 complete & build-verified.** Alien Villager wanders the Greenxertz surface (wary-neutral), spawns naturally + via egg, carries a per-individual variant. `compileJava` + `runData` both BUILD SUCCESSFUL. Fixed `ResourceKey.location()` → `identifier()` (26.1 rename) along the way. Next: Phase 1 (palette/accessory render layers).
