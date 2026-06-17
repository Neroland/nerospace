# Multiloader & multi-version support for Nerospace

Scope of this document: what it would take to ship Nerospace on **both NeoForge and Fabric**, and whether one project can target **multiple Minecraft versions** (e.g. 26.1 *and* 26.2) at the same time.

Read the second question first — it changes how you structure everything.

---

## 1. Can one project support Minecraft 26.1 *and* 26.2 at the same time?

**A single built jar targets exactly one Minecraft version.** You cannot produce one artifact that loads on both 26.1 and 26.2. Reasons:

- NeoForge artifacts are pinned per MC version. The version string itself encodes it: `26.1.2.76` is *only* for MC 26.1.2; MC 26.2 gets its own `26.2.x.y` line. Fabric Loader is version-agnostic, but Fabric API and the Yarn/Mojmap mappings are per-MC-version.
- Minecraft's internal classes, method signatures and registries change between minor versions. Code compiled against 26.1 mappings will not resolve against 26.2 (this project already relies on exact 26.1 signatures — see the `interact`/`interactAt` merge note in `CLAUDE.md`).

**So "support both versions" means "produce a separate jar per version from one source tree."** Two ways to do that:

| Strategy | What it is | Cost |
|---|---|---|
| **Git branches per MC version** (status quo for most mods) | One branch per MC version; cherry-pick fixes across them. | Cheap to start, expensive to maintain — every fix is N cherry-picks. |
| **[Stonecutter](https://stonecutter.kikugie.dev/)** (recommended) | A Gradle plugin that keeps a *single* source tree and uses preprocessor comments to swap version-specific fragments at build time. Builds one jar per declared version into `versions/<ver>/build/libs/`. | Higher up-front setup; near-zero per-version maintenance after. |

With Stonecutter you annotate the handful of lines that differ between versions, e.g.:

```java
//? if >=26.2 {
/*newSignature();
*///?} else {
oldSignature();
//?}
```

Everything else stays as ordinary code shared by all versions.

### The two axes are independent and combine

Loader (NeoForge / Fabric) and MC version (26.1 / 26.2) are orthogonal. Supporting both on both is a **matrix** of jars:

```
                 MC 26.1            MC 26.2
NeoForge   nerospace-26.1-neoforge  nerospace-26.2-neoforge
Fabric     nerospace-26.1-fabric    nerospace-26.2-fabric
```

[**Stonecraft**](https://stonecraft.meza.gg/) is a Gradle plugin that wires Stonecutter (versions) together with Architectury (loaders) specifically to manage this matrix with less boilerplate. If you want both axes, start there.

---

## 2. Supporting both NeoForge and Fabric

### 2.1 Reality check first

Nerospace is **deeply** coupled to NeoForge — far more than a typical "blocks and items" mod. The hard dependencies are:

- The entire machine/storage/pipe/rocket system is built on the **NeoForge capabilities + `net.neoforged.neoforge.transfer` framework** (Energy / Fluid / Item / custom Gas `ResourceHandler`s) — ~30+ files.
- **20+ event handlers** via `@SubscribeEvent` / `@EventBusSubscriber`.
- **NeoForge-only features**: data Attachments, `FluidType`/`BaseFlowingFluid`, biome modifiers, `ModConfigSpec`, the payload networking system, `GuiLayer` HUD rendering.

Fabric has equivalents for *most* of these, but **not the same APIs** — in several cases (energy, attachments, fluids) the ecosystems are fundamentally different. This is a real port, not a config flag. Budget accordingly; the capability/transfer abstraction alone is the bulk of the work.

### 2.2 Choose a project layout

Two established patterns. Both split loader-specific code from shared code.

**Option A — Architectury** ([API](https://www.curseforge.com/minecraft/mc-mods/architectury-api))
A `common` module written against Architectury's cross-loader abstractions (`@ExpectPlatform`, Architectury `DeferredRegister`, Event API), plus thin `fabric` and `neoforge` modules. Most batteries included; adds the Architectury API as a runtime dependency.

**Option B — MultiLoader-Template** ([illusivesoulworks](https://github.com/illusivesoulworks/multiloader-template) / Jared-style)
A `common` source set compiled against **vanilla Mojmap only** (no loader APIs), plus `fabric` and `neoforge` subprojects. Loader-specific behavior is reached through a hand-rolled `@ExpectPlatform`-style services pattern using Java's `ServiceLoader`. No extra runtime dependency; you write more of the glue yourself.

**Recommendation for Nerospace:** Option A (Architectury). With this much capability and event code to abstract, the ready-made cross-loader registries and event API save substantial work, and Stonecraft can wire it to Stonecutter if you also want multi-version.

### 2.3 Target build structure

The current single ModDevGradle build becomes the `neoforge` subproject. New top-level layout:

```
settings.gradle            // includes :common, :fabric, :neoforge
build.gradle               // shared config, Architectury loom plugin
gradle.properties          // versions for both loaders + Fabric API/Loom
common/                    // vanilla-only + Architectury abstractions
  src/main/java/za/co/neroland/nerospace/...
fabric/
  src/main/java/.../fabric/    // FabricModInit, platform impls
  src/main/resources/fabric.mod.json
neoforge/
  src/main/java/.../neoforge/  // current Nerospace.java lives here
  src/main/resources/META-INF/neoforge.mods.toml   // existing template
```

Build tooling: Fabric uses **Loom**; NeoForge keeps **ModDevGradle** (or NeoGradle under Architectury). The `tools/gradle-mcp` server and the Python asset/model generators are loader-agnostic and keep working — they operate on `src/main/resources` and Blockbench sources, which move to `common`.

### 2.4 Work breakdown — what changes, file by file

The pattern throughout: **shared logic moves to `common`; anything importing `net.neoforged.*` becomes an interface in `common` with a NeoForge impl and a Fabric impl.**

| Subsystem | Current (NeoForge) | Fabric equivalent | Migration |
|---|---|---|---|
| **Mod entry** | `Nerospace.java` `@Mod` + `IEventBus` ctor | `ModInitializer.onInitialize()` | Common `init()` called from both. NeoForge keeps `@Mod`; Fabric adds `FabricModInit implements ModInitializer` + `ClientModInitializer`. |
| **Registration** | `DeferredRegister`, `DeferredBlock/Item/Holder` (12 registry classes) | `Registry.register(...)` / Fabric registry helpers | Replace with Architectury `DeferredRegister` (one API, both loaders) in `common`. Largest mechanical change but low-risk. |
| **Energy/Fluid/Item/Gas transfer** ⚠️ | `net.neoforged.neoforge.transfer.*` `ResourceHandler`s, `Capabilities.Energy/Fluid/Item`, custom `GasCapability` (~30 files) | Fabric Transfer API (`Storage<T>`, `FluidVariant`, `ItemVariant`) + **Team Reborn Energy** for power | **Hardest part.** No shared energy standard: NeoForge energy ≠ Fabric/TR energy. Define `common` capability interfaces (`EnergyContainer`, fluid/item/gas storage) used by all block-entity logic; implement provider exposure per loader (NeoForge `RegisterCapabilitiesEvent` vs Fabric `*Storage.SIDED.registerForBlockEntity`). Plan real time here. |
| **Events** (20+ `@SubscribeEvent`) | NeoForge event bus | Fabric callbacks (`ServerTickEvents`, `ServerPlayConnectionEvents`, `AttackBlockCallback`, `ServerEntityEvents`…) + Mixins where no callback exists | Move handler *logic* to `common` static methods; register them per loader. Architectury Event API covers many. `LivingFallEvent`, `PlayerInteractEvent` etc. map to Fabric callbacks; some need a Mixin. |
| **Networking** | `RegisterPayloadHandlersEvent`, `PayloadRegistrar`, `PacketDistributor` | `PayloadTypeRegistry` + `ServerPlayNetworking`/`ClientPlayNetworking` | Payload record classes (`OxygenFieldSyncPayload`, `SetPipeModePayload`) implement vanilla `CustomPacketPayload` → mostly shared in `common`. Registration + send/distribute differ per loader. |
| **Config** | `ModConfigSpec` (`Config.java`) | cloth-config / midnightlib, or plain JSON | Abstract config access behind a `common` interface; back it with `ModConfigSpec` (NeoForge) and a Fabric lib. |
| **Attachments** ⚠️ | `AttachmentType` (`ModAttachments.java`) — oxygen, progress, etc. | No direct equivalent | Use **Cardinal Components API** on Fabric, or a custom per-entity NBT layer. Non-trivial; design a `common` attachment abstraction. |
| **Fluids** | `FluidType` + `BaseFlowingFluid` (`ModFluids.java`) | Fabric fluid API (`FlowableFluid`) + fluid render handler | Reimplement fluid registration/rendering per loader behind a common factory. |
| **Creative tabs** | `ModCreativeModeTabs` (NeoForge builder) | `FabricItemGroup` / vanilla `CreativeModeTab` | Small; abstract the tab builder. |
| **Menus** | `IMenuTypeExtension` (`ModMenuTypes`) | `ExtendedScreenHandlerType` | Both support extra open-data; thin per-loader factory. |
| **Biome/worldgen mods** | `BiomeModifier`, `AddFeaturesBiomeModifier` | Fabric `BiomeModifications` API | Reimplement the modifier per loader; configured/placed features (`ModConfiguredFeatures`, `ModPlacedFeatures`) are vanilla and stay in `common`. |
| **Datagen** | `GatherDataEvent` + NeoForge providers (`ModModelProvider` uses `ExtendedModelTemplate`, `BlockTagsProvider`, `LanguageProvider`…) | Fabric Data Generation API (`fabric-datagen`) | Each provider re-parents to its loader's base class. Provider *content* (recipes, loot, tags, lang) is shared logic. NeoForge `ExtendedModelTemplate` has no Fabric equivalent — rework those models or run datagen only on NeoForge and copy outputs. |
| **Chunk loading** | `RegisterTicketControllersEvent`, `TicketController` (terraformer, quarry) | Fabric `ServerChunkManager`/`LoadingValidationCallback` forced-chunk API | Abstract a `common` chunk-loader service; implement per loader. |
| **Client HUD** | `GuiLayer`, `RegisterGuiLayersEvent`, `RenderGuiLayerEvent` (oxygen HUD) | `HudLayerRegistrationCallback` / `HudRenderCallback` | Move draw logic to `common`; register per loader. |
| **Client renderers** | `EntityRenderersEvent`, `RegisterMenuScreensEvent` | `EntityRendererRegistry`, `MenuScreens`/`HandledScreens` | Renderer/screen classes themselves are mostly vanilla; only registration changes. |
| **Config screen** | `IConfigScreenFactory` (NeoForge) | **ModMenu API** on Fabric | Per-loader; optional. |
| **Commands** | `RegisterCommandsEvent` | Fabric `CommandRegistrationCallback` | Command bodies are vanilla Brigadier → shared; registration differs. |
| **Gametests** | `RegisterGameTestsEvent` | Fabric `fabric-gametest` | Per-loader registration; test bodies shared. |
| **JEI compat** | `compat/jei` (NeoForge JEI) | JEI also ships a Fabric build | The JEI plugin API is the same across loaders; mostly recompile against the Fabric JEI artifact. |
| **Telemetry** | `FMLEnvironment` for client/server detect, Sentry JarJar | Fabric `FabricLoader.getEnvironmentType()`; Fabric jar-in-jar nesting | Abstract environment detection; both loaders support nested jars. |

⚠️ = highest-effort / highest-risk items.

### 2.5 What does *not* change

These are vanilla Minecraft and live in `common` untouched: entity AI and creature classes, block/block-entity mechanics, the pipe-network routing math, world-gen feature *logic*, Brigadier command bodies, screen layout code, and all art/asset pipelines (`tools/gen_textures.py`, `gen_bbmodels.py`, `model_sync.py`, Blockbench sources). Textures, models, lang, loot and recipe JSON under `src/main/resources` move to `common` and are shared by both loaders.

### 2.6 Suggested sequence

1. Stand up the Architectury `common`/`fabric`/`neoforge` skeleton; move existing NeoForge code into `:neoforge` and confirm it still builds (`mcp__gradle__gradle_build`).
2. Migrate registration to Architectury `DeferredRegister` in `common`. Re-verify NeoForge build.
3. Define `common` abstractions for **capabilities/transfer** and implement the NeoForge side (re-wiring existing handlers). This is the long pole.
4. Bring up the Fabric side incrementally: registration → networking → events → transfer (Team Reborn Energy + Fabric Transfer API) → attachments (Cardinal Components) → client.
5. Add `fabric.mod.json`; split datagen per loader.
6. (Optional) Layer in Stonecutter/Stonecraft for 26.1 + 26.2 once the loader split is stable.

Verify each step with the gradle MCP server before moving on, per `CLAUDE.md`.

---

## 3. Bottom line

- **One jar = one MC version.** Supporting 26.1 and 26.2 means separate jars from one source tree — use **Stonecutter** rather than divergent branches.
- **NeoForge + Fabric is a substantial port**, not a setting. Use **Architectury** (Option A) for the loader split; expect the **capabilities/energy-transfer** layer, **attachments**, and **fluids** to dominate the effort because Fabric's equivalents are different APIs, not drop-in replacements.
- Both axes combine into a 2×2 build matrix; **Stonecraft** exists to manage exactly that combination.

### Sources
- [Stonecutter — multi-version Gradle plugin](https://stonecutter.kikugie.dev/)
- [Stonecraft — Stonecutter + Architectury wiring](https://stonecraft.meza.gg/)
- [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
- [illusivesoulworks/multiloader-template](https://github.com/illusivesoulworks/multiloader-template)
- [Kotlin-Multiloader-Template (common source set rationale)](https://github.com/Erdragh/Kotlin-Multiloader-Template)
