# Nerospace multiloader scaffold

A self-contained skeleton for building Nerospace on **both NeoForge and Fabric**
(loader axis, via **Architectury**) and across **multiple Minecraft versions**
(version axis, via **Stonecutter**).

> **This does not touch the working build.** The single-loader NeoForge mod at
> the repo root is unchanged and still builds normally. This directory is a
> parallel, reviewable scaffold you promote to the root only when you're ready
> to commit to the migration. See [`docs/MULTILOADER.md`](../docs/MULTILOADER.md)
> for the full subsystem-by-subsystem migration plan.

## What is and isn't here

This is a **skeleton**, not a port. It contains:

- the Gradle wiring for a 3-module Architectury project (`common` / `fabric` / `neoforge`);
- loader entry points that delegate to a shared `NerospaceCommon.init()`;
- a dependency-free platform abstraction (`platform/Services` + `IPlatformHelper`)
  with a Fabric and a NeoForge implementation;
- one example cross-loader registration (`registry/ModRegistries`) proving the path;
- mod metadata for both loaders (`neoforge.mods.toml`, `fabric.mod.json`);
- Stonecutter declared and ready to activate for the version axis.

It does **not** contain the migrated mod. None of the ~200 existing source files
have been moved — that is the actual migration work, sequenced in
[`docs/MULTILOADER.md`](../docs/MULTILOADER.md).

## Layout

```
multiloader/
├── settings.gradle        loader split (active) + Stonecutter (ready)
├── build.gradle           Architectury root + shared subproject config
├── gradle.properties      all version pins (per-MC-version)
├── stonecutter.gradle     version-axis controller (stub until activated)
├── common/                vanilla-only + cross-loader abstractions
│   ├── NerospaceCommon            shared entry point
│   ├── platform/Services          ServiceLoader resolver
│   ├── platform/IPlatformHelper   the loader seam (grow this during migration)
│   └── registry/ModRegistries     Architectury DeferredRegister example
├── fabric/                Fabric entry points + platform impl + fabric.mod.json
└── neoforge/              NeoForge @Mod entry + platform impl + neoforge.mods.toml
```

## ⚠️ Before the first build — pin real versions

The Fabric and Architectury artifacts in `gradle.properties` are
**placeholders shaped like real coordinates**, not confirmed-resolvable values.
Bleeding-edge Minecraft versions often ship NeoForge first, with Fabric API and
Architectury following later. Confirm and update, for **each** version in
`mc_versions`:

| Property | Where to confirm |
| --- | --- |
| `fabric_loader_version`, `fabric_api_version_<mc>` | <https://fabricmc.net/develop> |
| `architectury_loom_version`, `architectury_plugin_version`, `architectury_api_version_<mc>` | <https://docs.architectury.dev> / [maven.architectury.dev](https://maven.architectury.dev) |
| `neo_version_<mc>` | <https://projects.neoforged.net/neoforged/neoforge> |

If Fabric/Architectury have **not** ported to your target Minecraft version yet,
the `fabric` and `neoforge` configuration will fail to resolve dependencies.
That is an ecosystem-availability limit, not a scaffold defect — the NeoForge
side of the matrix can proceed independently in the meantime.

The plugin versions in `settings.gradle` (Stonecutter) and `build.gradle`
(`architectury-plugin`, `dev.architectury.loom`) are pinned literally because
the Gradle `plugins {}` block can't read `gradle.properties`. Keep them in sync
with the matching `*_version` values.

## Building

The scaffold is a nested Gradle build; drive it with the repo's wrapper:

```bash
# from the repo root
./gradlew -p multiloader build           # both loaders, active MC version
./gradlew -p multiloader :fabric:build
./gradlew -p multiloader :neoforge:build
```

Output jars land in `multiloader/<loader>/build/libs/`. The active Minecraft
version is `minecraft_version` in `gradle.properties`.

## Finishing the version axis (Stonecutter)

The loader axis (Architectury) is active. The version axis is **declared but not
wired into the build**, so the skeleton configures cleanly on its own. To build
the full 2×2 matrix (versions × loaders), pick one:

**A — Stonecraft (recommended, turnkey).** [Stonecraft](https://stonecraft.meza.gg/)
is a settings plugin that wires Stonecutter to Architectury automatically from
the `mc_versions` list. Swap the `dev.kikugie.stonecutter` plugin in
`settings.gradle` for `dev.meza.stonecraft` and follow its quick-start. Least
hand-wiring.

**B — Hand-wired Stonecutter.** Uncomment the `stonecutter { create(rootProject) }`
block in `settings.gradle`, flesh out `stonecutter.gradle` (the stub shows the
shape), and use version comments in source to absorb signature drift between
Minecraft versions:

```java
//? if >=26.2 {
/*newSignature();
*///?} else {
oldSignature();
//?}
```

Either way, keep the version list in `settings.gradle` / `stonecutter.gradle` in
sync with `mc_versions` in `gradle.properties`.

## The platform seam

Common code must not import `net.neoforged.*` or `net.fabricmc.*`. Where it needs
loader behaviour, it calls `Services.PLATFORM.<method>()`. Each loader module
provides one `IPlatformHelper` implementation, registered through its
`META-INF/services/...IPlatformHelper` file and resolved at runtime by
`ServiceLoader`. Expand `IPlatformHelper` (and add sibling service interfaces) as
you migrate capabilities, networking, config and attachments — this interface is
where every "NeoForge does X, Fabric does Y" decision is funnelled.

## Promoting to the repo root

When the migration is far enough along to replace the single-loader build:

1. Move the existing `src/main/java/...` business logic into `common` (strip
   loader imports behind `Services`); keep NeoForge-only code in `neoforge`.
2. Move `multiloader/{settings,build}.gradle` + `gradle.properties` to the repo
   root (merging the JEI/datagen/tooling config from the current root build).
3. Repoint the `tools/` generators and `tools/gradle-mcp` at the new module
   paths (they're loader-agnostic and otherwise unchanged).
4. Delete this `multiloader/` directory.

Until then, the root build remains the source of truth.

## References

- [`docs/MULTILOADER.md`](../docs/MULTILOADER.md) — full migration plan & subsystem map
- [Architectury docs](https://docs.architectury.dev) · [architectury-loom](https://github.com/architectury/architectury-loom)
- [Stonecutter](https://stonecutter.kikugie.dev/) · [Stonecraft](https://stonecraft.meza.gg/)
- [MultiLoader-Template (illusivesoulworks)](https://github.com/illusivesoulworks/multiloader-template)
