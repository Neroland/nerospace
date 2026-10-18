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

Cindara is **hot** and Glacira is **cold**: unprotected, the planetary hazard makes your suit work
overtime — oxygen drains at **4×** the normal rate (there's no separate damage; running dry is still
the danger). The HUD badge warns with a red **HEAT!** / **COLD!** while you're exposed, with ember
puffs (Cindara) or a creeping frost vignette (Glacira) as feedback.

| | **Thermal Suit** | **Cryo Suit** |
| --- | --- | --- |
| Shields against | Cindara heat | Glacira cold |
| Craft (per piece) | Tier 2 piece + 4 **Cindrite** | Tier 2 piece + 4 **Glacite** |
| Repair material | Cindrite | Glacite |
| Class | Tier 2 (tank 900) | Tier 2 (tank 900) |

The shield engages only when **all four pieces match** the hazard; the HUD badge then shows
**SUIT HEAT** / **SUIT COLD** and oxygen drains at the normal 1× rate. Mixing variant pieces with
other pieces keeps life support — you just lose the hazard shield.

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
  `nerospace:oxygen_suit_cold_{...}` (Cryo)

- Config: `oxygenCapacityMultiplier` (air capacity) and `oxygenDrainMultiplier` (drain speed). See

  [Configuration](Configuration).
