# Runtime verification

Checklists that need a running client/server, run before a release. Record what was run, on which
cell, at the bottom of each section. Nothing here needs a real player account: single-player or a
LAN world is enough. **Never run these against a production server or its saves.**

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
