# Nerospace cross-loader build (MultiLoader-Template, flattened to the repo root)

Builds Nerospace on **NeoForge, MinecraftForge/Forge, and Fabric** from one shared codebase, on the
**de-obfuscated Minecraft 26.x** toolchain.

> **This build IS the repo root.** Per `post_port.md` Phase 2 the multiloader was
> flattened onto the repo root; the retired standalone single-loader build now
> lives under `legacy/` (frozen, not built or shipped). Full history:
> [`docs/MULTILOADER.md`](../docs/MULTILOADER.md) and [`post_port.md`](../post_port.md).

## Status (verified 2026-06-23)

Built from public artifacts on this machine:

| Cell | Toolchain | 26.2 | 26.1.2 |
| --- | --- | --- | --- |
| `common` | ModDevGradle (NeoForm) | ✅ builds (`26.2-1`) | NeoForm `26.1.2-1` |
| `fabric` | Fabric Loom `1.17.12` | ✅ **builds** (`fabric-api 0.152.2+26.2`) | ✅ builds (`fabric-api 0.151.0+26.1.2`; needs access widener — see below) |
| `neoforge` | ModDevGradle (NeoForge) | ✅ builds (`26.2.0.6-beta`, on public NeoForged Maven) | NeoForge `26.1.2.76` |
| `forge` | ForgeGradle `7.0.29` (Forge) | ✅ builds (`26.2-65.0.0`) | ✅ builds (`26.1.2-64.0.10`) |

All six Stonecutter cells are wired as real projects:
`:neoforge:26.1.2`, `:neoforge:26.2`, `:forge:26.1.2`, `:forge:26.2`,
`:fabric:26.1.2`, and `:fabric:26.2`.

## Why this layout (not Architectury)

