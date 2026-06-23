# Post-Port Cutover Runbook — retire the standalone root, make `multiloader/` the default

**Status:** historical cutover runbook. It was written when the supported matrix was 4 cells
(NeoForge & Fabric × MC 26.1.2 & 26.2). The current flattened repo-root build supports 6 cells by adding
Forge (`:forge:26.1.2`, `:forge:26.2`); see [`docs/MULTILOADER_BUILD.md`](MULTILOADER_BUILD.md) for the
current build matrix. This document is kept as the record of retiring the old single-loader project and
promoting `multiloader/` to the canonical repo-root build.

It is deliberately **phased and reversible**. Nothing is deleted up front — the old root is **moved into `legacy/`** and kept until you are 120% confident every mechanic is ported (see the parity checklist at the end). Run each phase on a branch, verify, and only proceed when green.

> Run everything below from the repo root: `C:\Users\dario\Documents\projects\github\nerospace`
> Use `git mv` (not plain `mv`) so file history follows the move.

---

## 0. Pre-flight

```bash
git checkout main
git pull
git switch -c retire-standalone-root      # the cutover branch
git status                                 # must be clean before starting
```

Record the current green baseline so you can compare after each phase:

```bash
cd multiloader
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :fabric:26.1.2:build :fabric:26.2:build
cd ..
```

All four must say `BUILD SUCCESSFUL`. (This is the state the port is in right now.)

---

## What stays vs. what moves

The repo is two independent things sharing one folder:

| Category | Examples | Action |
|---|---|---|
| **Standalone root build** (the old single-loader mod) | `src/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/`, `nerospace.code-workspace`, `.eclipse/`, `.settings/`, `bin/` | → **`legacy/`** (Phase 1) |
| **The cross-loader build** | `multiloader/` (common + neoforge + fabric, its own `gradlew`/`settings.gradle`/`stonecutter.gradle`) | stays now; **promoted to root** in Phase 2 |
| **Shared tooling & assets** (used by BOTH builds) | `tools/` (gen_textures / gen_bbmodels / model_sync / ecj.prefs / gradle-mcp), `art/blockbench/`, `docs/`, `wiki/` | **stays at root — do NOT move** |
| **Repo metadata & docs** | `LICENSE`, `README.md`, `CLAUDE.md`, `CHANGELOG.md`, `PRIVACY.md`, `PROJECT_PLAN.md`, `RELEASE_CHECKLIST.md`, `AGENTS.md`, `ALIEN_VILLAGERS_*.md`, `.gitignore`, `.gitattributes`, `.markdownlint.json`, `.github/` | **stays** (some get edited) |
| **Generated / ignored** | `build/`, `run/`, `.gradle/`, `temp/` | not tracked — leave or `rm -rf`; don't bother moving |

> Why `tools/` must stay at the repo root: the multiloader references it as `../tools` —
> `multiloader/neoforge/build.gradle` → `rootProject.file('../tools/ecj.prefs')`, and
> `multiloader/stonecutter.gradle` runs `tools/model_sync.py` / `tools/gen_textures.py` / `tools/gen_bbmodels.py`
> from the repo root. Moving `tools/` breaks ecjCheck and the asset/model generators.

---

## Phase 1 — Retire the standalone root into `legacy/` (low-risk, reversible)

This is the core step. After it, the repo root no longer has a competing Gradle project, and `multiloader/` is the only buildable mod. The multiloader is a **separate Gradle build**, so moving the root build files does **not** affect it, and `../tools` still resolves (tools stays put).

```bash
mkdir legacy

# Old single-loader source + build scripts
git mv src                    legacy/src
git mv build.gradle           legacy/build.gradle
git mv settings.gradle        legacy/settings.gradle
git mv gradle.properties      legacy/gradle.properties

# Root Gradle wrapper (the multiloader has its own under multiloader/)
git mv gradlew                legacy/gradlew
git mv gradlew.bat            legacy/gradlew.bat
git mv gradle                 legacy/gradle

# Root-only IDE project files (the standalone Eclipse/VSCode setup)
git mv nerospace.code-workspace legacy/nerospace.code-workspace

# These may or may not be tracked — move only the ones git knows about:
git ls-files .eclipse .settings bin | sed 's#/.*##' | sort -u   # see what's tracked
# then for each tracked dir, e.g.:  git mv .eclipse legacy/.eclipse   (skip if "fatal: not under version control")
```

