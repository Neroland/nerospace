# Nerospace 2.4.0-beta.1 — handover

State on 2026-11-21. **All six asks are built and gated.** `mod_version` is `2.4.0-beta.1`; the changelog
entry sits under `[Unreleased]`. No agent committed, tagged or published anything; every change since
2.3.1 is yours to review and commit.

Companion documents:

- `docs/SMOKE-2.4.0.md` — what was tested headlessly, and your in-game checklist.
- `docs/decisions/2.4.0-compliance.md` — the POPIA/GDPR review.
- `docs/decisions/2.4.0-survey.md`, `2.4.0-seams.md`, `2.4.0-stage1-status.md` — stages 0 to 3.
- `docs/decisions/2.4.0-europa.md` (design), `2.4.0-europa-review.md` (design review),
  `2.4.0-europa-amendments.md` (the fixes that made it buildable; authoritative where they differ).
- `docs/decisions/2.4.0-europa-build-status.md` — what each of the six Europa work packages built, with
  every deviation.
- `docs/decisions/2.4.0-europa-verify-*.md` — one independent verification per package.

## Where each ask stands

| Ask | Decision | State |
| --- | --- | --- |
| 1. Space looks like Earth with shaders | D5 to D8 | Built and unit-tested. Not run with a shader mod. |
| 2. Earth, Moon, Mars visible in space; square bodies | D1 to D4 | Built for the nine 26.x cells. Not seen in a render client. 1.21.x keeps the colour floor. |
| 3. Shader clouds on the Moon | D6 | Same code as ask 1. |
| 4. Space higher up | D11 to D14 | Built and tested: `rockets.ascentHeight`, default 96, range 24 to 256. |
| 5. Launch flames | D15, D16 | Ignition burst built and tested. No render-type change (decision 6 below). |
| 6. Europa | D17 to D25 | Built in six work packages, each verified by a separate agent. Not run in game. |

## Gate results

Final gate, run 2026-11-21 after the last change (WP6):

- All 27 `:<loader>:<mc>:build` tasks green with `--rerun-tasks`, with 884 unit tests in 211 classes on
  each of the nine NeoForge nodes, 0 failed.
- `ecjCheck`: 0 errors on the five nodes checked (`:neoforge:26.3`, `:fabric:26.3`, `:forge:26.3`,
  `:neoforge:26.2`, `:neoforge:1.21.1`). A bare `ecjCheck` over every node fails on the fifteen 1.21.4 to
  1.21.11 nodes with unresolved EMI imports in `compat/emi/*`, which no 2.4.0 work touched.
- `:fabric:26.3:compileGametestJava` passes.
- All 2,436 JSON files parse. Every generator `--check` is clean except `sky/earth_disk_globe.png`, which a
  Linux shell has always reported stale.
- `markdown_check` clean on every wiki page touched.
- No dedicated server or render client was started. See `docs/SMOKE-2.4.0.md`.

## How the work was checked

Each Europa work package was built by one agent and then read by a second agent that had not written it.
Their findings, all fixed before the next package unless marked accepted:

| Package | Blocker | Major | Notable fixes |
| --- | --- | --- | --- |
| Design | 0 | 8 | Resolved in `2.4.0-europa-amendments.md` before any code. |
| WP1 + WP2 | 0 | 0 | Registration order, a salt-yield cap, lava counted as liquid, a chunk-edge load. |
| WP3 | 1 | 0 | A 26.3 feature file that would have stopped every world loading on 26.3; the test that missed it; the Cryo + Rad card line. |
| WP4 | 0 | 2 | The terraformed sea would have frozen through vanilla's height-adjusted freeze; the Vent Ray lost its vent. |
| WP5 | 0 | 0 | The villager-planet fallback; Star Guide chapters 10 to 16 now frozen step by step. |
| WP6 + release scan | 0 | 0 | Sky codec unchanged for every existing profile; 21 wiki claims checked against code; no hidden-content leak on the wiki. |

## Sky coverage (D2)

