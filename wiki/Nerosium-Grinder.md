# Nerosium Grinder

Your first machine — grinds ore into dust to double your metal yield.

## Overview

The Nerosium Grinder processes nerosium inputs into **Nerosium Dust** over time. Since each dust smelts
back into a Nerosium Ingot, grinding ore before smelting **doubles** your ingot output.

## Obtaining

**Craft** (shaped):

```text
I I I
I F I
C C C
```

`I` = Nerosium Ingot · `F` = Furnace · `C` = Cobblestone

## How it works

- **Two slots:** an **input** (top/sides) and an **output** (bottom).
- **Grinding recipes:**
  - Nerosium Ore / Deepslate Nerosium Ore / Raw Nerosium → **2 Nerosium Dust**
  - Nerosium Ingot → **1 Nerosium Dust**
- **Power:** it has an internal energy buffer (10,000 FE) and currently **self-charges**
  (~15 FE/tick), spending ~30 FE/tick while grinding. A full grind takes ~100 progress ticks.
- **Automation:** the inventory is exposed via the item capability — hoppers/pipes can **insert into
  the input from the top or sides and extract dust from the bottom**.
- **GUI:** shows a power gauge and grind-progress; emits a **comparator signal** from its energy level.

## Tips

Feed it raw nerosium or ore, pipe the dust into a furnace array, and you get 2 ingots per ore instead
of 1.

## Details

- ID: `nerospace:nerosium_grinder` · Tool: pickaxe, iron tier · Drops: itself