architectury-loom **cannot consume de-obfuscated Minecraft 26.x mappings**
(issue [#328](https://github.com/architectury/architectury-loom/issues/328),
open, no fix). So this scaffold uses the **MultiLoader-Template** approach — each
module on its loader's *native, de-obf-ready* toolchain:

- `common`   → `net.neoforged.moddev` in **NeoForm-only** mode (de-obfuscated vanilla)
- `fabric`   → `net.fabricmc.fabric-loom` (de-obf: **no `mappings` line**)
- `neoforge` → `net.neoforged.moddev` (full NeoForge userdev)
- `forge`    → `net.minecraftforge.gradle` (full Forge userdev)

Shared game logic lives **once** in `common`; its source is pulled into `fabric`
`neoforge`, and `forge` (single copy -> no drift). No Architectury API.

## Layout

```text
<repo root>/
├── gradlew(.bat) + gradle/        the wrapper, Gradle 9.5.1 (Loom 1.17 needs >= 9.4)
├── settings.gradle                plugin versions + Stonecutter version tree (fabric/neoforge/forge × 26.1.2/26.2)
├── stonecutter.gradle             the REAL root build script (splices common source into each node)
├── build.gradle                   inert (Stonecutter repoints buildFileName to stonecutter.gradle)
├── gradle.properties              per-version pins (neo_form / neo_version / forge_version / fabric_api / jei)
├── common/                        net.neoforged.moddev (NeoForm) — shared source (NOT a node; spliced in)
│   └── src/main/java/.../NerospaceCommon, platform/{Services,IPlatformHelper}, registry/
├── fabric/                        net.fabricmc.fabric-loom + fabric.mod.json + platform impl
├── neoforge/                      net.neoforged.moddev + neoforge.mods.toml + platform impl
├── forge/                         net.minecraftforge.gradle + mods.toml + platform impl
└── legacy/                        the retired standalone single-loader build (frozen, not shipped)
```

## Building

Run from the **repo root** with its Gradle 9.5.1 wrapper (Loom 1.17 needs ≥ 9.4):

```bash
# Stonecutter: each loader x MC version is its OWN node — the node path picks the version.
./gradlew :fabric:26.2:build         # Fabric on 26.2
./gradlew :neoforge:26.1.2:build     # NeoForge on 26.1.x
./gradlew :forge:26.2:build          # Forge on 26.2

# ...or all six cells at once:
./gradlew :fabric:26.1.2:build :fabric:26.2:build :neoforge:26.1.2:build :neoforge:26.2:build :forge:26.1.2:build :forge:26.2:build

# The IDE-active version is set in stonecutter.gradle (`stonecutter.active '26.2'`);
# switch it with the generated `stonecutterSwitchTo26_1_2` / `stonecutterSwitchTo26_2` tasks.
# Per-version pins still come from gradle.properties (neo_version_<mc>, forge_version_<mc>, fabric_api_version_<mc>, ...).
```

Jars land in `<loader>/versions/<mc>/build/libs/` (relative to the repo root).

## All six cells build

NeoForge's own loader userdev for 26.2 is on the public Maven as a beta
(`neo_version_26.2=26.2.0.6-beta` is pinned and resolves from the NeoForged
Maven). If a future pin ever fails to resolve, **self-build it**:

```bash
git clone https://github.com/neoforged/NeoForge && cd NeoForge
git checkout 26.2.x
./gradlew :neoforge:publishToMavenLocal      # JDK 25; publishes net.neoforged:neoforge:<ver>
```

The `neoforge` module already has `mavenLocal()` in its repositories, so set
`neo_version_26.2` to the version it printed and build. When the official jar
lands on Maven, it's a **one-line change** (the version pin) — no refactor.

Forge's 26.x userdev artifacts are published on the official Forge Maven:
`forge_version_26.1.2=26.1.2-64.0.10` and `forge_version_26.2=26.2-65.0.0`.
Forge uses ForgeGradle 7 and Gradle 9.5.1.

## CI / VS Code

- **`.github/workflows/multiloader.yml`** — strict loader × version matrix for all
  six cells.
- **`.vscode/`** — `ML: …` tasks (build/run/debug per loader+MC) run the repo-root
  `./gradlew`. `runClient`/`runServer` come from Fabric Loom (`:fabric`) and
  ModDevGradle (`:neoforge`) / ForgeGradle (`:forge`); the `Debug: <cell>` configs
  attach on :5005 (NeoForge/Forge via `-PnerospaceDebug`, Fabric via `--debug-jvm`).

## Scope boundary

This unblocks and documents the **build**. Shared logic goes in `common`;
loader-specific behaviour goes through the `platform/Services` seam (registration
is per-loader — there is no Architectury API `DeferredRegister` here).

## Promotion to the repo root — DONE

The promotion described here was carried out in `post_port.md` Phase 1–2: the
standalone single-loader build was retired to `legacy/` (Phase 1) and the
multiloader was flattened onto the repo root (Phase 2). `tools/` now targets
`common/` directly. The four Stonecutter cells are the source of truth; `legacy/`
is frozen until the port is 120% confirmed.

## Sources

- [architectury-loom #328 — no de-obf 26.x](https://github.com/architectury/architectury-loom/issues/328)
- [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) · [official Fabric example (de-obf)](https://github.com/FabricMC/fabric-example-mod)
- [NeoForm](https://projects.neoforged.net/neoforged/neoform) · [NeoForge](https://projects.neoforged.net/neoforged/neoforge) · [Forge downloads](https://files.minecraftforge.net/) · [Fabric develop](https://fabricmc.net/develop)

## Build matrix status (2026-06-23)

All six loader × version cells build from **public artifacts**, and CI
(`.github/workflows/multiloader.yml`) builds all six strictly (any failure fails the run):

| | 26.1.2 | 26.2 |
| --- | --- | --- |
| **neoforge** | ✅ `26.1.2.76` | ✅ `26.2.0.6-beta` (public Maven) |
| **forge** | ✅ `26.1.2-64.0.10` | ✅ `26.2-65.0.0` |
| **fabric** | ✅ (access widener) | ✅ `fabric-api 0.152.2+26.2` |

Fabric @ 26.1.2 needs `fabric/src/main/resources/nerospace.accesswidener` because vanilla
MC 26.1.2 kept `BlockEntityType`'s constructor + `BlockEntitySupplier` private (Mojang made them
public in 26.2; NeoForge widens them on both). The widener is a no-op on 26.2.
