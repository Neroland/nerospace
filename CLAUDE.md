# Project context for AI coding agents — nerospace

> This and `AGENTS.md` are kept identical; update both together.

## The mod

- **nerospace** — a space-tech Minecraft mod: rockets + tiered launch pads, planets/dimensions
  (Greenxertz, Cindara, Glacira, orbital Station void), oxygen survival + space/hazard suits, power
  machines (generators, grinder, refinery, fuel tank, oxygen generator, terraformer, hydration),
  universal pipes (energy/fluid/gas/item), quarry, terraforming + meteors, alien village, Star Guide
  progression. Author: **Neroland** (Dario). Repo: github.com/Neroland/nerospace.
- Mod id: **`nerospace`** (matches the registry namespace + every loader manifest). Package root:
  `za.co.neroland.nerospace`.
- Targets **MC 26.1.2 AND 26.2** on **NeoForge, MinecraftForge/Forge, and Fabric** → the **"6 cells"**. **Java 25.**
  Mappings = official Mojang names (26.x ships de-obfuscated; no Parchment).

## Working with Dario (the developer)

- **Keep responses concise and direct** — minimal verbosity, minimal formatting (he set this preference).
- **POPIA & GDPR**: keep all logging/telemetry/scripts compliant — only public version strings, never
  personal data. (Sentry telemetry is opt-out + scrubbed.)
- **NEVER commit or push automatically.** Leave changes **staged** in the working tree; Dario reviews and
  commits with **native git on his Windows machine — that is the source of truth.** Branches: `main`
  (release), `enhancement/multi-loader-support`, `refactor/post-port-cleanup` (current working branch);
  all pushed to origin. Recovery net if a change goes wrong: `git reset --hard @{u}`.
- **Python = pyenv (pyenv-win).** Repo tools auto-detect via `PYENV`/`PYENV_ROOT`, else
  `~/.pyenv/pyenv-win`; override with `-PpythonCmd=<path>`.

## Repo layout — FLATTENED cross-loader build (post_port.md Phase 2)

- **The build IS the repo root.** `common/` (shared, raw-vanilla source spliced into every node),
  `neoforge/` (ModDevGradle), `forge/` (ForgeGradle), `fabric/` (Fabric Loom). Root build files: `settings.gradle`,
  `stonecutter.gradle` (the REAL root build script — Stonecutter repoints `buildFileName` here; the root
  `build.gradle` is inert), `gradle.properties`, `gradlew`, `gradle/`.
- **Version/loader axis = Stonecutter.** Each loader×MC is a real node `:<loader>:<mc>`
  (`:fabric:26.1.2 :fabric:26.2 :neoforge:26.1.2 :neoforge:26.2 :forge:26.1.2 :forge:26.2`). `common` is NOT a node — its source is
  spliced via `rootProject.ext.commonJava` / `commonResources`. Dep pins live in `gradle.properties` as
  `*_version_<mc>` keys; `mc_versions=26.1.2,26.2`.
- `legacy/` = the retired **standalone single-loader** project (kept until the port is 120% confirmed;
  NOT built or shipped). The cutover runbook is `post_port.md`.
- Shared at root (do NOT move): `tools/` (generators + the gradle-mcp server), `art/blockbench/`, `docs/`,
  `.github/`.

## Build & verify — gradle MCP ONLY (the agent sandbox cannot decompile)

- A local gradle MCP server (`tools/gradle-mcp/server.js`, MCP server `gradle`) runs `gradlew` natively
  with JDK 25. **`project_dir` = the repo root** (the default; the flattened build lives there).
- Build the cells: `mcp__gradle__gradle_build` tasks `[":neoforge:26.1.2:build", ":neoforge:26.2:build",
  ":forge:26.1.2:build", ":forge:26.2:build", ":fabric:26.1.2:build", ":fabric:26.2:build"]` → poll `mcp__gradle__gradle_status` until `outcome` is
  SUCCESSFUL/FAILED → on failure `mcp__gradle__gradle_log` (grep `\.java:[0-9]+: error`). A compile
  failure returns in ~1 s; a full build longer.
- Static analysis: `mcp__gradle__gradle_analyze` (runs `ecjCheck` = the VS Code Problems panel, via
  `tools/ecj.prefs`). **Baseline: 0 errors, ~25 pre-existing warnings** (nullness on Mojang codec/cap
  generics, redundant `(Fluid)`/`(int)` casts, 4 dead pipe-relay methods, 1 unused import). The task only
  FAILS on errors.
- **ALWAYS verify all 6 cells BUILD SUCCESSFUL + ecjCheck 0 errors before marking a task done.** Never
  sign off on an uncompiled change. The build does NOT validate lang/JSON — validate resource edits with
  `python json.load`.
