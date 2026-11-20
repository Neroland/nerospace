# Nerospace 2.4.0-beta.1 — handover

State on 2026-11-20. **Nothing is committed, tagged or published.** `mod_version` is `2.4.0-beta.1`; the
changelog entry sits under `[Unreleased]`. Five of the six asks are built and gated. **Europa is not in
this build**: it is designed and independently reviewed, and the review says the design must change before
it can be built.

Companion documents: `docs/decisions/2.4.0-survey.md` (Stage 0), `docs/decisions/2.4.0-seams.md`,
`docs/decisions/2.4.0-stage1-status.md`, `docs/decisions/2.4.0-compliance.md` (Stage 6),
`docs/SMOKE-2.4.0.md` (Stage 7 record and your in-game checklist), `docs/decisions/2.4.0-europa.md` and
`docs/decisions/2.4.0-europa-review.md`.

## Where each ask stands

| Ask | Decision | State |
| --- | --- | --- |
| 1. Space looks like Earth with shaders | D5 to D8 | Built, compiled on all 27 cells, unit-tested. Not run with a shader mod. |
| 2. Earth, Moon, Mars visible in space; square bodies | D1 to D4 | Built for the nine 26.x cells, compiled, unit-tested. Not seen in a render client. 1.21.x keeps the colour floor. |
| 3. Shader clouds on the Moon | D6 | Same code as ask 1. |
| 4. Space higher up | D11 to D14 | Built and tested: `rockets.ascentHeight`, default 96, range 24 to 256. |
| 5. Launch flames | D15, D16 | Ignition burst built and tested. No render-type change (see below). |
| 6. Europa | D17 to D25 | Design: `docs/decisions/2.4.0-europa.md`. Independent review: `docs/decisions/2.4.0-europa-review.md`, **8 open MAJOR**. Not built. |

## Gate results

Final gate, run 2026-11-20 on the tree as handed over:

- All 27 `:<loader>:<mc>:build` tasks green with `--rerun-tasks` (three batches of nine, every task
  executed), including the unit tests on all nine NeoForge nodes.
- `ecjCheck`: 0 errors on the nine 26.x nodes and the three 1.21.1 nodes; no warnings in the new files.
- `ecjCheck` was **not** run on the fifteen 1.21.4 to 1.21.11 nodes in the final gate. An earlier run there
  failed with unresolved EMI imports in `compat/emi/*`. Those lines have no EMI at all and none of that
  code was touched, so this looks older than 2.4.0, but it was not run on a clean 2.3.1 tree to prove it.
- Every touched JSON parses. `tools/gen_sky_overlays.py --check` passes.
- No dedicated server or render client was started. See `docs/SMOKE-2.4.0.md`.

What the definition of done still lacks: Europa (ask 6), and the recorded runtime checks.

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
2. **Cindara and Glacira** stay on the pack's overworld look. They could not be viewed under a pack.
3. **`sky.bodyStyle` default** is `globe`. In `square` style the sun's aureole glow still draws; only the
   limb glow is blanked.
4. **Ascent default** is 96 blocks, about 8 seconds.
5. **Oxygen for a launch.** `Crew.FLIGHT_TICKS` still budgets a 100-tick ascent. A 96-block climb lasts
   159 ticks, so the per-seat oxygen requirement is slightly short of the real flight. Crew rules were
   left unchanged as the brief says.
6. **Launch flames under shaders (D15).** The plume and lamps already use render types that Iris routes
   as emissive entity geometry on every supported version, so nothing was changed. If the plume is
   missing under a specific pack in your check, that is the case to fix.
7. **One loader-neutral sky mixin** (`nerospace-sky.mixins.json`) is used instead of one per loader. A
   guard keeps NeoForge 26.3 from drawing twice. The 26.1.2 resource overlay now also carries a mixin
   config, which is a new use of that overlay.
8. **Mars shows Earth as a night-gated decal**, not a body, because bodies have no night gate.
9. **Europa.** The design cannot be built as written. The review's eight MAJOR findings need design
   changes first, and three of them are your call: the temperature band (`FRIGID` with a milder biome tag,
   instead of `COLD`), whether terraformed Europa is breathable, and whether every block must have a
   crafting recipe. The world generation also needs tuning in a dev client, which the build agent does
   not have.

