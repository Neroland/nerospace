# Smoke — 2.6.0-beta.1 Pathfinder Suit

Runtime checks below are for a scratch dedicated server and the owner's render client. They were not executed in the agent session (no Minecraft client, and the full cell build was still being brought up). Unit tests that stand in for the protection and flight maths are named next to each row.

## Server

- [ ] Load a 2.4.x world. A worn Tier 1, Tier 2, Thermal and Cryo suit protects as before on the first tick, then becomes the Pathfinder piece with the same protection. `HazardParityTest`, `LegacySuitConversionTest`.
- [ ] A suit in a chest, on an armour stand, on a zombie, and in a villager trade is unchanged until a player takes it. The trade result converts in the buyer's inventory.
- [ ] Cindara heat with Thermal Weave on all four pieces deals no extra oxygen drain. Glacira frigid cold needs Cryo Weave. Mars mild cold is stopped by Mk II with no weave. Radiation needs Rad Lining on all four. `HazardParityTest`.
- [ ] Suit Fabricator: install, remove, plate Mk I → Mk II, convert a legacy piece, charge a chest from a powered side.
- [ ] Suit Dock charges a player standing on it.
- [ ] Jump-Jet hover on the Overworld, Mars, Luna and the station, inside and outside a Gravity Deck.
- [ ] Flight Harness only after deep space, only at Mk IV. Fail-safe descent at 3% battery. Combat lock after player damage.
- [ ] `suit.flight.dimensionMode=none` and a station with suit flight denied both refuse flight.
- [ ] A flying suited player is not kicked. An unsuited player moving too fast still is. **Not yet met** — see the movement-mixin risk in the handover.
- [ ] A lined new suit is in `#nerospace:suits/radiation_capable` and carries `nerospace:rad_lining`.

## Render client (owner)

- [ ] Suit Fabricator GUI at GUI scale 2, 3 and 4.
- [ ] Worn suit texture on Fabric, Forge and NeoForge. One sheet in this build (`suit`), not a different sheet per frame.
- [ ] Jet plume with and without a shader pack. Plumes are not spawned yet; this box stays open.
- [ ] HUD energy bar, frame badge and module line. The oxygen HUD is unchanged; the suit energy bar is not drawn yet (`hud.suitEnergyBar` exists).
- [ ] Suit Panel key. The key is not registered yet.
