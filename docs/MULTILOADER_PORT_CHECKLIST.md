# Nerospace multiloader — port checklist

Audit of what the standalone NeoForge mod (`src/main/java`, 264 classes) still needs ported into the
cross-loader `multiloader/` project. As of this audit: **~169 classes ported, ~95 remaining**, all four
build cells (NeoForge + Fabric × MC 26.1.2 + 26.2) green.

> **2026-06-21 update — terraforming slice 2 (biomes + tags data).** All 4 cells green. Added `world/ModBiomes`
> (4 terraformed biome ResourceKey constants) + copied the 4 terraformed biome JSON + 2 terraform tag JSON.
> Data foundation for the conversion engine (slice 3).

> **2026-06-21 update — terraforming started (slice 1: chunk-attachment seam).** All 4 cells green.
> Extended the data-attachment seam for per-chunk terraform data (`TERRAFORMED` + `TERRAFORM_STAGE`) —
> NeoForge `chunk.getData/setData`, Fabric `chunk.getAttachedOrCreate/setAttached` (same registries as the
> player oxygen attachment, `LevelChunk` target); wired terraformed-ground into `OxygenManager.isBreathable`.
> The signature terraform subsystem is sliced into 6 (see Atmosphere section); this is the critical-path
> foundation. No new class count yet (seam extension); slices 2–6 add the ~18 terraform classes.

> **2026-06-21 update — oxygen diffusion field (server half) ported.** All 4 cells green. Added
> `world/{OxygenField, OxygenFieldManager (SavedData, fastutil flood-fill sim), OxygenFieldEvents}`; the
> Oxygen Generator now feeds the field from its tank and `OxygenManager.isBreathable` reads it, so **sealed
> rooms are genuinely breathable** (open space only gets a bubble). Field config inlined; the cosmetic client
> visual layer (sync payload + particle/haze/boundary overlay) is the deferred follow-up. fastutil resolves on
> common NeoForm.