- Resolving exact 26.x signatures when an `@Override` won't resolve: don't guess — register a temporary
  task that shells `javap` over `configurations.compileClasspath` and `println`s matching lines (run with
  config-cache off, capture the classpath at config time, DRAIN process output before `waitFor` to avoid a
  pipe deadlock, remove the task after). (Found this way: 26.1 merged `interact`/`interactAt` into
  `Entity#interact(Player, InteractionHand, Vec3)`.)

## CRITICAL — git from the agent (the sandbox mount corrupts .git)

- The Cowork sandbox **bash mount mis-reads/writes git internals** (`.git/index`, `packed-refs`): phantom
  detached HEAD, "bad signature", "unterminated line", stale reads. **Do NOT run git through bash.** Large
  bash in-place writes (>~25 KB, e.g. `sed -i`/python) also TRUNCATE files (lang/JSON) — use the Edit/Write
  tools for those.
- **Run native git via a temporary gradle init-script task executed by the gradle MCP** (runs on Dario's
  machine, where git is fine):

  ```groovy
  gradle.rootProject { tasks.register('x') { doLast {
      def p = ['git', /* args */].execute(null, rootProject.projectDir)
      p.waitForProcessOutput(System.out, System.err)   // DRAIN then wait (no deadlock)
  } } }
  ```

  Run it with `mcp__gradle__gradle_build` `tasks:['x']`, `extra_args:['--init-script','<ABS path on the dev
  machine>']` — do NOT add `--console=plain` (the MCP appends it; a duplicate makes gradle print usage).
  Read results from the native gradle log; delete the init script after. `git mv a b c … dir/` moves
  several items into a dir in one command. NEVER `git mv` the running wrapper (`gradle/`) — use an
  independent helper project. If a multi-source `git mv` fails partway, reconcile with `git add -A`.
  File CONTENT writes via Edit/Write DO propagate to disk; it's git metadata + cross-tool read-back that's
  unreliable — trust the native gradle log and Dario's native git over a bash `git status`.

## Conventions (cross-loader)

- **Resources are HAND-AUTHORED in `common/src/main/resources`** — the multiloader does **not** run
  datagen. Recipes/loot/tags/lang/blockstates/models/`items/` are committed JSON; validate with
  `python json.load` after any edit. (Datapack folders are singular: `recipe/`, `loot_table/`.)
- **Platform seams (ServiceLoader, no Architectury).** `common/.../platform/Services.java` loads 7
  services; each loader ships exactly one impl of each plus a `META-INF/services` entry:
  `IPlatformHelper` (name/dev/isModLoaded/isClient/configDir/version + the data-attachment getters/setters
  oxygen / terraformed / terraformStage / starGuideSeen), `EnergyLookup`, `FluidLookup`, `FluidFactory`,
  `GasLookup`, `NetworkPlatform`, `registry.RegistrationProvider$Factory`. **`common/` is effectively
  read-only for loader work — reuse the EXACT seams; don't add new common interfaces.**
- One RegistrationProvider DeferredRegister setup per content type. Menus are non-extended; renderers
  bake-direct; energy/fluid/gas via `EnergyBuffer`/`FluidTank`/`GasTank` + vanilla `WorldlyContainer`
  (simulate-then-commit, no transfer transactions); cross-loader BERs via
  `client/ClientBlockEntityRenderers.Sink` (NeoForge/Forge `RegisterRenderers` / Fabric `BlockEntityRenderers`).
- **26.x gotchas:** `ContainerData` syncs as 16-bit SHORTS — values >32767 wrap negative on the client
  (sync power as a per-mille ≤1000). `Item.getDescriptionId()` is `final` → BlockItems show the raw key
  unless a mirrored `item.nerospace.<id>` lang alias exists. `level.getDefaultClockTime()` compiles but
  THROWS at runtime where the data-driven clock markers aren't loaded — wrap try-once → permanent
  `getGameTime()` fallback. `net.minecraft.world.inventory.ClickType` moved. NeoForge/Forge debug tasks use
  `-PnerospaceDebug`; Fabric Loom honours Gradle `--debug-jvm` (see IDE section).
- **Mekanism / cross-mod integration is DEFERRED.** Forge itself is supported as the third loader via
  ForgeGradle 7 (`forge_version_26.1.2=26.1.2-64.0.10`, `forge_version_26.2=26.2-65.0.0`), but Mekanism
  integration stays out of scope.

## Assets (textures + models) — generators own the trivial ones

- Generators target the flattened `common` module with `--multiloader` (routed via
  `tools/nerospace_target.py` → `common/src/main/{java,resources}`):
  `python tools/gen_textures.py --multiloader && python tools/gen_bbmodels.py --multiloader`
  (or `./gradlew genAssets`). **ADDITIVE-ONLY** — they skip any asset that already exists (so reruns only
  fill gaps and never clobber edited art; `--force` to deliberately re-render).
- Every registered block/item needs a model + a 16×16 PNG under
  `common/src/main/resources/assets/nerospace/textures/{block,item}/<id>.png`. Don't hand-paint textures
  pixel-by-pixel; let the generators own trivial model JSON. (Non-trivial models — solar housing, pipe
  glass, GUIs, per-node icon `*_item.json` — ARE hand-authored here.)
