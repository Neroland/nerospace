# Runtime verification

Checklists that need a running client/server, run before a release. Record what was run, on which
cell, at the bottom of each section. Nothing here needs a real player account: single-player or a
LAN world is enough. **Never run these against a production server or its saves.**

## Planets and stations (2.0.0)

Cells: NeoForge 26.3 dev client for everything (`:neoforge:26.3:runClient`); Fabric 26.3 and Forge 26.3 for the
"other cells" items; one loader each on 26.1.2 and 26.2 for the upgrade and world-creation items. Use a creative world
with cheats unless a step says otherwise. Switch the language with Options → Language and the GUI scale with
Options → Video Settings → GUI Scale.

### A. Upgrading a 1.x world (run first)

1. Copy a world last saved by Nerospace 1.6.0 and open the copy on 2.0. Expect: it loads with no
   saved-data recovery, no backup restore and no codec errors in `logs/latest.log`.
2. Stations, named pads, a parked rocket, a running Terraformer, oxygen rooms, cargo routes, Village Cores and Star
   Guide progress are all there and working. Each old station opens the Station Console as a *Waystation* with no
   members and its original room untouched.
3. On Cindara, Glacira and Greenxertz, walk from explored land into new land: old chunks unchanged, a visible seam,
   new biomes beyond it. Greenxertz still has its 20-minute day; beds still skip the night.

### B. World creation on 26.3

1. On each 26.3 loader, create a brand-new single-player world from the title screen. Expect: the world opens without
   a crash or a datapack error screen.
2. Visit every planet once (`/execute in nerospace:<planet> run tp @s 0 120 0`): each loads, has its own blocks and
   biomes, and creatures spawn within a few minutes.

### C. Skies, one per world (NeoForge 26.3)

For each world, check the sky at sunrise, noon, sunset and midnight (`/nerospace clock <planet> set 0`, `0.25`,
`0.5`, `0.75`); nothing of the vanilla sky shows underneath.

1. **Luna:** black sky with stars even at noon, a faint galaxy band, a white sun, the Earth over the horizon.
2. **Mars:** butterscotch day, blue sunset, Phobos crossing quickly, Deimos slowly; brown tint in a dust storm
   (`/nerospace weather mars start`).
3. **Cindara:** red giant; at phase 0.45 the dark moon covers it and the light drops (eclipse), then dusk.
4. **Glacira:** pale sun, the moons Rime and Hoar with phases; auroras at night, brighter in diamond dust.
5. **Greenxertz:** twin suns (amber and pale green), ordinary clouds, a real day/night; pink tint in a spore bloom.
6. **Nyxara:** sun fixed on the western horizon, galaxy in the east; walk east from x = 0 to past the band edge and
   back: the horizon shifts from orange (west) to violet to deep purple (east) with no jump. Flares wash it gold.
7. **Orbital Station:** the homeworld below, slowly turning; stars and galaxy always visible.
8. **Other cells** (Fabric and Forge 26.3, and one 26.1.2 / 26.2 cell): each world shows its own colours, no fog
   wall, Luna and the station black with stars.

### D. Frame time (26.3)

1. With F3 open, stand on each planet at noon and at midnight for 30 s, in clear weather and during its weather event.
   Compare the frame time with the Overworld at the same render distance. Expect: no visible stutter and no
   noticeable drop against the vanilla sky.
2. Set `particles.enabled=false` in `config/nerospace.properties`, reload: no ambient particles at all.

### E. Screens at GUI scale 2, 3 and 4, in English, Italian and Chinese

At every scale and in every language: no text overlaps, nothing runs outside its panel, long labels end in "…" and
show the full text in a tooltip.

1. **Rocket screen:** the destination list of planet cards scrolls; the selected card shows gravity, hazard and suit,
   weather, clock and landing pad. Nyxara shows its lock reason until Deep Space progress.
2. **Launch Controller:** `◀ name ▶` destination arrows, pad/wall/gantry icons, hub gauges.
3. **Station Console:** all four tabs (Overview, Members, Docking, Build); rename box and buttons do not collide.
4. **Star Guide:** chapter rail scrolls with the wheel; Worlds chapters named by planet; bonus step dotted with a
   star; description scrolls.
