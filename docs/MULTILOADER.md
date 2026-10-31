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

Forge jars: players get the jarJar `-all` jar (Sentry nested in `META-INF/jarjar/`). The plain jar is the Maven
artifact for sibling mods and carries no JarJar metadata; `forge/build.gradle` strips it from `jar` and adds it to
`jarJar` directly. In the dev run Sentry comes only from the `implementation 'io.sentry:sentry'` library, so Core
must resolve to a plain jar without JarJar metadata too (Core 1.17.0+, or a 1.16.0 rebuilt locally with the same `forge/build.gradle` change). A plain jar that
names a nested jar it does not contain stops every Forge dev run with "io.sentry ... no jar was provided".

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
| Planet sleeping: bed gate (after vanilla has set the respawn point) | `world.planet.PlanetSleep.refusal(player, pos)` / `allowsTime(player, pos)` | `NerospaceFabric`: `EntitySleepEvents.ALLOW_SLEEPING` (fired from `Player.startSleepInBed`, after `ServerPlayer`'s checks) | `NerospaceNeoForge`: `CanPlayerSleepEvent` (only when vanilla found no problem, or only monsters) | `NerospaceForge`: `SleepingTimeCheckEvent` → `Result.DENY` (also asked every tick while asleep; the action-bar line is sent only on the click) |
| Planet sleeping: end-of-night skip / rest wake-up, before any level ticks | `world.planet.PlanetSleep.serverTickStart(server)` | `NerospaceFabric`: `ServerTickEvents.START_SERVER_TICK` | `NerospaceNeoForge`: `ServerTickEvent.Pre` | `NerospaceForge`: `TickEvent.ServerTickEvent.Pre` |
| Sky profile loading | `client.sky.SkyProfiles.INSTANCE` (reload listener) | `NerospaceFabricClient`: `ResourceLoader.get(CLIENT_RESOURCES).registerReloadListener` | `NeoForgeSky`: `AddClientReloadListenersEvent` | `ForgeClientSetup`: `RegisterClientReloadListenersEvent` |
| Sky drawing | `client.sky.PlanetSkyRenderer.frame` + `scene` (pure `SkyScene` maths) | none yet (no Fabric API sky hook on 26.x) | `NeoForgePlanetSky` (`CustomSkyboxRenderer` `nerospace:planet`, 26.3 only) | none yet |
| Weather smoothing for sky and particles | `client.ClientPlanetWeather.clientTick(level)` | via `WeatherAmbience.tick()` | via `WeatherAmbience.tick()` | via `WeatherAmbience.tick()` |
| World fog colour (sky profile fog by phase, position, weather, eclipse) | `client.sky.PlanetSkyRenderer.fogRgb(camera, partialTick)` | `fabric/.../mixin/FogColorPlanetMixin` (client, `@Pseudo`, `require = 0`, TAIL of `FogRenderer.computeFogColor`; no Fabric API fog event on 26.x) | `NeoForgeClientSetup`: `ViewportEvent.ComputeFogColor` | `ForgeClientSetup`: `ViewportEvent.ComputeFogColor.BUS` |
| Ambient particle providers | `client.particle.ClientParticles.registerAll(sink)` | `NerospaceFabricClient`: `ParticleProviderRegistry` | `NeoForgeClientSetup`: `RegisterParticleProvidersEvent` | `ForgeClientSetup`: `RegisterParticleProvidersEvent` |
| Ambient particles + weather loops | `client.WeatherAmbience.tick()` | `NerospaceFabricClient`: `END_CLIENT_TICK` | `NeoForgeClientSetup`: `ClientTickEvent.Post` | `ForgeClientSetup`: `TickEvent.ClientTickEvent.Post` |
| HUD: oxygen gauge, status lines, clock dial | `client.OxygenHud.render(g)` (draws `client.ClockHud` too; geometry in `client.ui.HudLayout`) | `NerospaceFabricClient`: `HudElementRegistry` | `NeoForgeClientSetup`: `RegisterGuiLayersEvent` | `ForgeClientSetup`: `AddGuiOverlayLayersEvent` |

Planet sleeping keeps vanilla time frozen on every planet with its own clock. Each such dimension type declares
`minecraft:gameplay/bed_rule {can_set_spawn: always, can_sleep: always}` (no `error_message`: Forge's time check
would print vanilla's line over the planet's own), so vanilla never refuses for the time of day and never kicks a
sleeper out; `PlanetSleep` is the only "when" rule (`PlanetSleepRules`, pure and unit-tested). The skip runs at the
start of the server tick, so it always sees a full night of sleepers one tick before vanilla's own check inside
`ServerLevel.tick` and wakes them first: vanilla never skips a planet night (on 1.21.x that would move the shared
Overworld day time). The version seams are in `compat.mc.SleepCompat`: the `players_sleeping_percentage` game rule
(registry-backed `gamerules.GameRules` from 1.21.11, `GameRules.Key` before) and the bed refusal (a record carrying its
message from 1.21.11, an enum before, where the line is sent separately). 1.21.1-1.21.10 dimension types have no
`bed_rule` attribute: they need `natural: true` / `bed_works: true` instead, and `Level.isDay()` is false on a
fixed-time world there, so vanilla lets the attempt through to the planet gate.

Sky colours are carried by the dimension types themselves (`minecraft:visual/*` attributes: sky, fog, cloud and
sunrise colours, a fixed sun/moon angle, star brightness), so a cell without a sky hook still shows the planet's
palette. On NeoForge 26.3 every Nerospace dimension type also sets `neoforge:custom_skybox: nerospace:planet`, which
hands the sky to `NeoForgePlanetSky`: gradient by clock phase, stars, galaxy band, aurora, haze, suns, moons and
planets from `assets/nerospace/sky/<planet>.json`. A level without a profile (or with `vanillaSkybox: true`) gets the
vanilla sky.

Attribute formats (all three lines): `cloud_color` and `sunrise_sunset_color` are ARGB (`#AARRGGBB`), the other
colours RGB (`#RRGGBB`); `moon_phase` is a name such as `full_moon`; `fog_start_distance` / `fog_end_distance` are in
blocks (leave them out unless you mean a fixed fog wall). `SkyAssetsTest` checks these.

Ambient particles have one source on every loader: `client.WeatherAmbience`, which honours `particles.enabled`,
the per-type rates, `weather.particleBudget` and the open-sky rule. The planet biomes deliberately carry no
`minecraft:visual/ambient_particles`: vanilla would roll those around the player every tick with no budget, indoors
and underground too, and no config could turn them off. As a second guard, the particle providers create nothing
while `particles.enabled=false`. The sky's orbiting bodies read the continuous planet day count
(`ClientPlanetClock.days`), not the wrapped phase, so a body whose period is not one day never jumps at sunrise;
Nyxara's band width comes from the world's bound value carried on the clock payload, with the config as fallback.

### What each cell shows in the sky

| Cell | Sky | World fog colour |
| --- | --- | --- |
| NeoForge 26.3 | the full planet sky (`NeoForgePlanetSky`): gradient, natural-colour stars with twinkle, galaxy, aurora curtains, decals (halo, corona, diamond ring, flare rays, refraction bow, nebulae, limb sunrise), bodies with limb glow, night shade and city lights, cloud deck with underglow, shooting stars, haze | the profile fog via `ViewportEvent.ComputeFogColor`; the sky disc fades into the same colour |
| NeoForge 26.1.2, 26.2 | the colour floor: the dimension type's `minecraft:visual/*` attributes and vanilla sun/moon at the fixed angles | the profile fog via `ViewportEvent.ComputeFogColor` |
| Forge 26.1.2, 26.2, 26.3 | the colour floor | the profile fog via `ViewportEvent.ComputeFogColor.BUS` |
| Fabric 26.1.2, 26.2, 26.3 | the colour floor | the profile fog via `FogColorPlanetMixin`; if a line moves `computeFogColor` the injection is skipped and the attribute fog stays |

On every floor cell the bodies, aurora, eclipse, galaxy, decals and shooting stars do not exist; the planet's identity
is its colours. The fog push leaves water, lava and powder-snow fog and blindness / darkness / night vision alone. Weather on the sky
(tints, fades, aurora gates, cloud opacity) and the ambient particles read `ClientPlanetWeather.smoothedIntensity`
(0.6 s ease, ticked from `WeatherAmbience.tick`), so a forced or cleared event never pops. The client switches
`sky.aurora`, `sky.shootingStars` and `sky.quality` (`full` / `lite`) only affect the 26.3 renderer.

The sky path allocates nothing per frame. `PlanetSkyRenderer` keeps one `SkyScene` (recomputed in place from pooled
quads and scratch arrays, reused when the fog hook and the sky pass ask for the same instant and camera position), one
`Inputs` and the last `Frame`; profile colour strings are parsed once. The 26.3 renderer fills one scratch matrix and
colour per draw for `writeTransform`, keys its meshes in fastutil primitive maps, and rebuilds the aurora curtains from
the fold model into persistent direct byte buffers uploaded with `CommandEncoder.writeToBuffer` into buffers created
once with `GpuBuffer.USAGE_VERTEX | USAGE_COPY_DST` (the writes happen before the render pass opens; Blaze3D refuses
buffer writes inside one), skipping the upload while the aurora clock has not moved; everything else is static meshes
built once. All of them are closed on a sky-profile reload (`SkyProfiles.generation()`) and when the sky disables itself
after a render error, and rebuilt lazily. A body's night shade is one frame of a 16-frame phase sheet
(`<texture>_00 … _15`, from `tools/gen_sky_textures.py`), chosen by the lit fraction and drawn centred on the body; a
resource pack that replaces the shade texture supplies its own sheet beside it. `SkyProfile` has more top-level fields than one
`RecordCodecBuilder` group allows (16), so its codec joins two map codecs with `Codec.mapPair`; the `Aurora` and `Decal`
records are at 16 fields, so a new field must go into a nested record (`Fold`, `Drift`, `Visibility`).

### Loader-only resource overlays (NeoForge 26.3 skybox)

`neoforge:custom_skybox` exists only on NeoForge, and Fabric and Forge reject a dimension type that names an unknown
attribute, so the selection cannot live in the shared resources. `stonecutter.gradle` therefore also stages
`<loader>/src/main/resources-<mc>` overlays (after the shared `common/src/main/resources-<mc>` ones of the same
version, so the loader copy wins). The NeoForge 26.3 dimension types live in
`neoforge/src/main/resources-26.3/data/nerospace/dimension_type/` and are generated: **after editing a dimension
type, run `python tools/gen_sky_overlays.py`** (`--check` verifies). `SkyAssetsTest` fails on a missing or stale
overlay and on the attribute ever appearing in the shared files.

### Chunk render layers (glass, ice, crystals, windows)

There is no per-block render-layer registration in this mod, on any loader, and none is needed. On every
26.x line (26.1.2, 26.2, 26.3) vanilla picks each quad's chunk section layer from its texture's transparency
(`ChunkSectionLayer.byTransparency`); the old `ItemBlockRenderTypes` table no longer exists. NeoForge, Forge and
Fabric all follow it, so one texture rule covers all nine cells:

| Texture alpha | Layer | Use for |
| --- | --- | --- |
| every pixel 255 | SOLID | ordinary blocks |
| some pixels 0, none in 1–254 | CUTOUT | holes: panes, plants, a sliced block's cut faces |
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
| `world/planet/nyxara/NyxaraContent` | `>=26.3` | `PushReaction.DESTROY` was renamed `POPPED` (Shard Drift, Budding Nyx Crystal and the clusters break when pushed) |
| `world/planet/mars/entity/DustDevil` | `<26.3` / `>=26.3` | client interpolation: a hand-held `InterpolationHandler` ticked in `tick()` before 26.3, the `createInterpolationHandler()` override (`LinearInterpolationHandler`) from 26.3; `hurtMarked` was renamed `needsSync` for the pushed item velocity |

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

### 26.3 biome format

26.3 moved a biome's mob spawns out of the top-level `spawners` / `spawn_costs` fields into the
`minecraft:gameplay/natural_mob_spawns` environment attribute (`{"argument": {"spawn_costs": {...},
"spawns_by_category": {"<category>": [{"type", "count", "weight"}]}}, "modifier": "overlay"}`, where `count` is an
int or an int provider such as `minecraft:uniform`). 26.3 ignores the old fields without an error, so a biome written
for 26.1.2 / 26.2 loads on 26.3 but spawns nothing. Every other biome field Nerospace uses (`attributes` sky/fog
colours, `effects` water/grass/foliage colours, `carvers`, `features`, climate) reads the same on all three lines.

Rule: **after adding or editing any biome in `common/src/main/resources/data/nerospace/worldgen/biome/`, run
`python tools/gen_biome_overlays.py`.** It writes the 26.3 copy of every biome to
`common/src/main/resources-26.3/data/nerospace/worldgen/biome/` (spawns converted, `count` an int when
`minCount == maxCount`, else `minecraft:uniform`; every category written, empty ones as `[]`; nothing else changed).
Never edit the overlay by hand. `python tools/gen_biome_overlays.py --check` fails on a missing, stale or orphaned
overlay, and `BiomeOverlay26_3Test` fails the build if a biome has no overlay, the overlay keeps `spawners` /
`spawn_costs`, its spawns differ from the shared biome, or it is not exactly the generator's transform of the shared
biome (a `features` / `effects` / `attributes` edit without a re-run is caught too).

## Build

Run from the repo root:

```bash
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build \
  :forge:26.1.2:build :forge:26.2:build \
  :fabric:26.1.2:build :fabric:26.2:build
```

Jars land in `<loader>/versions/<mc>/build/libs/`.

Publishing (`.github/workflows/publish.yml`) builds all 27 cells (the 26.x cells above plus Minecraft 1.21.1,
1.21.4, 1.21.5, 1.21.8, 1.21.10 and 1.21.11 on each loader) and tags each upload with its loader and game
versions (the 1.21.8 jar also lists 1.21.7, the 1.21.10 jar also lists 1.21.9).

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
