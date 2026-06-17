# Changelog

All notable changes to **Nerospace** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-alpha.1] - 2026-06-15

First public **alpha** of Nerospace, for Minecraft 26.1.2 on NeoForge. This pre-release carries
the full 1.0.0 feature set (see below) for early testing — expect rough edges and balance changes
before the stable release. Please report issues on the
[tracker](https://github.com/Neroland/nerospace/issues).

### Added

**Materials & machines**

- Nerosium ore (stone + deepslate) with the full chain: raw → ingot, dust, storage blocks,
  and the nerosium pickaxe.
- The **Nerosium Grinder** — powered ore-doubling via nerosium dust.
- Planetary materials with their own chains: **Nerosteel** and **Xertz Quartz** (Greenxertz),
  **Cindrite** (Cindara), **Glacite** (Glacira).
- Generators: **Combustion Generator** (burns fuel) and **Passive Generator** (trickle charge).
- The **Fuel Refinery** — turns coal + blaze powder + energy into pipeable rocket fuel.
- Common `c:` tags on all ores/ingots/dusts/gems/storage blocks, and tag-based recipe inputs,
  for clean cross-mod and recipe-viewer compatibility.

**Logistics**

- The **Universal Pipe** — energy, fluids, gas, and items in one tube, with per-face ×
  per-layer modes, visible travelling items, and coloured flow streams.
- The **Configurator** tool + pipe config panel, item filters, and speed/capacity upgrades.
- Storage endpoints: Battery, Fluid Tank, Gas Tank, Item Store (+ creative endless variants).
- NeoForge capabilities exposed on every sensible block face (hopper/pipe automation works
  everywhere, including feeding rockets through the launch pad).

**Rockets & travel**

- Four rocket tiers with per-tier models: **Tier 1** (Orbital Station) → **Tier 2**
  (+ Greenxertz) → **Tier 3** (+ Cindara) → **Tier 4** (+ Glacira).
- The **3×3 Launch Pad** with formation gating, plus the **Heavy Launch Complex** — a 5×5 pad
  with a **Launch Gantry** (right-click to board) that Tier 4 requires and Tier 3 accepts as
  an alternative to its Station Wall ring.
- The **Fuel Tank** pad module — auto-fuels the deployed rocket (up to 480 mB/t on a Heavy
  complex), with canister auto-feed.
- A rocket UI with painted panel, segmented fuel gauge, trajectory arc, and destination cycling.

**Dimensions**

- **Greenxertz** — a green planet with its own ores and creatures.
- **Cindara** — a volcanic moon with a heat hazard.
- **Glacira** — a frozen moon with a cold hazard.
- The **Orbital Station** — and **player-founded stations**: rename a **Station Charter** in an
  anvil, fly there, and found your own named station anchored by a **Station Core** (up to 64
  stations per world, selectable as rocket destinations).

**Oxygen & survival**

- Airless dimensions with a per-block **oxygen field**: sealed rooms fill, leaks drain,
  doors/trapdoors/glass count as boundaries.
- The **Oxygen Generator** — grid-powered electrolysis producing pipeable O₂ — plus airlock
  refilling from Gas Tanks.
- The **Oxygen Suit** in two tiers, plus hazard variants: the **Thermal Suit** (Cindara heat)
  and **Cryo Suit** (Glacira cold) — unprotected exposure quadruples oxygen drain.
- An O₂ HUD gauge with suit/hazard badges.

**Terraforming**

- The **Terraformer** — expanding terrain conversion with staged maturation:
  **Rooted → Hydrated → Living**, including a water cycle fed by the **Hydration Module**
  (glacite), per-planet mature biomes with real weather, and the **Terraform Monitor** readout.
- Three breedable livestock species on terraformed ground: **Meadow Loper**, **Ember Strutter**,
  **Woolly Drift** (food + fleece drops).

**Creatures**

- Native mobs with bespoke models, animations, and glow layers: Xertz Stalker, Quartz Crawler,
  Greenling (Greenxertz), Cinder Stalker (Cindara), Frost Strider (Glacira).

**Progression & polish**

- The **Star Guide** — a pedestal block + guidebook opening an interactive 7-chapter
  progression tree (31 steps), backed by a full advancement tree.
- A creative `/nerospace gallery` showcase command with live demos of every system.
- Bespoke art across the board: shaped machine models, per-tier rocket geometry, per-creature
  textures, animated GUI gauges, fluid/gas visuals.
- Config: five clamped multiplier keys (`oxygenDrain`, `oxygenCapacity`, `energyRate`,
  `fuelCost`, `machineSpeed`) over internal base values — modpack-friendly tuning.
- Optional, anonymous crash reporting (Sentry, EU servers) with full disclosure in
  [PRIVACY.md](PRIVACY.md) and a `telemetryEnabled = false` opt-out.

[1.0.0-alpha.1]: https://github.com/Neroland/nerospace/releases/tag/v1.0.0-alpha.1