5. **Terraformer and Terraform Monitor:** the refusal reason on the station and on Nyxara outside the band.
6. **HUD:** clock dial above the oxygen gauge with sunrise on the right; phase name shown for 2 s on a change; the
   weather line with its intensity bar; hazard and Geiger icons. Try `hud.clockDialScale=2`.

### F. Destination selection

1. On a Tier 4 rocket, click each destination card, then use the mouse wheel and Up/Down/Enter. Expect: the highlight
   moves at once with a click sound, the selection survives closing and reopening the screen, and a locked world is
   refused with its reason.
2. Same on the Launch Controller with `◀` / `▶`.
3. Repeat step 1 on 26.2: same behaviour.

### G. Landing on pads

1. Name-tag a Rocket Launch Pad "Base" in the Overworld and another "Base" on Mars. Fly from one to the other.
   Expect: the rocket appears 48 blocks up, descends with its engines firing and stands centred on the pad, facing
   its front; you stay seated until touchdown.
2. Choose a specific pad with the pad button: the rocket lands there. Break that pad first: you are told and it
   lands at the next spot.
3. Park a rocket on a pad and fly a second one there: the occupied pad is skipped.
4. Fly to a world with no pad nearby: a Landing Pod is placed on flat ground, with the rocket standing on it.
5. Break the target pad during a descent: the rocket steers to the next spot. Disconnect mid-descent and reconnect:
   you are on the pad.

### H. Station flows

1. Found a station from a rocket with a Station Charter: the charter is used up on arrival; without one, *New
   Station* is hidden.
2. In the console: change class (once per day), add a second player as a member (LAN), build the Habitat Ring,
   Solar Wing, Hydroponics Bay, Gravity Deck and (Research class) Observatory; build again = free repair; a block in
   the way stops the build and the console says where.
3. Members-only docking: a non-member's rocket is refused; a visitor rocket left riderless for ten minutes can be sent
   home by a member.
4. Solar Wings power an adjacent battery; a crop grows on a Hydroponics Tray under a Hull Window; a Gravity Plate gives
   normal gravity above it.

### I. Privacy export and erasure

1. As a player who has visited planets, owns a station and a cargo pad and is a member of another station, run
   `/nerospace privacy export`. Expect: JSON shown only to you, listing your own data and nothing about anyone else.
2. Run `/neroland data eraseme`. Expect: removed from every member list, station ownership cleared (the station stays),
   and a second `/nerospace privacy export` lists no stations, memberships, pads, routes or planet visits.

### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | All nine cells build and the plain-JVM tests pass; the agent sessions that built 2.0 could not start a client. Headless server checks (upgrade load, landing, erasure) passed; the visual, GUI and frame-time items above are for the owner before tagging. |

## 2.0.x Nyxara crystal / sky / clocks (unreleased)

One owner checklist for the unreleased 2.0.x work: the sky gallery and planet clocks (J, K), the Nyxara crystal (L),
the sky upgrade (M) and planet sleeping (N). Run it before the 2.0.x release and record each part at the bottom of its
section. The three checks the release must not skip:

- **Every `/nerospace gallery sky` state on every planet**: J.2 (you land, the sky holds, weather arrives at its peak)
  and M.1 (the look on NeoForge 26.3), plus M.5 for the colour floor and the fog on the other cells.
- **Nyxara crystal biomes at render distances 8, 12 and 16** (GUI scale 2 and 3): L.1.
- **Frame time with the aurora on and off** (and with `sky.quality=lite`): M.3.

A practical order: J, L and M on the NeoForge 26.3 dev client (`here` moves you between set pieces), then K, N and M.5
on the Fabric and Forge clients.

### Sky gallery and planet clocks

Cells: NeoForge 26.3 dev client for the skies (`:neoforge:26.3:runClient`), plus one other loader on any version for
the command itself. Creative world with cheats, as an operator.

#### J. `/nerospace gallery sky`

1. `/nerospace gallery sky`: every world is listed with its states. Typing `/nerospace gallery sky mars ` offers only
   Mars's states (`dawn`, `noon`, `dusk`, `night`, `blue_sunset`, `dust_storm`), and the same for each world.
