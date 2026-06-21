# Nerospace multiloader — port checklist

Audit of what the standalone NeoForge mod (`src/main/java`, 264 classes) still needs ported into the
cross-loader `multiloader/` project. As of this audit: **~217 classes ported, ~47 remaining**, all four
build cells (NeoForge + Fabric × MC 26.1.2 + 26.2) green.

> **2026-06-21 update — config seam slice 1 (energy multiplier).** All 4 cells compile green. Extended the
> properties `NerospaceConfig` with `energyRateMultiplier` (0.1×..10×) + a `scale()` clamp helper, wired into
> all three generators' FE/tick. Establishes the cross-loader balance-config pattern (properties file, no
> `ModConfigSpec` seam); the root's other 4 multipliers wire in incrementally (kept out of the file until wired).

> **2026-06-21 update — station founding (charter-driven; closes the last advancement).** All 4 cells green
> (full `:neoforge:build`+`:fabric:build` on 26.2; compile on 26.1.2). Ported `rocket/{StationRegistry
> (SavedData, POPIA-clean — no player identity), StationCoreBlock, StationCoreBlockEntity}` + a new
> `item/StationCharterItem` whose right-click founds a station (allocates a slot, lays the 7×7 pad, binds a
> Station Core in the `nerospace:station` void dim, travels there) and code-grants `guide/station_charter` —
> **decoupling founding from the deferred rocket FOUND row.** Registered block (no block item / loot table) +
> BE + charter item; copied assets + lang; repointed the Star-Guide step + advancement icons to the now-real
> `station_charter` item. **All 42 advancements now track real completion.** Break the Core to unregister +
> reclaim the (named) charter. Deferred (slice 2): the rocket's per-station selection/return rows.

> **2026-06-21 update — Star Guide slice 2d (terraform advancements code-granted).** All 4 cells compile
> green. `progression/StarGuideGrants` awards `guide/terraformed_ground` + `guide/living_world` from the
> per-player tick when the player stands on terraformed/living ground — routing around `ModCriteria` by
> directly awarding the impossible-criterion advancements. **41/42 advancements now track real completion;**
> only `station_charter` stays inert (blocked on station founding).

> **2026-06-21 update — Star Guide slice 2c (seen-pulse; the guide is now feature-complete).** All 4 cells
> compile green. Added a `List<Integer>` `STAR_GUIDE_SEEN` player attachment via the existing seam +
> restored the menu seen-masks + screen pulse (completed-but-unseen steps pulse until clicked). 26.x gotcha:
> NeoForge `AttachmentType.builder` default must be a lambda (`List::of` is ambiguous). Star Guide = browse +
> live progress + hologram + seen-pulse; only converting the 3 `impossible` advancements remains (blocked on
> ModCriteria).

> **2026-06-21 update — Star Guide slice 2b (hologram BER + reusable BER seam).** All 4 cells compile green
> (26.1.2 + 26.2). Added the first cross-loader block-entity-renderer seam (`ClientBlockEntityRenderers.Sink`)
> + the Star Guide pedestal hologram renderer (spinning next-step icon). **26.x gotcha: `BlockEntityRendererProvider`
> is 2-type-param `<T, S extends BlockEntityRenderState>`.** The seam is reusable for future BERs (solar
> sun-tracking deck, quarry drill head, etc.).

> **2026-06-21 update — Star Guide slice 2a (advancement data — the guide now tracks progress).** All 4
> cells green (full `:neoforge:build`+`:fabric:build` on 26.2; no Java changed — pure data). Copied all 42
> nerospace advancements into common; 39 use vanilla triggers and track real completion, the 3 custom-trigger
> ones were converted to `minecraft:impossible` (load + parent chain intact, inert until granted), and 2
> display icons were repointed off unported items. The Star Guide steps now light up as the player progresses.

