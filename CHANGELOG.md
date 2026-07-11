# Changelog

All notable changes to **Nerospace** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

**Public environment and progression APIs (`za.co.neroland.nerospace.api`)**

- Historical planet-visit queries and first-visit events, persisted by UUID without timestamps and
  routed through Core's shared player-data erasure hook.
- Immutable atmosphere/environment snapshots covering loaded state, planet, hazard, gravity, oxygen,
  terraforming stage, and breathability.
- Bounded, expiring oxygen contributions keyed by caller-owned source ids, with no player attribution
  and no effect in unloaded regions.
- Persisted reversible regional terraforming overlays guarded by a pluggable claim policy. The default
  denies mutation; public snapshots expose no owner UUID and no internal manager.
- Plain-JVM tests for visit persistence/erasure, oxygen add/remove/decay, atmosphere resolution,
  overlay persistence, rollback, and authorization.

**Marker-less quarry setup (frame outline)**

- The quarry area can now be defined **without landmarks**: outline a closed rectangle with
  hand-placed **Frame Casing** (it is now a placeable block item that places `quarry_frame`)
  and put the Quarry Controller beside it — the controller detects the complete perimeter
  (same plane, same size limits as the landmark path) and starts mining inside it immediately.
  The landmark flow is unchanged.
- The GUI now shows *why* the quarry is paused ("Paused — frame incomplete / out of power /
  output buffer full / fluid buffer full / out of Frame Casing / wrong planet").

**Public cargo-rocket route API (`za.co.neroland.nerospace.api`)**

- New semver-stable route surface for logistics consumers (NeroLogistics' `RouteProvider` seam):
  - `NerospaceRoutes` — static lookup facade: `endpoints()` (Home + Orbital Station + the three
    moons, stable order), `endpoint`/`isEndpoint`, `route(from, to)`, `routesFrom(from)`, and
    `isOpen(...)` liveness checks against loaded dimensions.
  - `RouteEndpoint` — immutable endpoint record (dimension key, display name, optional `PlanetId`).
  - `CargoRoute` — immutable directed route record carrying the minimum rocket tier (1–4), the
    per-launch fuel cost in millibuckets (the same cross-dimension launch cost a crewed rocket
    pays, `fuelCost`-config-scaled at query time), and a canonical transit duration in ticks
    (`TRANSIT_TICKS_PER_STEP` per step of separation in the destination order).
- Read-only and player-data-free; the internals (`rocket.Destinations`, `rocket.RocketTravel`,
  `rocket.RocketTier`) remain internal.

**Meteor Tracker is now craftable (survival)**

- New shaped recipe: a vanilla **Compass** with **Nerosteel Ingots** above and below and
  **Redstone Dust** at the sides. The tracker was previously creative-only. (The four
  destination compasses remain creative-only pending a rework.)

**Optional Energized Power interop declared**

- All three loader manifests now declare **Energized Power** as an optional/suggested
  dependency with `AFTER` load ordering, so its capabilities and `c:` tags are ready
  before Nerospace loads when it is installed. Nerospace stays fully independent of it.

### Fixed

**Machine blocks now drop themselves when mined**

- Several blocks used `requiresCorrectToolForDrops` but were missing from the
  `minecraft:mineable/pickaxe` tag, so **no tool ever counted as correct and they dropped
  nothing**: Fuel Tank, Fuel Refinery, Oxygen Generator, Terraformer, Hydration Module,
  Terraform Monitor, Star Guide, Solar Panel (all tiers), Quarry Controller, Rocket Launch
  Pad, Launch Gantry, Village Core (plus Meteor Core, Launch Controller and its filler for
  mining-speed parity). All are now pickaxe-mineable; loot-table-dropping ones also need an
  iron tool, matching the other machines. Solar Panel T2/T3 keep their one-item multiblock
  drop from code (their loot tables are now empty to prevent a duplicate drop).

### Changed

- Raised the required Neroland Core floor to `1.8.0` and bumped Nerospace to `1.0.0-beta.8`.

**Quarry frame lifecycle**

- **Breaking a frame block now drops a Frame Casing**, and the quarry notices the broken ring:
  it rebuilds the gap from its casing stock, or pauses with "frame incomplete" until the
  player patches the ring by hand or inserts casings into the frame slots.
- **A finished dig dismantles its frame**: the standing frame blocks are removed and their
  casings returned to the controller's frame slots (spilled at the controller if the slots
  overflow). Breaking the controller still drops everything it holds.
- **Breaking the controller mid-dig no longer silently deletes the frame.** The orphaned frame
  blocks now *decay*: each one crumbles (break effect, **dropping its Frame Casing** — the same
  drop as mining it yourself) at its own random moment, the ring slowly dissolving block by
  block over roughly the next 30 seconds to 4 minutes (tunable via the new
  `quarryFrameDecayTicks` config key). Placing a new controller beside a still-complete
  orphaned ring re-adopts it and stops the decay.

**Storage blocks moved to Neroland Core**

- The **Battery**, **Fluid Tank**, **Gas Tank**, **Item Store**, and **Trash Can** (the first
  four with their **Creative** variants) now ship in the shared **Neroland Core** library
  instead of Nerospace. Their ids changed from `nerospace:*` to `nerolandcore:battery`,
  `nerolandcore:fluid_tank`, `nerolandcore:gas_tank`, `nerolandcore:item_store`,
  `nerolandcore:trash_can` (and `nerolandcore:creative_*`), so every Neroland mod now shares one
  set of storage endpoints. They craft and behave exactly as before.
- Bumped the required **Neroland Core** dependency to **1.1.0** (the release that adds these
  blocks and the generic fluid/gas storage APIs; the Trash Can also brings Core's first menu
  type and client screen).
- The **Universal Pipe** still interoperates with all of these blocks — it bridges Core's
  `nerolandcore:fluid` / `nerolandcore:gas` (and energy / item) capabilities onto Nerospace's
  own `nerospace:fluid` / `nerospace:gas` lookups, so existing pipe networks keep working
  (including voiding into the Trash Can).

**Generators now push power directly (no cables needed)**

- The **Combustion Generator**, **Passive Generator**, and **Solar Panel** now default
  their energy **auto-eject to ON**: they push power into adjacent receivers every tick —
  machines, batteries, pipes, and (via Neroland Core 1.3.2's Forge-Energy fallback)
  third-party FE blocks. Auto-eject can still be toggled off per machine in the
  side-config UI.

**Quarry pacing rebalance**

- The quarry's mining interval increased from 8 to 10 ticks — a modest slow-down to
  rebalance throughput against its power draw.

**Neroland Core dependency bumped to 1.3.2**

- Brings the standard **Forge-Energy fallback** in Core's energy lookup: the **Universal
  Pipe** now connects to and transfers with third-party FE cables and machines (e.g.
  **Energized Power**) on NeoForge and Forge, and Core's **Battery pushes power into
  adjacent blocks** without cables. See Neroland Core's changelog for details.

### Build & CI

- New **auto-assign** workflow: the maintainer is assigned to newly opened issues and PRs
  (including forked PRs via a limited, API-only `pull_request_target`).
- `publish.yml` release-note extraction now tries the exact version section first, falls
  back to `[Unreleased]`, and only uses the placeholder when neither exists.

### Migration note (existing worlds)

- **Block-id aliases preserve existing placements.** Blocks already placed as
  `nerospace:battery`, `nerospace:fluid_tank`, `nerospace:gas_tank`,
  `nerospace:item_store`, or `nerospace:trash_can` (and `creative_*`) are remapped to
  the matching `nerolandcore:` block on load:
  - **Forge** — via Forge's `MissingMappingsEvent` (fully supported).
  - **NeoForge / Fabric** — modern NeoForge removed that event and Fabric never had
    one, so a small, strictly-scoped mixin remaps these ids at the registry
    lookup instead. It is `require = 0` (best-effort): if it can't apply on a given
    version it safely no-ops rather than crashing. **Verify on your save after
    updating**; if a placed block ever drops, break and re-place it with the
    Neroland Core variant — **items and stored contents are unaffected** regardless.

## [1.0.0-beta.1] - 2026-06-27

First public **beta** of Nerospace — and the first **multi-loader** build. Nerospace now runs on
**NeoForge, Forge, and Fabric**, across **Minecraft 26.1.2 and 26.2** (the alpha was NeoForge 26.1.2
only). Each download is published tagged with its exact loader + game version. This release carries the
full 1.0.0 alpha feature set (reproduced below) plus everything added since. Expect balance changes
before the stable release; please report issues on the
[tracker](https://github.com/Neroland/nerospace/issues).

### Added since 1.0.0-alpha.1

**Wider platform support**

- Now available on **NeoForge, Forge, and Fabric** for **Minecraft 26.1.2 and 26.2** — six builds in
  all, each one a self-contained jar tagged for its exact loader and game version on CurseForge,
  Modrinth, and GitHub.

**Rocket return & the Launch Controller**

- The new **Launch Controller** block turns a launch pad into a full launch hub: a holographic preview
  of the docked rocket, powered operation, board-and-launch from the console, and clearer pad/fuel
  status feedback.
- **Rockets now come home** — return pads, arrival sites, and a return-travel system let you fly back
  from any world, with a named launch-pad registry so destinations and return points stay put.
- **Rockets carry oxygen life-support in flight**, and a launch pad can sink gas to keep the cabin
  breathable during ascent.
- **Founded stations gained ownership controls** — rename, claim ownership, and decommission your
  stations, with return-pad travel between them.

**Planetary gravity**

- Each world now has its own **gravity**. The Orbital Station is near-weightless, **Glacira** is a
  floaty low-gravity moon, **Greenxertz** is light, and **Cindara** sits closer to normal — so movement,
  jump height, and falling all feel different from planet to planet. Gravity affects everything: you,
  mobs, dropped items, arrows, falling blocks, and incoming meteors (which descend more slowly on
  low-gravity worlds). Individual biomes can carry a lighter or heavier pull than their planet's baseline,
  and **terraforming a planet restores normal, Earth-like gravity** to the ground you reclaim.
- New config option **`gravityMultiplier`** (`config/nerospace.properties`, range 0.1–10, default 1.0):
  a global scale on all gravity — set it below 1 for an even floatier game, or above 1 for a heavier one.
- Creative/op debug command **`/nerospace gravity`** reports the gravity in effect where you're standing
  (value, source, and your current gravity stat) for tuning and bug reports.

### Changed since 1.0.0-alpha.1

- Expanded anonymous crash-reporting context and tracing coverage (still opt-out, still scrubbed of
  personal data — see [PRIVACY.md](PRIVACY.md)).

### Full feature set (carried from 1.0.0-alpha.1)

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
- Loader-native capabilities exposed on every sensible block face (hopper/pipe automation works
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

[1.0.0-beta.1]: https://github.com/Neroland/nerospace/releases/tag/v1.0.0-beta.1
[1.0.0-alpha.1]: https://github.com/Neroland/nerospace/releases/tag/v1.0.0-alpha.1
