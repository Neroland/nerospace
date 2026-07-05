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
same colours as the panel rows (down blue, up yellow, north red, south green, west purple, east
orange) — faces with a filter installed glow brighter — and any pipe holding a filter shows a
**floating copy of the filter item** above it, so filtered segments stand out across a whole line.

## Tips

**Items only enter the network through faces set to In** — Auto pushes items out but never pulls
them (energy/fluid/gas Auto still pull). Set the face touching each source chest or machine
output to **In**; faces feeding machines and chests are happiest on **Out** or Auto.

## Details

- ID: `nerospace:configurator` · Stack size: 1