2. For **every state on every world**, run `/nerospace gallery sky <planet> <state> here`. Expect: you land on solid,
   dry ground on that world (a small floor on the station), facing the view; the sky jumps to that time at once and
   stays there; weather states arrive at full strength with no ramp. Note any state that looks wrong:
   - Greenxertz: `dawn`, `noon`, `dusk`, `night`, `twin_noon`, `spore_bloom` (the Overworld's time changes too, and
     the day keeps running).
   - Cindara: `dawn`, `noon`, `dusk`, `night`, `eclipse`, `ash_fall`.
   - Glacira: `dawn`, `noon`, `dusk`, `night`, `twilight`, `aurora`, `diamond_dust`.
   - Orbital Station: `dawn`, `noon`, `dusk`, `night`, `sunrise`, `planet_night` (it should look east and down at
     the homeworld; the look itself is M.1).
   - Luna: `dawn`, `noon`, `dusk`, `night`, `earthrise`.
   - Mars: `dawn`, `noon`, `dusk`, `night`, `blue_sunset`, `dust_storm`.
   - Nyxara: `dayside`, `band`, `nightside`, `galaxy_side`, `flare`, and `dawn`, `noon`, `dusk`, `night`. Each side
     spot is past the ragged edge of the band.
3. With a second player on the same world, run a state: both see the change at the same moment.
4. Run a state without `here` on Nyxara: the chat names an X to go to and you are not moved.
5. `/nerospace gallery sky reset mars`: the Mars clock runs again and the storm ends. `/nerospace gallery sky reset`
   does the same everywhere. `/nerospace clock mars` afterwards shows the clock moving.
6. As a non-operator in creative, `/nerospace gallery sky` is not offered and is refused.
7. Switch the game to Italian and Chinese and repeat step 1 and one state: the messages are translated (the Italian and
   Chinese texts are provisional and need a native check).

#### K. Planet clocks, crops, beds and daylight detectors

1. On every world plant wheat, carrots, potatoes, beetroot, melon and pumpkin stems, sugar cane, a sapling and a sweet
   berry bush under open sky, next to water, and wait or use `/tick sprint`. Expect: all of them grow at any planet
   clock phase (the headless crop probe saw this; this is the in-game check).
2. Beds: run section N (planet sleeping), which replaces the bed check that stood here before planets got their own
   nights. The rule that carries over: no bed explodes on any world.
3. Place a daylight detector on each world and change the clock with `/nerospace gallery sky`. The probe found its
   signal stays the same at every planet phase except on Greenxertz; note whether that matters for your builds.
4. Watch a Solar Panel's output through a day on Mars and Cindara (peak at noon, low at night, dimmed in a dust storm
   and during the eclipse) and on Nyxara (steady).

#### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | The nine cells build and the plain-JVM tests pass; the crop probe ran headless on NeoForge 26.3. |

### Nyxara crystal

Cells: NeoForge 26.3 dev client (`:neoforge:26.3:runClient`) and one 26.1.2 or 26.2 client (Fabric or Forge), new
creative world, cheats on. Use `/nerospace gallery sky nyxara <dayside|band|nightside|flare> here` to move around.

#### L. Crystal biomes, translucency and the flare prism

1. **Render distance 8, 12 and 16**, at GUI scale 2 and 3, in each region: the Dayside (pale Sunglass ground, Crazed
   patches, shard drifts), the Twilight Band (violet ground, rose patches, spire forests, some spires tipped with a
   prism, clusters) and the Nightside (dark Smoke ground, flat Mirror lakes). Expect: the ground reads as crystal, not
   stone; cliffs show Nyx Stone; no missing-texture blocks. Note the frame time (F3) in a spire forest and over a
   lake at each distance, against a stone area of the band (budget: about +1.2 ms in a spire forest, +1 ms at a lake
   view, at distance 12).
2. **Translucency.** Look through a Crystal Spire, a Nyx Prism and a Mirror lake against the sky and against terrain:
   you see what is behind them (tinted), and never the void or missing faces. On the 26.1.2 / 26.2 client the same
   three blocks must look the same as on 26.3. Clusters show clean cut-out edges; the ground blocks are opaque.
