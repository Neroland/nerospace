# Quarry Controller

A BuildCraft-style automated miner: mark out an area with landmarks and the controller builds a
glowing frame and excavates the whole rectangle, layer by layer, down to bedrock.

## Overview

The Quarry Controller is the brain of the miner. You mark a rectangular area with **[Quarry
Landmarks](Quarry-Landmark)**, place the controller beside it, give it **frame material** and
**power**, and it:

1. **Builds a frame** — a glowing, see-through structural ring around the claimed rectangle.
2. **Mines** the whole rectangle **layer by layer**, top to bedrock, like a 3D printer in reverse —
   a drill head travels the gantry to each block.
3. **Buffers and auto-ejects** everything it digs: mined items into an internal inventory, and any
   liquids it hits into an internal fluid tank — both push out to adjacent storage / pipes.

It never destroys what it can't store: if its buffers fill or the power runs out, it **pauses** (the
GUI tells you why) and resumes on its own once you fix it.

## Obtaining

**Craft** (shaped):

```text
I D I
F R F
I I I
```

`I` = [Nerosteel Ingot](Items) · `D` = Diamond · `F` = [Frame Casing](Upgrade-Modules) ·
`R` = Block of Redstone

> Tier 1 is the craftable version today. Tier 2 / Tier 3 controllers (bigger areas, more module
> slots, harsh-planet access) are planned — see [Tiers & planets](#tiers--planets).

## Setting it up

1. **Place 3 [Quarry Landmarks](Quarry-Landmark) in an L** at the **same Y level** to define the
   corners of the rectangle. The longest side must fit the tier's cap (**Tier 1 = 16×16**).
2. **Place the controller next to / in line with** a landmark — it scans along the axes to find the
   cluster, then **consumes the landmarks** and starts.
3. **Put [Frame Casing](Upgrade-Modules) in the frame slot** (top-left of the GUI). The frame costs
   **one casing per open-air perimeter cell** (cells already backed by terrain are free).
4. **Pipe power in** (see below). Building the frame is free, but **mining needs energy**.

## How it works

- **Power:** an internal **200,000 FE** buffer, filled through the energy capability on any side —
  connect a [Universal Pipe](Universal-Pipe) from a [Combustion](Combustion-Generator)/[Passive
  Generator](Passive-Generator) or a [Battery](Battery). **Dig speed scales with the power you
  supply**, up to the tier's per-tick ceiling × your modules' speed bonus × the planet's speed
  factor. Base cost is **40 FE per block** (lowered by Efficiency modules).
- **Output buffer:** 12 internal slots for mined items; **auto-ejects** into an adjacent inventory /
  pipe. Mining **pauses** ("buffer full") when it can't fit a drop — nothing is ever voided.
- **Fluid buffer:** source liquids (water, lava) in the dig area are **sucked up** into a
  **16,000 mB** internal tank that auto-ejects to an adjacent [Fluid Tank](Fluid-Tank) / pipe.
- **Obstacles:** it **skips** bedrock and other unbreakable blocks, and **skips the entire column**
  under a tile-entity (chests, spawners, machines) so they're left intact. It also skips other
  quarries' frames.
- **Drill head + gantry:** while it runs, a glowing gantry traces the frame and a **drill head**
  moves to the exact block being mined, with a vertical beam down the shaft.
- **Far edges:** a large area is force-loaded a chunk at a time while actively mining, so the dig
  keeps going as you range its edges; the tickets are released when it finishes or is removed.
- **Reclaiming:** breaking the controller tears down its frame.

### GUI status lines

| Line | Meaning |
|---|---|
| **Idle — place landmarks** | No valid region found yet. |
| **Building frame** | Placing the frame ring (consuming casings). |
| **Mining** | Digging — `Depth` shows how many layers below the frame plane it has reached. |
| **Paused** | Stopped — out of casings, out of power, buffers full, or wrong planet for this tier. |
| **Finished** | Reached bedrock; frame stays until you break the controller. |

## Tiers & planets

The miner runs **anywhere it has power**, but the harsh outer moons are gated by tier:

| Tier | Max area | Module slots | Base speed | Planets it can mine |
|---|---|---|---|---|
| **Tier 1** | 16 × 16 | 1 | 2 blocks/cycle | Overworld, Greenxertz, Orbital Station |
| Tier 2 *(planned)* | 32 × 32 | 2 | 4 blocks/cycle | + **Cindara** |
| Tier 3 *(planned)* | 64 × 64 | 4 | 8 blocks/cycle | + **Glacira** |

Mining **speed/yield also varies per planet** (the dense outer moons mine a little slower). A
too-low tier on a gated planet pauses with "wrong planet".

## Upgrades & the future

- **[Upgrade Modules](Upgrade-Modules)** (Speed / Efficiency / Fortune / Silk Touch) slot into the
  controller and tune its behaviour. They're a **cross-machine** system — the same cards will work
  in other machines.
- **Filters** (whitelist-keep, void the rest — e.g. trash cobble) are a planned follow-up; the
  output pipeline is already built to drop them in.

## Details

- ID: `nerospace:quarry_controller` · Tool: pickaxe, iron tier · Drops: itself
- Companion blocks: [Quarry Landmark](Quarry-Landmark), Quarry Frame (machine-placed; no item,
  drops nothing)
- Capabilities: energy **in** (any side); mined **items out** (any side); **fluid out** (any side).
  The frame-casing and module slots are configuration-only — they can't be piped in or out, so
  automation can never pull your modules or casings (load casings by hand in the GUI)
- Config: scales with the standard `energyRateMultiplier`, `fuelCostMultiplier`, and
  `machineSpeedMultiplier` (see [Configuration](Configuration))
