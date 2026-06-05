# Nerospace — Oxygen Field & Terraform System (design spec)

Status: **design / not yet implemented.** This is the "super advanced" successor to the Phase 8c–8e
oxygen system. It replaces the current per-player, on-demand flood-fill in `GreenxertzAtmosphere`
with a **persistent per-block oxygen field** that propagates, dissipates and leaks, and adds a
**Terraformer** machine that slowly converts a dead planet into a livable one.

It builds on what already exists — do **not** rewrite these, extend them:
- `ModAttachments.OXYGEN` (per-player int) — kept as the player's *personal* reserve / suit tank.
- `OxygenGeneratorBlockEntity` — kept, but becomes an **oxygen source** feeding the field instead of
  projecting a raw radius bubble.
- `GreenxertzAtmosphere#onPlayerTick` — kept as the player drain/refill/suffocate loop, but its
  `isBreathable` / `isInSealedRoom` are replaced by an **O(1) field lookup**.
- Machine scaffolding (energy buffer via `SimpleEnergyHandler`, `Capabilities.Energy.BLOCK`, fuel
  upkeep, `ContainerData` GUI sync) — reused verbatim for the Terraformer.

Decisions locked with Dario:
- Oxygen model = **hybrid field near sources** (real per-block values, simulated only in a bounded
  active region around each source).
- Visuals = **all four layers** (drifting particles, soft haze/tint, boundary shimmer + sound, HUD
  gauge), each independently config-gated.
- Terraform = **full**: terrain + permanent atmosphere + seeded resources.
- Terraform growth = **expanding sphere, energy-gated, uncapped** — as long as it has power it keeps
  growing; **higher machine tiers expand faster**.

---

## Part 1 — Oxygen field

### 1.1 The core idea

One diffusion-with-decay rule produces *both* behaviours Dario asked for, for free:

- **Open space → dissipates.** Oxygen injected at a source spreads to neighbours, but every cell also
  bleeds a little to the vacuum each step. Far from the source, loss outpaces supply, so concentration
  falls off with distance and the breathable bubble stays small.
- **Sealed space → fills the volume.** Walls block the bleed, so oxygen accumulates until the whole
  enclosed volume reaches near-max.
- **A hole → leaks out.** Cells next to an opening act as sinks; the room reaches a steady-state
  gradient draining toward the hole. A small hole bleeds slowly; a big hole means the room can't stay
  pressurised. Exactly the "fills the closed area and leaks out of any open blocks" behaviour.

So we don't special-case "sealed vs open" at all — the physics decides. That's what makes it feel
advanced instead of scripted.

### 1.2 Storage

Per-block oxygen is **sparse** (only oxygenated cells exist) and should unload with its chunk.

- **Per-`LevelChunk` data attachment** `ModAttachments.OXYGEN_FIELD`: a `Long2ByteMap`
  (chunk-local packed pos → concentration `0..MAX`, `MAX = 15` is plenty and 1 byte). Sparse — absent
  key = 0 = vacuum. Lifetime ties to chunk load/save; mark `chunk.setUnsaved(true)` on change.
- A per-`ServerLevel` **`OxygenFieldManager`** (a `SavedData` from `level.getDataStorage()`) holds the
  light index that survives independent of which chunks are loaded: the set of **active sources**
  (generator positions) and per-source bookkeeping (active-cell count, frontier cursor). The actual
  cell values live in the chunk attachments; the manager just drives simulation and answers lookups.

> 26.1 note: attachments are registered on `ModAttachments` like `OXYGEN` already is; `SavedData` in
> 26.1 uses the `SavedData.Factory` + `Codec` constructor — javap-probe `getDataStorage().computeIfAbsent`
> for the exact signature before writing it.

### 1.3 Simulation (server, throttled)

A relaxation pass over the **active set** (cells with concentration > 0 plus their passable
neighbours), run every `oxygenSimIntervalTicks` (default **5** ticks ≈ 4 Hz, not every tick):

```
for each active cell p (double-buffered: read old, write new):
    if p is a source cell:           new[p] = MAX                       // injection clamp
    else:
        inflow = sum over passable neighbours n of (old[n] - old[p]) * DIFFUSION
        new[p] = clamp(old[p] + inflow - DECAY, 0, MAX)                 // DECAY = bleed to vacuum
    if new[p] == 0: drop p from the field (and from the active set)
    else: add p's passable neighbours to next active set
```