3. **Flare prism.** Stand in the band near a prism-tipped spire and run `/nerospace gallery sky nyxara flare`. Expect:
   prisms brighten, and a violet-to-gold particle ray runs 6 blocks east (away from the sun) from each one. With
   `particles.enabled=false` the ray is gone and the glow stays. Standing in the ray during the flare without a
   Rad-Lined suit hurts more than standing beside it. `/nerospace gallery sky reset nyxara` ends it.
4. **Crystal behaviour.** Jump onto natural Crazed Crystal from 4 blocks (it and its neighbours shatter, the fall
   hurts less); walk barefoot in Shard Drift (it hurts) and with boots (it does not); walk on Rose Crystal (the trail
   glows for 3 seconds); walk on Smoke Crystal (no footsteps); hit a spire at its base and its top (low and high
   chime); dig a Mirror lake block and place it in the band (it goes dark); place Twilight Crystal under the open sky
   and in a cave (glows / dark).
5. **Sunglass shade.** On the Dayside without a Rad-Lined suit, stand in the open (radiation damage), then under a
   Sunglass Crystal roof 2-4 blocks above your head (no damage at Dayside level 2).
6. Repeat step 1 once in Italian and Chinese and check the crystal names (provisional, need a native check).
7. **After the review fixes.** Mine a full Nyx Crystal Cluster by hand (1 shard) and with a pickaxe (2); judge the
   Mirror-lake density by eye on the Nightside (about one lake every eight chunks); a Crystal Shard shows the Neroland
   Core materials slot border; a camera clipped into a Mirror lake shows no overlay.

#### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | The nine cells build and the plain-JVM tests pass; the worldgen probe, relight bench and fixture checks ran headless on NeoForge 26.3. |

### Sky upgrade

Cells: NeoForge 26.3 dev client (`:neoforge:26.3:runClient`) for the full sky, plus one client on each other loader
(a Fabric and a Forge 26.1.2 or 26.2 client) for the fallback. Creative world, cheats on, operator. Every state below is
`/nerospace gallery sky <world> <state> here`; `/nerospace gallery sky reset` afterwards. The frames in the art preview
set are the reference look; the game draws the same shapes (stars are placed differently).

#### M. Planet skies, frame time and fallback

1. **Set pieces (NeoForge 26.3).** Expect, per world:
   - Luna `night`, `earthrise`: hard natural-colour stars that do not twinkle, a faint nebula veil; Earth rises in the
     east with a blue rim, a dark side with city lights and the terminator facing away from the sun; shooting stars
     a few times a minute, also by day.
   - Mars `noon`, `dusk`, `blue_sunset`, `night`, `dust_storm`: butterscotch day; at `dusk` a tan sky; at
     `blue_sunset` (looking west) a **clearly blue** glow around the setting sun on a grey-blue horizon; Phobos and
     Deimos show phases; `dust_storm` turns the sky rust, hides the stars, dims the moons. Then `night` and
     `/nerospace weather start` + `/nerospace weather clear`: a faint green aurora over the north for about a minute.
   - Cindara `eclipse` (looking west): the sky dims, a pearl corona around the dark moon, a diamond-ring flash on the
     limb shortly before and after totality (run `/nerospace clock set` through 0.41-0.49 to see both); `ash_fall`:
     the low cloud deck thickens and greys, its underside glows orange near the horizon.
   - Glacira `noon`: a faint 22° halo with sun dogs; `aurora` (looking north): three folding green-teal curtains with
     rays that scroll and drift; `diamond_dust`: brighter curtains with a rose upper fringe, the halo much stronger by
     day; `twilight`: long mauve dusk.
   - Greenxertz `twin_noon` (looking up): both suns; `spore_bloom`: a pink shimmer all round the horizon; `night`:
     green twinkling stars and a soft green horizon glow.
   - Nyxara `dayside` (looking west), `band`, `galaxy_side`, `flare`: the galaxy climbing from the east and two
     nebulae; a rainbow-like arc low in the east from the band; violet-cyan curtains only on the nightside; in a
     flare two counter-rotating ray fans from the western sun, a shimmer along the western horizon and a gold wash,
     pointing the same way as the prism rays on the ground.
   - Station `noon`, `sunrise` (looking east, slightly down), `planet_night`: the homeworld fills the lower eastern sky
     with a blue rim; at `planet_night` the near side is **mostly dark** with city lights (at most a thin lit edge
     toward the sun), and at `sunrise` the near side is still dark with lights while the sun comes up through the
     homeworld's rim with a warm flash; nebulae and the galaxy behind. On a mostly lit body the city lights fade out.
