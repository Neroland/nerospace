# Rockets

The four crewed rockets that carry you off-world, what each one looks like, and what it does on the
pad and in flight. For fuel numbers, pad sizes and destinations see [Rocket Launch Pad](Rocket-Launch-Pad);
for the uncrewed freight hull see [Cargo Rocket](Cargo-Rocket).

## Overview

Deploy a rocket item onto a [launch pad](Rocket-Launch-Pad), fuel it, right-click it to open the flight
console and press **Launch**. You board as the engines light and arrive on the destination pad a few
seconds later. Every tier can still fly to every lower tier's destinations.

| Tier       | Reaches         | Pad                  | Look                                                                                                                          |
| ---------- | --------------- | -------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **Tier 1** | Orbital Station | one pad block        | Red livery, single bell, four raked fins, four landing legs, porthole cockpit                                                 |
| **Tier 2** | + Greenxertz    | 3×3                  | Nerosium-purple livery, twin strap-on boosters, spinning radar dish, two fins                                                 |
| **Tier 3** | + Cindara       | ringed 3×3           | Gold / green livery, main bell ringed by four verniers, fold-out solar wings, scanning dish boom, panoramic canopy, long nose |
| **Tier 4** | + Glacira       | Heavy Launch Complex | Ice-blue livery, wide five-engine core, four boosters, grid fins, heavy legs, the tallest nose                                |

Each tier is bigger than the last, both in geometry and in scale, so the progression reads from across
the base.

## The cockpit

Every hull has a window band at the rider's eye height, cut out of the hull and glazed from the inside
with tinted glass. Behind it is a lit cabin: a console with a screen under the forward window, a ceiling
lamp, and light strips on the side walls. The rider is seated so their eyes sit exactly on the window
band, so in first person you look out through the glass at the pad falling away; from outside, the
cabin glows through the windows at night.

The **hatch** on the south side stands open while nobody is aboard, with a ladder down to the pad, and
seals the moment you launch. While it is open you can see straight into the cockpit through the doorway.

## On the pad

- Landing legs are planted; on Tier 3 the solar wings stand deployed.
- Navigation beacons blink red (west) and green (east); a white strobe flashes at the nose.
- Tier 2's radar dish spins; Tier 3 and Tier 4 carry a dish on a side boom that scans back and forth.
- A fuelled hull vents a little cold vapour at its wall.

## Launch sequence

1. **Ignition** — the hatch seals, exhaust plumes bloom and flicker under every bell (white-hot core,
   tier-coloured fringe), the hull shudders, and a cloud rolls outward across the pad. Solar wings and
   the dish boom fold in.
2. **Climb** — the landing legs sweep out and up along the hull, the engine bells gimbal, and on Tier 4
   the grid fins deploy.
3. **Staging** — past two-thirds of the climb, Tier 2 and Tier 4 boosters go dark and fall away.
4. **Arrival** — the rocket re-appears on the destination pad, legs down and hatch opening.

The hull and its glass are lit by the world; only the lamps, beacons and exhaust are full-bright, so a
rocket parked at night is a dark silhouette with glowing windows.

## Cargo Rocket

The [Cargo Rocket](Cargo-Rocket) borrows the Tier 2 hull and livery at a slightly larger scale. Nobody
boards it, so its hatch stays sealed and it carries no ladder.

## For contributors

The rocket geometry, the Blockbench files under `art/blockbench/entity/` and the texture sheets are all
generated from one spec, `tools/rocket_models.py`; the animation lives in `RocketModel`. Edit the
Python, run it with `--multiloader` (or `./gradlew genAssets`) and rebuild.
