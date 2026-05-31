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
  matching lines is reliable. Run with
  `-Dorg.gradle.configuration-cache=false`, capture the classpath into a
  local at configuration time, and DRAIN the process output before
  `waitFor` (pipe-deadlock). Remove the task afterward. (Found this way:
  26.1 merged `interact`/`interactAt` into
  `Entity#interact(Player, InteractionHand, Vec3)`.)
- DELETING generated files from the agent sandbox: the repo mount blocks
  `unlink`, so use the Cowork file-delete permission. Datagen does NOT
  remove stale JSON — delete orphaned generated files by hand when a
  provider stops emitting them.
- DO NOT: Commit and Push automatically
