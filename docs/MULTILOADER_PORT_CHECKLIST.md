# Nerospace multiloader — port checklist

Audit of what the standalone NeoForge mod (`src/main/java`, 264 classes) still needs ported into the
cross-loader `multiloader/` project. As of this audit: **~110 classes ported, ~154 remaining**, all four
build cells (NeoForge + Fabric × MC 26.1.2 + 26.2) green.

> **2026-06-20 update — fuel machines ported.** All 4 cells green. Added 8 classes:
> `machine/{FuelTankBlock, FuelTankBlockEntity, FuelRefineryBlock, FuelRefineryBlockEntity}` +
> `menu/{FuelTankMenu, FuelRefineryMenu}` + `client/{FuelTankScreen, FuelRefineryScreen}`, registered
> the 2 blocks / BEs / menus / block-items and wired Energy/Item/Fluid caps on both loaders. Rebuilt on
> the shared `FluidTank` + `EnergyBuffer` + vanilla `WorldlyContainer` slots (no `MachineItemHandler` in
> the multiloader); `Tuning` values inlined. Assets + 4 lang keys copied. The Fuel Tank closes the loop
> with the rockets batch: refinery (coal + blaze powder + power → fuel) → pipe → fuel tank → auto-fuels a
> padded rocket.

> **2026-06-20 update — rockets (core) ported.** All 4 cells green. Added 17 classes:
> `rocket/{RocketTier, Destinations, LaunchPadMultiblock, RocketLaunchPadBlock, LaunchGantryBlock,
> RocketItem, RocketEntity, RocketMenu}` + `client/{RocketModel, RocketT2/T3/T4Model, RocketRenderState,
> RocketRenderer, TexturedContainerScreen, SpaceButton, RocketScreen}`, registered the entity / menu /
> blocks / 4 tier items, and copied the rocket assets (entity+item+block textures, GUI, item/block models,
> blockstates, loot, recipes, 25 lang keys). **Cross-loader rewrites:** fuel store on the shared
> `FluidTank` (not NeoForge transfer); intake is a plain `SimpleContainer(1)`; the menu is **non-extended**
> (rocket ref server-side only, client reads synced `ContainerData`, opened via vanilla
> `openMenu(MenuProvider)`); the renderer **bakes each tier layer directly** (no model-layer registry);
> dropped the NeoForge-only `shouldRiderSit()` override. **Deferred** (own batches): the multi-station
> founding system (`StationCoreBlock`+BE, `StationRegistry`, Station Charter, `founded_station` criterion —
> needs data-attachment + criteria seams + structures) and the pipe/hopper **automation proxy** that feeds
> fuel into a docked rocket (needs the entity item-capability seam). Runtime behaviour (travel/teleport,
> rendering) is unverifiable headlessly — compile-verified on all 4 cells only.

Legend: `[x]` done · `[~]` partial / simplified · `[ ]` not started.
Risk = how much is loader-coupled or **runtime-only-verifiable** (rendering / world / behaviour can't be
checked by a headless build).

---

## ✅ Done (cross-loader, all 4 cells green)

- [x] **Platform seams** — `Services`/`IPlatformHelper`, `RegistrationProvider` (+ per-loader factories),
  capability seams for item / energy / fluid / gas (expose + query), `FluidFactory` seam.
