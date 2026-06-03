# Terraformer

The end-game machine: slowly converts a dead planet into livable, breathable, vibrant land.

## Overview
Place a Terraformer on a barren world, power it, and it advances an **ever-expanding circular frontier**
outward from itself — converting the surface to grass and dirt, scattering plants, marking the ground
**permanently breathable**, and recolouring it to a distinctive **terraformed biome** (vibrant neon
emerald grass and turquoise water) so you can see exactly what's been changed.

## Obtaining
**Craft** (shaped):
```
N D N
D O D
N B N
```
`N` = Nerosteel Ingot · `D` = Dirt · `O` = [Oxygen Generator](Oxygen-Generator) · `B` = Block of Nerosteel

## How it works
- **Fuel → power:** burns the same fuels as the Oxygen Generator into an internal energy buffer; the
  buffer drains as it works (higher tiers / more power = faster).
- **Expanding frontier:** each work cycle it converts a ring of surface columns, then grows its radius
  — **uncapped**, but energy-throttled (it spends energy per block). Converting the ground:
  - turns exposed stone/sand/dirt-type surfaces into **grass**, with **dirt** beneath;
  - flags the chunk **permanently breathable at/above the surface** (no generator needed there);
  - writes the **terraformed biome** so foliage/water recolour;
  - sparsely scatters grass, flowers and saplings.
- **Tiers:** drop an upgrade into the upgrade slot — a **Nerosteel Ingot → Tier 2**, a **Cindrite →
  Tier 3**. Higher tiers convert more columns per cycle; **Tier 3** also seeds a low rate of ore into
  the converted subsurface (configurable, defaults to Nerospace ores).
- **Lazy / catch-up:** columns in unloaded chunks are skipped and converted later when you explore into
  them, so terraforming finishes as you walk the planet (no forced chunk-loading by default).

## Details
- ID: `nerospace:terraformer` · Tool: pickaxe, iron tier · Drops: itself
- Config: `terraformEnergyPerBlock`, `terraformWorkIntervalTicks`, `terraformPlantChance`,
  `terraformResourcesEnabled`, `terraformResourceOres`, …
