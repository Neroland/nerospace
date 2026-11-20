# Nerospace 2.4.0-beta.1 — smoke record and owner checklist

Written 2026-11-20. Private dev notes; not part of the wiki.

## What was run

Everything below ran through Gradle only. **No dedicated server and no render client was started for
2.4.0**, so the headless flights the brief asks for (ascent at 24, 96 and 256 on a server) were not run;
they are on the checklist. What stands in for them is unit tests on the pure maths and the save shape.

| Check | How | Result |
| --- | --- | --- |
| Ascent curve at 24 blocks is tick-identical to 2.3.1 | `AscentMathTest`, `RocketAscentSaveCompatTest` | pass |
| Curve sums to the configured height within one block, 24 to 256 | `AscentMathTest` | pass |
| Duration grows with height and is capped | `AscentMathTest` | pass |
| A 2.3.1 rocket saved mid-ascent completes on the old climb | `RocketAscentSaveCompatTest` | pass |
| New entity-data field is declared and defined last | `RocketAscentSaveCompatTest` | pass |
| Ignition burst is bounded and fires once | `IgnitionBurstTest` | pass |
| Shader resolution table, pack precedence, `off`, no shader mod | `ShaderCompatRulesTest` | pass |
| Sky body style, profile parsing, hook source checks | `SkyHooksTest`, `SkyAssetsTest` | pass |
| Config keys and defaults | `ConfigSchemaCompatTest` | pass |
| Log scrub | `ShaderLogScrubTest` | pass |

Gate results are in `docs/HANDOVER-2.4.0.md`.

## Owner checklist (needs the game)

Tick each on **26.3** unless a version is named.

### Sky (asks 1 and 2)

- [ ] Fabric 26.3: Luna shows Earth with phases; the station shows Earth below, the Moon and the sun; Mars
      shows Phobos, Deimos, a small sun, and Earth as a bright star near dusk or dawn.
- [ ] Forge 26.3: the same three worlds.
- [ ] NeoForge 26.3: the same three worlds, and they look as they did in 2.3.1.
- [ ] One of Fabric, Forge or NeoForge on **26.2** and one on **26.1.2**: Luna's sky draws (these cells use
      a different hook and a different GPU path from 26.3).
- [ ] Greenxertz, Cindara, Glacira and Nyxara skies are unchanged.
- [ ] The Overworld, Nether and End skies are untouched.
- [ ] `sky.bodyStyle = square`: bodies are flat squares; phases read correctly; switch back to `globe`
      without a restart. Note whether the sun's glow should also be dropped in square style.
- [ ] F3+T (resource reload) on Luna: the sky comes back, no errors in the log.
- [ ] A 1.21.x cell still starts and shows the plain sky colours.

### Shader packs (asks 1 and 3)

Test at least three widely used packs on Fabric 26.3 and one on a 1.21.x cell. Record pack names and
versions here only, never in the wiki or the changelog.

For each pack and each of station, Luna and Mars, record: sky colour, clouds, whether the bodies draw,
whether the plume draws.

| Pack and version | World | Sky colour | Clouds | Bodies | Plume |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

- [ ] With Iris installed and **no** pack selected, everything looks as it does without Iris.
- [ ] The game starts with Iris on Fabric 26.3, NeoForge 26.3 and one 1.21.x cell.
- [ ] Luna with a pack: no blue sky, no pack clouds.
- [ ] `shaders.dimensionCompat = off`: the old behaviour returns without a restart.
- [ ] `shaders.drawPlanetSky = false`: the pack's own sky shows.
- [ ] The client log has at most one "shader compatibility inactive" line, and no pack name or path in any
      Nerospace line.
- [ ] Forge with no shader mod: starts normally (the shader mixin config must contribute nothing).

### Rockets (asks 4 and 5)

- [ ] Default config: a Tier 1 launch climbs about 96 blocks over about 8 seconds, then arrives.
- [ ] `rockets.ascentHeight = 24`: the climb is the old one.
- [ ] `rockets.ascentHeight = 256`: the climb completes; riders stay seated; passing the build limit is fine.
- [ ] Dedicated server with a second client watching: legs, staging and plume animate over the whole climb,
      not just the first 100 ticks.
- [ ] A cargo rocket lifts off and its route completes.
- [ ] A launch under a roof 40 blocks up: the hull stalls, the launch still completes (unchanged rule).
- [ ] The ignition burst shows at lift-off, bigger on Tier 4; with Particles set to Minimal it thins out.
- [ ] The plume and lamps show with and without a shader pack.
- [ ] Load a 2.3.1 world with a rocket parked on a pad: it loads and launches.
- [ ] Oxygen: a crewed launch between two airless worlds at height 96 does not run a seat dry mid-flight
      (the per-seat budget still assumes the old 100-tick ascent).

### Screens

No screen was touched in this build, so there is no GUI-scale check.
