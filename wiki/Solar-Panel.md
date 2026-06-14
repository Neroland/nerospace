# Solar Panel

A sun-tracking generator that pools with its neighbours into one big solar array.

## Obtaining

**Craft** (shaped): a glass deck over quartz/copper cells on a nerosteel housing —

```text
G G G
Q C Q
N N N
```

`G` = Glass · `Q` = Xertz Quartz · `C` = Copper Ingot · `N` = Nerosteel Ingot

## How it works

- **Sunlight in, FE out.** Output follows the sun: full at noon, tapering to **nothing at night**.
  The panel needs a **clear view of the sky** — anything solid directly above it stops generation.
- **Weather** cuts output: rain/snow drops it to ~40%, a thunderstorm to ~25%.
- **Airless dimensions** (Orbital Station, Greenxertz, Cindara, Glacira, founded stations) have a
  permanent sun, so panels there run at full **and earn a ×2 bonus** — solar is the natural off-world
  power source.
- A single Tier 1 panel makes **20 FE/t** at noon into a **50,000 FE** buffer (both scale with the
  `energyRateMultiplier` config). The buffer is **extract-only** — it never accepts a push.

### Arrays

- Place same-tier panels **next to each other** and they automatically merge into one **array**: the
  array's storage is the **sum of every panel's buffer** and its generation is the **sum of every
  panel's output**. Build wider for more of both — arrays can be almost any size.
- **Every side is an output port.** A Universal Pipe (energy layer) — or any machine/Battery — touching
  *any* panel pulls from the shared array pool, so one pipe drains the whole array.
- **Tiers never mix.** A Tier 1 array and a Tier 2 array placed side by side stay separate; only panels
  of the *same* tier pool together.

### Appearance

The photovoltaic deck sits on a **T-pole mount** above its steel base. By day it **tilts to track the
sun** across the sky; at night it **folds flat** and parks. Panels in a row line their decks up
edge-to-edge into one continuous, good-looking array — all of them moving in sync.

## Tiers

| Tier | Footprint | Status |
|---|---|---|
| **Tier 1** | 1×1 | Available |
| Tier 2 | 2×2 | Planned |
| Tier 3 | 3×3 | Planned |

Higher tiers generate more per panel and **fold the previous tier's panel into their recipe** (a Tier 1
panel is an ingredient for Tier 2, and so on).

## Details

- ID: `nerospace:solar_panel_t1` · Tool: pickaxe, iron tier · Drops: itself
- Emits a comparator signal from the array's charge.
- Config: `energyRateMultiplier` (scales both output and storage). See **[Configuration](Configuration)**.
