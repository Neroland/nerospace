# Multiloader & multi-version support for Nerospace

Scope of this document: what it would take to ship Nerospace on **both NeoForge and Fabric**, and whether one project can target **multiple Minecraft versions** (e.g. 26.1 *and* 26.2) at the same time.

Read the second question first — it changes how you structure everything.

---

## 0. Architecture decision (recommended): one codebase, no drift

The guiding constraint for this project is **do not let the codebase drift apart** — never end up fixing the same bug twice across copies. That rules out the tempting-but-fatal pattern of a branch per loader or per Minecraft version. The decision:

- **One repository, one source tree.** Shared game logic lives exactly once in a `common` module; only the thin per-loader entry points and platform shims are loader-specific. This is the entire point of the common-module split — there is no second copy of the mechanics to drift.
- **Stonecutter for the version axis (26.1 / 26.2).** Both versions build from the same tree, with preprocessor comments only on the handful of lines where the APIs differ. This is the purpose-built anti-drift tool; prefer it over version branches.
- **Architectury for the loader axis — when it's ready.** Architectury is the *easiest to work in*: its API gives cross-loader `DeferredRegister`, events, networking and config, so `common` does more with less hand-written glue. Adopt it as the target once architectury-loom supports de-obfuscated Minecraft 26.x (see §2.x / Troubleshooting in the scaffold README).
- **Until architectury-loom catches up, stay NeoForge-only on the existing single project.** It is the lowest-friction, zero-drift state today: one toolchain, one copy, already building and publishing. Bringing up Fabric now buys friction without a working Fabric build, because architectury-loom can't yet consume de-obfuscated 26.x mappings.
- **If Fabric is needed *before* architectury-loom is ready,** use the MultiLoader-Template layout (Fabric Loom for `:fabric`, ModDevGradle for `:neoforge`) instead — each loader's native, de-obf-ready toolchain. It still shares one `common`, so it doesn't drift; it just costs more hand-written glue than Architectury. Do **not** unblock architectury-loom by hand-feeding it a synthetic mappings file — that satisfies the config check but breaks loom's remap pipeline downstream.

In one line: **single repo + `common` module + Stonecutter = no drift; Architectury makes that the easiest to work in; until architectury-loom supports de-obf 26.x, stay NeoForge-only rather than splitting.**

---

## 0b. Field notes: how the ecosystem builds multiloader on de-obf 26.x (researched 2026-06-18)

Hard facts gathered while trying to unblock 26.2, so the next person doesn't re-walk this:

- **Architectury is a dead end for 26.x right now.** architectury-loom issue [#328 "26.1 Support"](https://github.com/architectury/architectury-loom/issues/328) is open (since 2026-02-01) with no fix, no PR, no assignee: it cannot consume de-obfuscated 26.x mappings. Verified directly against this repo via the gradle MCP — every mappings config fails (`officialMojangMappings()` → "Failed to find official mojang mappings"; omitted → "Configuration 'mappings' has no dependencies"; `loom.layered {}` → `NullPointerException`). Do **not** try to force it with a synthetic mappings file; that passes config but breaks loom's remap pipeline.

- **What mods actually use instead: the MultiLoader-Template — no architectury-loom.** The canonical [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) (its source comments reference NeoForge's `26.2.x` branch, so it's current) wires each module to its loader's *native* toolchain:
  - `common`  → `net.neoforged.moddev` (ModDevGradle, via NeoForm) — no mappings step
  - `fabric`  → `net.fabricmc.fabric-loom` (de-obf-ready; **omits** the `mappings` line, like the official Fabric template)
  - `neoforge`→ `net.neoforged.moddev`
  - shared code lives once in `common`; loader modules consume it through `build-logic` convention plugins (`multiloader-common` / `multiloader-loader`). No drift, no Architectury API.

- **Both native toolchains support de-obf 26.x; architectury-loom is the only one that doesn't** — confirmed: the root NeoForge ModDevGradle build compiles against 26.1.2, and Fabric's official template builds 26.2.

- **26.2 Maven reality (checked 2026-06-20) — what is and isn't published.** Gradle compiles against *published artifacts*, not a git branch, so what matters is which jars are on the NeoForged/Fabric Maven:
  - **NeoForm 26.2: published** (`net.neoforged:neoform`, pinned `26.2-1`). NeoForm is the de-obfuscated vanilla base the MultiLoader-Template's `common` (ModDevGradle) compiles against — so **`common` builds on 26.2.**
  - **Fabric 26.2: published** (Fabric Loader `0.19.3` + Fabric API `0.152.1+26.2`) — so **`fabric` builds on 26.2.**
  - **NeoForge loader userdev 26.2: published** (`net.neoforged:neoforge`, latest `26.2.0.6-beta`) — so **`neoforge` builds on 26.2** from the public NeoForged Maven; no self-build needed.
  - All four cells (common + both Fabric cells + NeoForge) build on 26.2 from public artifacts. (Historically NeoForge 26.2 lagged NeoForm; if a future pin ever fails to resolve, fall back to self-building the `26.2.x` branch → `./gradlew publishToMavenLocal` → `mavenLocal()` → set `neo_version_26.2`.)

- **Build-unblock ≠ mod port.** Getting a Fabric 26.2 jar to *compile* is separate from porting Nerospace's NeoForge-specific systems (capabilities/transfer, attachments, fluids, networking) to Fabric — that migration (§2) is the real effort and is unchanged by the toolchain choice.

**Status: IMPLEMENTED and verified (2026-06-20).** The cross-loader build (since
flattened onto the repo root — `post_port.md` Phase 2; the standalone build is
retired under `legacy/`) uses the MultiLoader-Template layout (ModDevGradle `common` on NeoForm + Fabric
Loom `fabric` + ModDevGradle `neoforge`) — architectury-loom is gone.
`./gradlew :neoforge:26.2:build :fabric:26.2:build` is **BUILD
SUCCESSFUL** on this machine: `common`/`neoforge` against NeoForm `26.2-1` +
NeoForge `26.2.0.6-beta`, `fabric` against Fabric Loom `1.17` + Fabric API
`0.152.1+26.2` (no `mappings`). All four loader × version cells now build from
public artifacts. See [`docs/MULTILOADER_BUILD.md`](MULTILOADER_BUILD.md) for the per-cell status.

**Version axis = Stonecutter (2026-06-22).** The old `-Pminecraft_version` flag is
gone. Each loader × MC version is now its own Gradle node (`:fabric:26.1.2`,
`:fabric:26.2`, `:neoforge:26.1.2`, `:neoforge:26.2`) created by Stonecutter
(`settings.gradle` + `stonecutter.gradle` controller). Because each node is a
distinct project with its OWN resolved classpath, VS Code can switch the running
MC version per Run-&-Debug config (each targets `nerospace-<loader>-<version>`).
Build a cell with `:loader:version:build`; the active IDE version is set by
`stonecutter.active` and switched with the `stonecutterSwitchTo…` tasks.

### Field-notes sources

- [architectury-loom #328 — 26.1 Support](https://github.com/architectury/architectury-loom/issues/328)
- [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template)
- [Official Fabric example mod (de-obf, omits `mappings`)](https://github.com/FabricMC/fabric-example-mod)
- [Mojang: removing obfuscation](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition) · [Fabric: removing obfuscation from Fabric](https://fabricmc.net/2025/10/31/obfuscation.html)

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
