# Oxygen Suit

Four-piece armour that doubles as personal life support on airless worlds. Two tiers.

## Overview
Wearing the **full set** (all four pieces) keeps you breathing off-world by draining a finite air
tank instead of suffocating. The tank refills instantly in any breathable zone (oxygen field,
terraformed ground, launch-pad safe zone) — and, since the suit-and-station integration, also at
**airlocks** (below).

| | Tier 1 Oxygen Suit | Tier 2 Oxygen Suit |
|---|---|---|
| Air tank | 300 (`oxygenMax`) | **600** (`oxygenSuitT2Max`) |
| Airlock refill | 20 air / ~0.5 s | **40 air / ~0.5 s** |
| Protection | diamond-class | slightly tougher, +toughness |
| Repair material | Nerosteel Ingot | Cindrite |

A **mixed** T1/T2 set still works as life support but counts as **Tier 1** for tank size and refill
speed. Any missing piece = no life support.

## Airlock refill
Within a few blocks (default **3**, `oxygenAirlockRadius`) of a [Gas Tank](Gas-Tank) or
[Oxygen Generator](Oxygen-Generator) **holding Oxygen**, a worn suit refills its air from that store —
each air unit drains gas (default **5 mB**, `oxygenAirlockMbPerAir`). A Gas Tank at the base entrance
therefore acts as an airlock: step inside, top up, head back out. The Creative Gas Tank works too.

## Obtaining
**Tier 1** (shaped, nerosteel): helmet `NNN / NGN` (G = Glass visor), chestplate `N N / NCN / NNN`
(C = Rocket Fuel Canister life-support core), leggings and boots all-nerosteel.

**Tier 2** (shaped): each Tier 1 piece surrounded by 4 **Cindrite** —
```
. C .
C P C
. C .
```
`P` = the matching Tier 1 piece · `C` = Cindrite

Cindrite only mines on **Cindara** (Tier 3 rocket, itself gated on
[Station Wall](Station-Wall)), so the Tier 2 suit sits at the same progression depth as the Tier 3
rocket.

## Details
- IDs: `nerospace:oxygen_suit_{helmet,chestplate,leggings,boots}` and
  `nerospace:oxygen_suit_t2_{helmet,chestplate,leggings,boots}`
- Config: `oxygenMax`, `oxygenSuitT2Max`, `oxygenSuitDrain`, `oxygenAirlockRadius`,
  `oxygenAirlockRefillPerCheck`, `oxygenAirlockMbPerAir`
