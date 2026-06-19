# Nerospace multiloader scaffold (MultiLoader-Template)

A self-contained skeleton for building Nerospace on **both NeoForge and Fabric**
from one shared codebase, on the **de-obfuscated Minecraft 26.x** toolchain.

> **Does not affect the working build.** The single-loader NeoForge mod at the
> repo root is untouched. This is a parallel scaffold you promote to the root
> when ready. Full migration plan: [`docs/MULTILOADER.md`](../docs/MULTILOADER.md).

## Status (verified 2026-06-18)

Built via the gradle MCP on this machine:

| Cell | Toolchain | 26.2 | 26.1.2 |
| --- | --- | --- | --- |
| `common` | ModDevGradle (NeoForm) | ✅ builds (`26.2-1`) | NeoForm `26.1.2-1` |
| `fabric` | Fabric Loom `1.17.11` | ✅ **builds** (`fabric-api 0.152.1+26.2`) | ✅ builds (`fabric-api 0.150.0+26.1.2`; needs access widener — see below) |
| `neoforge` | ModDevGradle (NeoForge) | ✅ builds (`26.2.0.3-beta`, on public NeoForged Maven) | NeoForge `26.1.2.76` |

`./gradlew :common:build :fabric:build -Pminecraft_version=26.2` → **BUILD SUCCESSFUL**.

## Why this layout (not Architectury)

architectury-loom **cannot consume de-obfuscated Minecraft 26.x mappings**
(issue [#328](https://github.com/architectury/architectury-loom/issues/328),
open, no fix). So this scaffold uses the **MultiLoader-Template** approach — each
module on its loader's *native, de-obf-ready* toolchain:

- `common`   → `net.neoforged.moddev` in **NeoForm-only** mode (de-obfuscated vanilla)
- `fabric`   → `net.fabricmc.fabric-loom` (de-obf: **no `mappings` line**)
- `neoforge` → `net.neoforged.moddev` (full NeoForge userdev)

Shared game logic lives **once** in `common`; its source is pulled into `fabric`
and `neoforge` (single copy → no drift). No Architectury API.

## Layout

```text
multiloader/
├── gradlew(.bat) + gradle/        own wrapper, Gradle 9.5.1 (Loom 1.17 needs >= 9.4)
├── settings.gradle                plugin versions + includes common/fabric/neoforge
├── build.gradle                   shared subproject config (Java 25, repos, token expand)
├── gradle.properties              per-version pins (neo_form / neo_version / fabric_api)
├── common/                        net.neoforged.moddev (NeoForm) — shared source
│   └── src/main/java/.../NerospaceCommon, platform/{Services,IPlatformHelper}, registry/
├── fabric/                        net.fabricmc.fabric-loom + fabric.mod.json + platform impl
└── neoforge/                      net.neoforged.moddev + neoforge.mods.toml + platform impl
```

## Building

It's a standalone build with its **own Gradle 9.5.1 wrapper** — run from inside
`multiloader/` (never `../gradlew -p multiloader`, which uses the root's 9.2.1):

```bash
cd multiloader

./gradlew :common:build :fabric:build -Pminecraft_version=26.2   # verified green
./gradlew :neoforge:build             -Pminecraft_version=26.1.2 # NeoForge on 26.1.x

# default version is gradle.properties -> minecraft_version (26.2);
# -Pminecraft_version selects the matching *_<mc> pins.
```

Jars land in `multiloader/<loader>/build/libs/`.

## All four cells build (was: NeoForge 26.2 pending)

NeoForge's own loader userdev for 26.2 may already be on Maven as a beta
(`neo_version_26.2=26.2.0.1-beta` is pinned — the official MultiLoader-Template's
default). If it doesn't resolve yet, **self-build it** until it publishes:

```bash
git clone https://github.com/neoforged/NeoForge && cd NeoForge
git checkout 26.2.x
./gradlew :neoforge:publishToMavenLocal      # JDK 25; publishes net.neoforged:neoforge:<ver>
```

The `neoforge` module already has `mavenLocal()` in its repositories, so set
`neo_version_26.2` to the version it printed and build. When the official jar
lands on Maven, it's a **one-line change** (the version pin) — no refactor.

## CI / VS Code

- **`.github/workflows/multiloader.yml`** — loader × version matrix; `fabric @ 26.2`
  is marked non-experimental (verified), the rest stay `continue-on-error` until
  their pins/artifacts are confirmed.
- **Root `.vscode/`** — `ML: …` tasks (build/run per loader, version picker) run
  `multiloader/gradlew`. `runClient`/`runServer` come from Fabric Loom (`:fabric`)
  and ModDevGradle (`:neoforge`).

## Scope boundary

This unblocks the **build**. It does not port Nerospace's NeoForge-specific
systems (capabilities/transfer, attachments, fluids, networking) to Fabric — that
migration is the real effort and is tracked in
[`docs/MULTILOADER.md`](../docs/MULTILOADER.md) §2. Shared logic goes in `common`;
loader-specific behaviour goes through the `platform/Services` seam (registration
is per-loader — there is no Architectury API `DeferredRegister` here).

## Promoting to the repo root

When ready to replace the single-loader build: move the existing
`src/main/java/...` logic into `common` (loader-specific bits behind `Services`),
move these build files to the repo root (merging the root's JEI/datagen/tooling
config), repoint `tools/` at the module paths, and retire the root single-loader
build. Until then the root build remains the source of truth.

## Sources

- [architectury-loom #328 — no de-obf 26.x](https://github.com/architectury/architectury-loom/issues/328)
- [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) · [official Fabric example (de-obf)](https://github.com/FabricMC/fabric-example-mod)
- [NeoForm](https://projects.neoforged.net/neoforged/neoform) · [NeoForge](https://projects.neoforged.net/neoforged/neoforge) · [Fabric develop](https://fabricmc.net/develop)

## Build matrix status (2026-06-19)

All four loader × version cells build from **public artifacts** (verified via the gradle MCP),
and CI (`.github/workflows/multiloader.yml`) builds all four strictly (any failure fails the run):

| | 26.1.2 | 26.2 |
| --- | --- | --- |
| **neoforge** | ✅ `26.1.2.76` | ✅ `26.2.0.3-beta` (public Maven) |
| **fabric** | ✅ (access widener) | ✅ `fabric-api 0.152.1+26.2` |

Fabric @ 26.1.2 needs `fabric/src/main/resources/nerospace.accesswidener` because vanilla
MC 26.1.2 kept `BlockEntityType`'s constructor + `BlockEntitySupplier` private (Mojang made them
public in 26.2; NeoForge widens them on both). The widener is a no-op on 26.2.
