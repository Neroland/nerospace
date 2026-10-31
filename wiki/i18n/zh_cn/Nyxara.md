# Nyxara

Nyxara is a late-game, tidally locked world. Its sun never moves: it hangs on the **western** horizon forever, while a
galaxy fills the eastern sky. Instead of day and night following each other in time, they sit side by side on the map.

|             |                                                                            |
| ----------- | -------------------------------------------------------------------------- |
| Dimension   | `nerospace:nyxara`                                                         |
| Rocket      | Tier 4, after Deep Space progress                                          |
| Gravity     | 0.9 × Overworld                                            |
| Day         | none: the clock is fixed, the sun never moves              |
| Weather     | Solar flares, on average every four Overworld days, lasting 1 to 2 minutes |
| Hazard      | Radiation everywhere; plus heat on the Dayside and cold on the Nightside   |
| Villagers   | Duskborn (Twilight Band only)                           |
| Native life | Dusk Moth, on terraformed ground only                                      |

## Getting there

- **Tier 4 rocket.** Nyxara is on the Tier 4 destination list, next to Glacira.
- **Deep Space progress is required before launch.** The rocket screen shows Nyxara locked with the gate name until
  you have it. If you have already landed on Cindara or Glacira, you have it.
- **Bring a Rad-Lined suit.** See _Hazards and protection_ below.

## The three regions

The planet is divided by **east–west position** (block X), not by climate:

| Region            | Where                                                   | What you find                                                                                                                                                                                                                                        |
| ----------------- | ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Dayside**       | west of the band (−X)                | sun-bleached crystal plains: Sunglass Crystal, patches of Crazed Crystal, drifts of loose shards, Dawnband; hot and irradiated                                                                                                       |
| **Twilight Band** | centred on the landing point (X = 0) | the habitable strip: violet Twilight Crystal fields with rose patches, Crystal Spire forests (some tipped with a Nyx Prism), crystal clusters, geode hollows, Void Crystal outcrops, Dawnband Stone, the Duskborn |
| **Nightside**     | east of the band (+X)                | frozen dark: Smoke Crystal ground, flat Mirror Crystal lakes, Shadow Lichen in caves, Voidglass Ore, deep Gravity Shard deposits                                                                                                     |

Nyxara reads as crystal, not stone. The ground of every region is a few blocks of glassy crystal; Nyx Stone lies
under it and shows on steep cliffs, where the cliff cuts through to the rock.

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

### Crystal blocks

The crystal blocks have translucent surfaces. Void Crystal pulses gently, while Smoke and Mirror Crystal have animated highlights.

Most crystal drops itself. The glassy pieces (Crazed Crystal, Mirror Crystal, the Nyx Prism, full clusters, Shard
Drift) break into **Crystal Shards** unless you use Silk Touch. Four Crystal Shards craft a Twilight Crystal, so the
ground is buildable without Silk Touch.

| Block                   | Where                          | What it does                                                                                                                                                                                                                                                                                                                                                                  |
| ----------------------- | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Sunglass Crystal**    | Dayside ground                 | Amber-clear crystal that shades you: with Sunglass in any of the three blocks above your head, the ambient radiation counts one level lower (Dayside 2 becomes 1). A flare still adds its level. Dayside caves and crystal roofs are the natural shelter                                                   |
| **Crazed Crystal**      | Dayside patches                | Heat-crazed safety glass. Land on natural Crazed Crystal from 3 or more blocks and it shatters under you (and up to 7 connected blocks around it) into shards, and you take half the fall damage. Crystal you placed yourself never shatters                                                                               |
| **Shard Drift**         | Dayside, loose cover           | Layers of loose shards (1 to 8, like snow). It cuts: standing in it **without boots** costs one point of damage a second (the first cut can take up to a second to land). Any boots protect you. Dig it with a shovel for shards                                        |
| **Twilight Crystal**    | Twilight Band ground           | Glows with the sky light it soaks up: light up to 7 under the open sky, dark underground. It re-reads the sky only when it is placed or something next to it changes, so a freshly generated shallow cave floor can still glow until you disturb it                                                                                           |
| **Rose Crystal**        | Twilight Band patches, geodes  | Lights up (light 9) for three seconds where a player steps on it, so your trail glows behind you. Idle light 2                                                                                                                                                                                                                             |
| **Crystal Spire**       | Twilight Band forests          | Translucent crystal columns, 4 to 10 blocks tall. Strike one, or hit it with an arrow, and it chimes: the higher up the column, the higher the note                                                                                                                                                                                           |
| **Nyx Prism**           | Tips of some spires; craftable | Glows 6. During a flare it blazes (light 12) and throws a ray of light 6 blocks east, away from the sun. **Stand in a prism beam during a flare at your own risk:** inside the beam the flare raises radiation by two levels instead of one. Counts as a beam refractor, like Void Crystal |
| **Budding Nyx Crystal** | Geode cores                    | Grows Nyx Crystal Clusters on its free faces, much faster during a flare. Breaks if a piston pushes it; Silk Touch keeps it, otherwise it drops nothing                                                                                                                                                                                                       |
| **Nyx Crystal Cluster** | Geodes, band ground            | Grows in four stages. A full cluster drops two shards to a pickaxe (Fortune adds more) and one to anything else; smaller stages drop nothing                                                                                                                                                                                               |
| **Smoke Crystal**       | Nightside ground, lake shores  | Hushed crystal: nothing makes a sound walking or landing on it, and it muffles sculk vibrations. Faint inner glow (light 1)                                                                                                                                                                                                |
| **Mirror Crystal**      | Nightside lakes                | Glassy, see-through crystal that reflects the galaxy: under the open sky on the Nightside it glows (light 5); covered, or anywhere else, it goes dark                                                                                                                                                                                      |

