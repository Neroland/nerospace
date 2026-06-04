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
- **Sneak-right-click a pipe** → opens the **configuration panel**: a 6-face × 4-layer grid of
  buttons; click any cell to cycle its mode. Changes apply instantly.
- **Right-click a pipe face** → quick-cycles that face's mode for the currently selected layer.
- **Sneak-right-click in air** (or on non-pipe blocks) → cycles which layer the quick mode edits
  (Energy → Fluid → Gas → Items).

## Tips
Faces touching an endless source (or any block that both gives and takes) should be set to **In** so
the pipe doesn't push its buffer straight back. Faces feeding machines are happiest on **Out**.

## Details
- ID: `nerospace:configurator` · Stack size: 1
