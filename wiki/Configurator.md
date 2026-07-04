# Configurator

The network tool — sets what each pipe face does, per resource layer.

## Obtaining

**Craft** (shaped): two Nerosteel Ingots over Redstone, wand-style.

## How it works

Every Universal Pipe face has four independent I/O modes (one per layer: Energy, Fluid, Gas, Items):

- **Auto** — pulls from providers and pushes to receivers (default)
- **In** — pull only
- **Out** — push only
- **Off** — disconnected for that layer

With the Configurator:

- **Sneak-right-click a pipe** → opens the **configuration panel**: one row per face with its
  colour chip, its mode for the selected layer (click ▸ to cycle), and its **filter slot** —
  the physical [Pipe Filter or Advanced Pipe Filter](Pipe-Filters-and-Upgrades) installed on
  that face. Drop a filter in to filter the face, take it out to clear it, and **hover the
  slot** to read exactly what the filter matches.

- **Right-click a pipe face** → quick-cycles that face's mode for the currently selected layer.
- **Sneak-right-click in air** (or on non-pipe blocks) → cycles which layer the quick mode edits

  (Energy → Fluid → Gas → Items).

While the Configurator is in your hand, every Universal Pipe **shades its six faces** with the
same colours as the panel rows (down slate, up white, north red, south green, west purple, east
amber) — faces with a filter installed glow brighter — and any pipe holding a filter shows a
**floating copy of the filter item** above it, so filtered segments stand out across a whole line.

## Tips

Faces touching an endless source (or any block that both gives and takes) should be set to **In** so
the pipe doesn't push its buffer straight back. Faces feeding machines are happiest on **Out**.

## Details

- ID: `nerospace:configurator` · Stack size: 1
