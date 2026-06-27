# Terraformer

<!-- nerospace:render -->
<p align="right"><img src="images/terraformer.gif" alt="Terraformer" width="150" align="right"></p>
<!-- /nerospace:render -->

The end-game machine: slowly converts a dead planet into livable, breathable, **living** land — in
three visible stages.

## Overview

Place a Terraformer on a barren world, power it, and it advances an **ever-expanding circular frontier**
outward from itself. With deeper terraforming the machine runs **three frontiers**, each trailing the
last, so a long-running world is always a gradient — raw chemistry at the edge, a living ecosystem at
the centre:

| Stage | Name | What it does |
| --- | --- | --- |
| 1 | **Rooted** | grass + dirt, permanently breathable, vibrant neon **terraformed biome**, sparse plants |
| 2 | **Hydrated** | basins below the machine's water table fill with real water — costs **glacite** via a [Hydration Module](Hydration-Module) |
| 3 | **Living** | the biome settles into a natural **per-planet palette** (meadow / savanna / tundra) with grown trees, **rain or snow**, and starter herds of the planet's [livestock](Creatures) |

## Obtaining

**Craft** (shaped):

```text
N D N
D O D
N B N
```

`N` = Nerosteel Ingot · `D` = Dirt · `O` = [Oxygen Generator](Oxygen-Generator) · `B` = Block of Nerosteel

## How it works

- **Grid power:** runs exclusively on piped energy — connect a Universal Pipe carrying FE. Its

  100,000 FE buffer drains as it works (higher tiers / more power = faster).

- **Expanding frontiers:** each work cycle it converts a ring of surface columns per stage, then grows

  that stage's radius — **uncapped**, but energy-throttled. Stage 1 keeps priority (breathable ground
  never waits); the trailing stages take smaller shares and always stay inside the stage ahead.
  Stage-2 columns cost **2×** energy plus **1 hydration unit per water source**; stage-3 columns cost
  **4×** energy.

- **Water (Hydrated):** the water table sits one block below the machine's base. Basins under it fill

  flush with the ground; hills stay dry; chasms deeper than the cap are skipped. No glacite in the
  buffer = stage 2 stalls (the GUI says "Needs glacite"). On Glacira the lakes refreeze naturally.

- **Life (Living):** the mature biome turns on real **weather** (rain; snow accumulates on Glacira),

  sparse **grown trees** (oak/birch, acacia, spruce by planet), and seeds starter pairs of the
  planet's livestock (population-capped).

- **Cosmetic drift:** settled land keeps sprouting sparse ground cover on its own — pure garnish,

  budgeted and toggleable (`terraformDriftEnabled`).

- **Tiers:** drop an upgrade into the upgrade slot — a **Nerosteel Ingot → Tier 2**, a **Cindrite →

  Tier 3**. Higher tiers convert more columns per cycle; **Tier 3** also seeds a low rate of ore into
  the converted subsurface (configurable, defaults to Nerospace ores).

- **Lazy / catch-up:** columns in unloaded chunks are skipped and converted later when you explore into

  them — all stages replay on chunk load, so a terraformed planet finishes as you walk it (no forced
  chunk-loading by default).

- **Old worlds:** existing Terraformers keep working untouched — their land counts as stage 1 and the

  new stages simply sweep over it once you build the new blocks. No migration.

## Details

- ID: `nerospace:terraformer` · Tool: pickaxe, iron tier · Drops: itself
- Companions: [Hydration Module](Hydration-Module) (glacite intake, must touch),

  [Terraform Monitor](Terraform-Monitor) (stage readout + comparator)

- Config: `terraformWaterEnabled`, `terraformWaterMaxDepth`, `terraformDriftEnabled`,

  `terraformDriftPerSecond`, `terraformFaunaEnabled`, `terraformPlantsEnabled`,
  `terraformResourcesEnabled`, `terraformResourceOres`, `terraformMaxColumnsPerTick`, …
