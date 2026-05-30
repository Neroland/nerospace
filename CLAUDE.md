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
- DO NOT: Commit and Push automatically