2. **No pops.** Start and clear each world's weather (`gallery sky <world> <weather state>`, then `reset`): the tint,
   haze, aurora gate, cloud opacity and particles fade over about 1.5 s both ways. Stand through a sunrise with
   `/nerospace clock thaw` on Glacira and Mars: no body or curtain jumps; set the clock and freeze it: the aurora keeps
   moving smoothly (it runs on game time).
3. **Frame time (F3, 1080p, render distance 12)** on each world's busiest state, with `sky.aurora=true` and `false`
   and with `sky.quality=lite`. Budget (full / lite, ms of sky): Luna 0.30 / 0.22, Mars 0.40 / 0.30, Cindara 0.50 /
   0.32, Glacira 0.70 / 0.40, Greenxertz 0.40 / 0.30, Nyxara 0.65 / 0.42, station 0.45 / 0.35; `lite` must stay under
   0.45 ms everywhere. Compare with the vanilla Overworld sky.
4. **Client switches.** `sky.aurora=false` removes every curtain; `sky.shootingStars=false` removes the streaks;
   `sky.quality=lite` keeps one Glacira curtain and drops Cindara's cloud deck. Reload the config (or restart) between
   changes. A server never changes these.
5. **Fallback cells (Fabric and Forge 26.1.2 / 26.2 / 26.3, NeoForge 26.1.2 / 26.2).** Each world shows its colour
   floor (vanilla sun/moon shapes) and the **world fog follows the profile**: Mars fog darkens at `night` and turns rust
   in `dust_storm`; Cindara's fog dims in `eclipse`; Nyxara's fog changes between `dayside`, `band` and `nightside`.
   Underwater, in lava, in powder snow and with Blindness the vanilla fog is unchanged. No log errors from the fog
   mixin on Fabric.
6. **Fog agreement on 26.3.** Near the horizon the terrain fog and the sky meet without a visible band at every state.
7. **Night Vision.** On Mars or Glacira at `night`, `/effect give @s minecraft:night_vision`: the world fog is vanilla's
   brightened fog (not the dark night fog under a bright view); remove the effect and the planet fog returns. Same on
   one fallback cell.
8. **Resource reload (26.3).** On Glacira `aurora`, press F3+T: the sky redraws after the reload (meshes rebuilt), no
   log errors, no visible hitch beyond the reload itself.

#### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | The nine cells build and the plain-JVM tests pass (sky codec, scene maths, continuity, gallery set pieces, smoothing, config keys); the fixture checks ran headless on NeoForge 26.3. Frame time and the look are owner-side (no headless client capture on 26.3). |

### Planet sleeping

Cells: one client per loader (NeoForge, Fabric and Forge, any 26.x line), survival world with cheats on, operator; a
second player (a LAN guest or a second client) for step 3. Bring a bed and a few torches. `/nerospace clock <planet>
get` shows the phase and the day count; `/nerospace clock <planet> set <phase>` moves it. Section K step 2 now points
here (it recorded the old behaviour, no sleep off Greenxertz).

#### N. Sleeping on each world

1. **Mars at night.** `/nerospace clock mars set 0.75`, place a bed, sleep. Expect: you fall asleep, after about five
   seconds the sky snaps to sunrise, the HUD dial reads *Sunrise* and `clock mars get` shows the next sol. Start a
   dust storm first (`/nerospace weather mars start`) and check it is still blowing after the skip.
2. **Noon refuses.** `/nerospace clock mars set 0.25`, use the bed. Expect "You can only sleep in the planet's night"
   on the action bar, no sleep, no explosion, and dying sends you back to that bed. Repeat at Cindara's eclipse
   (`/nerospace clock cindara set 0.45`): refused.