Recipes: four Crystal Shards make a Twilight Crystal; eight shards around a Voidglass make a Nyx Prism; two shards and
a glass block make two Mirror Crystals. Crystal Shards are in `#c:gems/crystal_shard`.

## Hazards and protection

All of Nyxara is irradiated, and each region adds its own hazard:

| Region        | Radiation level                                                                    | Also     | What keeps you safe                                                      |
| ------------- | ---------------------------------------------------------------------------------- | -------- | ------------------------------------------------------------------------ |
| Twilight Band | 1 (2 during a flare, outdoors)                                  | —        | a Rad-Lined suit                                                         |
| Dayside       | 2 (3 during a flare, outdoors); 1 under a Sunglass Crystal roof | hot      | a Rad-Lined **Thermal** Suit                                             |
| Nightside     | 3                                                                                  | freezing | a Rad-Lined **Cryo** Suit, or Rad Lining beside a flaring Dawnband Stone |

Without the matching protection your suit drains oxygen four times as fast. Radiation also hurts you once it reaches
level 2, anywhere outside a breathable zone: one point of damage every two seconds at level 2, two at level 3. In the band that only happens during
a flare. A sealed oxygen field keeps the radiation out, and the HUD warns about whichever hazard would hurt you next.
Sunglass Crystal overhead lowers the level by one; a flaring Nyx Prism's beam raises a flare by one more. The roof
only changes the radiation damage: your suit still drains at the hazard rate under it.
See [Rad Lining](Oxygen-Suit#rad-lining-radiation).

## Clock and weather

Nyxara's clock is fixed: the sun stays on the western horizon and there is no day or night to wait for. Light levels
follow the normal block-light rules, so mobs still spawn in the dark.

Every few days the fixed sun **flares** for one to two minutes. The sky washes gold. Dawnband Stone lights up and keeps
the cold away, Nyx Prisms blaze and cast their beams, Budding Nyx Crystal grows faster, and Shadow Lichen withers. Out in the open on the Dayside and in the Twilight Band, radiation rises one
level while the flare lasts, enough to hurt in the band too. A sealed oxygen field keeps it out, and a Rad-Lined suit
still protects you. The Nightside faces away from the sun and is unaffected. See
[Planet Clock and Weather](Planet-Clock-and-Weather).

**Beds:** day and night are places here, so where you put the bed decides. On the Nightside you can lie down at any
time and get up rested a few seconds later (nothing is skipped: the sun never moves). In the Twilight Band and on
the Dayside the bed refuses ("The sun never sets here"), but it still sets your respawn point. See
[Sleeping](Planet-Clock-and-Weather#sleeping).

**Open water freezes on the Nightside:** water under the open sky there turns to ice like in a snowy Overworld biome,
unless the block light at the water is 10 or more, and sugar cane planted by it breaks. Keep a light source within a
few blocks of the water, or farm under a roof.

## Sky

The sky changes as you travel, not as time passes. The sky overhead is always dark and full of stars, with a
vivid galaxy climbing from the east and two nebulae. Over the Dayside the western horizon burns orange under the
big, pinned sun; in the Twilight Band it turns violet and a crystal-refracted rainbow arc stands low in the east; over
the Nightside it fades to deep purple and violet-and-cyan **aurora curtains** fold over the crystal. During a **flare**
two fans of sun rays burst from the western horizon around the sun, a shimmer of crystal light runs along the
western horizon in the band, and everything washes gold (on the ground, the Nyx Prisms throw their rays the same
way, east). The aurora shows only over the Nightside: from the band you see the rainbow arc and the nebulae, not the
curtains.

The full sky is drawn on NeoForge for Minecraft 26.3. On every other version Nyxara shows its colours with the vanilla sky shapes, and the world fog still follows the weather and where you stand.

## The Duskborn

The Twilight Band is home to the **Duskborn**, Nyxara's tall, slender **[villagers](Alien-Villagers)** with glowing
markings. They trade voidglass, void crystal and Tier 2 suit parts (only the helmet until you reach their highest
trust tier), and they buy shadow lichen. Their hamlets, ruins and rare Mega-Cities, built from nyx stone, void crystal
and tinted glass on the crystal floor, stand only inside the band, never on the Dayside or the Nightside. See
**[Alien Structures](Alien-Structures)**.

## Terraforming and fauna

Nyxara's terraformed **Twilight Grove** only roots in the Twilight Band. Place a [Terraformer](Terraformer) on the
Dayside or Nightside and it refuses: its screen and the Terraform Monitor say _only roots in the twilight band_, and
it uses no energy. Inside the band it spreads outward and simply skips any ground beyond the band's edge. The grove
keeps the band's mild radiation, and terraformed ground seeds Voidglass and Nerosium.

The grove's native creature is the **Dusk Moth**, a fragile flier with glowing eye-spot wings. It only spawns where
no lamp lights the ground (block light 7 or lower), drops Night Fibre and is never harmed by Nyxara. See
[Creatures](Creatures).

## Star Guide

**Worlds: Nyxara**: land, mine Voidglass, build with Dawnband Stone, last two minutes under the open sky through a
flare, and root your first terraformed ground.