Catch any stragglers — anything still tracked at the root that belongs to the old build:

```bash
git ls-files --directory -- ':(top)*' | grep -vE '^(legacy|multiloader|tools|art|docs|wiki|\.github)/' | grep -vE '^[A-Z_]+\.md$|^LICENSE$|^\.git|^\.markdownlint|^nerospace-multiloader\.code-workspace$'
```

Add a `legacy/README.md` so future-you knows why it exists:

```bash
cat > legacy/README.md <<'EOF'
# legacy/ — the original standalone NeoForge 26.1.2 single-loader project

Frozen reference, kept until the cross-loader port in the repo root is 120% confirmed
complete (see ../post_port.md parity checklist). NOT built or shipped. Delete once signed off.
EOF
git add legacy/README.md
```

**Ignore the old build outputs** so a stray `legacy` build can't dirty the tree — append to `.gitignore`:

```
# retired standalone root
legacy/build/
legacy/.gradle/
legacy/bin/
legacy/run/
```

### Verify Phase 1

```bash
cd multiloader && ./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :fabric:26.1.2:build :fabric:26.2:build && cd ..
```

Still 4× `BUILD SUCCESSFUL` (nothing the multiloader needs was moved). CI is unaffected — `multiloader.yml` and `publish.yml` already `cd multiloader`. Commit:

```bash
git add -A && git commit -m "chore: retire standalone single-loader root into legacy/"
```

**You can stop here and live in this state indefinitely** — the multiloader is already the de-facto project (build it from `multiloader/`). Phase 2 is cosmetic/ergonomic (so `./gradlew` works from the repo root). Defer it until you're comfortable.

---

## Phase 2 — Promote `multiloader/` to the repo root (optional, higher-touch)

Goal: drop the nested `multiloader/` directory so the repo root *is* the mod (`./gradlew :neoforge:26.1.2:build` from the top). This touches path references, so do it as its own commit and re-verify.

### 2a. Move the multiloader contents up

```bash
# from repo root, with Phase 1 committed
git mv multiloader/common           common
git mv multiloader/neoforge         neoforge
git mv multiloader/fabric           fabric
git mv multiloader/settings.gradle  settings.gradle
git mv multiloader/build.gradle     build.gradle
git mv multiloader/stonecutter.gradle stonecutter.gradle
git mv multiloader/gradle.properties  gradle.properties
git mv multiloader/gradlew          gradlew
git mv multiloader/gradlew.bat      gradlew.bat
git mv multiloader/gradle           gradle
# bring over any remaining tracked files (.vscode, .settings, versions/, etc.):
git ls-files multiloader | sed 's#multiloader/##'      # review the remainder, git mv each
rmdir multiloader 2>/dev/null || true
```

### 2b. Fix the path references that assumed the nested layout

These are the **only** code-level edits Phase 2 needs. `rootProject` is now the repo root, and `tools/` is now a sibling (not `../tools`):

1. **`neoforge/build.gradle`** — the ecjCheck `-properties` path:
   - `rootProject.file('../tools/ecj.prefs')` → `rootProject.file('tools/ecj.prefs')`
