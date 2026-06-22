# Nerospace multiloader — port checklist

Audit of what the standalone NeoForge mod (`src/main/java`, 264 classes) still needs ported into the
cross-loader `multiloader/` project. As of this audit: **~218 classes ported, ~46 remaining**, all four
build cells (NeoForge + Fabric × MC 26.1.2 + 26.2) green.

> **2026-06-22 update — quarry drill-head BER DONE (the flagship machine now digs visibly).** All 4 cells
> green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline, 0 new).
> `client/{QuarryControllerRenderer, QuarryControllerRenderState}` draw a gantry crane + spinning drill head
> tracking the dig column (textured `entityCutout` submission geometry, gantry/bit verbatim from root),
> registered via the BER seam. `QuarryControllerBlockEntity` gained `getUpdatePacket`/`getUpdateTag` +
> throttled `sendBlockUpdated` (syncs region/state/cursor) + `render*` accessors + client `disp*` smoothing.
> Head eases toward the current dig cell (simplified from the root's mined-history lerp). **26.x gotcha: the
> root's `getRenderBoundingBox(T)` BER override isn't on the de-obf signature — javac rejects it (ecjCheck's
> lenient prefs missed it); dropped it, `shouldRenderOffScreen()=true` keeps the gantry visible.** ~230 classes.

> **2026-06-22 update — rocket item-canister intake proxy DONE (rocket automation now fluid + items).** All
> 4 cells green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline,
> 0 new). `rocket/RocketPadItemContainer` (stateless single-slot `WorldlyContainer` over the pad position,
> re-finds the docked rocket each call via the new `LaunchPadMultiblock.dockedRocket` helper and forwards to
> its `getFuelInput()`) exposed as the launch-pad block's Item capability — NeoForge `registerBlock`
> (sided `WorldlyContainerWrapper`) / Fabric `ItemStorage.SIDED.registerForBlocks` + `ContainerStorage.of`.
> Accepts only fuel containers while a non-launching rocket is docked; rejects everything (no item loss) when
> the pad is empty; lets automation reclaim the empty bucket the rocket leaves but never pull a full canister.
> Reuses the no-BE block-cap pattern from the fluid proxy. **The rocket pipe/hopper automation proxy is now
> complete (fluid + items).** ~228 classes.

> **2026-06-22 update — rocket auto-fuel automation proxy DONE (the last real gameplay gap).** All 4 cells
> green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline, 0 new).
> `rocket/RocketPadFluidProxy` (a stateless `NerospaceFluidStorage` over a pad position) is exposed as the
> **launch-pad block's FLUID capability** and forwards `rocket_fuel` into the docked rocket via
> `RocketEntity.addFuel` (finds it through `LaunchPadMultiblock.connectedPads`+`rocketAbove`; sink only,
> refuses mid-launch). **New cross-loader pattern: a BLOCK-level capability with no block entity** — NeoForge
> `RegisterCapabilitiesEvent.registerBlock(FLUID, (level,pos,state,be,side)->proxy, PAD)`, Fabric
> `FLUID.registerForBlocks((world,pos,state,be,side)->proxy, PAD)` (first use of the no-BE registration on
> both loaders; reusable for other entity-on-block proxies). A pipe or any fluid source beside the pad now
> refuels the rocket — Refinery → pipe → pad → rocket — routing around the absent entity-capability seam. The
> item-canister intake (hoppers) is the only remaining secondary bit. **~227 classes.**

> **2026-06-22 update — gas-tank airlock suit-refill DONE (oxygen enhancement).** All 4 cells green
> (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline, 0 new).
> `OxygenManager` now refills a worn suit from a nearby Gas Tank / Creative Gas Tank / Oxygen Generator
> holding Oxygen — radius scan → `GasLookup.INSTANCE.find` → drain whole air units (`AIRLOCK_MB_PER_AIR` mB
> each) + bubble SFX, in the not-breathable branch (drain, then top up). A tank by the base door acts as an
> airlock. Unblocked by the `GasLookup` seam the advanced-pipes batch added (the original deferral note is
> now stale). The only remaining oxygen deferral is the custom-trigger advancement criteria (`ModCriteria`
> 26.1↔26.2 package split).

> **2026-06-22 update — pipe stream pulses DONE → advanced pipes slices A+B COMPLETE.** All 4 cells green
> (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline, 0 new). The
> Universal Pipe renderer now draws the coloured energy/fluid/gas stream pulses along active arms (red/blue/
> cyan), reading the `CONNECTIONS` blockstate + buffered amounts + per-face modes; ported `renderStreams`/
> `crossQuads`/`quad` near-verbatim via `submitCustomGeometry` + `RenderTypes.lightning()` (resolves on both
> versions). Broadened the BE client-sync to two cadences (fast 3t for in-flight items, slow 10t for buffered
> content) so streams stay current without per-tick spam. **The Universal Pipe is now fully featured** (4-layer
> relay + per-face config GUI + connected model + travelling-item + stream visuals); only the optional 591-line
> `PipeNetwork` routing graph stays deferred (pure refactor — the per-BE relay already moves everything).

> **2026-06-22 update — connected-pipe model DONE (B2 follow-on; unblocks the stream layer).** All 4 cells
> green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21 baseline, 0 new;
> 3 JSON files validated). `UniversalPipeBlock` gained the 6 vanilla boolean connection properties +
> per-mask `VoxelShape`s + a multipart blockstate (core + 6 rotated arms) + `universal_pipe_core`/`_arm`
> models (existing texture). `canConnect` uses the lookup seams + `Container` (not NeoForge `Capabilities`).
> **Cross-version-safe connection refresh: recomputed from the BE server tick (throttled `setBlock`) +
> `getStateForPlacement`, deliberately avoiding the fragile `neighborChanged`/`updateShape` overrides.**
> Pipes now visually join neighbours (arms) with matching collision, and the travelling items flow through
> the arms. **~226 classes.**

> **2026-06-22 update — advanced-pipes slice B2 (travelling-item visuals) started + items lane DONE.**
> All 4 cells green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21
> baseline warnings, 0 new). Items now visibly flow through Universal Pipes: `pipe/TravellingItem` (rebuilt
> on vanilla `ItemStack`) + `client/UniversalPipeRenderer` + `client/UniversalPipeRenderState`, registered
> via the `ClientBlockEntityRenderers` BER seam. **Cosmetic-echo design (zero relay risk):** the instant
> item relay is untouched; each successful push spawns a transient `TravellingItem` that the BE advances,
> expires and persists, riding the BE update packet (`getUpdatePacket`/`getUpdateTag`=`saveCustomOnly`,
> throttled `sendBlockUpdated`); the renderer advances locally between syncs for smooth motion. **Deferred
> within B2:** the coloured energy/fluid/gas stream pulses (need per-face connection blockstates the
> single-cube pipe model lacks) and the optional 591-line `PipeNetwork` routing graph. **~226 classes.**

