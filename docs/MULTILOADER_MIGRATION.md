# Nerospace multiloader — migration status & remaining-work plan

Companion to `MULTILOADER.md` (which holds the architecture decision and toolchain
field notes). This file tracks **what content has been ported into `multiloader/common`
and how the remaining systems should be ported**, based on the concrete NeoForge ↔ Fabric
API divergences found during the port.

Last updated: 2026-06-20. Verified build targets: all four cells — **NeoForge @ 26.1.2 / 26.2**
and **Fabric @ 26.1.2 / 26.2** — `BUILD SUCCESSFUL` via the gradle MCP after every batch.

> **2026-06-20 progress update.** Every cross-loader **platform mechanism is now built and
> verified**: registration; the item / energy / fluid capability seams (both *expose* and
> *query*); block-entity tickers; and menus + screens. A working **energy network** exists
> (generators → pipe → machines/battery). Ported block entities (10): `item_store`, `battery`,
> `creative_battery`, `fluid_tank`, `trash_can` (item+fluid void), `combustion_generator`,
> `passive_generator`, `nerosium_grinder`, `universal_pipe`. Plus overworld nerosium-ore worldgen.
> All content the seams support **without** a deferred subsystem is now ported. What remains
> (below, §3b/§3c) is **subsystem work**, not per-block batches — each is a focused effort and
> several are runtime-verification-dependent (rendering / world / behavior can't be checked headlessly).
>
> **2026-06-20 (later): rocket-fuel FLUID ported** — all 4 cells green. A `FluidFactory` platform
> seam creates the still/flowing `Fluid`: NeoForge uses `BaseFlowingFluid` backed by a registered
> `FluidType`; Fabric uses a hand-written vanilla `FlowingFluid` subclass (`RocketFuelFluid`, override
> set mirrors `WaterFluid`). Common registers the fluids (`ModFluids` + the seam), the `LiquidBlock`
> (`RocketFuelLiquidBlock`, a public-ctor subclass to dodge the protected vanilla ctor cross-loader),
> and `rocket_fuel_bucket` (`BucketItem`). `ModRegistries` now runs fluids first (eager-Fabric order).
> In-world fluid rendering is wired on NeoForge (`RegisterFluidModelsEvent` + `FluidModel.Unbaked`, both
> 26.1.2 & 26.2). **Known follow-up:** the Fabric client fluid-render module (`fabric-api`
> `FluidRenderHandlerRegistry`) is not on the de-obf Loom classpath here, so on Fabric the liquid renders
> with the default texture in-world (bucket icon, tank storage, and all behaviour still work); revisit
> when that fabric-api module is available. This unblocks the refinery / fuel tank.
>
> **2026-06-20 (later still): GAS layer ported** — all 4 cells green. Self-contained cross-loader gas
> layer mirroring the energy/fluid seams: `GasResource` (plain vanilla enum, replacing the root's
> NeoForge-transfer `Resource`), `NerospaceGasStorage` + `GasTank`, and a `GasLookup` query seam
> (NeoForge `BlockCapability` `nerospace:gas` + Fabric `BlockApiLookup`). Ported blocks: `gas_tank`
> and a GUI-less `oxygen_generator` (grid-powered electrolyser: spends energy → synthesises oxygen into
> an extract-only gas port). The **universal pipe now relays gas as well as energy**, so the network runs
> end-to-end: generator → pipe → oxygen generator → pipe → gas tank. (The world oxygen-field effect +
> HUD + the generator GUI are a deferred atmosphere subsystem.)
>
> **2026-06-20 (later still): ALL 10 mobs ported** — all 4 cells green. On the entity seam below, added
> `cinder_stalker`, `frost_strider`, `ruin_warden`, the three terraform livestock (`meadow_loper`,
> `ember_strutter`, `woolly_drift` via a shared `TerraformLivestock` base), and the **alien villager**
> (full `Merchant` trading + per-player `Reputation` + gift loop + per-individual render tint/skin, with
> its own renderer). Ported the `village` trade package (`Reputation`, `AlienTrades`) and a plain
> `xertz_resonator` item (its gear behaviour deferred). The villager's per-dimension planet pick is
> temporarily fixed to Greenxertz until `ModDimensions` lands. Natural spawning + spawn eggs remain
> deferred (mobs are summonable; spawning waits on the planet dimensions/biomes).
>
> **2026-06-20 (later still): ENTITY seam + Greenxertz creatures ported** — all 4 cells green. New
> cross-loader seam: entity types via `RegistrationProvider` over `ENTITY_TYPE` (`EntityType.Builder…
> build(key)`); **attributes** via `ModEntityAttributes` applied per loader (NeoForge
> `EntityAttributeCreationEvent`, Fabric `FabricDefaultAttributeRegistry`); **renderers** via a common
> `ClientEntityRenderers` sink (NeoForge `RegisterRenderers`, Fabric `EntityRendererRegistry`). Models
> are baked directly (`createBodyLayer().bakeRoot()`), so **no model-layer registry** is needed on
> either loader (Fabric's `EntityModelLayerRegistry` isn't on the de-obf classpath). Ported: `xertz_stalker`,
> `quartz_crawler`, `greenling` (full vanilla AI) + their shared `GreenxertzMobModel`/renderer/glow-eyes
> layer, the three distinct geometry models, `ModSounds` (vanilla-aliased via `sounds.json`), and entity
> textures. Natural-spawn placement + spawn eggs are deferred (mobs are summonable; spawning waits on
> the planet dimensions). The remaining mobs (alien villager, ruin warden, cinder/frost striders,
> terraform livestock) follow this same seam.
>
> **2026-06-20 (later still): item relay added to the universal pipe** — all 4 cells green. The pipe is
> now a `WorldlyContainer` (3-slot buffer) and moves items by plain vanilla `Container` adjacency (no new
> seam — works with vanilla chests/furnaces and the mod's machines on both loaders): it pulls from
> non-pipe neighbours and pushes to any neighbour, so the single pipe carries **energy + gas + items**.
> Item cap exposed on both loaders (NeoForge `Capabilities.Item.BLOCK`, Fabric `ItemStorage.SIDED`).
>
> **2026-06-20 (later still): solar_panel ported** — all 4 cells green. Single-tier GUI-less daylight
> generator (`getSkyDarken()` + open-sky check → energy; halved in storms), exposes the energy
> capability (extract-only) so the pipe network drains it. Root's tiered sun-tracking array + BER are
> a deferred enhancement.
>
> Remaining, by subsystem (rough size): **dimensions** (Greenxertz/Cindara/Glacira biomes+dims+travel;
> unblocks the planet ores' worldgen, mob natural-spawning, and the villager's per-planet variant);
> **rockets** (items, tiers, launch logic); **quarry** (area mining); **structures**
> (station/village/meteor cores + events); **atmosphere/terraforming** (oxygen field, terraformer,
> monitor, hydration); **solar panel tiers/array/BER** (single-tier base is done); **star guide**
> (progression UI); **creative item/fluid/gas stores** (infinite-resource config — marginal); plus mob
> **spawn eggs + natural-spawn rules** (deferred with dimensions). **All 10 mobs are otherwise ported.**
> Recommended order: dimensions → rockets → quarry → structures → atmosphere → the rest.

---

## 1. What is ported

All of the following lives once in `multiloader/common` and drives both loaders through the
`RegistrationProvider` seam (`common/registry/RegistrationProvider.java`) + the per-loader
`RegistrationFactory` services. Creative-tab placement is defined once in
`ModItems.creativeTabItems()` and applied by each loader entry point.

| Area | Count | Notes |
|------|-------|-------|
| Blocks | 20 | ores, storage blocks, station + alien decorative, meteor rock |
| Items (non-block) | 32 | materials, `nerosium_pickaxe`, 16 oxygen-suit armor pieces, alien/utility items |
| Block items | 20 | one per block |
| Loot tables | 20 | ore drops (silk/fortune), block self-drops |
| Recipes | 30 | crafting (tag-based) + smelting/blasting for nerosium & nerosteel |
| Tags | block: `mineable/pickaxe`, `needs_iron_tool`; item: `c:` ores/ingots/gems/raw/storage | |
| Lang | 52 keys | |
| Equipment (worn armor) | 4 defs + textures | oxygen suit base/T2/heat/cold |

**The pattern that worked (keep using it for all data-only content):** the root project's
datagen has already emitted every asset/loot/recipe/tag/lang JSON under
`src/generated/resources`. Migration = (a) write the registration in `common` via
`RegistrationProvider`, (b) copy the matching generated JSON + the committed textures into
`multiloader/common/src/main/resources`, (c) build both loaders. No multiloader datagen is
needed yet — the root is the source of truth for the JSON.

---

## 2. Confirmed cross-loader facts (so they aren't re-discovered)

- **26.x is de-obfuscated** → Fabric Loom uses **no `mappings`**; NeoForge uses ModDevGradle/NeoForm.
- **`ResourceLocation` is `net.minecraft.resources.Identifier`** in 26.x; **`ResourceKey.identifier()`** (not `.location()`).
- Item-model definitions live in **`assets/<ns>/items/<id>.json`** (1.21.4+ format), not `models/item`.
- Loot/recipe/tag dirs are **singular**: `loot_table/`, `recipe/`, `tags/block`, `tags/item`.
- **`Item.Properties.pickaxe(...)` / `humanoidArmor(...)` are vanilla** (present on the Fabric classpath) — tools/armor compile in `common`.
- **Fabric API is consumed with plain `implementation`** (the umbrella + modules resolve transitively; `modImplementation` is *not* registered in Loom's de-obf mode). Creative-tab API is `net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(...)`.
- **NeoForge 26.2 userdev is not on Maven** → self-build to `mavenLocal` (already working on the dev machine).
- Editing an *existing* resource file can read stale through the agent's mount cache; the **dev-side Gradle output is the source of truth** (confirmed via a dev-side read).

---

## 3. Remaining work

### 3a. Data-only — same fast path as above (low risk, no platform code)

These are vanilla JSON the root already generated; port the same way (copy generated JSON,
add any trivial registration). They make existing content *function in the world*:

- **Worldgen** (`ModFeatures`, `world/` 18 files): `configured_feature` + `placed_feature`
  for the ores are vanilla JSON (common). **Biome injection differs**: NeoForge = a
  `biome_modifier` JSON (data, common-ish); Fabric = `BiomeModifications` API (code, fabric-api).
  → put the feature JSON in common; add a tiny per-loader biome hook. This is what makes the
  migrated ores actually spawn (today they're craftable/creative only).
- **Sounds** (`ModSounds`): `sounds.json` + `.ogg` assets — common.
- **Advancements / criteria triggers** (`ModCriteria`): JSON advancements are common; custom
  trigger *types* need registration (mostly common).
- **Data components** (`ModDataComponents`, 3): vanilla `DataComponentType` registry — common
  via `RegistrationProvider` over `Registries.DATA_COMPONENT_TYPE`.
- Remaining **recipes/loot/tags** for already-migrated content.

### 3b. Loader-divergent systems — need the platform seam (`Services`)

Ordered by how much they unblock. Each needs a common interface + two loader impls; none can
be runtime-verified in this environment (compile-verify only — flag rendering/behavior for a
real client test).

1. **Fluids** (`fluid/`, `ModFluids`) — *smallest divergent unit; good seam pilot.*
   - NeoForge: `FluidType` (no Fabric analog) + `BaseFlowingFluid.Source/.Flowing` +
     `BaseFlowingFluid.Properties` linking type/still/flowing/bucket/block.
   - Fabric: extend vanilla `FlowableFluid` directly (own `Source`/`Flowing` like `WaterFluid`),
     register render handler via fabric-api (`FluidRenderHandlerRegistry`) + `FluidVariantAttributes`.
   - Seam: `IFluidPlatform { Fluid rocketFuelStill(); Fluid rocketFuelFlowing(); }` registered
     per loader; the `LiquidBlock` + `BucketItem` (vanilla) stay in common, built from a supplier
     of the still fluid. Watch registration order (fluid before block/bucket).

2. **Block entities + menus** (`ModBlockEntities` 27, `ModMenuTypes` 13, `machine/` 43,
   `storage/` 19, `solar/`, `pipe/`, `rocket/`).
   - `BlockEntityType` / `MenuType` registration is vanilla → `RegistrationProvider` over the
     respective registries (common).
   - Block-entity *ticking* and menu *opening* differ slightly (NeoForge `menuProvider`/
     `openMenu` vs Fabric `ExtendedScreenHandlerFactory`); screens are **client-only** and
     registered differently (NeoForge `RegisterMenuScreensEvent` vs Fabric `MenuScreens`/
     `HandledScreens`).

3. **Capabilities / storage** — *the biggest divergence* (`ModCapabilities` 37, `gas/`, `module/`).
   - NeoForge: `Capabilities.{ItemHandler,FluidHandler,EnergyStorage}` + `RegisterCapabilitiesEvent`.
   - Fabric: `team.reborn.energy` / Fabric Transfer API (`Storage<ItemVariant>`, `Storage<FluidVariant>`)
     + `BlockApiLookup` registration.
   - Seam: a common storage abstraction (`IPlatformEnergy/IItemStore/IFluidStore`) exposed by
     block entities; each loader adapts it to its capability/lookup system. This is the design
     that should be settled **before** porting the machines en masse — it shapes every machine.

4. **Networking** (`network/` 5): NeoForge payload registration (`RegisterPayloadHandlersEvent`,
   `CustomPacketPayload`) vs Fabric `PayloadTypeRegistry` + `ClientPlayNetworking`/`ServerPlayNetworking`.
   Seam: common payload records + a `INetworkPlatform.sendToServer/sendToClient`.

5. **Entities** (`ModEntities` 13, `entity/` 12, `village/`): `EntityType` registration is vanilla
   (common); **attributes** (NeoForge `EntityAttributeCreationEvent` vs Fabric
   `FabricDefaultAttributeRegistry`) and **renderers/models** (client) are per-loader. The
   `model_sync` tooling is root-only — multiloader entity models would be authored fresh or shared.

6. **Attachments** (`ModAttachments` 5): NeoForge `AttachmentType` vs Fabric
   (`fabric-data-attachment-api-v1`, already on the classpath). Seam over `Services`.

7. **Client rendering** (`client/` 52): BERs, entity renderers, screens, HUD, model layers,
   item properties — all client-only and loader-divergent (NeoForge client events vs Fabric
   `ClientModInitializer` + registries). Port alongside each system's server side.

8. **Dimensions** (`ModDimensions` 4, `ModDimensionTypes`): dimension/dimension_type JSON is
   common; the dimension *travel* code and any custom chunk generators are code (mostly common,
   some per-loader hooks).

### 3c. Out of scope / defer
- `datagen/` (9) — root-only; not needed unless multiloader grows its own datagen.
- `gametest/`, `telemetry/`, `compat/`, `command/` — port last; `compat` (JEI etc.) is per-mod.

---

## 4. Recommended order

1. **Worldgen ore features (3a)** — cheap, makes migrated ores spawn; introduces the one small
   per-loader biome hook.
2. **Capability/storage seam design (3b-3)** — settle the abstraction first; it gates machines.
3. **Fluids (3b-1)** — pilot the divergent-registration seam end to end.
4. **One machine vertical slice** — a single block entity + menu + screen + storage on both
   loaders, to prove 3b-2/3/7 together before bulk-porting `machine/`.
5. Bulk machines/storage → networking → entities → dimensions/rockets → client polish.

## 5. Standing constraints
- **No runtime verification here** — every batch is compile-verified on both loaders via the
  gradle MCP; rendering/behavior/worldgen need a real client/world test on the dev machine.
- **`.vscode` JSON + re-edited resources** can read stale through the mount; trust dev-side Gradle.
- **Commit/push stays manual** (per project rules).
