# Multiloader & multi-version support for Nerospace

Nerospace ships from one source tree to **nine** build cells:

| Loader | MC 26.1.2 | MC 26.2 | MC 26.3 |
| --- | --- | --- | --- |
| NeoForge | `:neoforge:26.1.2` | `:neoforge:26.2` | `:neoforge:26.3` |
| MinecraftForge / Forge | `:forge:26.1.2` | `:forge:26.2` | `:forge:26.3` |
| Fabric | `:fabric:26.1.2` | `:fabric:26.2` | `:fabric:26.3` |

The build is flattened to the repo root. `common/` is shared vanilla-style code, and each loader subtree
contains only loader-specific bootstrapping, manifests, services, capabilities/lookups, events, networking,
client setup, and build wiring.

## Toolchains

- `common/` compiles against NeoForm/de-obfuscated vanilla through ModDevGradle.
- `neoforge/` uses ModDevGradle and NeoForge userdev.
- `forge/` uses ForgeGradle 7 and Forge userdev.
- `fabric/` uses Fabric Loom with de-obfuscated 26.x mappings; no extra mappings dependency is declared.
- Stonecutter owns the MC-version axis. Every loader x MC version is a real Gradle project.

Current public artifact pins live in `gradle.properties`:

| Key family | Purpose |
| --- | --- |
| `neo_form_version_<mc>` | de-obfuscated vanilla base for `common` |
| `neo_version_<mc>` | NeoForge userdev |
| `forge_version_<mc>` | Forge userdev |
| `fabric_api_version_<mc>` | Fabric API |
| `jei_version_<mc>` | JEI common/API compat pins where published |

Forge is **not blocked** anymore. The supported Forge pins are:

- MC 26.1.2: `forge_version_26.1.2=26.1.2-64.0.10`
- MC 26.2: `forge_version_26.2=26.2-65.0.0`

## Architecture

`common/` must remain free of loader APIs. Shared code reaches loader behavior through Java
`ServiceLoader` seams in `common/.../platform/Services.java`:

- `IPlatformHelper`
- `EnergyLookup`
- `FluidLookup`
- `FluidFactory`
- `GasLookup`
- `NetworkPlatform`
- `registry.RegistrationProvider$Factory`

Each loader subtree ships exactly one implementation for each seam plus the matching
`META-INF/services` entries.

Registration is shared through `RegistrationProvider`. Fabric registers eagerly; NeoForge and Forge wrap
loader-native `DeferredRegister`s. Menus are vanilla/non-extended. Machine inventories use vanilla
`WorldlyContainer`; energy, fluid, and gas are shared Nerospace storage interfaces exposed through
loader-native lookups/capabilities.

### Loader-specific notes

- NeoForge uses `BlockCapability`, `AttachmentType`, `RegisterPayloadHandlersEvent`, `RegisterGuiLayersEvent`,
  and `RegisterFluidModelsEvent`.
- Forge uses classic `CapabilityManager`/`AttachCapabilitiesEvent` providers, `ChannelBuilder` payload
  channels, `AddGuiOverlayLayersEvent`, and `ModelEvent.BakeFluidModels`.
- Fabric uses Fabric callbacks, API lookups, data attachments, payload registries, and HUD element
  registration.

The rocket launch pad has no block entity. NeoForge and Fabric expose block-level item/fluid automation
directly. Forge keeps Nerospace pipe fluid refueling through the `FluidLookup` special case; broader
Forge-standard item automation for that non-BE block should be runtime-tested before advertising parity.

### Planet clock, weather and sky hooks (per-loader call sites)

The planet work has one common entry point per concern; each loader calls it from its own hook. When you add
a loader or move a hook, update all three.

| Concern | Common entry point | Fabric | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| Planet clock + weather tick + client sync | `world.planet.PlanetTicks.serverTick(server)` | `NerospaceFabric`: `ServerTickEvents.END_SERVER_TICK` | `NerospaceNeoForge`: `ServerTickEvent.Post` | `NerospaceForge`: `TickEvent.ServerTickEvent.Post` |
| Sky profile loading | `client.sky.SkyProfiles.INSTANCE` (reload listener) | `NerospaceFabricClient`: `ResourceLoader.get(CLIENT_RESOURCES).registerReloadListener` | `NeoForgeSky`: `AddClientReloadListenersEvent` | `ForgeClientSetup`: `RegisterClientReloadListenersEvent` |
| Sky drawing | `client.sky.PlanetSkyRenderer.renderCurrent(level)` | none yet (no Fabric API sky hook on 26.x) | `NeoForgeSky`: `CustomSkyboxRenderer` `nerospace:planet` (26.3 only) | none yet |

Sky colours are carried by the dimension types themselves, so a cell without a sky hook still shows the
planet's colours; the renderer only adds bodies, auroras and the like on top.

### Chunk render layers (glass, ice, crystals, windows)

There is no per-block render-layer registration in this mod, on any loader, and none is needed. On every
26.x line (26.1.2, 26.2, 26.3) vanilla picks each quad's chunk section layer from its texture's transparency
(`ChunkSectionLayer.byTransparency`); the old `ItemBlockRenderTypes` table no longer exists. NeoForge, Forge and
Fabric all follow it, so one texture rule covers all nine cells:

| Texture alpha | Layer | Use for |
| --- | --- | --- |
| every pixel 255 | SOLID | ordinary blocks |
| some pixels 0, none in 1–254 | CUTOUT | holes: panes, plants, the Cheese Wheel's cut faces |
| any pixel in 1–254 | TRANSLUCENT | tinted glass and ice: Crater Glass, Dry Ice (later Hull Window, Magma Glass, Void Crystal, Frost Crystal) |

