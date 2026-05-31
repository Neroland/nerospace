# Project context for AI coding agents

- Target: Minecraft 26.1.2, NeoForge loader, Java 25, ModDevGradle.
- Mod id: nerospace  (must match @Mod annotation and registry namespace)
- Package root: za.co.neroland.nerospace
- Build:        ./gradlew build
- Dev client:   ./gradlew runClient
- Dev server:   ./gradlew runServer
- Mappings: official Mojang names (no Parchment; 26.1 is de-obfuscated).
- Scope note: building STANDALONE for now. Mekanism / cross-mod
  integration is DEFERRED until those mods port to 26.1 — do not add
  hard dependencies on them yet. Prefer tags + NeoForge capabilities.
- Conventions: one DeferredRegister setup per content type; generate
  JSON (models/recipes/loot/tags/lang) via datagen, not by hand.
- ALWAYS VERIFY THE BUILD before marking any task complete: run
  `./gradlew runData` (when datagen changed) then `./gradlew build`
  and confirm BUILD SUCCESSFUL. Never mark a task done on an
  uncompiled change. If the build can't be run in your environment,
  say so explicitly and leave the task open pending verification.
- BUILDING FROM AN AGENT (Cowork / Claude Code): a local **gradle MCP
  server** (`tools/gradle-mcp/server.js`, registered as MCP server
  `gradle`) runs gradlew on the developer's machine with JDK 25, so the full
  decompile + build complete even when the agent sandbox cannot.
  Verify with it instead of the sandbox: call `mcp__gradle__gradle_build`
  (or `mcp__gradle__gradle_run_data`) → it returns a build_id; poll
  `mcp__gradle__gradle_status` until `outcome` is SUCCESSFUL/FAILED, then
  read `mcp__gradle__gradle_log` (grep `\.java:[0-9]+: error`) for
  diagnostics. A compile failure returns in ~1s; a passing full build
  takes longer.
- RESOLVING EXACT 26.1 SIGNATURES: when an `@Override` fails with
  "does not override", don't guess — a temporary `tasks.register` that
  shells `javap` over `configurations.compileClasspath` and `println`s
  matching lines is reliable. Run it with
  `-Dorg.gradle.configuration-cache=false`, capture the classpath into a
  local at configuration time, and DRAIN the process output (e.g.
  `consumeProcessOutput`) BEFORE `waitFor` to avoid a pipe deadlock.
  Remove the task afterward. (Found this way: in 26.1
  `Entity#interact(Player, InteractionHand, Vec3)` — `interact`/`interactAt`
  were merged.)
- DELETING generated files from the agent sandbox: the repo mount blocks
  `unlink`, so use the Cowork file-delete permission. Datagen does NOT
  remove stale JSON, so when a provider stops emitting a file, delete the
  orphan by hand (else e.g. a removed recipe stays active).
- ASSETS (textures + models): every registered block/item needs BOTH
  (a) a datagen entry in `datagen/ModModelProvider` — blocks via
  `blockModels.createTrivialCube(...)`, flat items via
  `itemModels.generateFlatItem(..., ModelTemplates.FLAT_ITEM)` (tools use
  `FLAT_HANDHELD_ITEM`) — and (b) a 16x16 PNG at
  `src/main/resources/assets/nerospace/textures/{block,item}/<id>.png`.
  Do NOT hand-author model/blockstate JSON (datagen owns it) and do NOT
  hand-paint textures pixel-by-pixel — generate them with the repo tools.
  The generators are ADDITIVE-ONLY: they skip any asset that already
  exists, so reruns only fill gaps and NEVER overwrite committed/edited
  art (pass `--force` only to deliberately re-render). When you add
  content:
    1. add the datagen entry (above);
    2. in `tools/gen_textures.py` add a `gen_<id>()` that writes the PNG,
       and call it from `__main__`;
    3. in `tools/gen_bbmodels.py` append `<id>` to BLOCKS or ITEMS;
    4. run `python3 tools/gen_textures.py && python3 tools/gen_bbmodels.py`;
    5. run `./gradlew runData` then `./gradlew build`; confirm every model
       resolves (no "missing texture" placeholder).
  Palettes (keep families visually distinct): nerosium = red/purple,
  Greenxertz (nerosteel / xertz quartz) = green/steel, rockets = steel +
  per-tier accent (T1 red, T2 nerosium-purple + boosters, T3 gold/green +
  glow). Editable Blockbench sources live in `art/blockbench/{block,item}/`;
  each `.bbmodel` embeds the texture and links back to its resource path,
  so painting there + "Save All Textures" writes into the mod.
- ENTITY MODELS (Blockbench <-> Java, bidirectional): `tools/model_sync.py` keeps each
  `art/blockbench/entity/<name>.bbmodel` and its Java `EntityModel.createBodyLayer()` in sync
  BOTH ways. The Java geometry lives in a normalised block between `// model_sync:begin` and
  `// model_sync:end` (one bone per cube, absolute `addBox`, `PartPose.offset(0,0,0)`) — only that
  block is ever rewritten. Java entity space is Y-down but Blockbench is Y-up, so the tool applies
  `bb_y = 24 - java_y` both ways (models rest on the ground at java y=24). Direction is by file
  mtime; force with `--to-java` / `--to-bbmodel`; `--check` reports drift. The `syncModels` Gradle
  task runs it before `compileJava`, so editing either side and building converges them. Python is
  auto-detected and pyenv-aware (uses `PYENV`/`PYENV_ROOT`, else `~/.pyenv/pyenv-win`); override with
  `-PpythonCmd=<path>`. The three mob `.bbmodel`s share `GreenxertzCreatureModel` geometry: edit the
  authoritative one (`xertz_stalker.bbmodel`) or the Java; the mirrors get geometry but keep their own
  texture. LIMITATION: one cube per bone, no per-bone rotation (do richer modelling in Blockbench and
  export via its own "Export Java Entity").
- BRANDING: `tools/gen_logo.py` builds the CurseForge logo
  (`art/logo/nerospace_logo*.png`). The in-game mods-list icon is
  `src/main/resources/nerospace_logo.png`, referenced by `logoFile` in
  `src/main/templates/META-INF/neoforge.mods.toml`.
- DO NOT: Commit and Push automatically
