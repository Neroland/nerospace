# Solar Panel

<!-- nerospace:render -->

<p align="right"><img src="images/solar_panel.png" alt="Solar Panel" width="150" align="right"></p>
<!-- /nerospace:render -->

Sun-tracking generators that pool with their neighbours into one big solar array. Comes in **three
tiers** — Tier 1 is a single block; Tier 2 and Tier 3 are 2×2 and 3×3 **multiblocks** that you place
from a single item.

## Obtaining

**Tier 1** (shaped): a glass deck over quartz/copper cells on a nerosteel housing —

```text
G G G
Q C Q
N N N
```

`G` = Glass · `Q` = Xertz Quartz · `C` = Copper Ingot · `N` = Nerosteel Ingot

**Tier 2** — four Tier 1 panels around a Block of Nerosium:

```text
. P .
P N P
. P .
```

`P` = Tier 1 Solar Panel · `N` = Block of Nerosium

**Tier 3** — four Tier 2 panels around a Block of Gold:

```text
. Q .
Q G Q
. Q .
```

`Q` = Tier 2 Solar Panel · `G` = Block of Gold

Each tier folds the **previous panel** into its recipe, so upgrading reuses what you already built.

## How it works

- **Sunlight in, FE out.** Output follows the sun: full at noon, tapering to **nothing at night**.

  In normal dimensions the panel needs a **clear view of the sky** — anything solid directly above it
  stops generation.

- **Weather:** there is no fixed rain or thunder penalty. Output follows how dark the sky is:

  output × (10 − sky darkness) / 10. Rain and storms darken the sky, so output drops with them.

- **Nerospace planets** (Greenxertz, Cindara, Glacira, Luna, Mars and Nyxara) follow the planet's own day: a panel

  peaks at that planet's noon, dips at night, on Cindara dims during the eclipse and on Mars falls during a dust storm.
  Nyxara's sun never moves, so a panel there gives a steady ×2.04. Over a full planet day
  every planet gives **at least the old ×2 rate on average** (the peaks are higher to make up for the
  nights), so bases that run at night need a battery. The **Orbital Station** (and founded stations) stays
  a constant ×2. Panels do **not** need a view of the sky on a planet. Server owners can restore the old
  constant ×2 with `solar.clockDriven=false`. Solar is the natural off-world power source.

- Output and storage scale by tier (all values × the `energyRateMultiplier` config). The buffer

  never accepts a push from outside.

- **Auto-push:** panels push power into any machine or Battery that touches them.

| Tier       | Footprint | Output @ noon | Storage      |
| ---------- | --------- | -------------------------- | ------------ |
| **Tier 1** | 1×1       | 20 FE/t                    | 50,000 FE    |
| **Tier 2** | 2×2       | 100 FE/t                   | 250,000 FE   |
| **Tier 3** | 3×3       | 400 FE/t                   | 1,000,000 FE |

Each higher tier is stronger **per area** than tiling the lower one, and carries a much bigger buffer.

### Multiblocks (Tier 2 & 3)

- A Tier 2 / Tier 3 panel is a **single item that fills its whole N×N footprint** when placed — you need

  that flat area clear (it won't place into an obstructed footprint).

- It behaves as **one unit**: one big tilting deck, one pooled buffer, energy on every outer face.

- **Breaking any cell returns the whole panel as one item** — no duplication, and you get your panel

  back wherever you mined it.

### Arrays

- Place same-tier units **next to each other** and they automatically merge into one **array**: the

  array's storage is the **sum of every unit's buffer** and its generation the **sum of every unit's
  output**. Build wider for more of both — arrays can be almost any size.

- **Every side is an output port.** A Universal Pipe (energy layer) — or any machine/Battery — touching

  _any_ face pulls from the shared array pool, so one pipe drains the whole array.

- **Tiers never mix.** A Tier 1 array and a Tier 2 array placed side by side stay separate; only units

  of the _same_ tier pool together.

### Appearance

The tiers are **visibly distinct** — Tier 1 has a green accent and pivots on a **T-pole**, tracking the
sun east-to-west; Tier 2 (nerosium magenta) and Tier 3 (gold) are flat N×N arrays whose decks **tilt up
to face the sky by day and fold flat onto their housings at night**. All panels read the same world
time, so a field of them moves in lockstep.

## Details

- IDs: `nerospace:solar_panel` (Tier 1) / `nerospace:solar_panel_t2` / `nerospace:solar_panel_t3`
- Tool: Tier 1 needs an iron pickaxe; Tier 2 and Tier 3 need any pickaxe.
- Tier 1 drops itself; Tier 2/3 return one item for the whole multiblock when any cell is broken.
- Emits a comparator signal from the array's charge.
- Config: `energyRateMultiplier` (scales both output and storage). See **[Configuration](Configuration)**.
