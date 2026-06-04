# Roadmap

A public, high-level view of where Nerospace is and where it's going. Nerospace is an early work in
progress — order and scope may change. (Developer-facing detail lives in `FUTURE_WORK.md` in the repo.)

## ✅ Done

**Materials & machines**
- Nerosium material chain (ore → raw → ingot → dust) and the **Nerosium Grinder** (double ore yield).
- Greenxertz metals: **Nerosteel** and **Xertz Quartz**; Cindara's **Cindrite**.
- Storage blocks, **Station Floor/Wall**, and machine GUIs with a sci-fi look + comparator output.
- Capability automation (hoppers/pipes feed machines; fluid pipes fill tanks).

**Space travel**
- **Rockets** (Tier 1/2/3) with an interactive in-rocket UI, selectable destinations, and a launch flow.
- **Rocket Launch Pad** + auto-fuelling **Fuel Tank** (faster on a full 3×3 pad).
- Dimensions: **Greenxertz**, **Cindara**, and the **Orbital Station**.

**Survival & atmosphere**
- Airless dimensions with an **oxygen system**: per-block oxygen field, sealed-room fill, leak/evaporate.
- **Oxygen Generator** (fuelled) and the four-piece **Oxygen Suit** (portable life support).
- Bespoke **O₂ HUD bar**.

**Worlds & life**
- Custom skies: **space starfield** on Cindara & the Station, day/night + sun on Greenxertz.
- Four bespoke **creatures** with walk animations and emissive glow.
- **Terraformer**: expanding terrain conversion + permanent breathability + a vibrant **terraformed biome**.

**Logistics (the big pipe update)**
- The **Universal Pipe**: one translucent, connection-aware tube carrying **energy, fluids, gases and
  items at the same time**, with per-face × per-layer I/O modes.
- **Items travel visibly** through pipes (round-robin routing, reroute-or-wait, never spilled);
  coloured streams show energy/fluid/gas flow.
- **Configurator** tool + full 6×4 configuration panel; **Pipe Filters** and **Speed/Capacity
  Upgrades**.
- Generators (**Combustion**, **Passive**), **Battery**, **Fluid Tank**, **Gas Tank**, **Item Store**
  — plus creative endless-source variants of all four.
- A dedicated **gas system** (Oxygen first): the Oxygen Generator is now a grid-powered electrolysis
  machine producing pipeable O₂; the Terraformer is grid-powered too.

**Tooling**
- Creative `/nerospace gallery` showcase command with live pipe demonstrations per resource layer.

## 🛠️ In progress / next

- **Launch-pad multiblock** — key rocket size/placement to a properly formed 3×3 pad.
- **Rocket fuel-intake automation** — let hoppers feed canisters into a deployed rocket.
- **Oxygen Suit tiers** — Cindrite/station-gated upgrades (bigger tank, faster refill); suits
  refilling from piped/tanked oxygen.
- **Polish** — animated machine gauges, rocket-fuel fluid render, per-creature texture art.

## 🔭 Planned / exploring

See **[Future Features](Future-Features)** for the longer-term wish list.

## ⏳ Deferred

- **Cross-mod integration** (e.g. Mekanism) is deferred until those mods port to Minecraft 26.1. The
  mod is being built **standalone**; we prefer tags + NeoForge capabilities so integration is mostly
  free later.
