# Planet Gravity

Every world in Nerospace pulls with its **own gravity**. The Overworld feels normal, the
**Orbital Station** is almost weightless, and the planets sit somewhere in between — so how high you
jump, how fast you fall, and how far a thrown item flies all change depending on where you are.

## Gravity at a glance

Values are relative to normal Overworld gravity (`1.0×`). These are the current defaults and are fully
adjustable with the [`gravityMultiplier`](Configuration) config option.

| Dimension | Gravity | Feel |
| --- | --- | --- |
| Overworld | `1.0×` | Normal. |
| **Orbital Station** | `~0.1×` | Near-weightless — long, floaty hops. |
| **Greenxertz** | `~0.6×` | Light — noticeably higher jumps, softer landings. |
| **Cindara** | `~0.8×` | Slightly low — a touch floatier than home. |
| **Glacira** | `~0.4×` | Low — big, slow bounds across the ice. |

Lower gravity means a **higher jump** and a **slower fall**, so you also take **less fall damage** —
a long drop on Glacira that would hurt on the Overworld is survivable.

## What it affects

Gravity applies to **everything that falls**, not just you:

- **You and mobs** — movement, jump height, and fall speed/damage all scale. Native creatures bound
  around their low-gravity worlds the same way you do.
- **Dropped items, arrows, falling blocks, TNT, and XP orbs** — they hang and drift on low-gravity
  worlds and settle faster on heavy ones.
- **Meteors** — a falling [meteor](Meteor-Events) descends **more slowly** on a low-gravity world,
  drawing out its fiery arc.

Because gravity is applied through the game's own gravity stat (for you and mobs), movement stays
smooth and responsive — no lag or rubber-banding when you cross between worlds.

## Per-biome variation

A planet's gravity is its **baseline**, but individual **biomes** can pull lighter or heavier than
their world's default — a crater floor might be near-weightless while a dense basin feels almost
normal. Biome gravity is data-driven (datapack biome tags: `gravity_micro`, `gravity_low`,
`gravity_high`), so packs can tune or add zones without code.

## Terraforming restores normal gravity

Reclaimed ground pulls its weight. As a [Terraformer](Terraformer) converts a dead, low-gravity
planet into living land, that **terraformed ground returns to normal, Earth-like gravity** — so a
fully terraformed base feels like home even on a moon that started out floaty. (An explicit biome
gravity tag, if a pack sets one, takes priority over this.)

## Configuration

- **[`gravityMultiplier`](Configuration)** (`0.1`–`10`, default `1.0`) — a global scale on *all*
  gravity. Set it below `1` for an even floatier game everywhere, or above `1` for a heavier one.

## Checking gravity in-game

The creative/op command **`/nerospace gravity`** reports the gravity in effect where you're standing —
the value, where it comes from (biome, terraformed ground, or the planet default), and your current
gravity stat. Handy for tuning a pack or filing a bug report.

## Details

- Source priority: **biome gravity tag → terraformed ground (normal) → planet default → normal**, then
  scaled by `gravityMultiplier`.
- Affects: players, living mobs, dropped items, arrows, falling blocks, primed TNT, XP orbs, and
  falling meteors.
- Biome tags: `nerospace:gravity_micro`, `nerospace:gravity_low`, `nerospace:gravity_high`.
