# Nerospace — Oxygen / Atmosphere System (design spec)

Status: **spec + Phase 8c first slice**. This replaces the flat "suffocate off a launch pad"
placeholder (`GreenxertzAtmosphere`) with oxygen as a real, depletable resource and a machine that
creates breathable zones. Space suits build on this later.

## Goals

- Off-world dimensions (Greenxertz, Cindara, the Orbital Station) are **airless**. An unprotected
  player carries a finite **oxygen** supply that drains while exposed and must be replenished by
  standing in a **breathable zone**.
- Breathable zones are produced by **Oxygen Generator** machines (and, transitionally, by a Rocket
  Launch Pad — the landing site stays safe so arrivals aren't instantly punished).
- Oxygen runs out → suffocation damage, exactly as the old system, but now *gated on the oxygen
  stat* rather than raw proximity.
- A **HUD indicator** shows remaining oxygen.

## Model

### Oxygen as a resource
- Stored per player as a NeoForge **data attachment** (`ModAttachments.OXYGEN`, an `int`), persistent
  across logout and **copy-on-death**. Range `0 .. OXYGEN_MAX` (default 300, matching vanilla's
  air-supply scale so it maps cleanly to the bubble HUD).
- Only **off-world dimensions** touch oxygen; in the overworld the stat is ignored and untouched
  (vanilla water-breathing keeps working normally).

### Tick logic (server, `PlayerTickEvent.Post`)
Each tick, for a survival/adventure player in an airless dimension:
1. **In a breathable zone** → set oxygen to `OXYGEN_MAX` (refill).
2. **Otherwise** → subtract `OXYGEN_DRAIN_PER_TICK` (floored at 0).
3. **Oxygen at 0** → apply `ATMOSPHERE_DAMAGE` every `DAMAGE_INTERVAL` ticks + an occasional warning.
4. **Mirror** oxygen onto the player's vanilla **air supply** (`setAirSupply`) so the bubble HUD
   renders for free — full oxygen shows no bubbles, draining shows them empty out. (A bespoke
   oxygen-tank HUD bar is deferred until the 26.1 `GuiGraphicsExtractor` render path is worked out.)

Creative/spectator players are exempt; the whole system is gated by `ATMOSPHERE_DAMAGE_ENABLED`.

### Breathable zones
`isBreathable(level, pos)` scans a small cube around the player for either:
- a **Rocket Launch Pad** within `ATMOSPHERE_SAFE_RADIUS` (legacy landing-site safety), or
- an **active Oxygen Generator** within `OXYGEN_BUBBLE_RADIUS` (spherical).

### Oxygen Generator machine
- Block `oxygen_generator` backed by `OxygenGeneratorBlockEntity`, modelled on the Nerosium Grinder:
  an internal **energy buffer** (`SimpleEnergyHandler`) exposed via `Capabilities.Energy.BLOCK` so a
  real generator can power it later. For this slice it self-charges (RTG placeholder) and is
  **active** whenever charged, drawing a trickle per tick.
- While active it projects a breathable bubble of radius `OXYGEN_BUBBLE_RADIUS`.
- Crafted from nerosteel + a fuel canister + redstone/glass (a sealed pump).

## Config (`Config.java`)
- `oxygenMax` (300) — full oxygen.
- `oxygenDrainPerTick` (2) — drain while exposed (≈7.5 s from full to empty by default).
- `oxygenBubbleRadius` (5) — generator bubble radius.
- (reuses existing `atmosphereDamageEnabled`, `atmosphereDamage`, `atmosphereSafeRadius`.)

## Progression / gating
- The Oxygen Generator is the first survival way to make an off-world base habitable, so it gates
  comfortable exploration of Greenxertz/Cindara and station building.
- **Space suits** (next): wearable armour that supplies oxygen so the player can leave a bubble;
  gated behind nerosteel/cindrite + station materials. Suit check slots into `isBreathable` /
  the drain branch (a worn suit pauses drain or drains a suit tank instead).

## Out of scope for the first slice (follow-ups)
- Bespoke oxygen HUD bar (needs the `GuiGraphicsExtractor` path); using the air-bubble mirror for now.
- Sealed-room detection / oxygen distribution (flood-fill an enclosed space) — bubbles are a simple
  radius for now.
- Space suits and suit oxygen tanks.
- Real power generation to run the generator (it self-charges as a placeholder).
- Client-sync of the oxygen attachment (the air-supply mirror is already client-visible).
