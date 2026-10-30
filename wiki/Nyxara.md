# Nyxara

Nyxara is a late-game, tidally locked world. Its sun never moves: it hangs on the **western** horizon forever, while a
galaxy fills the eastern sky. Instead of day and night following each other in time, they sit side by side on the map.

| | |
|---|---|
| Dimension | `nerospace:nyxara` |
| Rocket | Tier 4, after Deep Space progress |
| Gravity | 0.9 × Overworld |
| Day | none: the clock is fixed, the sun never moves |
| Weather | Solar flares, on average every four Overworld days, lasting 1 to 2 minutes |
| Hazard | Radiation everywhere; plus heat on the Dayside and cold on the Nightside |
| Villagers | Duskborn (Twilight Band only) |
| Native life | Dusk Moth, on terraformed ground only |

## Getting there

- **Tier 4 rocket.** Nyxara is on the Tier 4 destination list, next to Glacira.
- **Deep Space progress is required before launch.** The rocket screen shows Nyxara locked with the gate name until
  you have it. If you have already landed on Cindara or Glacira, you have it.
- **Bring a Rad-Lined suit.** See *Hazards and protection* below.

## The three regions

The planet is divided by **east–west position** (block X), not by climate:

| Region | Where | What you find |
|---|---|---|
| **Dayside** | west of the band (−X) | scorched ground: Nyx Stone over smooth basalt, Dawnband patches, hot and irradiated |
| **Twilight Band** | centred on the landing point (X = 0) | the habitable strip: Nyx Stone, Void Crystal outcrops, Dawnband Stone, the Duskborn |
| **Nightside** | east of the band (+X) | frozen dark: Nyx Stone over blackstone, Shadow Lichen in caves, Voidglass Ore, deep Gravity Shard deposits |

The band is **2 048 blocks wide** by default. The edges are ragged, not a straight wall. Servers can change the width
with `nyxara.twilightBandWidth` (512–16 384). The width is saved with the world when Nyxara first loads, so changing
the config later only affects new worlds. Nerosium ore generates everywhere on the planet. Nyxara has its own terrain
shape: its hills and valleys do not line up with the Overworld's.

## Blocks and items

- **Nyx Stone**: block light that spreads near it fades two levels faster than normal. Good for stealthy builds.
  Sky light is unaffected. If a server turns `blocks.nyxStoneLightAbsorb` off, it behaves as ordinary opaque stone.
- **Dawnband Stone**: warm-veined stone. It glows faintly at rest. During a flare it lights up (light 11) and keeps
  the cold away within four blocks.
- **Shadow Lichen**: spreads across nearby surfaces, but only where the light level is 3 or lower. A torch stops it.
  It is capped per chunk. During a flare it withers away and drops nothing. Shear it for **Night Fibre** (three
  Night Fibre make a string).
- **Void Crystal**: purple crystal that glows at light 10. It bends beams: it is in the block tag
  `#nerospace:beam_refractors`, which other mods can read. Mine it with Silk Touch to keep it. Otherwise it drops a
  **Void Shard**. Four shards craft a crystal. Void Crystals above a station [Telescope](Orbital-Stations) extend its view.
- **Voidglass Ore**: drops **Voidglass** (Fortune helps). Four Voidglass around a glass pane make a **Voidglass Lens**.
- **Gravity Shard Block**: makes entities in the 3×5×3 column above it weightless with a slow upward drift (about
  Levitation I), whether you placed it or found it in the wild. Leave the column to come down. Nine **Gravity
  Shards** craft a block, and a block breaks back into nine.

## Hazards and protection

All of Nyxara is irradiated, and each region adds its own hazard:

| Region | Radiation level | Also | What keeps you safe |
|---|---|---|---|
| Twilight Band | 1 (2 during a flare, outdoors) | — | a Rad-Lined suit |
| Dayside | 2 (3 during a flare, outdoors) | hot | a Rad-Lined **Thermal** Suit |
| Nightside | 3 | freezing | a Rad-Lined **Cryo** Suit, or Rad Lining beside a flaring Dawnband Stone |

Without the matching protection your suit drains oxygen four times as fast. Radiation also hurts you once it reaches
level 2, anywhere outside a breathable zone: one point of damage every two seconds at level 2, two at level 3. In the band that only happens during
a flare. A sealed oxygen field keeps the radiation out, and the HUD warns about whichever hazard would hurt you next.
See [Rad Lining](Oxygen-Suit#rad-lining-radiation).

## Clock and weather

Nyxara's clock is fixed: the sun stays on the western horizon and there is no day or night to wait for. Light levels
follow the normal block-light rules, so mobs still spawn in the dark.

Every few days the fixed sun **flares** for one to two minutes. The sky washes gold. Dawnband Stone lights up and keeps
the cold away, and Shadow Lichen withers. Out in the open on the Dayside and in the Twilight Band, radiation rises one
level while the flare lasts, enough to hurt in the band too. A sealed oxygen field keeps it out, and a Rad-Lined suit
still protects you. The Nightside faces away from the sun and is unaffected. See
[Planet Clock and Weather](Planet-Clock-and-Weather).

## Sky

The sky changes as you travel, not as time passes. The sky overhead is always dark and full of stars, with a
vivid galaxy spanning the east. Over the Dayside the western horizon burns orange under the pinned sun; in the
Twilight Band it turns violet; over the Nightside it fades to deep purple. Flares wash everything gold. The full sky is drawn on NeoForge for Minecraft 26.3; on every other
version Nyxara shows its colours with the vanilla sky shapes.

## The Duskborn

The Twilight Band is home to the **Duskborn**, Nyxara's tall, slender **[villagers](Alien-Villagers)** with glowing
markings. They trade voidglass, void crystal and Tier 2 suit parts (only the helmet until you reach their highest
trust tier), and they buy shadow lichen. Their hamlets, ruins and rare Mega-Cities, built from nyx stone, void crystal
and tinted glass, stand only inside the band, never on the Dayside or the Nightside. See
**[Alien Structures](Alien-Structures)**.

## Terraforming and fauna

Nyxara's terraformed **Twilight Grove** only roots in the Twilight Band. Place a [Terraformer](Terraformer) on the
Dayside or Nightside and it refuses: its screen and the Terraform Monitor say *only roots in the twilight band*, and
it uses no energy. Inside the band it spreads outward and simply skips any ground beyond the band's edge. The grove
keeps the band's mild radiation, and terraformed ground seeds Voidglass and Nerosium.

The grove's native creature is the **Dusk Moth**, a fragile flier with glowing eye-spot wings. It only spawns where
no lamp lights the ground (block light 7 or lower), drops Night Fibre and is never harmed by Nyxara. See
[Creatures](Creatures).

## Star Guide

**Worlds: Nyxara**: land, mine Voidglass, build with Dawnband Stone, last two minutes under the open sky through a
flare, and root your first terraformed ground.
