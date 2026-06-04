# Nerospace — Roadmap

A public, high-level snapshot of the mod's progress and plans. Nerospace is an early **work in
progress** (Minecraft 26.1 / NeoForge), built standalone — order and scope may change.

📖 Full block-by-block docs and the detailed roadmap live in the **[Wiki](../../wiki)**
([Roadmap](../../wiki/Roadmap) · [Future Features](../../wiki/Future-Features)).

## ✅ Implemented
- **Materials & machines** — Nerosium chain + Nerosium Grinder; Nerosteel, Xertz Quartz, Cindrite;
  storage & station blocks; machine GUIs + comparator output; hopper/pipe automation (gametested:
  capability-fed items are consumed by every machine).
- **Space travel** — Tier 1/2/3 rockets with an interactive UI; formed **3×3 launch pad** gating
  (Tier 3 needs a Station Wall ring) + auto-fuelling Fuel Tank + hopper/pipe fuel feeding through the
  pad; the Greenxertz, Cindara and Orbital Station dimensions.
- **Survival** — airless atmospheres, per-block oxygen field (sealed-room fill, leaks, evaporation),
  the Oxygen Suit (two tiers — the cindrite Tier 2 doubles the tank) with **airlock refilling** from
  Gas Tanks / Oxygen Generators, and an O₂ HUD.
- **Logistics** — the **Universal Pipe** (energy + fluid + gas + items in one translucent tube, all at
  once) with per-face × per-layer modes, visible travelling items, coloured flow streams, the
  Configurator + config panel, filters and speed/capacity upgrades; Combustion & Passive Generators,
  Battery, Fluid/Gas Tanks, Item Store (+ creative endless sources); a dedicated **gas system** —
  the Oxygen Generator is a grid-powered electrolysis machine producing pipeable O₂, and the
  Terraformer runs on grid power.
- **Worlds & life** — space/starfield skies, four bespoke glowing creatures with walk animations, and
  the **Terraformer** (expanding terrain conversion + permanent breathability + a vibrant terraformed
  biome).
- **Tooling** — the creative `/nerospace gallery` showcase command with live pipe demos; a
  **gametest suite** (pad gating, intake automation, airlock refill, registry-sync round-trip).

## 🛠️ Next up
- Machine/visual polish (animated gauges, fluid render, per-creature texture art); bigger multiblock
  pad; space-suit hazard variants.

## 🔭 Later / exploring
- More planets & destinations, multiple player-built stations, deeper terraforming, bespoke audio,
  advancements/guidebook, and cross-mod interoperability (built on standard capabilities, so most of
  it comes free once other tech mods reach 26.1). See the
  [Future Features](../../wiki/Future-Features) page.

## Contributing / feedback
Ideas and bug reports are welcome — please open a GitHub issue.
