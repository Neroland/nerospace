# Fluid Tank

A passive single-fluid store for the pipe network. (Not to be confused with the rocket-fuelling
[Fuel Tank](Fuel-Tank) machine.)

## Obtaining

**Craft** (shaped): a nerosteel shell with glass windows —

```text
N G N
G   G
N G N
```

`N` = Nerosteel Ingot · `G` = Glass

## How it works

- Holds **16,000 mB** (configurable) of any one fluid.
- **Buckets:** right-click with a filled bucket to pour in, an empty bucket to draw out.
- **Pipes:** the fluid layer fills and drains it on every side — remember the network carries one

  fluid at a time.

- Bare-hand right-click reads out the contents.

A **Creative Fluid Tank** variant supplies endless fluid — see
[Creative Source Blocks](Creative-Source-Blocks).

## Details

- ID: `nerospace:fluid_tank` · Tool: pickaxe, iron tier · Drops: itself
- Config: `fluidTankCapacity`