> **2026-06-22 update — Artificer gear behaviour ported (the village's exclusive trades are now functional).**
> All 4 cells green (`:neoforge:build`+`:fabric:build` on both 26.2 and 26.1.2; ecjCheck 0 errors / 21
> baseline warnings, 0 new). Added `gear/XertzResonatorItem` (right-click ore-ping over a new `c:ores`
> convention tag — `ModTags.Blocks.ORES`, the cross-loader replacement for NeoForge `Tags.Blocks.ORES`) +
> `gear/AlienGearAbilities` (shared `negatesFall` predicate). Grav Striders' fall-negate is wired through a
> small per-loader event seam: NeoForge `LivingFallEvent.setDamageMultiplier(0)`, Fabric
> `ServerLivingEntityEvents.ALLOW_DAMAGE` vetoing `DamageTypes.FALL`. This is the cross-loader stand-in for
> the root's NeoForge-only `@EventBusSubscriber` `AlienGearEvents`. **~224 classes ported.**

> **2026-06-22 update — Village Core interactive controller ported (closes the last big gameplay gap).**
> All 4 cells green (full `:neoforge:build`+`:fabric:build` on **both** 26.2 and 26.1.2; ecjCheck 0 errors /
> 21 baseline warnings, 0 new). The decorative `VillageCoreBlock` stub is now the root's full teach-and-grow
> engine: ported `village/VillageBuildings` (building catalogue + quest table + box-structure generator) +
> `village/VillageCoreBlockEntity` (373-line controller: claim, nerosteel stockpile, reputation-gated build
> jobs with staged block-by-block placement, passive production, fetch quests, config-gated night raids,
> `ValueInput`/`ValueOutput` persistence) and replaced the block with the interactive `BaseEntityBlock`.
> Registered the `VILLAGE_CORE` block-entity type + 2 message lang keys; added an `alienRaidsEnabled`
> opt-out (default ON) to the properties `NerospaceConfig`. **Cross-version adaptations:** the after-dark
> raid gate uses vanilla `Level.getSkyDarken()` (not `isBrightOutside()`, which diverges 26.1.2↔26.2), and
> raids read the properties config seam rather than a NeoForge `ModConfigSpec`. Reuses the already-ported
> `AlienVillager` reputation API. The structures place the same block, so alien hamlets / ruins / mega-cities
> now ship a live, claimable, growable Village Core. **~222 classes ported.**

> **2026-06-21 update — /nerospace commands ported.** All 4 cells compile green. `command/NerospaceCommands`
> (the `/nerospace gallery` creative showcase) behind a cross-loader `register(CommandDispatcher)` seam
> (NeoForge `RegisterCommandsEvent` / Fabric `CommandRegistrationCallback`). Adapted: block iteration via
> `BuiltInRegistries.BLOCK` namespace filter (no `RegistrationProvider` iteration), single `SOLAR_PANEL`,
> `ArmorStand` constructor (no `EntityType.ARMOR_STAND` on 26.2), dropped unported `quarry.stageDisplay`.

> **2026-06-21 update — config seam COMPLETE (all 5 multipliers).** All 4 cells compile green. Slices 2–4
> added `oxygenDrain`/`oxygenCapacity` (→ OxygenManager), `fuelCost` (→ RocketTier.fuelPerLaunch), and
> `machineSpeed` (+ inverse `scaleInterval` → grinder/refinery/hydration/terraformer/quarry). All five of the
> root's balance multipliers (0.1×..10×) are now live cross-loader through the properties `NerospaceConfig`.

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
> reclaim the (named) charter. **Slice 2 DONE:** the rocket's per-station selection — the in-rocket UI cycles the
> Orbital Station destination between the origin platform and each founded station, and the rocket docks at the chosen one.

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
>
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
>
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
>
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
>
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

+ [x] **Platform seams** — `Services`/`IPlatformHelper`, `RegistrationProvider` (+ per-loader factories),
  capability seams for item / energy / fluid / gas (expose + query), `FluidFactory` seam.
+ [x] **Registries** — blocks, items, block-entities, menu types, entities, sounds, dimension keys,
  entity attributes (subset that's ported).
+ [x] **Logistics** — energy / fluid / gas / **item** transport; the universal pipe relays all four.
+ [x] **Machines / storage** — combustion + passive + solar generators, oxygen generator, nerosium
  grinder (+ 3 GUIs), item store, battery, creative battery, fluid tank, gas tank, trash can.
+ [x] **Rocket-fuel fluid** — `BaseFlowingFluid`/`FluidType` (NeoForge) vs hand-written `FlowingFluid`
  (Fabric), liquid block + bucket; NeoForge in-world render via the new `RegisterFluidModelsEvent`
  (`FluidModel.Unbaked` + `Material` + `FluidTintSources`).
+ [ ] **Fabric in-world fluid render — BLOCKED on the 26.x fluid-render overhaul (investigated 2026-06-22).**
  The old Fabric `fabric-rendering-fluids-v1` API (`FluidRenderHandlerRegistry` / `SimpleFluidRenderHandler`)
  is **gone** in this Fabric API — 26.x moved fluid rendering to the **vanilla** `net.minecraft.client.renderer.block.FluidModel`
  system (NeoForge registers it via its own `RegisterFluidModelsEvent`). Fabric's registration path for the
  new vanilla `FluidModel` isn't the old handler API and needs investigation against the decompiled client
  (likely a vanilla fluid-model registry or a new Fabric module). Attempt reverted to keep the build green;
  not a simple follow-up. Until then, rocket fuel shows the missing-texture art in-world **on Fabric only**
  (NeoForge is correct; the fluid still functions on both — buckets, tanks, pipes, refinery all work).
+ [x] **All 10 mobs** — xertz stalker, quartz crawler, greenling, ruin warden, cinder/frost striders,
  3 terraform livestock, alien villager (full Merchant trading + reputation). Models, renderers,
  glow layers, sounds, `village` trade tables.
+ [x] **Planet dimensions** — Greenxertz / Cindara / Glacira / Station (datapack data + `space`
  dimension_type + planet biomes that spawn the mobs and generate the ores).
+ [x] **Overworld nerosium ore** worldgen (NeoForge biome modifier + Fabric biome API).

---

## 🚧 Remaining subsystems

### Rockets & travel  (`rocket/` 11 + client + items) — **DONE (4 cells green); item-cap proxy deferred**

+ [x] `RocketTier`, `Destinations` (ported; `Tuning` values inlined as identity-multiplier base values).
+ [~] `RocketEntity` — rebuilt on the cross-loader `FluidTank` + a plain `SimpleContainer(1)` intake +
  vanilla `ServerPlayer.teleportTo`. **Per-station selection DONE:** `DATA_STATION` synced slot (−1 = origin),
  `cycleStation()` cycles origin → each founded station (founding order) → origin via `StationRegistry`, and
  `completeLaunch()` docks the rider at the selected station's `center()` (else the origin platform); the slot
  persists in `addAdditionalSaveData` (`StationSlot`). **Fuel-automation proxy DONE (4 cells green):**
  `rocket/RocketPadFluidProxy` exposes the **launch-pad block's FLUID capability** as a sink that forwards
  `rocket_fuel` into the docked rocket (`addFuel`), registered on the pad block (no BE) via NeoForge
  `event.registerBlock` / Fabric `FLUID.registerForBlocks` — so a pipe or any fluid source beside the pad
  refuels the rocket (Refinery → pipe → pad → rocket), routing around the absence of a cross-loader
  *entity*-capability seam. **Item-canister intake proxy DONE too (4 cells green):** `rocket/RocketPadItemContainer`
  (a stateless single-slot `WorldlyContainer` over the pad pos, forwarding to the docked rocket's
  `getFuelInput()`; accepts only fuel containers while a rocket is docked, and lets automation pull the
  empty bucket back out but never a full canister) exposed as the pad block's Item capability on both loaders
  (NeoForge `registerBlock` sided wrapper / Fabric `ItemStorage.SIDED.registerForBlocks` + `ContainerStorage.of`).
  **The rocket pipe/hopper automation proxy is now complete (fluid + items).** Risk: travel/teleport still
  unverifiable headlessly — compile-verified only.
+ [x] `RocketItem` ×4 tiers, `RocketMenu` + `RocketScreen`. Menu is **non-extended** (no loader-divergent
  extended-menu API); buttons route via `clickMenuButton`. **Station selection DONE:** `BUTTON_CYCLE_STATION`
  + a synced `[5]=stationSlot` data value + a `RocketScreen` "Dock:" cycler shown only when the Orbital Station
  is the chosen destination. **Real charter names** ride a small clientbound `StationSyncPayload` (slot→name
  parallel arrays, POPIA-clean — no player identity) pushed when the player opens a rocket and cached in
  `client/ClientStations`; the cycler shows the live name (falling back to "Station N"/"Origin Platform") since
  the int-only `ContainerData` can't carry strings. The standalone FOUND row stays dropped — founding is charter-driven.
+ [x] `RocketModel` (+ `RocketT2/T3/T4Model`), `RocketRenderer` (bakes each tier layer directly — no
  model-layer registry), `RocketRenderState`; entity + item textures copied.
+ [x] Launch pad / gantry: `RocketLaunchPadBlock`, `LaunchGantryBlock`, `LaunchPadMultiblock` (multiblock gating).
+ [x] **Station founding DONE (4 cells green).** `StationCoreBlock`(+BE), `StationRegistry` (SavedData,
  POPIA-clean), and a new `StationCharterItem` — right-click the charter to found a station (slot + 7×7 pad +
  bound Core in the void station dim) and travel there; breaking the Core unregisters + pops the named charter;
  `guide/station_charter` is code-granted on founding (routes around `ModCriteria`). Founding is **charter-driven**
  rather than via the rocket FOUND row; the rocket's **per-station selection is now DONE** (see `RocketEntity`/`RocketMenu` above).

### Quarry  (`machine/quarry/` 11 + client) — **DONE (4 cells green); modules + BER deferred**

+ [x] Area miner ported: `QuarryControllerBlock`(+BE) + `QuarryMenu`/`QuarryScreen`, `QuarryFrameBlock`,
  `QuarryLandmarkBlock`(+BE, client laser ticker), `QuarryRegion`, `MinerTier`, `OutputFilter`,
  `PlanetMiningProfile`. The dig (landmarks → frame ring → layer-by-layer excavation → drops buffered/
  auto-ejected, source fluids sucked) runs server-side; Energy/Item/Fluid caps on both loaders.
+ [~] **Chunk-loading**: `QuarryChunkLoader` (NeoForge `TicketController`) replaced by vanilla
  `ServerLevel.setChunkForced` (works on both loaders; one chunk pinned at a time, persisted + released
  on removal) — no cross-loader ticket seam needed.
+ [x] **Drill-head BER DONE (4 cells green).** `client/{QuarryControllerRenderState, QuarryControllerRenderer}`
  — a gantry crane riding the frame + a spinning drill head tracking the dig column, as textured submission
  geometry (`RenderTypes.entityCutout` over `quarry_gantry`/`quarry_drill`), registered via the
  `ClientBlockEntityRenderers` seam. The BE syncs its dig state (region/state/currentY/cursor ride a new
  `getUpdatePacket`/`getUpdateTag` + throttled `sendBlockUpdated` while working) and exposes
  `renderRegion`/`renderState`/`renderCurrentY`/`renderCursor` + client `dispX/Y/Z` smoothing fields.
  **Cross-loader simplification:** the head eases toward the current dig cell (`region.columnPos(cursor,
  currentY)`) instead of the root's per-block mined-history time-lerp (less synced state, same look).
  **26.x gotcha: `BlockEntityRenderer.getRenderBoundingBox(T)` is NOT on the de-obf BER signature** (javac
  rejects the `@Override`; ecjCheck's lenient prefs missed it — the full build is the authority) → dropped it
  and rely on `shouldRenderOffScreen()=true` (a valid override) to keep the gantry from culling.
+ [~] **Deferred (minor):** fluid **auto-eject** (the fluid buffer is drained by pipes instead). Upgrade
  modules are DONE (re-enabled in the `module/` batch — this note was stale). `Tuning` values inlined.

### Fuel machines  (`machine/Fuel*` — depends on the ported rocket-fuel fluid) — **DONE (4 cells green)**

+ [x] `FuelTankBlock`(+BE +menu +screen): stores `rocket_fuel`, accepts buckets/canisters, auto-fuels a
  rocket on an adjacent pad (4x on a full 3x3, 12x on a Heavy complex), comparator out. Rebuilt on the
  shared `FluidTank`; canister slot is a vanilla `WorldlyContainer` (Item cap on both loaders); Fluid cap
  exposed for pipe filling. Pump FX uses a vanilla sound (root's `ModSounds.FUEL_TANK_PUMP` alias not ported).
+ [x] `FuelRefineryBlock`(+BE +menu +screen): coal/charcoal + blaze powder + grid power → liquid
  `rocket_fuel` over a work cycle; Energy (insert-only) + Fluid (extract) + Item caps on both loaders.
  Rebuilt on `EnergyBuffer` + `FluidTank` + a vanilla `WorldlyContainer`; `Tuning` values inlined.
  Assets (textures, models, blockstates, loot, recipes) + 4 lang keys copied.

### Atmosphere / terraforming  (`world/Oxygen*`, `world/Terraform*`, `machine/Terraform*`, `HydrationModule`)

+ [~] **Oxygen survival core DONE (4 cells green)** — `OxygenManager` (per-player O2 drain/suffocate/refill,
  air-supply-bar mirror, full-suit detection) on a new **data-attachment seam**: `IPlatformHelper.get/setOxygen`
  backed by NeoForge `AttachmentType` (`NeoForgeAttachments`) and Fabric `AttachmentRegistry`
  (`FabricAttachments`); ticked per-loader (NeoForge `PlayerTickEvent`, Fabric `ServerTickEvents.END_SERVER_TICK`).
  Breathable = the diffusion field **or** near a Launch Pad (safe-zone radius).
+ [x] **Oxygen diffusion field — server half DONE (4 cells green).** `world/{OxygenField (tag-based
  sealing classifier —`OXYGEN_SEALING`/`OXYGEN_LEAKS`, doors/trapdoors, full-cube fallback),
  OxygenFieldManager (SavedData; sparse fastutil concentration field + source set; per-pass flood-fill
  detects sealed-vs-leaky/open volumes → sealed rooms fill to MAX, open space pressurises only a bubble;
  slow evaporation), OxygenFieldEvents (cross-loader`tick(MinecraftServer)`, throttled sim pass)}`.
  Wired into both server-tick hooks alongside the meteor driver; `OxygenManager.isBreathable` now reads the
  field; the **Oxygen Generator registers itself as a field source**, draining `EMIT_MB_PER_TICK` from its
  tank while sourcing (and clears on `setRemoved`). Sealed bases are now genuinely breathable. ~9 field
  config keys inlined.
+ [x] **Oxygen field client visuals DONE (4 cells green).** `network/OxygenFieldSyncPayload` (range snapshot,
  long[]/byte[]) registered clientbound and pushed from `OxygenFieldEvents` every 10t to nearby players;
  `client/ClientOxygenField` (data holder) + `client/ClientOxygenVisuals` (client-tick: drifting GLOW particles
  in breathable cells + a boundary-crossing sound). 2nd networking-seam consumer. **Deferred: the haze fog-tint
  layer** — rode a NeoForge-only `ViewportEvent.ComputeFogColor` with no portable Fabric counterpart.
+ [x] **Hazard shields DONE (4 cells green).** `OxygenManager` now applies a per-planet hazard (Cindara HEAT /
  Glacira COLD): ×4 oxygen drain unless a full set of the matching `HazardShield` suit variant is worn (mixed
  set = no shield). Adds `hazardFor`/`hazardShield`/`pieceVariant`/`hazardDrainMultiplier` + thematic feedback
  (frost vignette on cold, smoke shimmer on hot — no extra damage path). **Makes the already-ported thermal/cryo
  suit variants functional.**
+ [x] **Gas-tank airlock refill DONE (4 cells green).** `OxygenManager` now tops up a worn suit's air tank
  from a nearby Gas Tank / Creative Gas Tank / Oxygen Generator holding Oxygen (radius scan → `GasLookup`
  seam → drain whole air units at `AIRLOCK_MB_PER_AIR` mB each + a bubble SFX), in the not-breathable branch
  (drain, then refill). A tank by the base door now acts as an airlock without needing a breathable bubble.
  Unblocked by the `GasLookup` seam the advanced-pipes batch added.
+ [ ] **Deferred**: terraform-breathability advancement criteria (the custom-trigger `ModCriteria` split).
+ **Terraforming** (signature endgame) — sliced; **slice 1 DONE (4 cells green)**, rest sequenced:
  + [x] **Slice 1 — per-chunk data-attachment seam.** `IPlatformHelper.is/setTerraformed` +
    `get/setTerraformStage(LevelChunk)` backed by NeoForge `AttachmentType` (chunk `getData`/`setData`) and
    Fabric `AttachmentRegistry` (chunk `getAttachedOrCreate`/`setAttached`) — same registries as the player
    oxygen attachment, just a `LevelChunk` target (no new API surface). Wired into `OxygenManager.isBreathable`
    (terraformed chunk ⇒ breathable). Critical-path foundation for everything below.
  + [x] **Slice 2 — biome + tag data.** `world/ModBiomes` (4 terraformed `ResourceKey<Biome>` constants —
    the multiloader ships biomes as committed datapack JSON, so no datagen bootstrap needed) + copied the 4
    terraformed biome JSON (`terraformed`/`_meadow`/`_savanna`/`_tundra`, feature-free / runtime-written) +
    copied the 2 terraform block-tag JSON (`TERRAFORM_TO_GRASS`/`_DIRT` — TagKey constants already in `ModTags`).
    All 4 cells green; JSON python-validated. (Inert until slice 3 consumes them.)
  + [x] **Slice 3 — conversion engine.** `machine/TerraformConversion` (staged column conversion: stage 1
    Rooted = terrain→grass/dirt via `TERRAFORM_TO_GRASS/DIRT` tags + breathable flag + `TERRAFORMED` biome +
    plants/ore; stage 2 Hydrated = basin water fill; stage 3 Living = mature biome + trees + herds — stage
    bookkeeping rewired onto the `Services.PLATFORM` chunk seam), `machine/TerraformResources` (inlined ore
    list), `world/TerraformFauna` (inlined herd config). Worldgen APIs (`TreeFeatures`, `ConfiguredFeature.place`,
    `PalettedContainer` biome write, `EntityType.spawn`) all resolve on common. ~7 config/tuning keys inlined.
    All 4 cells green. (Inert until slice 4's machine + manager drive it.)
  + [x] **Slice 4a — TerraformManager + chunk-load seam.** `world/TerraformManager` (3rd `SavedData`,
    4-arg `SavedDataType`; tracks per-terraformer stage radii; `onChunkLoaded` replays staged conversion on
    in-range columns of newly-loaded chunks + biome-sync packet). Per-loader chunk-load hook: NeoForge
    `ChunkEvent.Load` (filter `ServerLevel`+`LevelChunk`), Fabric `ServerChunkEvents.CHUNK_LOAD` (**3-param
    SAM `(ServerLevel, LevelChunk, boolean newlyGenerated)`** — probed). All 4 cells green. (Inert until 4b.)
  + [x] **Slice 4b — Terraformer machine DONE (4 cells green).** `machine/{MachineRedstone, TerraformerBlock,
    TerraformerBlockEntity}` + `menu/TerraformerMenu` + `client/TerraformerScreen`. BE rewritten onto
    `EnergyBuffer` + a vanilla `WorldlyContainer`/`NonNullList` upgrade slot (dropped NeoForge
    `SimpleEnergyHandler`/`MachineItemHandler`/`ResourceHandler`); **force-load deferred** (unloaded columns
    handled by the slice-4a catch-up); Tuning/Config inlined; drives `TerraformConversion` 3-stage frontier +
    `TerraformManager.update` + biome-sync packet. Registered block/item/BE/menu + per-loader screen + energy/item
    caps; copied block (3 textures, FACING blockstate, multi-tex model) + GUI texture + 9 lang keys. **Placing a
    Terraformer now greens the planet outward (Rooted→Hydrated→Living).**
  + [x] **Slice 5 — Hydration Module DONE (4 cells green).** `machine/{HydrationModuleBlock,
    HydrationModuleBlockEntity}` + `menu/HydrationModuleMenu` + `client/HydrationModuleScreen`. Melts glacite
    (the `hydration_input` tag) from a `WorldlyContainer`/`NonNullList` slot into a TOUCHING Terraformer's
    hydration buffer (`acceptHydration`); no energy of its own. Registered block/item/BE/menu + per-loader
    screen + item cap; copied block (3 tex, FACING blockstate, model) + GUI + loot table + `hydration_input`
    tag JSON + 5 lang keys. **Also fixed: the Terraformer block was missing its loot table from 4b (added).**
  + [x] **Slice 6a — Terraform Monitor DONE (4 cells green).** `machine/{TerraformMonitorBlock,
    TerraformMonitorBlockEntity}` + `menu/TerraformMonitorMenu` + `client/TerraformMonitorScreen`. Pure readout
    (no inventory — `MenuProvider` + `ContainerData`): finds the nearest Terraformer via `TerraformManager`,
    shows stage radii / hydration / stall + the local column's stage on a comparator. Registered + per-loader
    screen + assets + loot table + 9 lang keys. No caps (no inventory).
  + [x] **Slice 6b — `TerraformDrift` DONE (4 cells green).** `world/TerraformDrift` — idle ground-cover
    garnish on settled terraformed land, near players, on a per-second budget; cross-loader `tick(MinecraftServer)`
    wired into both server-tick hooks (alongside meteor + oxygen-field). Config inlined.
  + [ ] **Remaining (optional, low value):** `TerraformChunkLoader` (the deferred opt-in active force-loader —
    needs a chunk-force-ticket seam; off by default so the chunk-load catch-up covers it).
    `world/GreenxertzAtmosphere` is **NOT terraforming** — it's the root's full oxygen-survival class, already
    superseded by the ported `OxygenManager` + diffusion field + terraformed-flag. Its only unported extras
    (hazard shields heat/cold, gas-tank airlock refill) are a separate **oxygen enhancement**, tracked below.

### Structures  (`world/*Feature`, `village/VillageCore*`, station core, `ModFeatures`) — **DONE (4 cells green)**

+ [x] `HamletFeature`, `MegaCityFeature`, `RuinFeature`, `AlienBuild`, `StructureSpacing` + `ModFeatures`
  (registers the 3 `Feature` types via `RegistrationProvider` over `FEATURE`). Copied the
  configured/placed-feature JSON and **re-added the 3 placed features to the Greenxertz biome JSON**
  (`greenxertz.json` feature step 6) — since Greenxertz is our own datapack biome, no biome-modifier seam
  needed. Mega-city spawns the (ported) Ruin Warden boss; ruin/mega-city fill vanilla loot chests.
+ [x] **`VillageCoreBlock` interactive controller DONE (4 cells green).** The decorative stub is now the
  full root controller: `VillageBuildings` (HUT@T2 / WORKSHOP@T3 catalogue + box-structure generator +
  fetch-quest table) + `VillageCoreBlockEntity` (claim → nerosteel stockpile → rep-gated teach-and-grow
  staged block placement → passive production → fetch quests → config-gated night raids; `ValueInput`/
  `ValueOutput` NBT) + the interactive `BaseEntityBlock` (deposit/claim/quest-handin/collect via vanilla
  `useItemOn`/`useWithoutItem` + the `createTickerHelper` seam). Registered the `VILLAGE_CORE` BE type
  (bound to the existing block) + 2 message lang keys; structures keep placing the same block and it is now
  live. **Cross-version adaptations:** raids read `NerospaceConfig.alienRaidsEnabled()` (the properties
  config seam, default ON/opt-out — no NeoForge `ModConfigSpec`), and the after-dark gate uses the
  long-standing vanilla `Level.getSkyDarken()` instead of `isBrightOutside()` (the de-obf day/night helpers
  diverge 26.1.2↔26.2). Reuses the already-ported `AlienVillager` rep API (`getTier`/`addReputation`).

### Meteor events  (`meteor/` 8 + client)

+ [x] **Creative slice** — `FallingMeteorEntity` (+ `FallingMeteorModel`/`FallingMeteorRenderer`/
  `FallingMeteorRenderState`, bake-direct), `MeteorCallerItem` (creative-only), `MeteorCoreBlock`(+BE,
  break-to-loot), `MeteorLoot`. Meteor Caller → falling meteor → crater of `meteor_rock` around a
  loot-bearing `meteor_core`. `METEOR_ROCK` + loot items (`alien_*`, raw ores) already existed; added
  `FALLING_METEOR` entity, `METEOR_CORE` block+BE (no block item — world-gen only), `METEOR_CALLER`
  item (TOOLS tab) + renderer; copied 3 textures + 4 asset JSON + 4 lang keys. Config meteor keys
  inlined (crater radius 3, bonus rolls 3). All 4 cells green.
+ [x] **Natural showers (scheduler)** — `MeteorSite` + `MeteorEventManager` (the multiloader's first
  `SavedData`) + cross-loader `MeteorEvents.tick(MinecraftServer)` driving the per-level scheduler on the
  4 surface dims (overworld + Greenxertz + Cindara + Glacira); wired into NeoForge `ServerTickEvent.Post`
  and Fabric `END_SERVER_TICK`; `FallingMeteorEntity` re-wired to call `onImpact`. Meteor pacing inlined
  (avg 9000s, warn 30s, 200–500 blocks, ≤4 active). **26.x gotcha: `SavedDataType` on pure-vanilla NeoForm
  has only the 4-arg ctor `(Identifier, Supplier, Codec, DataFixTypes)`** — the standalone mod's 3-arg call
  is a NeoForge convenience; pass `null` DataFixTypes (new mod data, no datafixer schema). All 4 cells green.
+ [x] **Tracker HUD** — `ModItems.METEOR_TRACKER` item + `network/MeteorSyncPayload` (the multiloader's
  **first networking payload** — registered clientbound in `ModNetwork.init()`, auto-wired by both loader
  seams) pushed to tracker holders every 10t from `MeteorEvents` + `client/ClientMeteorTracker` (data
  holder) + `client/MeteorTrackerHud` (action-bar readout via `Player.sendOverlayMessage`) driven by
  per-loader client-tick hooks (NeoForge `ClientTickEvent.Post`, Fabric `END_CLIENT_TICK`). **26.x gotcha:
  `Gui.setOverlayMessage(Component, boolean)` (the standalone mod's call) is gone from vanilla `Gui` —
  use `Player.sendOverlayMessage(Component)`** (probed). Proves the networking seam end-to-end. All 4 cells green.

### Star Guide / progression  (`progression/` 5 + client + item) — **slice 1 DONE (4 cells green)**

+ [x] **Slice 1 — browsable guide.** `progression/{StarGuide, StarGuideProgress, StarGuideBlock,
  StarGuideBlockEntity, StarGuideMenu}` + `item/StarGuideBookItem` + `client/StarGuideScreen`. Registered
  block/block-item/book/BE/menu + per-loader screen + assets + 98 lang keys. Opens from the book (in hand)
  or the pedestal (install the book). Reads advancement completion — **no `ModCriteria` dependency**.
+ [x] **Slice 2a — advancement DATA DONE (4 cells green).** Copied all 42 nerospace advancements; **39 use
  pure vanilla triggers** (`inventory_changed` / `changed_dimension` / `bred_animals`) and track real
  completion immediately. The **3 custom-trigger ones** (`terraformed_ground`/`living_world`/`station_charter`,
  which need the deferred `ModCriteria` whose `PlayerTrigger` base moved packages 26.1↔26.2) were rewritten to
  `minecraft:impossible` so they load and keep the parent chain intact (children `hydration_module`/`new_life`
  are not orphaned) — they display but stay incomplete until granted. Repointed 2 display icons off unported
  items (`station_charter`→`station_floor`, `new_life`→`meadow_loper_spawn_egg`). **The guide now tracks live
  progress.** All JSON parse-validated; item predicates + the 4 `changed_dimension` targets all resolve.
+ [x] **Slice 2b — hologram BER DONE (4 cells green).** Added a reusable cross-loader BER seam
  `client/ClientBlockEntityRenderers` (`Sink` mirrors `ClientEntityRenderers` — NeoForge
  `RegisterRenderers.registerBlockEntityRenderer`, Fabric `BlockEntityRendererRegistry.register`) +
  `client/{StarGuideHologramRenderer, StarGuideHologramRenderState}` (verbatim 26.x BER submission). The
  pedestal now floats the spinning next-step hologram. **26.x gotcha: `BlockEntityRendererProvider` takes 2
  type params `<T, S extends BlockEntityRenderState>`** (probed via build error) — the Sink carries both. The
  seam now unblocks future BERs (solar sun-tracking, quarry drill, etc.). (Fabric `BlockEntityRendererRegistry`
  is soft-deprecated — works; a later switch to vanilla `BlockEntityRenderers.register` is optional.)
+ [x] **Slice 2c — seen-pulse DONE (4 cells green).** Added a `List<Integer>` `STAR_GUIDE_SEEN` player
  attachment through the existing data-attachment seam (`IPlatformHelper.get/setStarGuideSeen` +
  `NeoForgeAttachments`/`FabricAttachments`, `Codec.INT.listOf()`, copy-on-death). Restored the menu's seen
  masks (`DATA_COUNT = CHAPTER_COUNT*2`, `clickMenuButton` marks seen via `Services.PLATFORM`) + the screen's
  completed-but-unseen pulse (clicking a step acknowledges it). **The Star Guide is now feature-complete**
  (browse + live progress + hologram + seen-pulse). 26.x gotcha: NeoForge `AttachmentType.builder(...)` is
  overloaded, so the default must be a lambda `() -> List.of()` (not the `List::of` method ref — ambiguous).
+ [x] **Slice 2d — terraform advancements code-granted (4 cells green).** `progression/StarGuideGrants`
  (driven from the per-player server tick, beside `OxygenManager.tick`) awards the impossible-criterion
  `guide/terraformed_ground` (chunk stage ≥ 1) and `guide/living_world` (stage ≥ 3) directly when the player
  stands on terraformed / fully-living ground — replicating the standalone mod's `PlayerTrigger` **without**
  `ModCriteria`. **41 of 42 advancements now track real completion.** 26.x: award via
  `getOrStartProgress(holder).getRemainingCriteria()` → `PlayerAdvancements.award(holder, criterion)`.
+ [x] **Slice 2e — DONE via station founding.** `guide/station_charter` is now code-granted when a station is
  founded (the charter item), and its Star-Guide step + advancement icons point at the now-real `station_charter`
  item. **All 42 advancements track real completion.** Only the `new_life` guide-step icon stays substituted
  (Meadow Loper spawn egg) until `LOPER_HAUNCH` is ported — purely cosmetic.

### Pipes — advanced  (`pipe/` + items + payload + renderer; basic pipe already ported) — **slices A + B DONE (4 cells green); only the optional PipeNetwork graph remains**

+ [x] **Slice A — per-face configuration layer.** `pipe/PipeIoMode` + `pipe/PipeResourceType` (vanilla
  enums); `item/{ConfiguratorItem, PipeFilterItem (vanilla ItemStack filter), PipeUpgradeItem ×2}`.
  `UniversalPipeBlockEntity` extended with per-face×per-type modes (packed long) + per-face item filters +
  speed/capacity upgrades; the energy/gas/item relay honours `canPull`/`canPush`/`OFF` + filters + speed
  throughput; `UniversalPipeBlock` sneak-empty-hand pops upgrades. Items registered (TOOLS tab) + assets +
  20 lang keys.
+ [x] **Fluid relay** — added the `platform/FluidLookup` query seam (common + both loaders + services) and a
  `FluidTank` + `relayFluid()` to the pipe BE (honours the FLUID face-mode + speed); the pipe's fluid handler
  is exposed as the FLUID cap on both loaders. The pipe now carries all four layers; the FLUID face-mode is
  live (e.g. Refinery → pipe → Fuel Tank).
+ [x] **Slice B1 DONE — per-face config GUI.** A slot-less `PipeConfigMenu` (`menu/`) + `PipeConfigScreen`
  (`client/`, plain hull panel, no texture asset, SpaceButtons) let the player edit one resource layer at a
  time across all six faces: 7 synced data values ([0]=layer, [1..6]=each face's mode), a layer cycler +
  one cycler per face, all routed through `clickMenuButton` (no packet). `UniversalPipeBlockEntity` now
  implements `MenuProvider` (+ a transient `configType` + `configData` `ContainerData`); the **Configurator's
  sneak+right-click on a pipe opens it** via the vanilla `openMenu` path. **Cross-loader adaptation:** uses a
  server-authoritative menu instead of the standalone mod's client-`PipeConfigScreen` + `SetPipeModePayload`
  + `PipeConfigOpenHandler`, so **no client-screen-open seam is needed** (menus + their screens already
  register cross-loader). Menu type registered + screen registered on both loaders; reuses the existing
  `pipe.nerospace.mode.*` lang.
+ [x] **Slice B2 — pipe renderer DONE (travelling items + stream pulses; 4 cells green).** `pipe/TravellingItem`
  (rebuilt on a vanilla `ItemStack` — the root's `ItemResource` isn't on common) + `client/UniversalPipeRenderer`
  + `client/UniversalPipeRenderState`, registered via the `ClientBlockEntityRenderers` BER seam. **(a) Items**
  visibly slide entry-face → centre → exit-face: a **cosmetic echo** — the instant item relay is unchanged; each
  successful push spawns a transient `TravellingItem` the BE advances + expires + persists. **(b) Stream pulses**
  (red energy / blue fluid / cyan gas) pulse along each active arm — `renderStreams`/`crossQuads`/`quad` ported
  near-verbatim via `collector.order(1).submitCustomGeometry(..., RenderTypes.lightning(), ...)`, reading the
  `CONNECTIONS` blockstate + buffered amounts (`getEnergy/getFluidTank/getGas().getAmount()`) + per-face
  `mode()`. The BE update packet (`getUpdatePacket` + `getUpdateTag` = `saveCustomOnly`) is throttled on two
  cadences — fast (3t) while items are in flight, slower (10t) while the pipe just holds content — so streams
  stay current without per-tick spam; the renderer advances item progress locally between syncs. **Only the
  optional `PipeNetwork` 591-line routing graph remains deferred** (the per-BE relay already moves all four
  layers — pure refactor, low value). Renderer/BE-sync APIs all proven by the Star Guide hologram + solar deck.
+ [x] **Connected-pipe model DONE (4 cells green).** `UniversalPipeBlock` now carries the 6 vanilla boolean
  connection properties (`NORTH`..`DOWN` / `CONNECTIONS[]`) + per-mask `VoxelShape`s (core + arms) + a
  multipart blockstate (core + 6 rotated arms) + `universal_pipe_core`/`_arm` models (reuse the existing
  texture). `canConnect` uses the `EnergyLookup`/`FluidLookup`/`GasLookup` seams + vanilla `Container`
  adjacency (not NeoForge `Capabilities`). **Cross-version note:** connections are recomputed from the BE's
  server tick (throttled, `setBlock` on change) + `getStateForPlacement`, deliberately AVOIDING the
  `neighborChanged`/`updateShape` overrides (their 26.x signatures with `Orientation`/`ScheduledTickAccess`
  are version-fragile and no other multiloader block overrides them). Pipes now visually join neighbours
  with arms and have matching collision — and the travelling items flow through the arms. This **unblocked
  the stream layer** (it reads `CONNECTIONS`) — now DONE (see Slice B2 above).

### Machine modules / upgrades  (`module/` 3) — **DONE (4 cells green)**

+ [x] `ModuleType`, `UpgradeModuleItem` (4 items: speed / efficiency / fortune / silk-touch) + `MachineModules`
  (rebuilt on a `NonNullList` instead of the root's `MachineItemHandler`). **Re-enabled in the quarry**:
  module slots restored in the controller's combined `WorldlyContainer` view + `QuarryMenu`, and the
  speed / energy / Silk-Touch / Fortune multipliers now drive the dig (the quarry's earlier `×1.0`
  deferral is resolved). Assets + 4 lang keys copied.

### Solar — tiers/array/BER  (`machine/Solar*` + `client/SolarPanel*`) — **DONE (4 cells green)**

+ [x] **Tiers + array pooling DONE.** `SolarTier` (T1/T2/T3, config-scaled FE/buffer via `NerospaceConfig`)
  + `SolarArray` (flood-fill same-tier pooling, rebalanced each tick so a pipe on ANY panel drains the
  whole run) + tier-aware `SolarPanelBlock` (comparator output) + `SolarPanelBlockEntity` rebuilt on the
  multiloader `EnergyBuffer` (the NeoForge transfer `SimpleEnergyHandler` isn't ported). `solar_panel`
  stays Tier 1 (**non-breaking**) and `solar_panel_t2` / `solar_panel_t3` are added; the shared `SOLAR_PANEL`
  BE type is bound to all three, so the existing per-loader energy cap (`be.getEnergy()`) covers them with
  no per-loader change. Daylight uses vanilla `getSkyDarken()` (the NeoForge dimension clock /
  `getDayTime()` / `LevelData.getDayTime()` aren't on the de-obf classpath); airless dims get the 2× sun
  bonus via `ModDimensions` keys. Assets: tier textures copied from root + hand-authored block/item/loot JSON; 2 lang keys.
+ [x] **Slice 2 DONE — multiblock + sun-tracking BER.** `SolarPanelBlock` gained the `ANCHOR` property +
  N×N placement/teardown (T2 2×2, T3 3×3 — clicked min-corner is the anchor, fillers forward their energy
  to it via `SolarPanelBlockEntity.getEnergy()` → `anchorEntity()`); blockstates carry `anchor=true|false`.
  `client/SolarPanelRenderer` + `SolarPanelRenderState` draw the tilting sun-tracking deck (one big deck per
  multiblock, on the anchor) via the BER seam — ported from the root's submission-model geometry
  (`submitCustomGeometry` + `RenderTypes.entityCutout` + raw `VertexConsumer`), **compiles on both 26.1.2 and
  26.2**. Cross-loader adaptations: deck angle from vanilla `getGameTime()` (no NeoForge dimension clock),
  airless 2× via `SolarPanelBlockEntity.isAirless`. Dropped (minor): the per-face connector stubs (needed
  client-side energy-cap queries). The solar subsystem is now feature-complete.

### Creative storage variants  (`storage/Creative*`) — **DONE (4 cells green)**

+ [x] `AbstractStorageBlock` (shared base) + `CreativeFluidTank` (endless rocket_fuel), `CreativeGasTank`
  (endless oxygen), `CreativeItemStore` (right-click to set an endless item source). Fluid/gas mirror
  the ported `CreativeBattery`'s infinite pattern on the cross-loader storage interfaces; the item store
  exposes its endless source through a vanilla `Container` (no NeoForge `InfiniteResourceHandler`).
  Fluid/Gas/Item caps wired on both loaders; assets + lang copied.

### Utility items  (`item/`) — **partly DONE (4 cells green)**

+ [x] `NerospaceSpawnEggItem` (+ **9 spawn eggs**: xertz stalker, quartz crawler, greenling, alien
  villager, cinder stalker, frost strider, meadow loper, ember strutter, woolly drift — ruin warden is
  summon-only). Lazy `EntityType` supplier (vanilla `SpawnEggItem` binds too early); SPAWN_EGGS tab.
+ [x] `DestinationCompassItem` (×4: station/greenxertz/cindara/glacira) + `GreenxertzNavigatorItem` —
  creative-only travel devices; TOOLS_AND_UTILITIES tab. Assets + 17 lang keys copied.
+ [x] `ConfiguratorItem`, `PipeFilterItem`, `PipeUpgradeItem` — DONE (advanced-pipes slice A; TOOLS tab).
+ [ ] `StarGuideBookItem` (depends on **star guide**).
+ [x] **Artificer gear behaviour DONE (4 cells green).** `gear/XertzResonatorItem` (right-click ore-ping —
  reuses the new `c:ores` convention `TagKey` in `ModTags.Blocks.ORES` instead of NeoForge `Tags.Blocks.ORES`,
  registered in place of the plain item) + `gear/AlienGearAbilities` (shared `negatesFall` predicate — the
  cross-loader stand-in for the root's NeoForge `@EventBusSubscriber` `AlienGearEvents`). Grav Striders'
  fall-negate is bound per loader: NeoForge `LivingFallEvent.setDamageMultiplier(0)`, Fabric
  `ServerLivingEntityEvents.ALLOW_DAMAGE` vetoing a `DamageTypes.FALL` source (both stable on 26.1.2 + 26.2).
  The village system's T4/T5 gear trades are now functional.

### Cross-cutting registries  (`registry/`)

+ [x] `ModTags` — pure `TagKey` constants (block + item; c:material + nerospace oxygen/terraform tags),
  ported verbatim (no registration; tag membership is data).
+ [x] `ModDataComponents` — `SELECTED_PIPE_TYPE` (int) + `FILTER_ITEM` (vanilla `ItemStack` instead of the
  root's NeoForge `ItemResource`), via `RegistrationProvider` over `DATA_COMPONENT_TYPE`. Consumed by the
  advanced-pipe configurator/filter (advanced pipes batch).
+ [~] `ModCriteria` (`terraformed_ground`/`living_ground`/`founded_station` `PlayerTrigger`s) — **deferred:
  confirmed cross-version vanilla package move** (probed 2026-06-21): on **26.1.2** the classes are
  `net.minecraft.advancements.CriterionTrigger` + `net.minecraft.advancements.criterion.PlayerTrigger`; on
  **26.2** both are under `net.minecraft.advancements.triggers`. A single shared `import` can't satisfy both
  MC versions, so this can't be a plain common class. Options when its first consumer (station founding /
  star guide / terraform) lands: (a) drop the custom advancement triggers (they're cosmetic — the systems
  work without firing them); (b) reflection (resolve `PlayerTrigger` by per-version FQN); or (c) add
  version-split source sets. Orphan until then.
+ [ ] `ModAttachments` (data attachments — needs a cross-loader seam: NeoForge attachments vs Fabric
  component/attachment API), `ModFeatures`, `ModConfiguredFeatures`/`ModPlacedFeatures`/`ModBiomes`/
  `ModBiomeModifiers` (datagen bootstraps — mostly superseded by the copied JSON), `ModDimensionTypes`
  (space type — JSON already copied).
+ [x] `ModCreativeModeTabs` → ported as `ModCreativeTab`: a **dedicated "Nerospace" tab** registered via
  the cross-loader `RegistrationProvider` over the vanilla `CREATIVE_MODE_TAB` registry, listing all
  items (`ModItems.creativeContents()`). **Fixes a latent runtime bug**: the earlier per-loader injection
  into vanilla tabs (`BuildCreativeModeTabContentsEvent` / `CreativeModeTabEvents`) never populated the
  tabs in-game (items were searchable but absent when browsing) — replaced on both loaders. Note: vanilla
  `CreativeModeTab.builder(Row, column)` (the no-arg overload + `withTabsBefore` are NeoForge-only).

### Networking  (`network/` 5) — **SEAM DONE (4 cells green); payloads ship with their consumers**

+ [x] Cross-loader packet seam: common `network/ModNetwork` (payload registry: `clientbound`/`serverbound`
  lists + `sendToPlayer`/`sendToServer`) + `platform/NetworkPlatform` send seam. NeoForge `NeoForgeNetwork`
  registers via `RegisterPayloadHandlersEvent` (`playToClient`/`playToServer`) and sends via
  `PacketDistributor.sendToPlayer` / **`ClientPacketDistributor.sendToServer`** (client-only). Fabric
  `FabricNetwork` registers via **`PayloadTypeRegistry.clientboundPlay()/serverboundPlay()`** +
  `Server/ClientPlayNetworking` receivers and sends via `Server/ClientPlayNetworking.send`. Verified the
  exact 26.2 APIs with a temporary javap probe (removed). No payloads registered yet — `OxygenFieldSyncPayload`,
  `MeteorSyncPayload`, `SetPipeModePayload` ship with their subsystems (each just calls `ModNetwork.clientbound/
  serverbound(...)`). Client-safety contract documented in `ModNetwork`.

### Commands & compat

+ [x] `command/NerospaceCommands` — **DONE (4 cells green).** `/nerospace gallery` [clear] creative showcase
  builder, behind a cross-loader `register(CommandDispatcher)` seam (NeoForge `RegisterCommandsEvent`, Fabric
  `CommandRegistrationCallback`). Cross-loader/version adaptations: iterate `BuiltInRegistries.BLOCK` filtered
  to the mod namespace (the `RegistrationProvider` has no entry iteration); single `SOLAR_PANEL` (tiers
  unported); spawn the armor stands via the `ArmorStand` constructor (the de-obf `EntityType.ARMOR_STAND`
  constant isn't on the 26.2 classpath); dropped the unported `quarry.stageDisplay` preview + the Creative
  Fluid Tank `setSource` (fixed rocket_fuel here).
+ [~] **JEI — dependency UNBLOCKED + wired (2026-06-22; was wrongly marked "no 26.x artifact").** JEI **does**
  publish for both: `jei-26.1.2-*:29.6.2.31` and `jei-26.2-*:30.1.0.10` (BlameJared maven, already inherited).
  Wired in `neoforge/build.gradle` as per-MC-version `jei_version_<mc>` (compileOnly common+neoforge API,
  runtimeOnly the full NeoForge artifact — never published as a hard dep), guarded by `if (jeiVersion)`.
  Both `:neoforge:build` cells green (26.2 + 26.1.2); baseline JEI integration now works (mod items appear).
  **Remaining = the `compat/jei` PLUGIN itself** (`NerospaceJeiPlugin` + Grinding/Refining/CombustionFuel
  categories, NeoForge source set). Two real porting tasks, not blockers: (1) the multiloader inlines recipe
  data — `GrinderRecipes` is a bare `getResult(input)` with no recipe-list/record, and there's no `Tuning`
  class — so the categories need recipe-list exposure (a `Grinding` record + lists for grinder/refinery/
  combustion) and Tuning→inlined-constant adaptation; (2) **JEI jumped 29→30 between the two MC versions**, a
  major-API bump, so the categories must compile against both JEI APIs (cross-version risk on the JEI side,
  on top of MC). Fabric recipe viewer (REI/EMI) is a separate follow-up.

### Config / tuning — **DONE (4 cells green): all 5 multipliers wired, cross-loader seam complete**

+ [x] **Slice 1 — config seam + energy multiplier.** Extended the properties-based `config/NerospaceConfig`
  (no NeoForge `ModConfigSpec` — the cross-loader seam is the properties file the telemetry batch added) with
  `energyRateMultiplier` (clamp 0.1×..10×, default 1) + a `scale(base, mult)` helper (min-1 clamp, mirroring
  the root `Tuning` contract); wired it into the Combustion / Passive / Solar generator FE-per-tick. Loads at
  mod init (before ticking). This proves the cross-loader balance-config pattern beyond the telemetry toggle.
+ [x] **Slice 2 — oxygen multipliers.** Added `oxygenDrainMultiplier` + `oxygenCapacityMultiplier` to
  `NerospaceConfig`; wired into `OxygenManager` (per-check drain + player/suit air capacity, both `scale`-clamped;
  the attachment default self-corrects on the first tick). **3 of the root's 5 multipliers now wired.**
+ [x] **Slice 3 — fuelCostMultiplier.** Added `fuelCostMultiplier`; wired into `RocketTier.fuelPerLaunch()`
  (scaled, still clamped to the tank so a launch is always possible). **4 of the root's 5 multipliers wired.**
+ [x] **Slice 4 — machineSpeedMultiplier (last multiplier; config seam COMPLETE).** Added `machineSpeedMultiplier`
  + a `scaleInterval` helper (inverse, clamped ≥1 tick); wired into the grinder + refinery (progress thresholds,
  both the completion check and the synced max-progress data), the hydration module + terraformer (modulo work
  intervals), and the quarry (folded into the mining rate). **All 5 of the root's balance multipliers are now
  live cross-loader** (energyRate, oxygenDrain, oxygenCapacity, fuelCost, machineSpeed) via the properties
  `NerospaceConfig` — no NeoForge `ModConfigSpec`. Base values stay inlined per machine (a central `Tuning`
  class is optional).

### Spawn rules

+ [x] `registry/ModSpawnPlacements` — natural-spawn placement rules for the 9 spawnable creatures
  (6× `ON_GROUND` light-independent; 3× terraform livestock gated on `GRASS_BLOCK`). Cross-loader
  spawn-placement seam (`ModSpawnPlacements.Sink`): NeoForge `RegisterSpawnPlacementsEvent`
  (`Operation.REPLACE`) vs Fabric vanilla `SpawnPlacements.register`. Both stable on 26.1.2 + 26.2.
  Ruin Warden has no rule (structure/event boss only).

---

## 📡 Sentry / telemetry  (`telemetry/`) — **POPIA/GDPR-sensitive** — DONE (4 cells green)

+ [x] `telemetry/NerospaceTelemetry` — the Sentry client: captures Nerospace exceptions/crashes, with
  **PII scrubbing** (no IP/identity/hostname; OS-account names scrubbed from file paths via the `USER_PATH`
  regex incl. `C:\Users\<name>\...`), **nerospace-only `beforeSend` filter**, **de-dup + 10/session cap**.
  Parameterised off `Services.PLATFORM` (mod version, loader name, dist) instead of FML.
+ [x] `telemetry/SentryLogAppender` — Log4j2 appender selecting ERROR/FATAL events touching Nerospace code.
+ [x] `config/NerospaceConfig` — minimal properties config (`config/nerospace.properties`); **`telemetryEnabled`
  default ON, opt-out** (user decision 2026-06-21). Config-dir via new `IPlatformHelper.getConfigDir()` seam.
+ [x] **Sentry SDK bundled per-loader** — common `compileOnly`, NeoForge `jarJar`, Fabric Loom `include`
  (both bundling tasks ran green). `NerospaceTelemetry.init()` called at each loader's bootstrap; **only
  initialises in a production (non-dev) environment**.
+ [ ] **Deferred**: `SentryTestBlock` (debug block) — minor dev tool.
+ ⚠️ **Runtime-verify on a shipped build**: the 4-cell compile + the jarJar/include tasks are green, but
  Sentry initialisation, the nerospace-only filter, and path scrubbing have NOT been runtime-tested here
  (dev-gated + sandbox mount lag). Confirm on a production jar before relying on it. DSN = root's EU ingest.

---

## 🛠️ Tools / sync engines  (`tools/`) — currently target the **root** mod only

These are dev-time generators, not shipped code. They write to the root's `src/main/resources` paths, so
they must be pointed at (or duplicated for) `multiloader/common/src/main/resources` to drive the
multiloader's assets instead of the current copy-from-root approach.

+ [ ] `model_sync.py` — **entity-model sync engine** (Blockbench `.bbmodel` ⇄ Java `LayerDefinition`,
  Y-flip, mtime-directional). Wire to the multiloader's `client/*Model.java` + `art/blockbench/entity`.
+ [ ] `gen_textures.py` — procedural 16×16 texture generator (additive). Repoint output dir.
+ [ ] `gen_bbmodels.py` — Blockbench source generator for block/item textures. Repoint.
+ [ ] `gen_logo.py` — CurseForge logo + in-game mods-list icon. Repoint / re-emit per loader.
+ [ ] `check_assets.py` — "every model resolves" validator. Repoint at the multiloader resource roots.
+ [ ] `render_contact_sheets.py` / `render_entity_previews.py` — QA atlases. Repoint.
+ [x] `gradle-mcp` (server.js) — the agent build server; already used to verify all 4 cells.
+ [x] `fix_markdown.py` / `markdown_check` — docs linting; loader-agnostic.

> Note: so far the multiloader reuses the root's already-generated JSON/textures by copying them. The
> tools only need porting if the multiloader becomes the source of truth (i.e. when the root mod is retired).

---

## Recommended order

rockets → fuel machines → quarry → atmosphere/terraforming → structures → meteor events → star guide →
advanced pipes → modules → networking seam (unblocks oxygen HUD / meteors / pipe modes) → config seam →
spawn rules → telemetry (after compliance sign-off) → creative variants / utility items / JEI → tools repoint.
