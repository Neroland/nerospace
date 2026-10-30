# Oxygen Suit

Four-piece armour that doubles as personal life support on airless worlds. Two tiers, plus two
Tier-2-class **hazard variants** for the extreme worlds.

## Overview

Wearing the **full set** (all four pieces) keeps you breathing off-world by draining a finite air
tank instead of suffocating. The tank refills instantly in any breathable zone (oxygen field,
terraformed ground, launch-pad safe zone) — and, since the suit-and-station integration, also at
**airlocks** (below).

| | Tier 1 Oxygen Suit | Tier 2 Oxygen Suit |
| --- | --- | --- |
| Air tank | 900 | 900 |
| Airlock refill | 90 air / 10 ticks | 90 air / 10 ticks |
| Protection | diamond-class | slightly tougher, +toughness |
| Repair material | Nerosteel Ingot | Cindrite |

Every full suit (Tier 1, Tier 2, Thermal or Cryo) holds **900 air**. Without a suit you have only
**300 air** (your own lungs). A **mixed** set still works as life support. Any missing piece = no
life support.

**Drain:** with a full suit you lose **3 air** per check (every 10 ticks). With no suit you lose
**30 air** per check. An unshielded hazard (below) multiplies drain by **4**.

## Hazard variants — Thermal & Cryo Suits

Cindara (and Nyxara's Dayside) is **hot**; Glacira, Mars (and Nyxara's Nightside) are **cold**: unprotected, the planetary hazard makes your suit work
overtime — oxygen drains at **4×** the normal rate (there's no separate damage; running dry is still
the danger).

Cold comes in two strengths. **Mars** is cold but not frigid: any full set of Tier 2 class pieces (Tier 2, Thermal
or Cryo, even mixed) keeps its cold out, so a Tier 1 suit is the only one that struggles there. **Glacira**, the
**Martian Polar Cap** and **Nyxara's Nightside** are **frigid**: only a full Cryo Suit (or, on Nyxara, a flaring
Dawnband Stone within four blocks) keeps that cold out. The HUD badge warns with a red **HEAT!** / **COLD!** while you're exposed, with ember
puffs (Cindara) or a creeping frost vignette (Glacira) as feedback.

| | **Thermal Suit** | **Cryo Suit** |
| --- | --- | --- |
| Shields against | Cindara heat, Nyxara Dayside heat (and Mars cold, like any Tier 2 suit) | all cold: Glacira, the Martian Polar Cap, Nyxara Nightside, and Mars |
| Craft (per piece) | Tier 2 piece + 4 **Cindrite** | Tier 2 piece + 4 **Glacite** |
| Repair material | Cindrite | Glacite |
| Class | Tier 2 (tank 900) | Tier 2 (tank 900) |

The shield engages only when **all four pieces match** the hazard; the HUD badge then shows
**SUIT HEAT** / **SUIT COLD** and oxygen drains at the normal 1× rate. Mixing variant pieces with
other pieces keeps life support — you just lose the heat and frigid-cold shield (a mix of Tier 2 class pieces still
keeps Mars' milder cold out).

## Rad Lining (radiation)

Mars dust storms and Nyxara's Dayside and Nightside are **irradiated**. Without protection, radiation makes your
suit drain at 4× and, from radiation level 2 upwards, hurts you while you stand outside a sealed oxygen field: one
point of damage every two seconds at level 2 (a dust storm, the Dayside), two at level 3 (the Nightside, or the
Dayside during a flare). The Moon and the orbital station never need it.

The **Rad Lining** is an upgrade, not a new suit. Craft a Rad Lining from Regolith, Raw Hematite and a Titanium
Ingot, then combine it at a **smithing table** with a Tier 2, Thermal or Cryo suit piece and one Titanium Ingot.
The piece keeps its name, tier and hazard shield, and gains a gold *Rad-Lined* line. The shield engages when **all
four** worn pieces are lined; the HUD then shows **RAD LINING**.

Where several hazards meet, you need every matching protection: the Nyxara Nightside is irradiated **and** cold
(a Rad-Lined Cryo Suit, or Rad Lining beside a flaring Dawnband Stone), the Dayside irradiated **and** hot
(a Rad-Lined Thermal Suit), and Mars in a dust storm is irradiated **and** cold (any Rad-Lined Tier 2 suit; a
Rad-Lined Cryo Suit on the Polar Cap). The HUD warns about the hazard that would hurt you next.

## Progression note

Cindrite (Thermal) is mined on Cindara itself, so the usual loop is: dash in a plain Tier 2 suit,
mine your first cindrite, then craft the Thermal Suit for serious expeditions. The same applies to
glacite and the Cryo Suit on Glacira.

## Airlock refill

Within **4 blocks** of a [Gas Tank](Gas-Tank) or [Oxygen Generator](Oxygen-Generator) **holding
Oxygen**, a worn suit refills from that store. Every suit gets **90 air** per check (every 10 ticks).
Each air unit costs **2 mB** of oxygen (scaled by `fuelCostMultiplier`). A Gas Tank at the base entrance
therefore acts as an airlock: step inside, top up, head back out. The Creative Gas Tank works too.

## Obtaining

**Tier 1** (shaped, nerosteel): helmet `NNN / NGN` (G = Glass visor), chestplate `N N / NCN / NNN`
(C = Rocket Fuel Canister life-support core), leggings and boots all-nerosteel.

**Tier 2** (shaped): each Tier 1 piece surrounded by 4 **Cindrite** —

```text
. C .
C P C
. C .
```

`P` = the matching Tier 1 piece · `C` = Cindrite

Cindrite only mines on **Cindara** (Tier 3 rocket, itself gated on
[Station Wall](Station-Wall)), so the Tier 2 suit sits at the same progression depth as the Tier 3
rocket.

## Details

- IDs: `nerospace:oxygen_suit_{helmet,chestplate,leggings,boots}`,

  `nerospace:oxygen_suit_t2_{...}`, `nerospace:oxygen_suit_heat_{...}` (Thermal),
  `nerospace:oxygen_suit_cold_{...}` (Cryo), `nerospace:rad_lining` (upgrade; a lined piece carries the
  `nerospace:rad_lining` item component)

- Config: `oxygenCapacityMultiplier` (air capacity) and `oxygenDrainMultiplier` (drain speed). See

  [Configuration](Configuration).