> **2026-06-21 update — Star Guide slice 1 (the browsable progression guide).** All 4 cells green (full
> `:neoforge:build`+`:fabric:build` on 26.2; compile on 26.1.2). Ported `progression/{StarGuide (9-chapter
> ×40-step content table), StarGuideProgress (reads advancements), StarGuideBlock (lectern pedestal),
> StarGuideBlockEntity (MenuProvider + next-step hologram compute/sync), StarGuideMenu}` +
> `item/StarGuideBookItem` + `client/StarGuideScreen` (built on the existing `TexturedContainerScreen` +
> `SpaceButton` — near-verbatim since the root already uses the 26.x submission model). Registered block +
> block-item + book item + BE + menu + per-loader screen; copied block/GUI/book assets + models + blockstate
> + loot table + **98 lang keys** (full chapter/step text). The guide opens from the **Star Guide Book** (in
> hand) or a **Star Guide pedestal** (install the book). **No `ModCriteria` needed** — the guide just reads
> advancement completion (missing advancements read as incomplete), sidestepping the 26.1↔26.2 criterion
> package split. Two steps (station_charter / new_life) use stand-in icons for the not-yet-ported
> STATION_CHARTER / LOPER_HAUNCH. **Deferred (slice 2):** the advancement DATA (so steps actually tick
> complete — the guide currently browses fully but tracks no completion until advancements land), the
> hologram BER (cosmetic; the BE already computes+syncs the stack), and the "seen-pulse" (needs a
> `STAR_GUIDE_SEEN` player-attachment seam).

> **2026-06-21 update — pipe fluid relay (closes the slice-A FLUID gap).** All 4 cells green (compile on
> 26.2; full `:neoforge:build`+`:fabric:build` on 26.1.2). Added a `platform/FluidLookup` query seam
> (mirrors `EnergyLookup`/`GasLookup`: common interface via `Services.load` + `NeoForgeFluidLookup`
> [`level.getCapability`] + `FabricFluidLookup` [`BlockApiLookup.find`] + both `META-INF/services` files).
> `UniversalPipeBlockEntity` gained a `FluidTank` + `getFluidTank()`, a `relayFluid()` mirroring the gas
> relay (honours the FLUID face-mode + speed throughput), tick wiring, and NBT persistence; the pipe's
> fluid handler is now exposed as the FLUID capability on both loaders. **The Universal Pipe now genuinely
> carries all four layers (energy/fluid/gas/item)** — e.g. piping `rocket_fuel` from a Refinery to a Fuel
> Tank — and the slice-A FLUID face-mode is now functional (no longer inert).

> **2026-06-21 update — advanced pipes slice A (per-face configuration layer).** All 4 cells green
> (compile on 26.1.2 + 26.2; full `:neoforge:build`+`:fabric:build` on 26.2). Added `pipe/PipeIoMode`
> + `pipe/PipeResourceType` (pure-vanilla enums) and the three pipe tools — `item/ConfiguratorItem`
> (cycle selected layer + cycle a face's I/O mode), `item/PipeFilterItem` (ItemResource→vanilla
> **ItemStack** filter), `item/PipeUpgradeItem` (speed/capacity ×2). Extended `UniversalPipeBlockEntity`
> with per-face×per-type `PipeIoMode` storage (packed long), per-face item filters, speed/capacity
> upgrade counts, and rewired the energy/gas/item relay to honour `canPull`/`canPush`/`OFF` + filters +
> the speed throughput multiplier; `UniversalPipeBlock` pops upgrades on sneak-empty-hand. Registered the
> 4 items (TOOLS tab) + copied 4 textures + item models/defs + 20 lang keys. **Pipes are now configurable
> per face.** Deferred to **slice B**: the full `PipeNetwork` graph + `TravellingItem` animation +
> `UniversalPipeRenderer`/`RenderState` (cosmetic, NeoForge-transfer-coupled) and the `PipeConfigScreen`
> GUI + `SetPipeModePayload` (needs a client-screen-open seam). Note: the multiloader relay still carries
> no FLUID layer, so the stored FLUID face-mode is inert until a fluid relay lands.

> **2026-06-21 update — telemetry (Sentry) ported.** All 4 cells green; Sentry bundled per-loader (NeoForge
> jarJar + Fabric include, both tasks green). `telemetry/{NerospaceTelemetry, SentryLogAppender}` +
> `config/NerospaceConfig` (opt-out toggle, **default ON** per user decision) + `IPlatformHelper.getConfigDir/
> getModVersion` seam. PII scrubbing + nerospace-only filter + de-dup/cap intact; production-gated (off in dev).
> ⚠️ Runtime-unverified (dev-gated + mount lag) — confirm on a shipped jar. Closes the last pre-existing pending
> task.

