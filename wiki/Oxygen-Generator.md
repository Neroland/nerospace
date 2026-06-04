# Oxygen Generator

Keeps you alive off-world by turning grid power into Oxygen gas — and pressurising the air around it.

## Overview
Every Nerospace dimension is **airless** — exposed players lose oxygen and suffocate. The Oxygen
Generator is an **electrolysis machine**: it consumes energy from the pipe network and produces
**Oxygen gas** into an internal tank. That tank both feeds the breathable field around the machine and
can be **piped out** through Universal Pipes (gas layer) into Gas Tanks or other rooms.

## Obtaining
**Craft** (shaped):
```
N G N
R C R
N N N
```
`N` = Nerosteel Ingot · `G` = Glass · `R` = Redstone · `C` = Rocket Fuel Canister

## How it works
- **Power in:** connect a Universal Pipe carrying energy (from a Combustion/Passive Generator or a
  Battery). The machine stores up to 10,000 FE and cannot burn fuel directly — it is grid-only.
- **Oxygen out:** while powered it produces up to 5 mB of Oxygen per tick (2 FE per mB) into an
  8,000 mB internal tank. Pipes connected to it can carry the gas away (green stream).
- **Oxygen field:** while the tank holds gas the machine is an oxygen **source**, slowly draining
  (2 mB/t) to keep the air up. Air spreads outward through connected open space:
  - **Sealed room** (walls/roof — full blocks and glass are airtight): the **whole room fills** and
    stays breathable.
  - **Open / leaky space:** only a bubble around the generator pressurises (the air escapes toward any
    opening), so seal your base for full coverage. A door or gap counts as a leak.
  - Search/coverage reaches up to ~16 blocks of connected air from the generator (configurable).
- **Loss:** if the tank runs dry or the machine is broken, the oxygen **evaporates over ~10 seconds**
  (configurable).
- **HUD:** a cyan **O₂ bar** appears above the hotbar in airless dimensions (it turns red when low).
- **Airlock:** a player wearing a full [Oxygen Suit](Oxygen-Suit) within a few blocks (default 3)
  refills the suit's air directly from the machine's tank, draining the gas — handy at a base door
  even when the room itself isn't breathable yet.
- **Automation:** emits a **comparator signal** from its oxygen tank level.

## Tips
Build an airtight room, run a power line to a generator inside it, and the room stays breathable as
long as the grid is up. Pipe surplus oxygen into a **Gas Tank** as a buffer for power outages. A full
**Oxygen Suit** is the portable alternative for exploring away from a generator.

## Details
- ID: `nerospace:oxygen_generator` · Tool: pickaxe, iron tier · Drops: itself
- GUI: power gauge + oxygen tank gauge with a Producing/No-power status
- Config: `oxygenBubbleRadius`, `oxygenLeakRange`, `oxygenEvaporateSeconds`, `oxygenBreathableThreshold`, …
