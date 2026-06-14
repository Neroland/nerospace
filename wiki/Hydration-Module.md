# Hydration Module

The glacite intake of deeper terraforming: melts glacite into **hydration units** that fuel the
[Terraformer](Terraformer)'s **Hydrated** stage (water).

## Overview

Deeper terraforming advances in stages — Rooted → Hydrated → Living. The Hydrated stage fills
basins below the Terraformer's water table with real water, and every water source placed costs
**1 hydration unit**. The Hydration Module is where those units come from: it melts glacite
(crystallised water-ice mined on [Glacira](Home)) into the Terraformer's hydration buffer.

## Obtaining

**Craft** (shaped):

```text
G N G
N F N
G N G
```

`G` = Glacite · `N` = Nerosteel Ingot · `F` = [Fluid Tank](Fluid-Tank)

## How it works

- **Must touch the Terraformer:** place it directly against any face of a Terraformer block.
  A module with even a one-block gap feeds nothing.
- **Feed it glacite:** drop glacite (16 units each) or Blocks of Glacite (144 units) into its input
  slot — by hand, hopper, or item pipe (the slot is exposed to automation).
- **It melts one item per pulse** into the linked Terraformer's hydration buffer (cap 1,024 units);
  it never melts an item the buffer can't fully hold, so no units are ever lost.
- **No glacite = the water stage stalls.** The Terraformer GUI (and a
  [Terraform Monitor](Terraform-Monitor)) shows "Needs glacite" while stage 2 waits.

## Details

- ID: `nerospace:hydration_module` · Tool: pickaxe, iron tier · Drops: itself
- Input items are tag-driven (`nerospace:hydration_input`) — glacite-only by default; modpacks can
  widen it (vanilla ice is deliberately excluded so the Glacira trip stays meaningful).
