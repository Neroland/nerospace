# Trash Can

A bottomless sink for unwanted items, fluids, and gas.

## Overview

Pipe or hopper anything into the Trash Can and it is **destroyed**. It accepts all three transfer
layers — **items, fluids, and gas** — from any side, with no extraction surface, so nothing can ever
be pulled back out. Handy for dumping the cobble and dirt a [Quarry Controller](Quarry-Controller)
digs up (until item filters arrive), venting excess gas, or draining a fluid line.

## Obtaining

**Craft** (shaped):

```text
I I I
I C I
I I I
```

`I` = Iron Ingot · `C` = Cactus

## How it works

- **Voids every layer:** exposes the item, fluid, and gas capabilities on **all six faces**; whatever
  is inserted is discarded.
- **Never backs up:** its internal sinks are emptied every tick, so it always has room and never
  rejects or returns anything.
- **Input only:** there is no way to extract from it — your modules, fuel, or anything else routed
  past it stays safe; only what is explicitly piped *into* the Trash Can is lost.
- **No energy:** it does not accept power (energy isn't "trash"); only items, fluids, and gas.

## Tips

Point a [Universal Pipe](Universal-Pipe) face set to **OUT** at the Trash Can to dump a filtered
stream, or sit one under a quarry/machine output to auto-clear overflow.

## Details

- ID: `nerospace:trash_can` · Tool: pickaxe · Drops: itself
- Capabilities: items **in**, fluid **in**, gas **in** (all sides) — all discarded; no extraction
