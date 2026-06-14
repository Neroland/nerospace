# Universal Pipe

One pipe for everything: energy, fluids, gases and items flow through the same translucent tube — at
the same time.

## Overview

The Universal Pipe is the backbone of Nerospace logistics. Placed pipes auto-connect to each other and
to any machine, tank or inventory, forming a **network** that behaves as one shared system. All four
resource layers ride the same connection graph simultaneously:

| Layer | Colour | Rule |
|---|---|---|
| Energy (FE) | red | shared pool, balanced across all segments |
| Fluid | blue | **one fluid per network** — the first fluid in claims it until drained |
| Gas | green | one gas per network; **breaking a pipe vents its gas** (visible puff) |
| Items | — | travel as **visible packets** (~2 blocks/s), round-robin between destinations |

## Obtaining

**Craft** (shaped, yields 8): a nerosteel sheath around a glass core —

```text
N N N
N G N
N N N
```

`N` = Nerosteel Ingot · `G` = Glass

## How it works

- **Connections:** the tube grows an arm toward anything it can talk to (pipes, machines, tanks,
  chests). Every face has an independent I/O mode **per layer**: Auto → In → Out → Off (set with the
  [Configurator](Configurator)).
- **Energy/fluid/gas:** the network pulls from providers, pushes to receivers and balances its own
  buffers — coloured pulse streams show what's flowing where.
- **Items:** pulling faces extract from inventories; packets physically travel through the tube,
  re-route at junctions, and **never spill** — if every destination is full they park and wait.
  Breaking a pipe drops the items inside it.
- **Filters & upgrades:** see [Pipe Filters and Upgrades](Pipe-Filters-and-Upgrades).
- **Readout:** right-click a pipe with an empty hand for its current contents; sneak-right-click with
  an empty hand pops installed upgrades out.

## Tips

One line can power a machine, feed it items and carry its outputs away at once — use face modes when
a single line serves both a source and a sink (set the source-side face to In so nothing flows back).

## Details

- ID: `nerospace:universal_pipe` · Tool: pickaxe, iron tier · Drops: itself
- Config: `energyPipeCapacity/Throughput`, `fluidPipe…`, `gasPipe…`, `itemPipeTicksPerBlock`,
  `itemPipeExtractAmount`, `itemPipeExtractPeriod`
