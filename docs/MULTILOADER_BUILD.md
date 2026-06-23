# Nerospace cross-loader build (MultiLoader-Template, flattened to the repo root)

Builds Nerospace on **both NeoForge and Fabric** from one shared codebase, on the
**de-obfuscated Minecraft 26.x** toolchain.

> **This build IS the repo root.** Per `post_port.md` Phase 2 the multiloader was
> flattened onto the repo root; the retired standalone single-loader build now
> lives under `legacy/` (frozen, not built or shipped). Full history:
> [`docs/MULTILOADER.md`](../docs/MULTILOADER.md) and [`post_port.md`](../post_port.md).

## Status (verified 2026-06-20)

Built via the gradle MCP on this machine:

| Cell | Toolchain | 26.2 | 26.1.2 |
| --- | --- | --- | --- |
| `common` | ModDevGradle (NeoForm) | ✅ builds (`26.2-1`) | NeoForm `26.1.2-1` |
| `fabric` | Fabric Loom `1.17.11` | ✅ **builds** (`fabric-api 0.152.1+26.2`) | ✅ builds (`fabric-api 0.151.0+26.1.2`; needs access widener — see below) |
| `neoforge` | ModDevGradle (NeoForge) | ✅ builds (`26.2.0.6-beta`, on public NeoForged Maven) | NeoForge `26.1.2.76` |

`./gradlew :fabric:26.2:build :neoforge:26.2:build` → **BUILD SUCCESSFUL** (all four cells green via Stonecutter nodes).

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
<repo root>/
├── gradlew(.bat) + gradle/        the wrapper, Gradle 9.5.1 (Loom 1.17 needs >= 9.4)
├── settings.gradle                plugin versions + Stonecutter version tree (fabric/neoforge × 26.1.2/26.2)
├── stonecutter.gradle             the REAL root build script (splices common source into each node)
├── build.gradle                   inert (Stonecutter repoints buildFileName to stonecutter.gradle)
├── gradle.properties              per-version pins (neo_form / neo_version / fabric_api / jei)
├── common/                        net.neoforged.moddev (NeoForm) — shared source (NOT a node; spliced in)
│   └── src/main/java/.../NerospaceCommon, platform/{Services,IPlatformHelper}, registry/
├── fabric/                        net.fabricmc.fabric-loom + fabric.mod.json + platform impl
├── neoforge/                      net.neoforged.moddev + neoforge.mods.toml + platform impl
└── legacy/                        the retired standalone single-loader build (frozen, not shipped)
```

## Building

Run from the **repo root** with its Gradle 9.5.1 wrapper (Loom 1.17 needs ≥ 9.4):

```bash
# Stonecutter: each loader x MC version is its OWN node — the node path picks the version.
./gradlew :fabric:26.2:build         # Fabric on 26.2
./gradlew :neoforge:26.1.2:build     # NeoForge on 26.1.x

# ...or all four cells at once:
./gradlew :fabric:26.1.2:build :fabric:26.2:build :neoforge:26.1.2:build :neoforge:26.2:build

# The IDE-active version is set in stonecutter.gradle (`stonecutter.active '26.2'`);
# switch it with the generated `stonecutterSwitchTo26_1_2` / `stonecutterSwitchTo26_2` tasks.
# Per-version pins still come from gradle.properties (neo_version_<mc>, fabric_api_version_<mc>, ...).
```

Jars land in `<loader>/versions/<mc>/build/libs/` (relative to the repo root).

## All four cells build (was: NeoForge 26.2 pending)

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

## CI / VS Code

- **`.github/workflows/multiloader.yml`** — loader × version matrix; `fabric @ 26.2`
  is marked non-experimental (verified), the rest stay `continue-on-error` until
  their pins/artifacts are confirmed.
- **`.vscode/`** — `ML: …` tasks (build/run/debug per loader+MC) run the repo-root
  `./gradlew`. `runClient`/`runServer` come from Fabric Loom (`:fabric`) and
  ModDevGradle (`:neoforge`); the `Debug: <cell>` configs attach on :5005 (NeoForge
  via `-PnerospaceDebug`, Fabric via `--debug-jvm` — ModDevGradle ignores `--debug-jvm`).

## Scope boundary

This unblocks the **build**. It does not port Nerospace's NeoForge-specific
systems (capabilities/transfer, attachments, fluids, networking) to Fabric — that
migration is the real effort and is tracked in
[`docs/MULTILOADER.md`](../docs/MULTILOADER.md) §2. Shared logic goes in `common`;
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
- [NeoForm](https://projects.neoforged.net/neoforged/neoform) · [NeoForge](https://projects.neoforged.net/neoforged/neoforge) · [Fabric develop](https://fabricmc.net/develop)

## Build matrix status (2026-06-20)

All four loader × version cells build from **public artifacts** (verified via the gradle MCP),
and CI (`.github/workflows/multiloader.yml`) builds all four strictly (any failure fails the run):

| | 26.1.2 | 26.2 |
| --- | --- | --- |
| **neoforge** | ✅ `26.1.2.76` | ✅ `26.2.0.6-beta` (public Maven) |
| **fabric** | ✅ (access widener) | ✅ `fabric-api 0.152.1+26.2` |

Fabric @ 26.1.2 needs `fabric/src/main/resources/nerospace.accesswidener` because vanilla
MC 26.1.2 kept `BlockEntityType`'s constructor + `BlockEntitySupplier` private (Mojang made them
public in 26.2; NeoForge widens them on both). The widener is a no-op on 26.2.