> **2026-06-21 update — oxygen field client visuals.** All 4 cells green. Added `network/OxygenFieldSyncPayload`
> + `client/{ClientOxygenField, ClientOxygenVisuals}`; the field now syncs to nearby clients and renders as
> drifting GLOW particles + a boundary sound — the breathable volume is finally visible. 2nd networking-seam
> consumer. The haze fog-tint layer is deferred (NeoForge-only fog event; no portable Fabric counterpart).

> **2026-06-21 update — oxygen hazard shields.** All 4 cells green. Extended `OxygenManager` with per-planet
> hazards (Cindara heat / Glacira cold → ×4 drain unless wearing the matching suit variant) + frost/smoke
> feedback. The ported thermal/cryo suit variants are now functional (previously inert). No new class — an
> in-place enhancement. Airlock refill still deferred (needs the gas-cap lookup).

> **2026-06-21 update — terraforming slice 6b: TerraformDrift (ambient cosmetic).** All 4 cells green.
> `world/TerraformDrift` ticked from the shared server hook. **Terraforming is now essentially complete**
> (slices 1–5 + 6a + drift); only the opt-in force-loader remains (off by default). Note: `GreenxertzAtmosphere`
> is the root's oxygen-survival class, already superseded by the ported `OxygenManager` — reclassified out of
> terraforming; its hazard-shield + airlock-refill extras are a separate optional oxygen enhancement.

> **2026-06-21 update — terraforming slice 6a: Terraform Monitor.** All 4 cells green. Added
> `machine/{TerraformMonitorBlock, TerraformMonitorBlockEntity}` + `menu/TerraformMonitorMenu` +
> `client/TerraformMonitorScreen` (pure readout, no inventory; reads `TerraformManager`). Registered + assets +
> loot table + lang. Terraforming is now slices 1–5 + 6a done; only optional ambient bits remain (6b: Drift,
> ChunkLoader, GreenxertzAtmosphere).