3. **Two players.** Both on Luna at night (`/nerospace clock luna set 0.75`), default `players_sleeping_percentage`
   100. One sleeps: nothing is skipped and the vanilla "1/2 players sleeping" line shows. The second sleeps: both wake
   at sunrise. Set the game rule to 50 and repeat: one sleeper is enough.
4. **Greenxertz is vanilla.** At Overworld night sleep on Greenxertz: the night is skipped once (the Overworld is at
   morning too, not a day later). Sleep on Luna at Luna's night and check the Overworld time did not move.
5. **Nyxara.** Beds in the Twilight Band and on the Dayside: "The sun never sets here", spawn still set. A bed on the
   Nightside: you sleep and get up on your own after about five seconds; the sky does not change.
6. **Station.** A bed on the station: "The station's lights never dim", no sleep, no explosion, and you respawn
   there.
7. **Frozen clock.** `/nerospace gallery sky luna night` (or `/nerospace clock luna freeze` at night), then sleep.
   Expect: you sleep, get up after about five seconds with "Time stands still here; you rest, but the night does not
   pass", and the sky stays at night. `/nerospace gallery sky reset luna`, sleep again: the night is skipped.
8. **advance_time off.** `/gamerule advance_time false` (`doDaylightCycle` before 1.21.11), then step 1 on Mars:
   the skip still happens (the planet clock ignores that rule). Set it back to true.
9. **Water freezing.** On Luna, Mars, Glacira, the Nyxara Nightside and the station, pour water under the open sky
   with no light near it and plant sugar cane beside it: within a few minutes the water ices over and the cane breaks.
   With a torch within a few blocks of the water it stays liquid. Check the wiki wording matches what you see.
10. **Languages.** Read the four refusal / rest lines in Italian and Chinese (they are provisional translations):
    `nerospace.sleep.day`, `nerospace.sleep.frozen`, `nerospace.sleep.station`, `nerospace.sleep.nyxara_bright`.

#### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | The nine cells build and the plain-JVM tests pass (sleep window, decisions, sleepers maths, forward clock set, bed rules); a headless sleep probe ran every row it can (all but the 1.21.x shared-time check) on NeoForge 26.3. Fabric and Forge hooks are untested at runtime (no headless harness on those loaders). |

## Rocket visuals (1.6.0)

Cell: any dev client; a creative world with a Tier 1 pad, a 3×3, a ringed 3×3 and a Heavy Launch Complex.

1. **Deploy all four tiers** (and a Cargo Rocket on a Cargo Pad). Expect: four clearly different hulls,
   each taller and busier than the last; livery red / purple / gold-green / ice-cyan with the tier
   numeral on the hull; legs planted on the pad, hatch standing open with a ladder (sealed and no ladder
   on the cargo hull); beacons blinking red/green, strobe flashing; Tier 2 dish spinning, Tier 3 dish boom
   scanning, Tier 3 solar wings deployed. At night the hulls are dark and only lamps/beacons glow.
2. **Windows.** From outside: tinted glass, a lit cabin behind it, console screen visible. Open the console
   and press Launch on each tier, then press F5 back to first person during the first second: your eye
   line is centred on the window band, you see the pad/world through the glass, the cabin liner, console
   and ceiling lamp are around you, and the hatch has sealed. Nothing clips into the camera.
3. **Launch.** Expect in order: hatch seals, plumes bloom and flicker under every bell (cores
   white-hot, tier-coloured fringe), hull shudders, a cloud rolls across the pad; wings / dish boom fold in
   during the first half second; legs sweep out and up along the hull; bells wobble; Tier 4 grid fins
   deploy about a third of the way up; Tier 2 / Tier 4 boosters go dark and fall away past two-thirds of
   the climb. The rocket never pops out of view while any part of it is on screen (look up as it passes).
4. **Fuelled, idle.** A fuelled hull vents occasional snowflake-like vapour at its wall; an empty one does not.
5. **Blockbench.** Open `art/blockbench/entity/rocket_t4.bbmodel`: bones, pivots and rotations load and
   the texture lines up (viewing surface only — edits go in `tools/rocket_models.py`).

### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | Built by an agent session without a client. All nine cells compile and `tools/rocket_models.py --check` is clean; the checklist above is what the owner runs before tagging. |

## Cargo rockets (1.3.0)

Cell: 26.3 NeoForge dev client at minimum (`:neoforge:26.3:runClient`), creative world with cheats.

1. **Build pad A (Overworld).** Place a Cargo Pad, complete a 3×3 with Rocket Launch Pads around it.
   Sneak-right-click → report says Tier 2. Expect: `Cargo Pad 1` registered (open the GUI: status
   "Ready — load the hold…").
2. **Build the rocket.** Deploy a Cargo Rocket on the Cargo Pad (message: deployed). A second deploy on the
   same cluster is refused. A crewed Tier-2 rocket item does nothing on the Cargo Pad.
3. **Fuel.** (a) Place a Fuel Tank against a pad block, fill it — the rocket gauge rises. (b) Break it, pipe
   rocket fuel into the Cargo Pad instead — the Buffer gauge fills, then drains into the rocket at
   80 mB/t. (c) Pipe water at the pad — nothing enters the buffer.
4. **Build pad B (Orbital Station).** Fly a crewed rocket to a founded station, place a Cargo Pad within
   six blocks of the station's landing pad (or complete a 3×3 there). Back at A: `>` cycles to
   "<station name> (Orbital Station)" and to "Cargo Pad 2 (Orbital Station)" once B is registered.
5. **Build pad C (Greenxertz).** Same, on Greenxertz; a 3×3 around it.
6. **Route A → B, launch.** Put ~10 stacks in A's hold, select B, note the quote (fuel + m:ss). Press
   Launch with too little fuel → "Not enough fuel". Fuel up → "Lift-off", the rocket ascends and vanishes,
   the hold is empty, status shows "in transit, Ns to go".
7. **Restart mid-flight.** Quit to title *during* the countdown, reopen the world. Status still counts
   down from where it was (overworld game time), then arrives.
8. **Arrival at B.** With B's chunk loaded: a cargo rocket appears on B, arrival particles + sound, the
   hold fills one stack every 2 ticks, status at A reads "delivered", advancement **Supply Line** fires
   for the owner. With B's chunk *unloaded* at arrival time (log out of the station dimension): status
   reads "waiting for the destination to load" until you go there.
9. **Unload via pipe.** A Universal Pipe in extract mode on any face of B pulls the delivered cargo into a
   chest; an Advanced Pipe Filter on that face filters it.
10. **Holding → crate.** Set `cargoHoldTimeoutMinutes=1`, `/neroland config reload`. Break pad C's Cargo
    Pad while a flight A → C is in the air (or park a crewed rocket on C). Expect: status "holding",
    NeroLink/log breadcrumb `hold flight=N`; after a minute a **Cargo Crate** item drops at C's position
    holding the manifest; right-click it — items land in your inventory. Nothing lost.
11. **Return empty.** Toggle "Return empty" on route A → C, launch; after unloading at C the rocket
    vanishes and a return flight arrives back on A (empty). Fuel for both legs was taken at launch.
12. **Schedules.** "Launch when full": fill every usable slot (18 by default) — it launches on its own.
    "Every 1 min": with one item in the hold and fuel, it launches once a minute.
13. **Permissions.** As a second player (LAN), open A: controls are greyed, Launch still works, A is not
    in *their* destination list until you mark it Public.
14. **Erase the owner.** `/neroland data eraseme` as the owner while a flight is in the air. Expect: the
    route on A is gone (destination shows "—"), A's GUI controls now work for anyone (unowned), the
    `route` NeroLink section for that UUID is empty, and the in-flight cargo still arrives (or crates).
    `data/nerospace_cargo_routes_backup.dat` no longer contains the UUID.
15. **Standalone.** Repeat 1, 2, 6, 8 with only Neroland Core + Nerospace installed.

### Record

| Date | Cell | Steps run | Result | Notes |
| --- | --- | --- | --- | --- |
| — | — | — | not yet run | The agent session that built the feature could not start a client (sandbox). All nine cells build green and the plain-JVM tests (formulas, registry, erasure conformance) pass; the checklist above is what the owner runs before tagging. |