| Cells | Hook | Checked how |
| --- | --- | --- |
| NeoForge 26.3 | `CustomSkyboxRenderer` adapter over the shared `PlanetSkyDraw` | compiled; signature from sources |
| Fabric 26.3, Forge 26.3 | mixin at the head of `SkyRenderer.render` | compiled; descriptor from bytecode and sources |
| Fabric, Forge, NeoForge on 26.1.2 and 26.2 (six cells) | mixin: the first sky call draws, the other four are cancelled for that pass | compiled; descriptors and call order from bytecode and sources |
| All eighteen 1.21.x cells | none; dimension colours as before | compiled |

Why 1.21.x stays on the colour floor: up to 1.21.4 the game draws in immediate mode; 1.21.5 has a render
pass without dynamic uniforms; 1.21.8 to 1.21.11 use a third variant. Each would be its own GPU backend
and none can be checked without a render client.

NeoForge 26.1.2 and 26.2 use the mixin and no dimension-type overlays, because the attribute id for the
custom skybox could not be confirmed on those versions and a wrong id breaks the whole dimension type.

## Decisions for the owner

1. **Planets under shader packs.** Mapping a world to the pack's End look removes the blue sky and the
   pack's clouds. The mainstream packs read at source level flatten or hide textured sky geometry in their
   End programs, so Earth, the Moon and Jupiter may not show under them. Shipped as designed and
   documented in `wiki/Shaders.md`. Alternatives are in `docs/decisions/2.4.0-survey.md`, section 3.5.
2. **Cindara and Glacira** use the pack's End look too: the owner chose it for both on 2026-11-21. They could
   not be viewed under a pack. Greenxertz is the only world left on the Overworld look.
