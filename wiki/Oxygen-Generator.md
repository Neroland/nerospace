# Oxygen Generator

Keeps you alive off-world by pressurising the air around it.

## Overview
Every Nerospace dimension is **airless** — exposed players lose oxygen and suffocate. An active Oxygen
Generator injects breathable air into the surrounding space so you can build a habitable base.

## Obtaining
**Craft** (shaped):
```
N G N
R C R
N N N
```
`N` = Nerosteel Ingot · `G` = Glass · `R` = Redstone · `C` = Rocket Fuel Canister

## How it works
- **Fuel → power:** put a fuel item in its slot (coal, charcoal, a coal block, a blaze rod, or a
  Rocket Fuel Canister). Burning fuel charges an internal energy buffer; the generator runs while it
  has power and goes idle when fuel and buffer are spent.
- **Oxygen field:** while powered it becomes an oxygen **source**. Air spreads outward through
  connected open space:
  - **Sealed room** (walls/roof — full blocks and glass are airtight): the **whole room fills** and
    stays breathable.
  - **Open / leaky space:** only a bubble around the generator pressurises (the air escapes toward any
    opening), so seal your base for full coverage. A door or gap counts as a leak.
  - Search/coverage reaches up to ~16 blocks of connected air from the generator (configurable).
- **Loss:** if the generator runs out of fuel or is broken, the oxygen **evaporates over ~10 seconds**
  (configurable).
- **HUD:** a cyan **O₂ bar** appears above the hotbar in airless dimensions (it turns red when low).
- **Automation:** hoppers/pipes can feed the fuel slot; emits a **comparator signal** from its charge.

## Tips
Build an airtight room (any full blocks or glass), drop a generator inside, and keep it fuelled. A full
**Oxygen Suit** is the portable alternative for exploring away from a generator.

## Details
- ID: `nerospace:oxygen_generator` · Tool: pickaxe, iron tier · Drops: itself
- Config: `oxygenBubbleRadius`, `oxygenLeakRange`, `oxygenEvaporateSeconds`, `oxygenBreathableThreshold`, …