- [x] **Registries** — blocks, items, block-entities, menu types, entities, sounds, dimension keys,
  entity attributes (subset that's ported).
- [x] **Logistics** — energy / fluid / gas / **item** transport; the universal pipe relays all four.
- [x] **Machines / storage** — combustion + passive + solar generators, oxygen generator, nerosium
  grinder (+ 3 GUIs), item store, battery, creative battery, fluid tank, gas tank, trash can.
- [x] **Rocket-fuel fluid** — `BaseFlowingFluid`/`FluidType` (NeoForge) vs hand-written `FlowingFluid`
  (Fabric), liquid block + bucket; NeoForge in-world render. (Fabric in-world render = follow-up.)
- [x] **All 10 mobs** — xertz stalker, quartz crawler, greenling, ruin warden, cinder/frost striders,
  3 terraform livestock, alien villager (full Merchant trading + reputation). Models, renderers,
  glow layers, sounds, `village` trade tables.
- [x] **Planet dimensions** — Greenxertz / Cindara / Glacira / Station (datapack data + `space`
  dimension_type + planet biomes that spawn the mobs and generate the ores).
- [x] **Overworld nerosium ore** worldgen (NeoForge biome modifier + Fabric biome API).

---

## 🚧 Remaining subsystems

### Rockets & travel  (`rocket/` 11 + client + items) — **core DONE (4 cells green); station-founding deferred**
- [x] `RocketTier`, `Destinations` (ported; `Tuning` values inlined as identity-multiplier base values).
- [~] `RocketEntity` — rebuilt on the cross-loader `FluidTank` + a plain `SimpleContainer(1)` intake +
  vanilla `ServerPlayer.teleportTo`. **Deferred:** the NeoForge-transfer entity item-capability
  **automation proxy** (pipe/hopper → docked rocket) and the multi-station selection. Risk: travel/teleport
  unverifiable headlessly — compile-verified only.
- [x] `RocketItem` ×4 tiers, `RocketMenu` + `RocketScreen` (destination selector + fuel gauge). Menu is
  **non-extended** (no loader-divergent extended-menu API); the station/FOUND rows are deferred.
- [x] `RocketModel` (+ `RocketT2/T3/T4Model`), `RocketRenderer` (bakes each tier layer directly — no
  model-layer registry), `RocketRenderState`; entity + item textures copied.
- [x] Launch pad / gantry: `RocketLaunchPadBlock`, `LaunchGantryBlock`, `LaunchPadMultiblock` (multiblock gating).
- [ ] `StationCoreBlock`(+BE), `StationRegistry` (multi-station slots), Station Charter, `founded_station`
  criterion — **deferred**: needs the data-attachment + criteria seams (+ structures). The Orbital Station
  destination currently docks the rider at the shared origin platform.

### Quarry  (`machine/quarry/` 11 + client)
- [ ] Area miner: controller block/BE + menu/screen, frame + landmark blocks/BE, `QuarryRegion`,
  `MinerTier`, `OutputFilter`, `PlanetMiningProfile`, `QuarryChunkLoader`. Risk: **high** (chunk-loading,
  fake-player-style mining, multiblock). Chunk-loading needs a cross-loader seam (NeoForge ticket API vs Fabric).

### Fuel machines  (`machine/Fuel*` — depends on the ported rocket-fuel fluid) — **DONE (4 cells green)**
- [x] `FuelTankBlock`(+BE +menu +screen): stores `rocket_fuel`, accepts buckets/canisters, auto-fuels a
  rocket on an adjacent pad (4x on a full 3x3, 12x on a Heavy complex), comparator out. Rebuilt on the
  shared `FluidTank`; canister slot is a vanilla `WorldlyContainer` (Item cap on both loaders); Fluid cap
  exposed for pipe filling. Pump FX uses a vanilla sound (root's `ModSounds.FUEL_TANK_PUMP` alias not ported).
- [x] `FuelRefineryBlock`(+BE +menu +screen): coal/charcoal + blaze powder + grid power → liquid
  `rocket_fuel` over a work cycle; Energy (insert-only) + Fluid (extract) + Item caps on both loaders.
  Rebuilt on `EnergyBuffer` + `FluidTank` + a vanilla `WorldlyContainer`; `Tuning` values inlined.
  Assets (textures, models, blockstates, loot, recipes) + 4 lang keys copied.

### Atmosphere / terraforming  (`world/Oxygen*`, `world/Terraform*`, `machine/Terraform*`, `HydrationModule`)
- [ ] Oxygen field (airless-dimension survival): `OxygenField`, `OxygenFieldManager`, `OxygenFieldEvents`,
  oxygen HUD + air-bubble suppression, suit checks. Needs the **networking seam** (sync to client).
- [ ] Terraformer + Terraform Monitor + Hydration Module (blocks/BE/menus/screens), `TerraformManager`,
  `TerraformConversion`, `TerraformDrift`, `TerraformFauna`, `TerraformChunkLoader`, `TerraformResources`,
  `GreenxertzAtmosphere`, terraformed biomes. Risk: **high** (world mutation, chunk-loading, events).

### Structures  (`world/*Feature`, `village/VillageCore*`, station core, `ModFeatures`)
- [ ] `HamletFeature`, `MegaCityFeature`, `RuinFeature`, `AlienBuild`, `StructureSpacing` + their
  configured/placed-feature JSON (the 3 features I **stripped** from the Greenxertz biome) + structure data.
- [ ] `VillageCoreBlock`(+BE) — per-village reputation aggregation.

### Meteor events  (`meteor/` 8 + client)
- [ ] `FallingMeteorEntity` (+ model/renderer/state), `MeteorCallerItem`, `MeteorCoreBlock`(+BE),
  `MeteorEventManager`, `MeteorEvents`, `MeteorSite`, `MeteorLoot`. Needs networking seam (impact sync).

### Star Guide / progression  (`progression/` 5 + client + item)
- [ ] `StarGuide`, `StarGuideProgress`, `StarGuideBlock`(+BE), `StarGuideMenu` + screen, hologram BER,
  `StarGuideBookItem`. Progression-tracking UI.

### Pipes — advanced  (`pipe/` 4 + items + payload + renderer; basic pipe already ported)
- [ ] `PipeNetwork`, `TravellingItem`, `PipeIoMode`, `PipeResourceType`, `PipeFilterItem`,
  `PipeUpgradeItem`, `SetPipeModePayload`, `UniversalPipeRenderer` (streams + travelling-item visuals,
  per-side I/O modes, filters). Needs networking seam.

### Machine modules / upgrades  (`module/` 3)
- [ ] `MachineModules`, `ModuleType`, `UpgradeModuleItem` — speed/efficiency upgrade items for machines.

### Solar — tiers/array/BER  (`solar/` 4; single-tier base **done**)
- [~] `SolarTier`, `SolarArray` (multi-panel pooling), the root tiered block/BE + sun-tracking BER.

### Creative storage variants  (`storage/Creative*`)
- [ ] `CreativeItemStore`, `CreativeFluidTank`, `CreativeGasTank` (+ `AbstractStorageBlock`) — infinite
  configurable sources. Marginal (creative-only).

### Utility items  (`item/`)
- [ ] `ConfiguratorItem`, `DestinationCompassItem`, `GreenxertzNavigatorItem`, `PipeFilterItem`,
  `PipeUpgradeItem`, `StarGuideBookItem`, `NerospaceSpawnEggItem` (+ **spawn eggs** for all mobs).
- [~] `gear/XertzResonatorItem` — ported as a **plain item**; real gear behaviour + `AlienGearEvents` pending.

### Cross-cutting registries  (`registry/`)
- [ ] `ModDataComponents`, `ModAttachments` (data attachments — needs a cross-loader seam: NeoForge
  attachments vs Fabric component/attachment API), `ModCriteria` (advancement triggers), `ModTags`,
  `ModFeatures`, `ModConfiguredFeatures`/`ModPlacedFeatures`/`ModBiomes`/`ModBiomeModifiers` (datagen
  bootstraps — mostly superseded by the copied JSON), `ModCreativeModeTabs` (a dedicated mod tab; we
  currently inject into vanilla tabs), `ModDimensionTypes` (space type — JSON already copied).

### Networking  (`network/` 5) — **needed by oxygen HUD, meteors, pipe modes**
- [ ] Cross-loader packet seam: NeoForge `PayloadRegistrar` vs Fabric networking API. `ModNetwork`,
  `ModPayloads`, `OxygenFieldSyncPayload`, `MeteorSyncPayload`, `SetPipeModePayload`.

### Commands & compat
- [ ] `command/NerospaceCommands` — `/nerospace` debug/admin commands (vanilla Brigadier; loader event differs).
- [ ] `compat/jei/*` — recipe-viewer integration. NeoForge = JEI; Fabric would use REI/EMI. Cross-mod, low priority.

### Config / tuning
- [ ] `Config` + `Tuning` — NeoForge `ModConfigSpec`-based; needs a cross-loader config seam (or a simple
  shared config). Many ported machines currently use inlined constants where the root reads `Tuning`.

### Spawn rules
- [ ] `entity/ModEntityEvents` — natural-spawn placement rules (ground/light) + a cross-loader spawn-placement
  seam (NeoForge `RegisterSpawnPlacementsEvent` vs Fabric). Mobs currently spawn via biome lists + vanilla defaults.

---

## 📡 Sentry / telemetry  (`telemetry/` 3 + `sentry_test` block) — **POPIA/GDPR-sensitive**
- [ ] `NerospaceTelemetry` — the Sentry client: captures Nerospace exceptions/crashes, with **PII
  scrubbing, de-dup, rate-limiting** and an active/opt-in gate.
- [ ] `SentryLogAppender` — Log4j2 appender that selects ERROR/FATAL events touching Nerospace code.
- [ ] `SentryTestBlock` — a debug block that forces a captured error.

**Compliance gate (per project preference — POPIA + GDPR):** before porting, confirm telemetry is
**opt-in** (off by default), transmits **no personal data** (scrub usernames, UUIDs, IPs, file paths,
world names), documents what's collected + retention, and offers a clear off switch. Verify the Sentry
DSN/endpoint and data-processing terms meet POPIA (SA) + GDPR (EU). Re-confirm the scrubbing covers
log paths like `C:\Users\<name>\...`. Do **not** port as-is until this is signed off.

---

## 🛠️ Tools / sync engines  (`tools/`) — currently target the **root** mod only
These are dev-time generators, not shipped code. They write to the root's `src/main/resources` paths, so
they must be pointed at (or duplicated for) `multiloader/common/src/main/resources` to drive the
multiloader's assets instead of the current copy-from-root approach.

- [ ] `model_sync.py` — **entity-model sync engine** (Blockbench `.bbmodel` ⇄ Java `LayerDefinition`,
  Y-flip, mtime-directional). Wire to the multiloader's `client/*Model.java` + `art/blockbench/entity`.
- [ ] `gen_textures.py` — procedural 16×16 texture generator (additive). Repoint output dir.
- [ ] `gen_bbmodels.py` — Blockbench source generator for block/item textures. Repoint.
- [ ] `gen_logo.py` — CurseForge logo + in-game mods-list icon. Repoint / re-emit per loader.
- [ ] `check_assets.py` — "every model resolves" validator. Repoint at the multiloader resource roots.
- [ ] `render_contact_sheets.py` / `render_entity_previews.py` — QA atlases. Repoint.
- [x] `gradle-mcp` (server.js) — the agent build server; already used to verify all 4 cells.
- [x] `fix_markdown.py` / `markdown_check` — docs linting; loader-agnostic.

> Note: so far the multiloader reuses the root's already-generated JSON/textures by copying them. The
> tools only need porting if the multiloader becomes the source of truth (i.e. when the root mod is retired).

---

## Recommended order
rockets → fuel machines → quarry → atmosphere/terraforming → structures → meteor events → star guide →
advanced pipes → modules → networking seam (unblocks oxygen HUD / meteors / pipe modes) → config seam →
spawn rules → telemetry (after compliance sign-off) → creative variants / utility items / JEI → tools repoint.