- **Double-buffer** (or BFS relaxation with a frontier queue) so update order doesn't bias flow.
- **DECAY** is the vacuum sink. It's why open-air bubbles stay finite and why holes leak. A cell that
  borders a *sealing* block on that side simply has no neighbour there — no inflow, no outflow, no
  decay across the wall.
- **Bounding cost:** the active set is naturally bounded (a sealed room = its volume; an open bubble =
  small because decay kills it within a few blocks). Add a hard `oxygenMaxActiveCellsPerSource` safety
  cap — if exceeded (e.g. a generator dumped into open vacuum), stop expanding the frontier; the bubble
  just stops growing. Round-robin sources and budget **N cells per tick** so a base full of generators
  can't spike a tick.
- **Pause when nobody's near:** skip simulation for sources with no player within `oxygenSyncRadius`
  (and no rendered field needed). State is persisted, so it resumes correctly. (Terraformed *permanent*
  oxygen is a flag, not simulated — see §3.4 — so this pause is safe.)

### 1.4 Block sealing classification

Replace today's `isPassable = state.isAir()` with a richer, **data-driven** classifier driven by
block tags generated in `ModBlockTagProvider`:

- `nerospace:oxygen_sealing` — full opaque cubes, glass, station walls → **block all flow** (let
  players build airtight rooms with windows: glass seals).
- `nerospace:oxygen_leaks` — fences, non-full blocks, torches, open trapdoors, slabs with a gap →
  **partial flow**.
- Air and `nerospace:oxygen_leaks` members that are *open* → full flow.
- **Doors / trapdoors**: flow only when `state.getValue(OPEN)` — open a door and the room leaks.

Using tags (not hardcoded `Block` checks) keeps it moddable and matches your "generate JSON via
datagen" convention.

### 1.5 Sources

Generalise beyond the one generator:

- An **`OxygenSource`** contract — simplest as a tag `nerospace:oxygen_source` + a block-entity
  `isActive()` check, mirroring how `OxygenGeneratorBlockEntity#isActive()` already gates on stored
  energy. The manager registers/forgets sources on block place/break/neighbour-update.
- The existing **Rocket Launch Pad** landing-site safety becomes a permanent low-strength source so
  arrivals still aren't punished (keeps the Phase 8c behaviour).
- Later passive sources (terraformed ground, alien flora) plug in via the same tag.

### 1.6 Breathability check (replaces the flood-fill)

In `GreenxertzAtmosphere#onPlayerTick`, swap the per-tick `isBreathable || isInSealedRoom` (currently
a scan + a flood-fill **every** `CHECK_INTERVAL_TICKS`) for:

```java
int o2 = fieldManager.concentrationAt(player.eyeBlockPosition());
boolean breathing = o2 >= Config.OXYGEN_BREATHABLE_THRESHOLD.get()
        || terraformedBreathable(level, player)        // §3.4 — O(1) chunk-flag test
        || isWearingFullSuit(player);                  // unchanged
```

This is a hash lookup instead of a BFS per player per scan — **cheaper** than today *and* correct
about leaks/dissipation. The personal `OXYGEN` reserve, suit tank, suffocation damage, and air-supply
mirror loops stay exactly as they are.

### 1.7 Client visuals (all four, each config-gated)

The field is server-authoritative. Sync a **downsampled, range-limited** view to nearby clients via a
NeoForge `PayloadRegistrar` packet: only cells within `oxygenSyncRadius` of the player, delta-encoded
on change. The client keeps a small local copy for rendering and interpolates.

A `Config.OxygenVisualQuality` enum (`OFF / MINIMAL / FULL`) plus per-layer intensity sliders lets
low-end machines drop the heavy layers.

1. **Drifting particles** — a custom additive `ParticleType` spawned sparsely in breathable cells at a
   rate ∝ local concentration, so leak edges naturally thin out. Cheap, short-lived, the "this air is
   alive" cue. Register via `RegisterParticleProvidersEvent`.
2. **Soft haze / tint** — a very light translucent fog when the camera is inside oxygenated air, density
   ∝ local concentration, fading to clear at the boundary. Hook the 26.1 fog event
   (`ViewportEvent.RenderFog` / `ComputeFogColor` — javap-probe the exact name). Keep alpha low; slider.