> **2026-06-21 update — terraforming slice 5: Hydration Module.** All 4 cells green. Added
> `machine/{HydrationModuleBlock, HydrationModuleBlockEntity}` + `menu/HydrationModuleMenu` +
> `client/HydrationModuleScreen` (BE on `WorldlyContainer`/`NonNullList`, melts glacite into a touching
> Terraformer's water-stage buffer). Registered + item cap + assets + `hydration_input` tag + loot table + lang.
> Also fixed a 4b omission: the Terraformer block had no loot table (added — it would have dropped nothing).
> Remaining terraform: slice 6 = Monitor + Drift + ChunkLoader + GreenxertzAtmosphere (all secondary).

> **2026-06-21 update — terraforming slice 4b: the Terraformer machine.** All 4 cells green. Added
> `machine/{MachineRedstone, TerraformerBlock, TerraformerBlockEntity}` + `menu/TerraformerMenu` +
> `client/TerraformerScreen`; the 584-line BE rewritten onto `EnergyBuffer` + a `WorldlyContainer` upgrade slot
> (force-load deferred). Registered + capped + assets/lang. **Placing a Terraformer now greens the planet
> outward through the three stages** — the signature feature is functional cross-loader. Remaining terraform
> slices: 5 Hydration Module, 6 Monitor + Drift + ChunkLoader + GreenxertzAtmosphere.

> **2026-06-21 update — terraforming slice 4a (TerraformManager + chunk-load seam).** All 4 cells green.
> Added `world/TerraformManager` (3rd SavedData; per-terraformer radii + `onChunkLoaded` catch-up) + a
> per-loader chunk-load hook. **26.x gotcha: Fabric `ServerChunkEvents.Load` SAM is 3-param
> `(ServerLevel, LevelChunk, boolean)`** (probed). Remaining: slice 4b = the Terraformer machine BE (rewrite
> onto EnergyBuffer/Container, defer force-load) + block/menu/screen — the slice that drives the engine.

> **2026-06-21 update — terraforming slice 3 (conversion engine).** All 4 cells green. Added
> `machine/{TerraformConversion (335ln staged converter), TerraformResources}` + `world/TerraformFauna`;
> stage bookkeeping rewired from `chunk.getData(ModAttachments…)` onto the slice-1 `Services.PLATFORM` chunk
> seam. Worldgen APIs (TreeFeatures/ConfiguredFeature.place/PalettedContainer/EntityType.spawn) resolve on
> common. Next = slice 4: Terraformer machine (block/BE 584ln/menu/screen) + TerraformManager (SavedData) +
> chunk-load catch-up hook — the slice that makes terraforming actually run.

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
- [x] **Station founding DONE (4 cells green).** `StationCoreBlock`(+BE), `StationRegistry` (SavedData,
  POPIA-clean), and a new `StationCharterItem` — right-click the charter to found a station (slot + 7×7 pad +
  bound Core in the void station dim) and travel there; breaking the Core unregisters + pops the named charter;
  `guide/station_charter` is code-granted on founding (routes around `ModCriteria`). Founding is **charter-driven**
  rather than via the rocket FOUND row (the rocket's per-station selection/return rows remain deferred).

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
  config keys inlined.
- [x] **Oxygen field client visuals DONE (4 cells green).** `network/OxygenFieldSyncPayload` (range snapshot,
  long[]/byte[]) registered clientbound and pushed from `OxygenFieldEvents` every 10t to nearby players;
  `client/ClientOxygenField` (data holder) + `client/ClientOxygenVisuals` (client-tick: drifting GLOW particles
  in breathable cells + a boundary-crossing sound). 2nd networking-seam consumer. **Deferred: the haze fog-tint
  layer** — rode a NeoForge-only `ViewportEvent.ComputeFogColor` with no portable Fabric counterpart.
- [x] **Hazard shields DONE (4 cells green).** `OxygenManager` now applies a per-planet hazard (Cindara HEAT /
  Glacira COLD): ×4 oxygen drain unless a full set of the matching `HazardShield` suit variant is worn (mixed
  set = no shield). Adds `hazardFor`/`hazardShield`/`pieceVariant`/`hazardDrainMultiplier` + thematic feedback
  (frost vignette on cold, smoke shimmer on hot — no extra damage path). **Makes the already-ported thermal/cryo
  suit variants functional.**
- [ ] **Deferred**: terraform-breathability advancement criteria, gas-tank airlock refill (needs the gas-cap
  lookup; the field/pad/terraformed already refill).
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
  - [x] **Slice 3 — conversion engine.** `machine/TerraformConversion` (staged column conversion: stage 1
    Rooted = terrain→grass/dirt via `TERRAFORM_TO_GRASS/DIRT` tags + breathable flag + `TERRAFORMED` biome +
    plants/ore; stage 2 Hydrated = basin water fill; stage 3 Living = mature biome + trees + herds — stage
    bookkeeping rewired onto the `Services.PLATFORM` chunk seam), `machine/TerraformResources` (inlined ore
    list), `world/TerraformFauna` (inlined herd config). Worldgen APIs (`TreeFeatures`, `ConfiguredFeature.place`,
    `PalettedContainer` biome write, `EntityType.spawn`) all resolve on common. ~7 config/tuning keys inlined.
    All 4 cells green. (Inert until slice 4's machine + manager drive it.)
  - [x] **Slice 4a — TerraformManager + chunk-load seam.** `world/TerraformManager` (3rd `SavedData`,
    4-arg `SavedDataType`; tracks per-terraformer stage radii; `onChunkLoaded` replays staged conversion on
    in-range columns of newly-loaded chunks + biome-sync packet). Per-loader chunk-load hook: NeoForge
    `ChunkEvent.Load` (filter `ServerLevel`+`LevelChunk`), Fabric `ServerChunkEvents.CHUNK_LOAD` (**3-param
    SAM `(ServerLevel, LevelChunk, boolean newlyGenerated)`** — probed). All 4 cells green. (Inert until 4b.)
  - [x] **Slice 4b — Terraformer machine DONE (4 cells green).** `machine/{MachineRedstone, TerraformerBlock,
    TerraformerBlockEntity}` + `menu/TerraformerMenu` + `client/TerraformerScreen`. BE rewritten onto
    `EnergyBuffer` + a vanilla `WorldlyContainer`/`NonNullList` upgrade slot (dropped NeoForge
    `SimpleEnergyHandler`/`MachineItemHandler`/`ResourceHandler`); **force-load deferred** (unloaded columns
    handled by the slice-4a catch-up); Tuning/Config inlined; drives `TerraformConversion` 3-stage frontier +
    `TerraformManager.update` + biome-sync packet. Registered block/item/BE/menu + per-loader screen + energy/item
    caps; copied block (3 textures, FACING blockstate, multi-tex model) + GUI texture + 9 lang keys. **Placing a
    Terraformer now greens the planet outward (Rooted→Hydrated→Living).**
  - [x] **Slice 5 — Hydration Module DONE (4 cells green).** `machine/{HydrationModuleBlock,
    HydrationModuleBlockEntity}` + `menu/HydrationModuleMenu` + `client/HydrationModuleScreen`. Melts glacite
    (the `hydration_input` tag) from a `WorldlyContainer`/`NonNullList` slot into a TOUCHING Terraformer's
    hydration buffer (`acceptHydration`); no energy of its own. Registered block/item/BE/menu + per-loader
    screen + item cap; copied block (3 tex, FACING blockstate, model) + GUI + loot table + `hydration_input`
    tag JSON + 5 lang keys. **Also fixed: the Terraformer block was missing its loot table from 4b (added).**
  - [x] **Slice 6a — Terraform Monitor DONE (4 cells green).** `machine/{TerraformMonitorBlock,
    TerraformMonitorBlockEntity}` + `menu/TerraformMonitorMenu` + `client/TerraformMonitorScreen`. Pure readout
    (no inventory — `MenuProvider` + `ContainerData`): finds the nearest Terraformer via `TerraformManager`,
    shows stage radii / hydration / stall + the local column's stage on a comparator. Registered + per-loader
    screen + assets + loot table + 9 lang keys. No caps (no inventory).
  - [x] **Slice 6b — `TerraformDrift` DONE (4 cells green).** `world/TerraformDrift` — idle ground-cover
    garnish on settled terraformed land, near players, on a per-second budget; cross-loader `tick(MinecraftServer)`
    wired into both server-tick hooks (alongside meteor + oxygen-field). Config inlined.
  - [ ] **Remaining (optional, low value):** `TerraformChunkLoader` (the deferred opt-in active force-loader —
    needs a chunk-force-ticket seam; off by default so the chunk-load catch-up covers it).
    `world/GreenxertzAtmosphere` is **NOT terraforming** — it's the root's full oxygen-survival class, already
    superseded by the ported `OxygenManager` + diffusion field + terraformed-flag. Its only unported extras
    (hazard shields heat/cold, gas-tank airlock refill) are a separate **oxygen enhancement**, tracked below.

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

### Star Guide / progression  (`progression/` 5 + client + item) — **slice 1 DONE (4 cells green)**
- [x] **Slice 1 — browsable guide.** `progression/{StarGuide, StarGuideProgress, StarGuideBlock,
  StarGuideBlockEntity, StarGuideMenu}` + `item/StarGuideBookItem` + `client/StarGuideScreen`. Registered
  block/block-item/book/BE/menu + per-loader screen + assets + 98 lang keys. Opens from the book (in hand)
  or the pedestal (install the book). Reads advancement completion — **no `ModCriteria` dependency**.
- [x] **Slice 2a — advancement DATA DONE (4 cells green).** Copied all 42 nerospace advancements; **39 use
  pure vanilla triggers** (`inventory_changed` / `changed_dimension` / `bred_animals`) and track real
  completion immediately. The **3 custom-trigger ones** (`terraformed_ground`/`living_world`/`station_charter`,
  which need the deferred `ModCriteria` whose `PlayerTrigger` base moved packages 26.1↔26.2) were rewritten to
  `minecraft:impossible` so they load and keep the parent chain intact (children `hydration_module`/`new_life`
  are not orphaned) — they display but stay incomplete until granted. Repointed 2 display icons off unported
  items (`station_charter`→`station_floor`, `new_life`→`meadow_loper_spawn_egg`). **The guide now tracks live
  progress.** All JSON parse-validated; item predicates + the 4 `changed_dimension` targets all resolve.
- [x] **Slice 2b — hologram BER DONE (4 cells green).** Added a reusable cross-loader BER seam
  `client/ClientBlockEntityRenderers` (`Sink` mirrors `ClientEntityRenderers` — NeoForge
  `RegisterRenderers.registerBlockEntityRenderer`, Fabric `BlockEntityRendererRegistry.register`) +
  `client/{StarGuideHologramRenderer, StarGuideHologramRenderState}` (verbatim 26.x BER submission). The
  pedestal now floats the spinning next-step hologram. **26.x gotcha: `BlockEntityRendererProvider` takes 2
  type params `<T, S extends BlockEntityRenderState>`** (probed via build error) — the Sink carries both. The
  seam now unblocks future BERs (solar sun-tracking, quarry drill, etc.). (Fabric `BlockEntityRendererRegistry`
  is soft-deprecated — works; a later switch to vanilla `BlockEntityRenderers.register` is optional.)
- [x] **Slice 2c — seen-pulse DONE (4 cells green).** Added a `List<Integer>` `STAR_GUIDE_SEEN` player
  attachment through the existing data-attachment seam (`IPlatformHelper.get/setStarGuideSeen` +
  `NeoForgeAttachments`/`FabricAttachments`, `Codec.INT.listOf()`, copy-on-death). Restored the menu's seen
  masks (`DATA_COUNT = CHAPTER_COUNT*2`, `clickMenuButton` marks seen via `Services.PLATFORM`) + the screen's
  completed-but-unseen pulse (clicking a step acknowledges it). **The Star Guide is now feature-complete**
  (browse + live progress + hologram + seen-pulse). 26.x gotcha: NeoForge `AttachmentType.builder(...)` is
  overloaded, so the default must be a lambda `() -> List.of()` (not the `List::of` method ref — ambiguous).
- [x] **Slice 2d — terraform advancements code-granted (4 cells green).** `progression/StarGuideGrants`
  (driven from the per-player server tick, beside `OxygenManager.tick`) awards the impossible-criterion
  `guide/terraformed_ground` (chunk stage ≥ 1) and `guide/living_world` (stage ≥ 3) directly when the player
  stands on terraformed / fully-living ground — replicating the standalone mod's `PlayerTrigger` **without**
  `ModCriteria`. **41 of 42 advancements now track real completion.** 26.x: award via
  `getOrStartProgress(holder).getRemainingCriteria()` → `PlayerAdvancements.award(holder, criterion)`.
- [x] **Slice 2e — DONE via station founding.** `guide/station_charter` is now code-granted when a station is
  founded (the charter item), and its Star-Guide step + advancement icons point at the now-real `station_charter`
  item. **All 42 advancements track real completion.** Only the `new_life` guide-step icon stays substituted
  (Meadow Loper spawn egg) until `LOPER_HAUNCH` is ported — purely cosmetic.

### Pipes — advanced  (`pipe/` + items + payload + renderer; basic pipe already ported) — **slice A DONE (4 cells green)**
- [x] **Slice A — per-face configuration layer.** `pipe/PipeIoMode` + `pipe/PipeResourceType` (vanilla
  enums); `item/{ConfiguratorItem, PipeFilterItem (vanilla ItemStack filter), PipeUpgradeItem ×2}`.
  `UniversalPipeBlockEntity` extended with per-face×per-type modes (packed long) + per-face item filters +
  speed/capacity upgrades; the energy/gas/item relay honours `canPull`/`canPush`/`OFF` + filters + speed
  throughput; `UniversalPipeBlock` sneak-empty-hand pops upgrades. Items registered (TOOLS tab) + assets +
  20 lang keys.
- [x] **Fluid relay** — added the `platform/FluidLookup` query seam (common + both loaders + services) and a
  `FluidTank` + `relayFluid()` to the pipe BE (honours the FLUID face-mode + speed); the pipe's fluid handler
  is exposed as the FLUID cap on both loaders. The pipe now carries all four layers; the FLUID face-mode is
  live (e.g. Refinery → pipe → Fuel Tank).
- [ ] **Slice B (deferred) — graph + visuals + GUI.** `PipeNetwork` (591-line graph; NeoForge-transfer-
  coupled), `TravellingItem` (animated stacks; ItemResource→ItemStack), `UniversalPipeRenderer` +
  `UniversalPipeRenderState` (stream + travelling-item visuals), `PipeConfigScreen` + `PipeConfigOpenHandler`
  + `network/SetPipeModePayload` (the per-face×per-type config GUI — needs a cross-loader client-screen-open
  seam). Cosmetic / convenience; the slice-A in-world cycling already configures pipes fully.

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
- [x] `ConfiguratorItem`, `PipeFilterItem`, `PipeUpgradeItem` — DONE (advanced-pipes slice A; TOOLS tab).
- [ ] `StarGuideBookItem` (depends on **star guide**).
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

### Config / tuning — **slice 1 DONE (4 cells green); cross-loader seam established**
- [x] **Slice 1 — config seam + energy multiplier.** Extended the properties-based `config/NerospaceConfig`
  (no NeoForge `ModConfigSpec` — the cross-loader seam is the properties file the telemetry batch added) with
  `energyRateMultiplier` (clamp 0.1×..10×, default 1) + a `scale(base, mult)` helper (min-1 clamp, mirroring
  the root `Tuning` contract); wired it into the Combustion / Passive / Solar generator FE-per-tick. Loads at
  mod init (before ticking). This proves the cross-loader balance-config pattern beyond the telemetry toggle.
- [ ] **Remaining (incremental).** The root's other four multipliers (`oxygenDrainMultiplier`,
  `oxygenCapacityMultiplier`, `fuelCostMultiplier`, `machineSpeedMultiplier`) + the base-value constants — wire
  each into its consumer(s) as a follow-up (kept out of the config file until wired, so every key does
  something). A full `Tuning` base-value class is optional (the multiloader inlines base values per machine).

### Spawn rules
- [x] `registry/ModSpawnPlacements` — natural-spawn placement rules for the 9 spawnable creatures
  (6× `ON_GROUND` light-independent; 3× terraform livestock gated on `GRASS_BLOCK`). Cross-loader
  spawn-placement seam (`ModSpawnPlacements.Sink`): NeoForge `RegisterSpawnPlacementsEvent`
  (`Operation.REPLACE`) vs Fabric vanilla `SpawnPlacements.register`. Both stable on 26.1.2 + 26.2.
  Ruin Warden has no rule (structure/event boss only).

---

## 📡 Sentry / telemetry  (`telemetry/`) — **POPIA/GDPR-sensitive** — DONE (4 cells green)
- [x] `telemetry/NerospaceTelemetry` — the Sentry client: captures Nerospace exceptions/crashes, with
  **PII scrubbing** (no IP/identity/hostname; OS-account names scrubbed from file paths via the `USER_PATH`
  regex incl. `C:\Users\<name>\...`), **nerospace-only `beforeSend` filter**, **de-dup + 10/session cap**.
  Parameterised off `Services.PLATFORM` (mod version, loader name, dist) instead of FML.
- [x] `telemetry/SentryLogAppender` — Log4j2 appender selecting ERROR/FATAL events touching Nerospace code.
- [x] `config/NerospaceConfig` — minimal properties config (`config/nerospace.properties`); **`telemetryEnabled`
  default ON, opt-out** (user decision 2026-06-21). Config-dir via new `IPlatformHelper.getConfigDir()` seam.
- [x] **Sentry SDK bundled per-loader** — common `compileOnly`, NeoForge `jarJar`, Fabric Loom `include`
  (both bundling tasks ran green). `NerospaceTelemetry.init()` called at each loader's bootstrap; **only
  initialises in a production (non-dev) environment**.
- [ ] **Deferred**: `SentryTestBlock` (debug block) — minor dev tool.
- ⚠️ **Runtime-verify on a shipped build**: the 4-cell compile + the jarJar/include tasks are green, but
  Sentry initialisation, the nerospace-only filter, and path scrubbing have NOT been runtime-tested here
  (dev-gated + sandbox mount lag). Confirm on a production jar before relying on it. DSN = root's EU ingest.

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