> **2026-06-21 update — meteor Tracker HUD ported (networking seam proven end-to-end).** All 4 cells green.
> Added `network/MeteorSyncPayload` (multiloader's FIRST payload), `client/{ClientMeteorTracker,
> MeteorTrackerHud}`, `ModItems.METEOR_TRACKER`; registered clientbound in `ModNetwork.init()` (both loader
> seams auto-wire it), pushed from `MeteorEvents` to tracker holders every 10t, readout on the action bar via
> per-loader client-tick hooks. **26.x gotcha: `Gui.setOverlayMessage(Component, boolean)` is gone from vanilla
> Gui → use `Player.sendOverlayMessage(Component)`** (probed). The meteor subsystem is now fully ported.

> **2026-06-21 update — meteor natural-shower scheduler ported.** All 4 cells green. Added `meteor/{MeteorSite,
> MeteorEventManager (multiloader's first SavedData), MeteorEvents}` + per-loader server-tick wiring
> (NeoForge `ServerTickEvent.Post`, Fabric `END_SERVER_TICK`). Meteors now fall naturally on the 4 surface
> dims. **26.x `SavedDataType` is 4-arg only on NeoForm** (Identifier, Supplier, Codec, DataFixTypes=null) —
> the 3-arg the standalone mod uses is a NeoForge convenience (found via the javap probe). Tracker HUD
> (item + sync payload + client readout) is the deferred networking-consumer follow-up.

> **2026-06-21 update — meteor creative slice ported.** All 4 cells green. Added `meteor/{FallingMeteorEntity,
> MeteorCoreBlock, MeteorCoreBlockEntity, MeteorCallerItem, MeteorLoot}` + client `{FallingMeteorModel,
> FallingMeteorRenderer, FallingMeteorRenderState}` (bake-direct). Creative Meteor Caller → falling meteor →
> crater + break-to-loot Meteor Core. Config meteor keys inlined; natural-shower scheduler + client tracker +
> sync payload deferred (a clean networking-consumer follow-up). Lang validated via the built jar (mount was
> serving a stale truncated copy — jar check is the reliable validator).

> **2026-06-21 update — spawn rules ported.** All 4 cells green. Added `registry/ModSpawnPlacements`
> (9 placement rules: 6× ground light-independent, 3× livestock on grass) behind a `Sink` seam —
> NeoForge `RegisterSpawnPlacementsEvent` (`Operation.REPLACE`), Fabric vanilla `SpawnPlacements.register`;
> both stable on 26.1.2 + 26.2. Mobs previously relied on biome lists + vanilla defaults only.

> **2026-06-20 update — quarry ported.** All 4 cells green. Added 11 classes:
> `machine/quarry/{MinerTier, QuarryRegion, OutputFilter, PlanetMiningProfile, QuarryFrameBlock,
> QuarryLandmarkBlock, QuarryLandmarkBlockEntity, QuarryControllerBlock, QuarryControllerBlockEntity,
> QuarryMenu}` + `client/QuarryScreen`. The 1000-line controller was rebuilt on the shared
> `EnergyBuffer`/`FluidTank` + a vanilla `WorldlyContainer` (frame in, output out); force-loads via
> vanilla `ServerLevel.setChunkForced` (no ticket seam); modules + the drill-head BER + fluid auto-eject
> deferred. Energy/Item/Fluid caps wired on both loaders; assets + 9 lang keys copied.

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

### Quarry  (`machine/quarry/` 11 + client) — **DONE (4 cells green); modules + BER deferred**
- [x] Area miner ported: `QuarryControllerBlock`(+BE) + `QuarryMenu`/`QuarryScreen`, `QuarryFrameBlock`,
  `QuarryLandmarkBlock`(+BE, client laser ticker), `QuarryRegion`, `MinerTier`, `OutputFilter`,
  `PlanetMiningProfile`. The dig (landmarks → frame ring → layer-by-layer excavation → drops buffered/
  auto-ejected, source fluids sucked) runs server-side; Energy/Item/Fluid caps on both loaders.
- [~] **Chunk-loading**: `QuarryChunkLoader` (NeoForge `TicketController`) replaced by vanilla
  `ServerLevel.setChunkForced` (works on both loaders; one chunk pinned at a time, persisted + released
  on removal) — no cross-loader ticket seam needed.
- [~] **Deferred**: upgrade modules (controller runs at ×1.0 speed/energy, no Silk/Fortune, no module
  slots — depends on the `module/` batch); the moving drill-head BER (`QuarryControllerRenderer`); and
  fluid **auto-eject** (the fluid buffer is drained by pipes instead). `Tuning` values inlined.

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
- [~] **Oxygen survival core DONE (4 cells green)** — `OxygenManager` (per-player O2 drain/suffocate/refill,
  air-supply-bar mirror, full-suit detection) on a new **data-attachment seam**: `IPlatformHelper.get/setOxygen`
  backed by NeoForge `AttachmentType` (`NeoForgeAttachments`) and Fabric `AttachmentRegistry`
  (`FabricAttachments`); ticked per-loader (NeoForge `PlayerTickEvent`, Fabric `ServerTickEvents.END_SERVER_TICK`).
  Breathable = the diffusion field **or** near a Launch Pad (safe-zone radius).
- [x] **Oxygen diffusion field — server half DONE (4 cells green).** `world/{OxygenField (tag-based
  sealing classifier — `OXYGEN_SEALING`/`OXYGEN_LEAKS`, doors/trapdoors, full-cube fallback),
  OxygenFieldManager (SavedData; sparse fastutil concentration field + source set; per-pass flood-fill
  detects sealed-vs-leaky/open volumes → sealed rooms fill to MAX, open space pressurises only a bubble;
  slow evaporation), OxygenFieldEvents (cross-loader `tick(MinecraftServer)`, throttled sim pass)}`.
  Wired into both server-tick hooks alongside the meteor driver; `OxygenManager.isBreathable` now reads the
  field; the **Oxygen Generator registers itself as a field source**, draining `EMIT_MB_PER_TICK` from its
  tank while sourcing (and clears on `setRemoved`). Sealed bases are now genuinely breathable. ~9 field
  config keys inlined. **Deferred (cosmetic): the client visual layer** — `OxygenFieldSyncPayload` +
  `ClientOxygenField` + the particle/haze/boundary overlay (gated on a visual-quality config).
- [ ] **Deferred**: terraform breathability + criteria, hazard shields/feedback, gas-tank airlock refill.
- **Terraforming** (signature endgame) — sliced; **slice 1 DONE (4 cells green)**, rest sequenced:
  - [x] **Slice 1 — per-chunk data-attachment seam.** `IPlatformHelper.is/setTerraformed` +
    `get/setTerraformStage(LevelChunk)` backed by NeoForge `AttachmentType` (chunk `getData`/`setData`) and
    Fabric `AttachmentRegistry` (chunk `getAttachedOrCreate`/`setAttached`) — same registries as the player
    oxygen attachment, just a `LevelChunk` target (no new API surface). Wired into `OxygenManager.isBreathable`
    (terraformed chunk ⇒ breathable). Critical-path foundation for everything below.
  - [x] **Slice 2 — biome + tag data.** `world/ModBiomes` (4 terraformed `ResourceKey<Biome>` constants —
    the multiloader ships biomes as committed datapack JSON, so no datagen bootstrap needed) + copied the 4
    terraformed biome JSON (`terraformed`/`_meadow`/`_savanna`/`_tundra`, feature-free / runtime-written) +
    copied the 2 terraform block-tag JSON (`TERRAFORM_TO_GRASS`/`_DIRT` — TagKey constants already in `ModTags`).
    All 4 cells green; JSON python-validated. (Inert until slice 3 consumes them.)
  - [ ] **Slice 3 — conversion engine.** `TerraformConversion` (rewrite stage bookkeeping onto the seam, not
    `chunk.getData(ModAttachments…)`), `TerraformResources`, `TerraformFauna`. Inline terraform config keys.
  - [ ] **Slice 4 — Terraformer machine.** `TerraformerBlock`(+BE 584ln)+menu+screen + `TerraformManager`
    (SavedData; 4-arg `SavedDataType`) + chunk-load catch-up hook (per-loader) + biome-sync packet.
  - [ ] **Slice 5 — Hydration Module** (block/BE/menu/screen) + stage-2 wiring.
  - [ ] **Slice 6 — Terraform Monitor** (block/BE/menu/screen) + `TerraformDrift` + `TerraformChunkLoader` +
    `GreenxertzAtmosphere`. Risk: **high** (world mutation, chunk-loading, events).

### Structures  (`world/*Feature`, `village/VillageCore*`, station core, `ModFeatures`) — **DONE (4 cells green)**
- [x] `HamletFeature`, `MegaCityFeature`, `RuinFeature`, `AlienBuild`, `StructureSpacing` + `ModFeatures`
  (registers the 3 `Feature` types via `RegistrationProvider` over `FEATURE`). Copied the
  configured/placed-feature JSON and **re-added the 3 placed features to the Greenxertz biome JSON**
  (`greenxertz.json` feature step 6) — since Greenxertz is our own datapack biome, no biome-modifier seam
  needed. Mega-city spawns the (ported) Ruin Warden boss; ruin/mega-city fill vanilla loot chests.
- [~] `VillageCoreBlock` — ported as a **plain decorative centerpiece block** (the structures' anchor).
  The interactive controller (`VillageCoreBlockEntity`: claim → teach-and-grow construction, fetch
  quests, night raids) is **deferred** — it pulls in `VillageBuildings` + the config seam.

### Meteor events  (`meteor/` 8 + client)
- [x] **Creative slice** — `FallingMeteorEntity` (+ `FallingMeteorModel`/`FallingMeteorRenderer`/
  `FallingMeteorRenderState`, bake-direct), `MeteorCallerItem` (creative-only), `MeteorCoreBlock`(+BE,
  break-to-loot), `MeteorLoot`. Meteor Caller → falling meteor → crater of `meteor_rock` around a
  loot-bearing `meteor_core`. `METEOR_ROCK` + loot items (`alien_*`, raw ores) already existed; added
  `FALLING_METEOR` entity, `METEOR_CORE` block+BE (no block item — world-gen only), `METEOR_CALLER`
  item (TOOLS tab) + renderer; copied 3 textures + 4 asset JSON + 4 lang keys. Config meteor keys
  inlined (crater radius 3, bonus rolls 3). All 4 cells green.
- [x] **Natural showers (scheduler)** — `MeteorSite` + `MeteorEventManager` (the multiloader's first
  `SavedData`) + cross-loader `MeteorEvents.tick(MinecraftServer)` driving the per-level scheduler on the
  4 surface dims (overworld + Greenxertz + Cindara + Glacira); wired into NeoForge `ServerTickEvent.Post`
  and Fabric `END_SERVER_TICK`; `FallingMeteorEntity` re-wired to call `onImpact`. Meteor pacing inlined
  (avg 9000s, warn 30s, 200–500 blocks, ≤4 active). **26.x gotcha: `SavedDataType` on pure-vanilla NeoForm
  has only the 4-arg ctor `(Identifier, Supplier, Codec, DataFixTypes)`** — the standalone mod's 3-arg call
  is a NeoForge convenience; pass `null` DataFixTypes (new mod data, no datafixer schema). All 4 cells green.
- [x] **Tracker HUD** — `ModItems.METEOR_TRACKER` item + `network/MeteorSyncPayload` (the multiloader's
  **first networking payload** — registered clientbound in `ModNetwork.init()`, auto-wired by both loader
  seams) pushed to tracker holders every 10t from `MeteorEvents` + `client/ClientMeteorTracker` (data
  holder) + `client/MeteorTrackerHud` (action-bar readout via `Player.sendOverlayMessage`) driven by
  per-loader client-tick hooks (NeoForge `ClientTickEvent.Post`, Fabric `END_CLIENT_TICK`). **26.x gotcha:
  `Gui.setOverlayMessage(Component, boolean)` (the standalone mod's call) is gone from vanilla `Gui` —
  use `Player.sendOverlayMessage(Component)`** (probed). Proves the networking seam end-to-end. All 4 cells green.

### Star Guide / progression  (`progression/` 5 + client + item)
- [ ] `StarGuide`, `StarGuideProgress`, `StarGuideBlock`(+BE), `StarGuideMenu` + screen, hologram BER,
  `StarGuideBookItem`. Progression-tracking UI.

### Pipes — advanced  (`pipe/` 4 + items + payload + renderer; basic pipe already ported)
- [ ] `PipeNetwork`, `TravellingItem`, `PipeIoMode`, `PipeResourceType`, `PipeFilterItem`,
  `PipeUpgradeItem`, `SetPipeModePayload`, `UniversalPipeRenderer` (streams + travelling-item visuals,
  per-side I/O modes, filters). Needs networking seam.

### Machine modules / upgrades  (`module/` 3) — **DONE (4 cells green)**
- [x] `ModuleType`, `UpgradeModuleItem` (4 items: speed / efficiency / fortune / silk-touch) + `MachineModules`
  (rebuilt on a `NonNullList` instead of the root's `MachineItemHandler`). **Re-enabled in the quarry**:
  module slots restored in the controller's combined `WorldlyContainer` view + `QuarryMenu`, and the
  speed / energy / Silk-Touch / Fortune multipliers now drive the dig (the quarry's earlier `×1.0`
  deferral is resolved). Assets + 4 lang keys copied.

### Solar — tiers/array/BER  (`solar/` 4; single-tier base **done**)
- [~] `SolarTier`, `SolarArray` (multi-panel pooling), the root tiered block/BE + sun-tracking BER.

### Creative storage variants  (`storage/Creative*`) — **DONE (4 cells green)**
- [x] `AbstractStorageBlock` (shared base) + `CreativeFluidTank` (endless rocket_fuel), `CreativeGasTank`
  (endless oxygen), `CreativeItemStore` (right-click to set an endless item source). Fluid/gas mirror
  the ported `CreativeBattery`'s infinite pattern on the cross-loader storage interfaces; the item store
  exposes its endless source through a vanilla `Container` (no NeoForge `InfiniteResourceHandler`).
  Fluid/Gas/Item caps wired on both loaders; assets + lang copied.

### Utility items  (`item/`) — **partly DONE (4 cells green)**
- [x] `NerospaceSpawnEggItem` (+ **9 spawn eggs**: xertz stalker, quartz crawler, greenling, alien
  villager, cinder stalker, frost strider, meadow loper, ember strutter, woolly drift — ruin warden is
  summon-only). Lazy `EntityType` supplier (vanilla `SpawnEggItem` binds too early); SPAWN_EGGS tab.
- [x] `DestinationCompassItem` (×4: station/greenxertz/cindara/glacira) + `GreenxertzNavigatorItem` —
  creative-only travel devices; TOOLS_AND_UTILITIES tab. Assets + 17 lang keys copied.
- [ ] `ConfiguratorItem`, `PipeFilterItem`, `PipeUpgradeItem` (depend on **advanced pipes**),
  `StarGuideBookItem` (depends on **star guide**).
- [~] `gear/XertzResonatorItem` — ported as a **plain item**; real gear behaviour + `AlienGearEvents` pending.

### Cross-cutting registries  (`registry/`)
- [x] `ModTags` — pure `TagKey` constants (block + item; c:material + nerospace oxygen/terraform tags),
  ported verbatim (no registration; tag membership is data).
- [x] `ModDataComponents` — `SELECTED_PIPE_TYPE` (int) + `FILTER_ITEM` (vanilla `ItemStack` instead of the
  root's NeoForge `ItemResource`), via `RegistrationProvider` over `DATA_COMPONENT_TYPE`. Consumed by the
  advanced-pipe configurator/filter (advanced pipes batch).
- [~] `ModCriteria` (`terraformed_ground`/`living_ground`/`founded_station` `PlayerTrigger`s) — **deferred:
  confirmed cross-version vanilla package move** (probed 2026-06-21): on **26.1.2** the classes are
  `net.minecraft.advancements.CriterionTrigger` + `net.minecraft.advancements.criterion.PlayerTrigger`; on
  **26.2** both are under `net.minecraft.advancements.triggers`. A single shared `import` can't satisfy both
  MC versions, so this can't be a plain common class. Options when its first consumer (station founding /
  star guide / terraform) lands: (a) drop the custom advancement triggers (they're cosmetic — the systems
  work without firing them); (b) reflection (resolve `PlayerTrigger` by per-version FQN); or (c) add
  version-split source sets. Orphan until then.
- [ ] `ModAttachments` (data attachments — needs a cross-loader seam: NeoForge attachments vs Fabric
  component/attachment API), `ModFeatures`, `ModConfiguredFeatures`/`ModPlacedFeatures`/`ModBiomes`/
  `ModBiomeModifiers` (datagen bootstraps — mostly superseded by the copied JSON), `ModDimensionTypes`
  (space type — JSON already copied).
- [x] `ModCreativeModeTabs` → ported as `ModCreativeTab`: a **dedicated "Nerospace" tab** registered via
  the cross-loader `RegistrationProvider` over the vanilla `CREATIVE_MODE_TAB` registry, listing all
  items (`ModItems.creativeContents()`). **Fixes a latent runtime bug**: the earlier per-loader injection
  into vanilla tabs (`BuildCreativeModeTabContentsEvent` / `CreativeModeTabEvents`) never populated the
  tabs in-game (items were searchable but absent when browsing) — replaced on both loaders. Note: vanilla
  `CreativeModeTab.builder(Row, column)` (the no-arg overload + `withTabsBefore` are NeoForge-only).

### Networking  (`network/` 5) — **SEAM DONE (4 cells green); payloads ship with their consumers**
- [x] Cross-loader packet seam: common `network/ModNetwork` (payload registry: `clientbound`/`serverbound`
  lists + `sendToPlayer`/`sendToServer`) + `platform/NetworkPlatform` send seam. NeoForge `NeoForgeNetwork`
  registers via `RegisterPayloadHandlersEvent` (`playToClient`/`playToServer`) and sends via
  `PacketDistributor.sendToPlayer` / **`ClientPacketDistributor.sendToServer`** (client-only). Fabric
  `FabricNetwork` registers via **`PayloadTypeRegistry.clientboundPlay()/serverboundPlay()`** +
  `Server/ClientPlayNetworking` receivers and sends via `Server/ClientPlayNetworking.send`. Verified the
  exact 26.2 APIs with a temporary javap probe (removed). No payloads registered yet — `OxygenFieldSyncPayload`,
  `MeteorSyncPayload`, `SetPipeModePayload` ship with their subsystems (each just calls `ModNetwork.clientbound/
  serverbound(...)`). Client-safety contract documented in `ModNetwork`.

### Commands & compat
- [ ] `command/NerospaceCommands` — `/nerospace` debug/admin commands (vanilla Brigadier; loader event differs).
- [ ] `compat/jei/*` — recipe-viewer integration. NeoForge = JEI; Fabric would use REI/EMI. Cross-mod, low priority.

### Config / tuning
- [ ] `Config` + `Tuning` — NeoForge `ModConfigSpec`-based; needs a cross-loader config seam (or a simple
  shared config). Many ported machines currently use inlined constants where the root reads `Tuning`.

### Spawn rules
- [x] `registry/ModSpawnPlacements` — natural-spawn placement rules for the 9 spawnable creatures
  (6× `ON_GROUND` light-independent; 3× terraform livestock gated on `GRASS_BLOCK`). Cross-loader
  spawn-placement seam (`ModSpawnPlacements.Sink`): NeoForge `RegisterSpawnPlacementsEvent`
  (`Operation.REPLACE`) vs Fabric vanilla `SpawnPlacements.register`. Both stable on 26.1.2 + 26.2.
  Ruin Warden has no rule (structure/event boss only).

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