3. **Boundary shimmer + sound** — detect "membrane" cells (breathable but bordering vacuum) and render
   a faint translucent quad on that face; crossfade an ambient loop when the player's breathable state
   flips. *Sound needs a real `.ogg`* (can't be generated) → register `SoundEvent` + `sounds.json`,
   placeholder with a vanilla ambient until audio exists (same caveat as the rocket launch sound).
4. **HUD gauge** — upgrade the current air-supply *mirror* into a bespoke O₂ readout (local
   concentration + suit-tank), drawn via the `GuiGraphicsExtractor` path you already use for the
   machine GUIs (`extractForeground` fill/text). Keep the air-supply mirror as a fallback.

### 1.8 New config keys (`Config.java`)

`oxygenBreathableThreshold` (e.g. 6/15), `oxygenMaxConcentration` (15), `oxygenSourceInjection`,
`oxygenDiffusionRate`, `oxygenDecayPerStep`, `oxygenSimIntervalTicks` (5),
`oxygenMaxActiveCellsPerSource`, `oxygenSyncRadius` (32), `oxygenVisualQuality` (enum) + per-layer
intensity sliders. Keep the existing `oxygenMax`, `oxygenDrainPerTick`, `oxygenSuitDrain`,
`atmosphere*` keys. `oxygenBubbleRadius` and `oxygenSealedRoomMax` are **retired** by the field model
(leave deprecated or remove with the flood-fill).

---

## Part 2 — Terraformer machine

A new `TerraformerBlockEntity`, built on the same chassis as `OxygenGeneratorBlockEntity` /
`FuelTankBlockEntity`: internal energy buffer (`SimpleEnergyHandler` + `Capabilities.Energy.BLOCK`),
fuel/energy upkeep, `ContainerData`-synced GUI, comparator output. Three **tiers** (T1/T2/T3) — either
distinct blocks or one block + an upgrade slot — controlling **work budget per tick** (conversion
speed) and the per-step radius advance.

### 2.1 Expanding-frontier algorithm (uncapped, energy-gated)

Storing a giant sphere is a non-starter, so store almost nothing and advance a frontier:

Per machine, persisted (tiny): `center`, `currentRadius` (double), and a `shellCursor`.

```
each work cycle (throttled + only if energy >= costPerBlock):
    process up to BUDGET(tier) columns from the current spherical shell [r, r+Δ]:
        convert the column (§2.2), spend energy per block
    advance shellCursor; when the shell is exhausted:
        currentRadius += Δ          // grow outward, no cap
```

- **Idempotent conversion** means we never need to remember which blocks we changed — re-running a
  shell is a no-op. So persistence is just `center + radius + cursor`. Survives reload trivially.
- **Energy = the real throttle.** Cost is *per block converted*, so a bigger sphere has a bigger shell
  and naturally takes longer in wall-clock time unless you feed more power or build a higher tier.
  "Uncapped" in radius, self-limiting in rate. Good balance without an artificial wall.
- **Hard per-tick work cap** regardless of tier, to protect TPS.

### 2.2 What a column converts (full)

Within the radius, per surface column, via a **data-driven conversion table** (tags like
`nerospace:terraform_to_dirt`, `..._to_grass`), so Greenxertz deadrock and Cindara basalt map
correctly and it's moddable:

- **Terrain:** topmost solid → grass block; the few layers under it → dirt; deeper stays native stone.
- **Atmosphere:** flag the column/chunk as **terraformed** → permanently breathable at/above ground
  (§3.4), no generator needed. The end-game payoff.
- **Plants:** sparse, throttled scatter of grass/flowers and saplings → trees (reuse vanilla
  features or a light placement pass).
- **Water/ice:** optional melt/fill of low cells (config, default modest).
- **Resources:** over time, sprinkle ores into the converted subsurface. **Balance guardrails:** gate
  behind T3 only, low rate, and a **configurable ore list defaulting to Nerospace ores** so it doesn't
  trivialise vanilla mining. Default the rate low; make the whole resource layer toggleable.

### 2.3 Loaded-chunk handling

The frontier will reach unloaded chunks. Two options, config-selectable:

- **Lazy (default):** skip unloaded columns; they convert the next time a player loads them (radius is
  stored, so the shell re-scans on demand). No runaway chunk loading.
- **Active:** a bounded chunk-loading **ticket** around the working arc so terraforming continues while
  you're away. Off by default — opt-in, with a max-loaded-chunks guard, because force-loading is the
  classic TPS/footprint footgun.

> 26.1 note: chunk tickets go through the `TicketController` / forge chunk-load API — javap-probe the
> exact 26.1 registration before relying on it.

### 2.4 Visual / feedback

Particles + sound at the active frontier, optional terraform beacon beam, gradual colour shift as a
shell converts. All config-gated, same `OFF/MINIMAL/FULL` quality enum as the oxygen visuals.

---

## Part 3 — Where the two systems meet

### 3.4 Terraformed = permanently breathable (cheap)

Do **not** simulate a planet-sized oxygen field for terraformed land — that's what the active-set cap
exists to prevent. Instead, mark terraformed chunks with a boolean `ModAttachments.TERRAFORMED`
(per-chunk). The atmosphere check short-circuits:

```java
boolean terraformedBreathable(level, player) {
    return level.getChunkAt(player.blockPosition()).getData(ModAttachments.TERRAFORMED)
        && player.getY() >= surfaceY(...);   // breathable at/above ground, not in a buried vault
}
```

O(1), no simulation. Generators + the diffusion field still handle sealed rooms and pre-terraform
bases; terraformed open ground is breathable by flag. Inside a sealed, *un*-terraformed bunker you
still need a generator — which is the right outcome.

---

## Part 4 — Suggested rollout (your phase style)

Each slice is independently shippable and **verified green via the gradle MCP** before the next, per
`CLAUDE.md`.

- **Phase O1 — field core:** attachments + `OxygenFieldManager` + diffusion sim + sealing tags +
  source registration; swap `GreenxertzAtmosphere`'s flood-fill for the field lookup. No new visuals
  yet (air-supply mirror still drives the HUD). Verify green.
- **Phase O2 — sync + particles:** client field-view packet + drifting-particle layer.
- **Phase O3 — haze + boundary + HUD:** fog tint, membrane shimmer + (placeholder) sound, bespoke HUD
  gauge.
- **Phase T1 — Terraformer machine + frontier:** block/BE/GUI + expanding terrain conversion (grass/
  dirt only) + lazy chunk handling. Verify green.
- **Phase T2 — atmosphere integration:** terraformed-chunk flag + permanent breathability + plants.
- **Phase T3 — resources + polish:** gated ore seeding, frontier visuals, balance config, optional
  active chunk-loading.

## Part 5 — 26.1 / NeoForge API checklist (probe before coding)

- `ModAttachments` registration for `OXYGEN_FIELD` (chunk) and `TERRAFORMED` (chunk); `setUnsaved` on
  change.
- `SavedData.Factory` + `Codec` signature for the per-level `OxygenFieldManager`.
- `RegisterParticleProvidersEvent` for the ambient O₂ particle.
- `PayloadRegistrar` (`registerPayloadHandlers`) for the field-view sync packet.
- Fog hook name (`ViewportEvent.RenderFog` / `ComputeFogColor` / `…Density`) — **javap-probe**, these
  moved in 26.x.
- `SoundEvent` + `sounds.json` (needs real `.ogg`; placeholder vanilla for now).
- Chunk `TicketController` API for opt-in active terraforming.
- `ModBlockTagProvider` datagen for `oxygen_sealing` / `oxygen_leaks` / `oxygen_source` /
  `terraform_to_*` tags; remember datagen doesn't delete stale JSON.

## Part 6 — Logging & data (POPIA / GDPR)

Per your standing preference: any debug logging added for these systems must stay
**non-personal** — log dimension, positions, and anonymous counts (active cells, converted blocks),
**never** usernames, UUIDs, IPs, or chat. Gate verbose field/terraform logging behind a debug config
flag (default off) so a shipped server isn't continuously writing player-correlatable traces. The
persisted state here (per-chunk oxygen/terraformed data, per-player `OXYGEN` reserve) is ordinary save
data required for gameplay, not analytics — fine to keep, but don't add any extra player-identifying
fields to it.
```