- Entity models: `tools/model_sync.py --multiloader` keeps `art/blockbench/entity/<name>.bbmodel` ↔ Java
  `EntityModel.createBodyLayer()` in sync both ways (markers `// model_sync:begin`/`:end`; one cube per
  bone, `bb_y = 24 - java_y`; `./gradlew syncModels`). Edit either side and build to converge.
- Palettes (keep families distinct): nerosium = red/purple; Greenxertz (nerosteel / xertz quartz) =
  green/steel; rockets = steel + per-tier accent (T1 red, T2 nerosium-purple + boosters, T3 gold/green).
  Editable `.bbmodel` sources in `art/blockbench/{block,item,entity}/`.
- Branding: `tools/gen_logo.py` → `art/logo/`; in-game mods-list icon
  `common/src/main/resources/nerospace_logo.png` (referenced by `logoFile` in each loader manifest).

## IDE (VS Code) run & debug

- Workspace: **`nerospace.code-workspace`** (single-root `"."`). Import the Stonecutter nodes as **static
  Eclipse projects**: `./gradlew eclipse` (the live Buildship/Loom import fails here, so
  `java.import.gradle.enabled=false`). Re-run `./gradlew eclipse` after dependency changes, then reload VS
  Code. The per-node Eclipse project names are `nerospace-<loader>-<mc>`.
- **Run** a cell: `tasks.json` → "ML: Run <loader> Client - <mc>" (`:<loader>:<mc>:runClient`).
- **Debug** a cell: `launch.json` → "Debug: <cell>" (F5). Its `preLaunchTask` is a background "ML: Debug …"
  task that starts the client suspended on JDWP **:5005**, and VS Code attaches once the JVM is listening.
  - **Fabric** debug uses Gradle `--debug-jvm` (Loom honours it).
  - **NeoForge and Forge** debug tasks pass **`-PnerospaceDebug`**, which makes the loader build script attach
    a gated JDWP agent (`runs { configureEach { if project.hasProperty('nerospaceDebug') ... }}`).
    No effect on normal runs/builds.

## Status & open follow-ups

- **Cross-loader port: COMPLETE + signed off** (5-agent parity audit; gaps ported — recipes, loot, lang,
  tags, ore-gen, comparators, etc.). The current support matrix is 6 cells including Forge. See
  `post_port.md` + `docs/MULTILOADER.md`.
- **post_port.md Phase 1** (retire standalone root → `legacy/`) + **Phase 2** (flatten the multiloader to
  the repo root + fix path refs/CI/IDE) are **DONE and STAGED (not committed)** on
  `refactor/post-port-cleanup`.
- **CI / scripts / docs cleanup: DONE (staged, 2026-06-23).** `.github/scripts/update_deps.py` +
  `.github/workflows/auto-deps.yml` now target the flattened repo-root `gradle.properties` /
  `settings.gradle` (the retired `legacy/` build is no longer auto-bumped); the dead
  `.github/workflows/build.yml` is deleted; the stale `multiloader/` path prose in the root
  `gradle.properties` header + `docs/MULTILOADER*.md` is fixed (README had none).
- **Bespoke O2/hazard HUD: PORTED cross-loader (staged, 2026-06-23).** New cross-loader graphical-HUD
  seam: shared draw in `common/.../client/OxygenHud.java`, registered via NeoForge
  `RegisterGuiLayersEvent` + Fabric `HudElementRegistry` (both functional interfaces taking
  `GuiGraphicsExtractor` + `DeltaTracker`; the 26.x HUD API was javap-verified on NeoForge/Fabric and
  ported to Forge's `AddGuiOverlayLayersEvent` —
  vanilla `LayeredDraw`/`HudRenderCallback` are GONE, the new model is `Hud`/`GuiGraphicsExtractor` +
  Fabric `…rendering.v1.hud.*`). The vanilla air-bubble row is suppressed on airless dims (NeoForge
  `RenderGuiLayerEvent.Pre` cancel `VanillaGuiLayers.AIR_LEVEL` / Fabric
  `replaceElement(VanillaHudElements.AIR_BAR, …)`). `OxygenManager` now exposes public
  `suitTier`/`hazardShield`/`hazardFor`. Needs client runs to confirm the
  on-screen visual (can't be runtime-tested from the agent).**
- **Judgment-call rebalances: KEPT as the deliberate final balance** (Dario's call, 2026-06-23). The
  multiloader's machine base FE values + oxygen drain / suit-tank numbers stay as-is — they differ from the
  retired root by large factors (an intentional retune, not drift). No gameplay change.

## DO NOT

- Commit or push automatically — leave changes staged for Dario.
- Run git through the sandbox bash mount — use the native gradle-init-script technique.
- Edit `common/` for loader-specific work — reuse the existing seams.
- Hand-paint textures or hand-author trivial model JSON — use the generators.
