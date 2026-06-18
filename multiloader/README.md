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

This is a **standalone nested Gradle build with its own wrapper** (Gradle
`9.5.1`). It is *not* driven by the repo-root wrapper: Architectury Loom 1.17.x
needs Gradle ≥ 9.4 (it calls `Configuration.extendsFrom(Provider…)`, added in
9.4.0), while the root stays on 9.2.1 for NeoForge/ModDevGradle. So run it from
inside `multiloader/`:

```bash
cd multiloader

./gradlew build                              # both loaders, default MC
./gradlew :neoforge:build
./gradlew :fabric:build

# target a specific Minecraft version (the "configurations"):
./gradlew :neoforge:build -Pminecraft_version=26.1.2
./gradlew :fabric:build   -Pminecraft_version=26.2
```

Output jars land in `multiloader/<loader>/build/libs/`. The default Minecraft
version is `minecraft_version` in `gradle.properties`; `-Pminecraft_version`
overrides it and selects the matching `*_<mc>` dependency pins.

> Do **not** use `../gradlew -p multiloader` — that runs the root's Gradle 9.2.1
> and fails with `NoSuchMethodError: Configuration.extendsFrom`. Always use
> `multiloader/gradlew`.

## The version matrix (26.1 and 26.2)

The "different configurations" are the cross product of loader × Minecraft
version:

| | MC 26.1.2 | MC 26.2 |
| --- | --- | --- |
| **NeoForge** | NeoForge `26.1.2.76` — exists | NeoForge `26.2` not published yet |
| **Fabric** | Loader/API exist | Loader `0.19.3` + API `0.152.1+26.2` exist |

Upstream artifacts now exist for most cells (Fabric shipped 26.2 — see the
official template). **But the loom toolchain blocks the build:** architectury-loom
1.17.x can't handle de-obfuscated Minecraft 26.x mappings yet, so *no* cell
compiles regardless of versions (see Troubleshooting). This is the gating issue,
not artifact availability.

A build targets one version at a time (one MC version per jar — see
[`docs/MULTILOADER.md`](../docs/MULTILOADER.md) §1). Stonecutter is only needed
once the *source* diverges between 26.1 and 26.2 APIs; until then the
property-driven matrix above is the version mechanism, and Stonecutter stays
declared-and-ready (see "Finishing the version axis").

## CI/CD

`.github/workflows/multiloader.yml` builds the full matrix on pushes/PRs that
touch `multiloader/**` (plus manual `workflow_dispatch`), independent of the
root build (`build.yml`). Each cell runs (with `working-directory: multiloader`,
so it uses the 9.5.1 wrapper) `./gradlew :<loader>:build
-Pminecraft_version=<mc>` and uploads the jars. Cells are `experimental: true`
(continue-on-error) because **all of them are currently blocked at loom's
mappings step** (architectury-loom can't do de-obfuscated 26.x — see
Troubleshooting), so none compile yet. Once architectury-loom ships de-obf 26.x
support, flip a cell's `experimental` to `false` when it builds so regressions
fail the workflow.

## VS Code

Run configs live in the repo-root `.vscode/` so they show up alongside the
existing NeoForge ones:

- **`tasks.json`** — `ML: …` tasks for build / runClient / runServer per loader,
  plus `genVsCodeRuns`. They prompt for the Minecraft version (`26.1.2` / `26.2`)
  and run `multiloader/gradlew` with `cwd=multiloader` (its 9.5.1 wrapper). This
  is the no-setup way to run the configurations.
- **`launch.json`** — a "Multiloader (Architectury)" group of debug launches.
  These are templates matching loom's `genVsCodeRuns` output; run the
  `ML: Regenerate VS Code run configs` task once to populate the authoritative
  classpaths/args.

## Troubleshooting

Three walls were found while wiring 26.x support (all verified against the
gradle MCP, 2026-06-18). The first two are fixed; the third is the gating
upstream limitation. None are scaffold bugs.

**FIXED — `NoSuchMethodError: Configuration.extendsFrom(Provider[])`** (applying
loom in `subprojects`). That Gradle API landed in **Gradle 9.4.0**;
architectury-loom 1.17.x needs it, and the repo root is on 9.2.1 (kept for
NeoForge/ModDevGradle). Fix applied: this build has **its own Gradle 9.5.1
wrapper**. Always run `multiloader/gradlew` from inside `multiloader/`, never
`../gradlew -p multiloader` (that uses the root's 9.2.1 and fails here).

**FIXED — stale version pins.** Minecraft 26.2 ("Chaos Cubed", 2026-06-16) *does*
have a Fabric toolchain: Loader `0.19.3`, Fabric API `0.152.1+26.2` (per the
official Fabric template). Those are now pinned in `gradle.properties`. (NeoForge
26.2 still isn't published.)

**GATING — `Failed to find official mojang mappings` (and friends).** **Minecraft
26.x is de-obfuscated** (Mojang [removed obfuscation](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition)
after 1.21.11; the game ships official names, no proguard file, no Intermediary —
see [Fabric's writeup](https://fabricmc.net/2025/10/31/obfuscation.html)).
Fabric's *own* loom handles this by **omitting the `mappings` line** (the
official template has none). **architectury-loom 1.17.x does not** — every
mappings config fails at `Configure project :common`:

| `mappings` config | result on architectury-loom 1.17.x |
| --- | --- |
| *(omitted)* | `Configuration 'mappings' has no dependencies` |
| `loom.officialMojangMappings()` | `Failed to find official mojang mappings for 26.x` (no proguard file) |
| `loom.layered {}` (empty) | `NullPointerException: srcNamespace is null` |

So architectury-loom (the fork that adds NeoForge/Forge support) still assumes an
obfuscated game with a mappings layer, and hasn't yet ported the de-obf handling
Fabric's loom has. **This blocks every cell of the multiloader** — no version pin
or mappings tweak in this scaffold can work around it. Plugin wiring is otherwise
correct (Stonecutter 0.9.2 + Architectury Plugin 3.4.164 + Loom 1.17.483 all
resolve and configure up to this step).

What this means in practice:

- **A pure-Fabric 26.2 mod builds today** using Fabric's own loom
  (`net.fabricmc.fabric-loom`, omit `mappings`) — but that's single-loader, not
  this shared-`common` Architectury setup.
- **NeoForge 26.x builds today** via the repo-root ModDevGradle build (handles
  de-obf natively). That's your shippable path now.
- **The Architectury multiloader is blocked** until architectury-loom ships
  de-obf 26.x support. When it does, update the `mappings` line in `build.gradle`
  per its docs (and bump `architectury_loom_version`), and the cells should
  compile. Track: <https://github.com/architectury/architectury-loom/issues>.

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