2. **`stonecutter.gradle`** — the asset/model-sync tasks compute the repo root as `multiloader/..`. Now the repo root *is* the project root. Update the `repoRoot` derivation (currently the multiloader's parent) to `rootDir` (or `rootProject.projectDir`), so `tools/model_sync.py`, `tools/gen_textures.py`, `tools/gen_bbmodels.py` still resolve. Grep for `repoRoot`, `..`, and `tools/` in this file and adjust each.
3. **`settings.gradle`** — optional: rename the project for cleanliness:
   - `rootProject.name = 'nerospace-multiloader'` → `rootProject.name = 'nerospace'`
4. **Per-node Eclipse classpaths / `.code-workspace`** — `nerospace-multiloader.code-workspace` and the generated `.classpath`/`launch.json` reference `multiloader/...` paths. Regenerate them: `./gradlew eclipse` (per the multiloader IDE note) and update the workspace file's folder paths from `multiloader/<x>` to `<x>`.

### 2c. Update CI to the flattened layout

In **`.github/workflows/multiloader.yml`** and **`.github/workflows/publish.yml`**, remove the `multiloader` nesting:
- `working-directory: multiloader` → delete the line (run from repo root)
- `chmod +x ./multiloader/gradlew` → `chmod +x ./gradlew`
- task invocations stay the same (`:neoforge:26.1.2:build` etc.)

Delete or archive **`.github/workflows/build.yml`** (the root single-loader CI — already disabled):
```bash
git rm .github/workflows/build.yml
```

### 2d. Point the local Gradle MCP at the new root

The `tools/gradle-mcp` server uses `GRADLE_PROJECT_DIR` (or per-call `project_dir`). After promotion, the project dir is the repo root:
- set `GRADLE_PROJECT_DIR=C:\Users\dario\Documents\projects\github\nerospace` (was `…\nerospace\multiloader`), **or** just pass `project_dir` = repo root per call.

### Verify Phase 2

```bash
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :fabric:26.1.2:build :fabric:26.2:build   # now from repo root
./gradlew ecjCheck
```

4× `BUILD SUCCESSFUL` + ecjCheck `0 errors`. Then commit:
```bash
git add -A && git commit -m "chore: promote multiloader to repo root; fix tools/CI/MCP paths"
```

---

## 3. Documentation & metadata updates (do alongside Phase 1/2)

- **`CLAUDE.md`** — update the build commands and the "BUILDING FROM AN AGENT" / gradle-MCP notes to the new location (drop `multiloader/` once Phase 2 lands). Remove "building STANDALONE" framing; the multiloader IS the project now.
- **`README.md`** — point setup/build instructions at the canonical build; mention 4-cell matrix (NeoForge/Fabric × 26.1.2/26.2).
- **`RELEASE_CHECKLIST.md`** / **`PROJECT_PLAN.md`** — update paths and "single-loader" references.
- **`gradle.properties`** — reconcile `mod_version` (root was `1.0.0-alpha.1`; multiloader is `1.0.0-alpha.3`). Keep the multiloader's (newer) as canonical.
- **`.vscode/`** — repoint to the promoted layout (or use the regenerated `.code-workspace`).

---

## 4. Verification gate (before merging the cutover branch)

- [ ] `./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :fabric:26.1.2:build :fabric:26.2:build` → 4× `BUILD SUCCESSFUL`
- [ ] `./gradlew ecjCheck` → 0 errors (warnings unchanged from baseline)
- [ ] `./gradlew :neoforge:26.1.2:runClient` (and one Fabric cell) — world loads, creative tabs populate, no missing-texture placeholders, no red error spam
- [ ] CI green on the branch (multiloader matrix workflow)
- [ ] `legacy/` is present and untouched; nothing the multiloader needs was moved into it

---

## 5. Rollback

Everything is `git mv` on a branch, so rollback is trivial:
```bash
git switch main          # abandon the branch, repo untouched
# or, mid-phase:
git restore --staged . && git checkout -- . && git clean -fd
```
If a phase merged and misbehaves, `git revert <merge>` restores the prior layout (history preserved because moves were `git mv`).

---

## 6. Final cleanup — only when 120% confident (delete `legacy/`)

Do **not** delete `legacy/` until the parity checklist below is fully signed off and the mod has been playtested on both loaders. Then:
```bash
git rm -r legacy
git commit -m "chore: drop legacy standalone root — port confirmed complete"
```

---

## Port parity status (basis for the legacy-deletion decision)

A five-domain audit (registries, assets/data, machine logistics, rockets/progression, world/mobs) was run root-vs-multiloader. Summary:

### ✅ Confirmed at parity
Blocks (53), items, block entities (26), entity types (12), menus (superset), fluids, sounds, data components, attachments (all on both loaders), dimensions (Greenxertz/Cindara/Glacira/Station), biomes, ores, structures/features, meteors, terraforming, oxygen-field, village core (full controller), commands, telemetry (POPIA/GDPR scrubbing intact), Star Guide, stations, alien gear, space-suit set, all machine GUIs, the universal-pipe network (all 4 layers).

### ✅ Gaps found and ported in this pass (now in the multiloader)
- **17 crafting recipes** that were registered-but-uncraftable (configurator, pipe_filter, 4 grinder modules, 2 machine upgrades, hydration_module, terraformer, terraform_monitor, star_guide, star_guide_book, station_charter, solar_panel ×3).
- **3 entity loot tables** (woolly_drift, meadow_loper, ember_strutter) — the livestock dropped nothing.
- **2 food items** (`loper_haunch`, `strutter_drumstick`) + the **`meteor_core` block-item** + their models/textures/lang.
- **76 lang keys** (machine/pipe readouts, generator/rocket/oxygen GUI strings, all mob subtitles) — were showing raw ids.
- **8 datapack tags** (c: aggregate parents `#c:ingots/#c:ores/#c:gems/#c:dusts/#c:raw_materials/#c:storage_blocks`, plus `oxygen_sealing`/`oxygen_leaks`/`oxygen_source`/`alien_materials`).
- **Overworld nerosteel ore** generation (placed-feature + NeoForge biome_modifier + Fabric injection) — closes a circular progression gate.
- **Comparator output** restored on Combustion/Passive/Oxygen generators.
- **Oxygen Generator redstone on/off gate**.
- **Station Core overwrite guard** in rocket docking (was a latent data-loss bug).
- **Alien Villager per-planet skin** (Cindara/Glacira were stubbed to Greenxertz).
- **Pipe Capacity upgrade** now actually enlarges pipe buffers (was inert).
- **Quarry fluid auto-eject** (no longer dead-pauses next to a tank).
- **Trash Can gas void-sink** + GAS capability on both loaders.
- **Creative Fluid Tank selectable source** (right-click bucket to set the fluid).
- **`fuelCostMultiplier`** now wired into machine running costs (refinery/grinder/terraformer/oxygen-gen/airlock), not just rocket launch.
- **Right-click readouts** on Fluid Tank / Battery / Gas Tank.

### ⚠️ Intentional divergences / open decisions (confirm before deleting `legacy/`)
These are **value/scope choices**, not lost code. The first three were **RESOLVED 2026-06-23** (see below); the rest remain pending your call:
- **Machine balance drift** vs root base values (Combustion 60→20 FE/t, Passive 10→8, Oxygen Generator 2→20 FE/mB, Battery 200k→1M cap, Grinder 30→20): **KEPT as the deliberate final balance** (Dario, 2026-06-23) — an intentional retune, not drift. No gameplay change.
- **Oxygen survival numerics** (bare drain 20→30 / suit drain 1→3 per check; suit air tank flat 900 vs root's tiered 300/600; airlock refill rate): **KEPT** (same call, 2026-06-23).
- **Bespoke O2 / hazard-shield HUD**: **PORTED cross-loader (2026-06-23).** `common/.../client/OxygenHud.java` is drawn via a new cross-loader graphical-HUD seam — NeoForge `RegisterGuiLayersEvent` (a `GuiLayer`) + Fabric `HudElementRegistry.addLast` (a `HudElement`), both taking the vanilla `GuiGraphicsExtractor`; the vanilla air bubbles are suppressed on airless dims (NeoForge `RenderGuiLayerEvent.Pre` cancel `VanillaGuiLayers.AIR_LEVEL` / Fabric `replaceElement(VanillaHudElements.AIR_BAR, …)`). Surfaces O₂ %, suit tier, and the hazard-shield / uncountered-hazard warning. All 4 cells green; **a client run is still needed to confirm the on-screen visual** (can't be runtime-tested from the agent). NB: 26.x removed `HudRenderCallback`/`LayeredDraw` — the seam targets the new `net.fabricmc.fabric.api.client.rendering.v1.hud.*` API, javap-verified identical on all four cells.
- **Greenxertz decoration step**: the 3 structures sit in the `underground_ores` step rather than `surface_structures` (they still generate; ordering only).
- **Config breadth**: the multiloader exposes ~6 tuning multipliers; the root had ~40 absolute config keys. Documented scope cut — expand if modpack tuning needs them.
- **Telemetry opt-out** is restart-only in the multiloader (root toggled live on config reload). Scrubbing/compliance unchanged.
- **Gametests** (root `src/gametest`) were not ported — test scaffolding, not shipped mechanics. Re-add if you want CI gametest coverage.
- **Pipe item transport** is instant buffer-pooling vs the root's segment-by-segment travelling-item packets (cosmetic travelling-item visuals still play on extract).

When the ⚠️ items are decided and the mod has been playtested on both loaders, sign off and run §6.
