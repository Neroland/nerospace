# Configuration

Nerospace's config (`config/nerospace-common.toml`) is deliberately small: the mod owns its base
balance numbers in code, and packs tune them through **five multipliers**. Everything else in the
file is genuinely absolute — booleans, radii, performance caps, advanced simulation tuning and
client visual preferences.

> **Breaking change (pre-1.0):** the old flat key list (`oxygenMax`, `batteryCapacity`,
> `combustionGeneratorFePerTick`, …) was removed without migration. Delete your old
> `nerospace-common.toml` and let it regenerate.

## The five multipliers

All five default to `1.0` and are clamped to **0.1 – 10.0**. Internally every scaled value is
clamped to at least 1, so extreme settings can never zero a rate, divide by zero, or stall
progression (e.g. the rocket launch cost is additionally clamped to the tank size, so a launch is
always possible with a full tank).

| Key | What it scales | Base values |
|---|---|---|
| `oxygenDrainMultiplier` | How fast oxygen is consumed: bare-lungs drain, suit tank drain, suffocation damage. >1 = harsher planets. | drain 2/t · suit drain 1/check · damage 1 half-heart |
| `oxygenCapacityMultiplier` | Air capacities: the player's oxygen supply and the Tier 2 suit tank. | player 300 · T2 suit 600 |
| `energyRateMultiplier` | The energy & storage economy: generator output, energy pipe throughput, and every buffer/tank capacity (battery, fluid/gas/fuel tanks, pipe buffers, machine buffers, rocket fuel tanks). | combustion 60 FE/t · passive 10 FE/t · energy pipe 4,000 FE/t & 8,000 FE · fluid/gas pipe 4,000 mB · battery 200,000 FE · fluid/gas tank 16,000 mB · fuel tank 32,000 mB · machine buffers 10k–100k FE · rocket tanks 3,000/6,000/12,000 mB |
| `fuelCostMultiplier` | Consumable costs: rocket fuel per launch (clamped to tank size), airlock oxygen-gas per air unit, machine energy costs. | launch 1,000/2,000/4,000 mB (T1/T2/T3) · airlock 5 mB/air · Terraformer 12 FE/block · Grinder 30 FE/t · O₂ Generator 2 FE/mB |
| `machineSpeedMultiplier` | Machine working speed: grinder craft time, oxygen make/emit rate, fuel tank pump rate, airlock refill rate, fluid/gas pipe throughput, item pipe travel & extraction, Terraformer work interval. | grinder 100 t/item · O₂ make 5 / emit 2 mB/t · pump 40 / 160 (3×3) mB/t · refill 20 air/check · fluid pipe 500 / gas pipe 250 mB/t · item pipe 10 t/block, pulse every 10 t · Terraformer cycle every 8 t |

Examples: `oxygenDrainMultiplier = 2.0` halves how long air lasts; `machineSpeedMultiplier = 2.0`
makes the grinder craft in 50 ticks and the Terraformer work twice as often;
`energyRateMultiplier = 0.5` halves generator output *and* storage, keeping the economy's shape.