3. **`sky.bodyStyle` default** is `globe` (owner's choice, 2026-11-21); `square` is the opt-in. In `square` style the sun's aureole glow still draws; only the
   limb glow is blanked. Since 2026-11-21 the square variants are flat renders at each globe texture's own
   resolution (same palette and surface, no limb darkening, hard square edge) instead of 16×16-cell squares.
4. **Ascent default** is 96 blocks, about 8 seconds.
5. **Oxygen for a launch.** Resolved 2026-11-21: the per-seat oxygen budget now uses the real ascent,
   `AscentMath.duration(rockets.ascentHeight)` plus the descent and settle (`Crew.flightTicks`). At
   `rockets.ascentHeight = 24` it is the 2.3.1 figure exactly (168 ticks, 34 mB a seat at the ride drain);
   at the default 96 it is 227 ticks (46 mB), at 256 it is 288 ticks (58 mB). `Crew.FLIGHT_TICKS` stays as
   the 24-block baseline.
6. **Launch flames under shaders (D15).** The plume and lamps already use render types that Iris routes
   as emissive entity geometry on every supported version, so nothing was changed. If the plume is
   missing under a specific pack in your check, that is the case to fix.
7. **One loader-neutral sky mixin** (`nerospace-sky.mixins.json`) is used instead of one per loader. A
   guard keeps NeoForge 26.3 from drawing twice. The 26.1.2 resource overlay now also carries a mixin
   config, which is a new use of that overlay.
8. **Mars shows Earth as a night-gated decal**, not a body, because bodies have no night gate.
9. **Europa's decisions taken during the build** (all in the amendments and the build status):
   - Surface band `FRIGID` (Rad-Lined Cryo Suit) with a milder band under the ice (any Tier 2 suit).
   - Terraforming ends in an open sea on top of the ice; the air never becomes breathable.
   - Every Europa block has a survival source and a recipe use. The europium easter egg stays decorative.
   - Two weather events on one world through a small additive `secondaryWeather` change; no new
     `WeatherEvent` constant (the plume fall reuses Diamond Dust, the radiation storm reuses Flare, so the
     HUD shows those names).
   - Ice fractures are their own worldgen feature, so the layer bounds are exact and testable.
   - The map layer is a proximity "Habitats" layer (village cores within 64 blocks; nothing stored).
   - Jupiter drifts through 32 baked frames. Removing the `rotation` object from `sky/europa.json` brings
     back a still Jupiter.
   - Smaller deviations a player would notice are on `wiki/Europa.md`; the full list is in the build status.
10. **Europa terrain was never tuned in a client.** The layer bounds are exact by construction, but how the
    surface, shell and ocean look and how often features appear needs your eye.

## Not verified (needs a render client)

- That the sky mixin applies and draws correctly on any Fabric or Forge cell and on NeoForge 26.1.2 and
  26.2, and that NeoForge 26.3 looks the same through the adapter.
- On 26.1.2 and 26.2 the shared path uses the camera's rotation matrix and the main render target rather
  than the pass's own model-view stack and frame-graph target.
- The square style, the Moon over the station, Earth over Mars.
- Anything with a shader mod installed: that the game starts on each supported Iris version, the End
  mapping, and the plume.
- The ascent at 24, 96 and 256 blocks on a dedicated server, and the ignition burst.
- **Europa, all of it in play:** that the dimension loads on each cell, how the terrain and features look
  and how often they appear, hazards by layer, the two weather events, the creatures' behaviour, the
  Brinekin habitats and trades, terraforming to an open sea, Jupiter's drift and the noon eclipse, the
  Star Guide chapter, advancements, quests, the compass and the Habitats map layer. Everything that can be
  checked without a game is covered by unit tests and data checks.

## Residual risks

- The shader support hooks one internal Iris method. If it moves, the support switches itself off and logs
  one line; the game still starts.
- Packs that hide sky geometry by design (see decision 1).
- Worldgen errors only show when a world loads. The verifiers cross-checked every Europa worldgen, loot,
  advancement and quest file against vanilla or existing files of the same type and caught one such error;
  a first load on each Minecraft line is still the real test.
- `tools/gen_sky_textures.py --check` reports `sky/earth_disk_globe.png` stale in a Linux shell. The file
  was not touched by this work.

## Privacy

No new store and no new per-player record. Europa adds values to records that already exist (planet visits,
advancements, gates, Star Guide steps, reputation with the Brinekin). The new Habitats map layer shows only
village cores within 64 blocks of the viewer and stores nothing; `PRIVACY.md` gained one paragraph saying
so. Log lines added in 2.4.0 carry fixed strings only; a scrub test pins them. The review is
`docs/decisions/2.4.0-compliance.md`.

## Owner to-dos

- Run the in-game checklist in `docs/SMOKE-2.4.0.md`, including the shader pack matrix and the Europa
  section.
- Move the changelog entry from `[Unreleased]` to a dated `2.4.0-beta.1` heading when you release.
- Delete by hand (the agents' mount cannot delete): `temp/europa_wp1_preview.png` and
  `build/skyjavap.init.gradle`. Optionally the older helpers in `build/`: `checkLang`, `gitconflicts`,
  `javapKeys`, `javapSky`, `javapSleep` (`.init.gradle`) and `maxerrs.gradle`.
- Commit, tag and publish when ready.

## Draft reply to the commenter

> Thanks for coming back with more detail, it helped a lot. In the next beta:
>
> - With shaders on, the Moon, Mars, the station, Nyxara and Europa now ask your shader pack for its dark
>   "End" look, so space is no longer blue and the pack's clouds are gone from the Moon. This works through
>   Iris.
> - Earth, the Moon and the other bodies now show in the sky on Fabric, Forge and NeoForge for 26.1.2,
>   26.2 and 26.3. If you prefer square, vanilla-style suns and planets, set `sky.bodyStyle = square`:
>   they keep the same detail as the round globes.
>   One honest limit: some shader packs replace the whole End sky with their own effect and hide what
>   mods draw there. The new Shaders wiki page explains the settings to try.
> - Rockets now climb four times higher before the jump to space, and server owners can raise it further
>   with `rockets.ascentHeight` (up to 256 blocks). A real 20,000-block climb is not possible: Minecraft
>   worlds are only a few hundred blocks tall, so the climb is the part you see and the rest is the
>   transfer.
> - Launches now kick out a burst of flame and smoke at lift-off, on top of the exhaust plume.
> - Europa is in: a Tier 4 ice moon under a giant Jupiter, with a cracked ice surface, a thick ice shell
>   and a dark ocean with hydrothermal vents underneath. It has its own creatures, the Brinekin villagers,
>   new materials for your tech, and terraforming that opens a sea on the ice.
