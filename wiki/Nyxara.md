# Nyxara

Nyxara is a late-game, tidally locked world. Its sun never moves: it hangs on the **western** horizon forever, while a
galaxy fills the eastern sky. Instead of day and night following each other in time, they sit side by side on the map.

## Getting there

- **Tier 4 rocket.** Nyxara is on the Tier 4 destination list, next to Glacira.
- **Deep Space progress is required before launch.** The rocket screen shows Nyxara locked with the gate name until
  you have it. If you have already landed on Cindara or Glacira, you have it.
- **Bring a Rad-Lined suit.** Nyxara is irradiated. The Rad Lining suit upgrade protects you. The Nightside is the
  worst, followed by the Dayside.

## The three regions

The planet is divided by **east–west position** (block X), not by climate:

| Region | Where | What you find |
|---|---|---|
| **Dayside** | west of the band (−X) | scorched ground: Nyx Stone over smooth basalt, Dawnband patches, hot and irradiated |
| **Twilight Band** | centred on the landing point (X = 0) | the habitable strip: Nyx Stone, Void Crystal outcrops, Dawnband Stone |
| **Nightside** | east of the band (+X) | frozen dark: Nyx Stone over blackstone, Shadow Lichen in caves, Voidglass Ore, deep Gravity Shard deposits |

The band is **2 048 blocks wide** by default. The edges are ragged, not a straight wall. Servers can change the width
with `nyxara.twilightBandWidth` (512–16 384). The width is saved with the world when Nyxara first loads, so changing
the config later only affects new worlds. Nerosium ore generates everywhere on the planet.

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
  **Void Shard**. Four shards craft a crystal.
- **Voidglass Ore**: drops **Voidglass** (Fortune helps). Four Voidglass around a glass pane make a **Voidglass Lens**.
- **Gravity Shard Block**: makes entities in the 3×5×3 column above it weightless with a slow upward drift (about
  Levitation I), whether you placed it or found it in the wild. Leave the column to come down. Nine **Gravity
  Shards** craft a block, and a block breaks back into nine.

## The Duskborn

The Twilight Band is home to the **Duskborn**, Nyxara's tall, slender **[villagers](Alien-Villagers)**. Their
hamlets, ruins and rare Mega-Cities, built from nyx stone, void crystal and tinted glass, stand only inside the
band, never on the dayside or the nightside. See **[Alien Structures](Alien-Structures)**.

## Weather: flares

Every few days the fixed sun flares for one to two minutes. The sky washes gold. Dawnband Stone lights up and keeps the
cold away, and Shadow Lichen withers. Out in the open on the Dayside and in the Twilight Band, radiation rises one
level while the flare lasts, enough to hurt in the band too. A sealed oxygen field keeps it out, and a Rad-Lined suit
still protects you. The Nightside faces away from the sun and is unaffected.

## Notes

- Nyxara's clock is fixed. Light levels follow the normal block-light rules, so mobs still spawn in the dark.

## Terraforming

Nyxara's terraformed **twilight grove** only roots in the Twilight Band. Place a Terraformer on the dayside or
nightside and it refuses: its screen and the Terraform Monitor say *only roots in the twilight band*, and it uses
no energy. Inside the band it spreads outward and simply skips any ground beyond the band's edge. The grove keeps
the band's mild radiation, and terraformed ground seeds Voidglass and Nerosium.

**Worlds: Nyxara** in the Star Guide: land, mine Voidglass, build with Dawnband Stone, last two minutes under the
open sky through a flare, and root your first terraformed ground.