A see-through block also needs the right block properties, or neighbours' faces are culled and you see the void:

- call `noOcclusion()` in its properties;
- extend `TransparentBlock` (glass-like) or `HalfTransparentBlock` (ice-like) so faces between two of the same
  block are skipped, as `LightLensBlock` (Crater Glass) and `DryIceBlock` do;
- no `render_type` in the model JSON (it is not read on Fabric and is redundant elsewhere).

`ChunkLayerTexturesTest` pins the intended layer of each see-through block by reading its model's textures. Add a
row there for every new glass, ice, crystal or window block, so a repainted texture cannot change the layer
unnoticed.

### MC-version branches in common code

Common code avoids Stonecutter `//? if` wherever a version-neutral API exists. Most existing branches are
renderer, worldgen and item API changes at 26.3 (`grep -rn "//? if" common/src/main/java`). Branches in the
planet-block framework (`block/`) and planet packages:

| File | Branch | Why |
| --- | --- | --- |
| `block/GeyserBlock` | `>=26.3` | `Entity.hurtMarked` was renamed `needsSync` (pushes the launch velocity to the client) |
| `world/planet/cindara/EmberStrike` | `>=26.2` | `EntityType.LIGHTNING_BOLT` moved to `EntityTypes` |
| `world/planet/greenxertz/block/BioBarkLogBlock` | `>=26.3` | `SoundEvents.AXE_STRIP` is a `Holder.Reference` (`.value()`) |
| `world/planet/greenxertz/block/GlowcapFungusBlock` | `>=26.3` | the `BonemealableBlock` methods take a trailing `BonemealSource` |
| `world/planet/greenxertz/world/GiantGlowcapFeature`, `GreenxertzContent.GIANT_GLOWCAP_FEATURE` | `<26.3` / `>=26.3` | the feature API split, as `RuinFeature` (`FEATURE_TYPES` codec vs `FEATURES` instance) |

Version-neutral by design: the Nyx Stone light mixins (`mixin/LightEngineNyxMixin`, `mixin/BlockLightEngineNyxMixin`)
target methods and the `newToLevel` local that are identical on 26.1.2, 26.2 and 26.3, and block classes do not
override `codec()` with `@Override` (26.3 removed it from `Block`).

### 26.3 heightmap tag rule

From 26.3 the `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR` and `OCEAN_FLOOR_WG` heightmaps no
longer ask `BlockState.blocksMotion()`; they count only blocks in `#minecraft:blocks_motion_in_heightmap(_no_leaves)`,
which resolve to the fixed vanilla list `#minecraft:blocks_motion_no_leaves`. A modded block missing from that tag is
see-through to those heightmaps on 26.3: rain and snow fall through it, mob spawn heights are wrong, and features
placed relative to `OCEAN_FLOOR_WG` (ores) silently never generate. 26.1.2 and 26.2 have no such tag.

Rule: **every new Nerospace block that has collision goes into
`common/src/main/resources-26.3/data/minecraft/tags/block/blocks_motion_no_leaves.json`** (`replace: false`; the
overlay is 26.3-only). Follow vanilla 26.3's own list: solid cubes, glass, panes, fences, chains, lanterns, pressure
plates, slabs and crystal clusters are in; fluids, plants, snow layers and carpets are out.
`HeightmapTagCoverageTest` enforces it: it discovers block ids from `assets/nerospace/blockstates/*.json` plus the
string ids passed to `BLOCKS.register(...)`/`block(...)` and fails on any id that is neither tagged nor in its
commented `EXCLUDED` map (currently `rocket_fuel`, a fluid, and `ash_layer`, a snow-like layer). It also fails on
stale or duplicate tag entries and if the tag file ever appears in the shared `resources/`.

## Build

Run from the repo root:

```bash
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build \
  :forge:26.1.2:build :forge:26.2:build \
  :fabric:26.1.2:build :fabric:26.2:build
```

Jars land in `<loader>/versions/<mc>/build/libs/`.

CI builds the same nine-cell matrix in `.github/workflows/multiloader.yml`. Publishing builds all nine cells
and tags the release for `fabric`, `neoforge`, `forge`, `26.1.2`, `26.2`, and `26.3`.

## IDE

Open `nerospace.code-workspace`, run `./gradlew eclipse` after dependency changes, then reload VS Code.

- Run: `ML: Run <loader> Client - <mc>`
- Debug: `Debug: <loader> <mc>` attaches to JDWP `:5005`
- Fabric debug uses `--debug-jvm`
- NeoForge and Forge debug use `-PnerospaceDebug`

## Runtime checks

Headless builds do not prove client visuals or loader integration behavior. Before release, run at least one
client per loader family and verify:

- main menu/mod metadata loads with the logo
- creative tab populates
- rocket fuel renders as a fluid
- oxygen/hazard HUD draws and does not double-render the vanilla air row where suppression exists
- Universal Pipe moves energy, fluid, gas, and items
- player oxygen and Star Guide seen data persist across relog/death
- chunk terraform data persists after save/reload
- Forge rocket-pad refueling works through Nerospace pipes; check standard Forge item automation separately

## Historical notes

Architectury/architectury-loom still was not suitable when the 26.x port began because it could not consume
de-obfuscated 26.x mappings. Nerospace therefore uses the MultiLoader-Template-style native-toolchain
layout with explicit ServiceLoader seams instead of Architectury APIs.

Classic Forge was previously documented as blocked because no compatible 26.2 Forge userdev was available
and older ForgeGradle paths rejected the repo's Gradle version. That note is obsolete: ForgeGradle 7 works
with this repo's Gradle 9.6.0 wrapper, and Forge 26.1.2/26.2 artifacts are now published.