Notes on extremes: at `0.1` every rate clamps to ≥1 (nothing stops working, it just gets slow);
at `10.0` machine input limits (e.g. the O₂ Generator's 500 FE/t insert cap) may become the
bottleneck before the multiplied rate — intended, since those caps protect grid balance.

## Absolute keys

### Telemetry

| Key | Default | Meaning |
|---|---|---|
| `telemetryEnabled` | `true` | Anonymous Nerospace-only crash reports (Sentry, EU servers, POPIA/GDPR-scrubbed — see `PRIVACY.md`). Set `false` to opt out; applies immediately on config reload. |

### Atmosphere

| Key | Default | Range | Meaning |
|---|---|---|---|
| `atmosphereDamageEnabled` | `true` | — | Master switch: whether airless dimensions hurt unprotected players. |
| `atmosphereSafeRadius` | `6` | 0–32 | Blocks from a Rocket Launch Pad treated as a safe, pressurised zone. |

### Oxygen structure

| Key | Default | Range | Meaning |
|---|---|---|---|
| `oxygenBubbleRadius` | `14` | 0–32 | Falloff radius of a generator's breathable bubble in open/leaky space (sealed rooms fill completely regardless). |
| `oxygenAirlockRadius` | `3` | 0–16 | Radius within which a worn suit refills from a Gas Tank / Oxygen Generator holding Oxygen. `0` disables airlock refilling. |

### Oxygen field simulation (ADVANCED)

Simulation tuning, **not** balance — wrong values can break oxygen/terraforming behaviour. Leave
at defaults unless debugging server performance.

| Key | Default | Range | Meaning |
|---|---|---|---|
| `oxygenMaxConcentration` | `15` | 1–15 | Max per-block oxygen concentration in the field. |
| `oxygenBreathableThreshold` | `6` | 1–15 | Concentration at/above which a cell is breathable. |
| `oxygenDiffusionRate` | `0.22` | 0–0.4 | Fraction of the neighbour gradient flowing into a cell per sim step. |
| `oxygenDecayPerStep` | `0.18` | 0–5 | Oxygen bled to vacuum per sim step (keeps open-air bubbles finite). |
| `oxygenSimIntervalTicks` | `5` | 1–100 | Server ticks between field relaxation passes. |
| `oxygenMaxActiveCellsPerSource` | `4096` | 64–65536 | Safety cap on simulated cells per source (TPS guard). |
| `oxygenLeakRange` | `16` | 4–64 | Max flood-fill distance from a generator for leaks/room walls. |
| `oxygenEvaporateSeconds` | `10` | 1–120 | How long oxygen lingers once unsupplied or leaking. |
| `oxygenSyncRadius` | `32` | 8–128 | Radius around a source within which the field simulates and syncs; sources with no player in range pause. |

### Client visuals

| Key | Default | Range | Meaning |
|---|---|---|---|
| `oxygenVisualQuality` | `FULL` | OFF/MINIMAL/FULL | Oxygen visuals: OFF (none), MINIMAL (HUD + sparse particles), FULL (+ haze tint + boundary shimmer). |
| `oxygenParticleIntensity` | `1.0` | 0–4 | Drifting-particle density (0 disables just this layer). |
| `oxygenHazeIntensity` | `1.0` | 0–2 | Haze/fog-tint alpha inside breathable air (0 disables the layer). |
| `oxygenBoundaryIntensity` | `1.0` | 0–2 | Boundary-shimmer membrane alpha (0 disables the layer). |
| `oxygenDebugLog` | `false` | — | Verbose, non-personal oxygen/terraform logging (never player identifiers — POPIA/GDPR). |

### Terraformer

| Key | Default | Range | Meaning |
|---|---|---|---|
| `terraformPlantsEnabled` | `true` | — | Scatter grass/flowers/saplings on converted ground. |
| `terraformWaterEnabled` | `true` | — | Fill low/exposed cells with water. |
| `terraformResourcesEnabled` | `true` | — | Tier-3 Terraformer seeds ores into the converted subsurface (low rate). |
| `terraformResourceOres` | Nerospace ores | list | Ore block ids the Tier-3 Terraformer may seed. |
| `terraformMaxColumnsPerTick` | `48` | 1–4096 | Hard per-tick work cap regardless of tier (TPS guard). |
| `terraformForceLoadChunks` | `false` | — | Force-load a bounded arc around the working frontier (TPS footgun — off by default). |
| `terraformMaxForcedChunks` | `16` | 0–256 | Guard on force-loaded chunks. |

## Removed keys (for modpack authors migrating)

Folded into multipliers: `atmosphereDamage`, `oxygenMax`, `oxygenDrainPerTick`, `oxygenSuitDrain`,
`oxygenSuitT2Max`, `oxygenAirlockRefillPerCheck`, `oxygenAirlockMbPerAir`,
`terraformEnergyPerBlock`, `terraformWorkIntervalTicks`, `energyPipeCapacity`,
`energyPipeThroughput`, `fluidPipeCapacity`, `fluidPipeThroughput`, `gasPipeCapacity`,
`gasPipeThroughput`, `itemPipeTicksPerBlock`, `itemPipeExtractAmount`, `itemPipeExtractPeriod`,
`batteryCapacity`, `fluidTankCapacity`, `gasTankCapacity`, `combustionGeneratorFePerTick`,
`passiveGeneratorFePerTick`.

Removed outright: `logDirtBlock`, `magicNumber`, `magicNumberIntroduction`, `items` (template
leftovers), `oxygenSealedRoomMax` (dead — the oxygen field replaced the sealed-room flood-fill),
`terraformPlantChance`, `terraformResourceChance` (internalised; the booleans above still gate
the features).
