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
./gradlew -p multiloader build                              # both loaders, default MC
./gradlew -p multiloader :fabric:build
./gradlew -p multiloader :neoforge:build

# target a specific Minecraft version (the "configurations"):
./gradlew -p multiloader :neoforge:build -Pminecraft_version=26.1.2
./gradlew -p multiloader :fabric:build   -Pminecraft_version=26.2
```

Output jars land in `multiloader/<loader>/build/libs/`. The default Minecraft
version is `minecraft_version` in `gradle.properties`; `-Pminecraft_version`
overrides it and selects the matching `*_<mc>` dependency pins.

## The version matrix (26.1 and 26.2)

The "different configurations" are the cross product of loader × Minecraft
version:

| | MC 26.1.2 | MC 26.2 |
| --- | --- | --- |
| **NeoForge** | NeoForge `26.1.2.76` — real | pending (no 26.2 NeoForge yet) |
| **Fabric** | pending (Fabric newest stable is 26.1.1) | pending (Fabric API for 26.2 not out) |

MC 26.2 ("Chaos Cubed") released 2026-06-16, so its modding toolchains haven't
shipped. The matrix is wired now and each cell lights up automatically when its
`*_<mc>` pin in `gradle.properties` resolves — no structural changes needed.

A build targets one version at a time (one MC version per jar — see
[`docs/MULTILOADER.md`](../docs/MULTILOADER.md) §1). Stonecutter is only needed
once the *source* diverges between 26.1 and 26.2 APIs; until then the
property-driven matrix above is the version mechanism, and Stonecutter stays
declared-and-ready (see "Finishing the version axis").

## CI/CD

`.github/workflows/multiloader.yml` builds the full matrix on pushes/PRs that
touch `multiloader/**` (plus manual `workflow_dispatch`), independent of the
root build (`build.yml`). It runs
`./gradlew -p multiloader :<loader>:build -Pminecraft_version=<mc>` per cell and
uploads the jars. Every cell is `experimental: true` (continue-on-error) while
the 26.x toolchains are pending — flip a cell's `experimental` to `false` once
it builds for real so a regression fails the workflow (start with
NeoForge @ 26.1.2).

## VS Code

Run configs live in the repo-root `.vscode/` so they show up alongside the
existing NeoForge ones:

- **`tasks.json`** — `ML: …` tasks for build / runClient / runServer per loader,
  plus `genVsCodeRuns`. They prompt for the Minecraft version (`26.1.2` / `26.2`)
  and shell out to `gradlew -p multiloader …`. This is the no-setup way to run
  the configurations.
- **`launch.json`** — a "Multiloader (Architectury)" group of debug launches.
  These are templates matching loom's `genVsCodeRuns` output; run the
  `ML: Regenerate VS Code run configs` task once (the toolchain must resolve
  first) to populate the authoritative classpaths/args.

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