## Not verified (needs a render client)

- That the sky mixin applies and draws correctly on any Fabric or Forge cell and on NeoForge 26.1.2 and
  26.2, and that NeoForge 26.3 looks the same through the adapter.
- On 26.1.2 and 26.2 the shared path uses the camera's rotation matrix and the main render target rather
  than the pass's own model-view stack and frame-graph target.
- The square style, the Moon over the station, Earth over Mars.
- Anything with a shader mod installed: that the game starts on each supported Iris version, the End
  mapping, and the plume.
- The ascent at 24, 96 and 256 blocks on a dedicated server, and the ignition burst.

## Residual risks

- The shader support hooks one internal Iris method. If it moves, the support switches itself off and logs
  one line; the game still starts.
- Packs that hide sky geometry by design (see decision 1).
- `tools/gen_sky_textures.py --check` reports `sky/earth_disk_globe.png` stale in a Linux shell. The file
  was not touched by this work.

## Privacy

No new personal data. The new log lines carry fixed strings only: no player names, UUIDs, positions,
shader-pack names or file paths. No new saved player record, no change to the link module or to
`PRIVACY.md`. The written review is `docs/decisions/2.4.0-compliance.md` (no open MAJOR); a scrub test
pins the log lines.

## Europa: what exists and what it needs

- `docs/decisions/2.4.0-europa.md`: the full design (planet model, 17-row block matrix, ores, biomes,
  hazards, weather, sky profile, creatures, culture, terraform table, easter egg, wiring).
- `docs/decisions/2.4.0-europa-review.md`: independent review. **8 MAJOR, 24 MINOR, 7 NOTE.** Every change
  it proposes outside the Europa package is additive, and a 2.3.1 save would load unchanged.
- The MAJOR findings, in short: the 26.3 world-generation files are not specified and need tuning in a dev
  client; the under-ice habitat would never generate at its planned height; the terraformed sea would
  freeze over; three ores would drop nothing through the quarry; the radiation storm would be far too
  rare; the eclipse window is wrong by about two times; the `COLD` band makes the rocket card ask for the
  wrong suit; ten blocks have no recipe use.
- The review gives a 15-slice build order and says which slices could be left out of a first beta.
- Lists the siblings may want later (trophies, a colony type, more map layers) are in the design,
  section 12.

Europa is its own piece of work. The brief's Stage 4 gate ("no open MAJOR") is not met, so Stage 5 was
not started.

## Owner to-dos

- Answer decision 9 so the Europa design can be corrected and built, or move Europa to a later release.
- Run the in-game checklist in `docs/SMOKE-2.4.0.md`, including the shader pack matrix.
- Move the changelog entry from `[Unreleased]` to a dated `2.4.0-beta.1` heading when you release.
- The wiki does not mention Europa anywhere. Add it to `wiki/Shaders.md` and `wiki/Configuration.md` when
  it ships.
- Delete the leftover scratch file `build/skyjavap.init.gradle` (ignored by git).
- Commit, tag and publish when ready.

## Draft reply to the commenter (covers asks 1 to 5; add Europa when it ships)

> Thanks for coming back with more detail, it helped a lot. In the next beta:
>
> - With shaders on, the Moon, Mars, the station and Nyxara now ask your shader pack for its dark "End"
>   look, so space is no longer blue and the pack's clouds are gone from the Moon. This works through Iris.
> - Earth, the Moon and the other bodies now show in the sky on Fabric, Forge and NeoForge for 26.1.2,
>   26.2 and 26.3. If you prefer square, vanilla-style suns and planets, set `sky.bodyStyle = square`.
>   One honest limit: some shader packs replace the whole End sky with their own effect and hide what
>   mods draw there. The new Shaders wiki page explains the settings to try.
> - Rockets now climb four times higher before the jump to space, and server owners can raise it further
>   with `rockets.ascentHeight` (up to 256 blocks). A real 20,000-block climb is not possible: Minecraft
>   worlds are only a few hundred blocks tall, so the climb is the part you see and the rest is the
>   transfer.
> - Launches now kick out a burst of flame and smoke at lift-off, on top of the exhaust plume.
> - Europa is designed and on its way.
