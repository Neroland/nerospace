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
