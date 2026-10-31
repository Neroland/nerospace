# Cindara

Cindara is a **Tier 3** destination: ember-cracked ash under a red giant that eclipses into dusk.

| | |
|---|---|
| Dimension | `nerospace:cindara` |
| Rocket | Tier 3 or Tier 4 |
| Gravity | 0.8 × Overworld |
| Day | two Overworld days (48 000 ticks); a late-afternoon eclipse runs straight into dusk |
| Weather | Ash Fall, on average every day and a half, lasting 2.5 to 7.5 minutes |
| Hazard | Heat (Thermal Suit) |
| Villagers | Ember-kin |
| Native life | Cinder Stalker; Ember Strutters on terraformed ground |

## Biomes

- **Cindara Ash Wastes**: the original Cindara biome. Ashstone plains with the odd sulfur patch and basalt spire.
- **Ember Plains**: Ashstone shot through with glowing Ember-Cracked Ashstone, and pools of lava. Cindrite is richer here (about 1.5 ×).
- **Sulfur Vents**: yellow Sulfur Crust fields between tall Basalt Spires.

New Cindara land has its own terrain shape (its hills no longer line up with the Overworld's), Ashstone over stone,
and the usual large copper and iron ore veins deep underground.

## Blocks and items

| Block / item | What makes it special |
|---|---|
| Ashstone | Soft: any pickaxe mines it instantly. Drops 2 Ash Dust (Silk Touch drops the block). |
| Ember-Cracked Ashstone | The cracks glow. Standing on it without heat-rated boots burns you. Water next to it quenches it into Ashstone. Needs a stone pickaxe. |
| Basalt Spire | Natural columns that ember strikes never hit. Needs an iron pickaxe. |
| Magma Glass | Glows (light 7). The only transparent block lava cannot destroy. Breaks without Silk Touch. Craft: 8 Glass around a Magma Block → 8. |
| Ash Layer | Builds up like snow during Ash Fall (the weather adds up to four layers; you can stack it to eight). Three or more layers slow you down. It only melts next to fire or lava. Shovel it for Ash Dust (one per layer). |
| Sulfur Crust | Cindara's new ore. Drops 2 to 4 Sulfur (Fortune applies). If fire touches it, it sets nearby flammable blocks alight and burns down to Ashstone. |
| Sulfur | Two Sulfur and one coal or charcoal craft into two Gunpowder. |
| Ash Dust | Two craft into Ashstone; one crafts into an Ash Layer. |
| Cindrite Ore | Unchanged. Generates at the same rate as before, more often in the Ember Plains. |

## Hazards and protection

Cindara is airless and **hot**. Without a full four-piece **Thermal Suit** your suit's oxygen drains four times as
fast, with ember puffs as a warning. Ember-Cracked Ashstone also burns bare boots, and ember strikes hurt (see
below). Cindara has no harmful radiation, so you do not need Rad Lining here. See [Oxygen Suit](Oxygen-Suit).

## Clock and weather

A Cindara day lasts 48 000 ticks (two Overworld days). Late in the afternoon a dark moon slides across the red giant:
the **eclipse** dims the daylight to a tenth for a few minutes and runs straight into dusk.

During **Ash Fall** grey ash settles on open ground and piles into Ash Layers. In the strongest part of a storm,
ember strikes (lightning) hit open ground under the sky near players, at most one near each player every five
seconds (servers can space them out further with `weather.emberStrikeIntervalTicks`). They hurt and ignite creatures
but never set blocks alight, and they leave dropped items, item frames, paintings, boats and minecarts alone. A Basalt
Spire is never struck. Ash Fall never starts close to the eclipse. See
[Planet Clock and Weather](Planet-Clock-and-Weather).

## Sky

A dusky sky with faint, warm stars, dominated by a huge red giant with a burning rim, and the dark moon that
eclipses it every day around midday-to-dusk (clock phase 0.40 to 0.50). As the eclipse deepens the sky dims, a pearly
**corona** blooms around the darkened sun, and a **diamond ring** flashes on its edge just before and after totality.
A deck of ash clouds hangs low over the horizon, lit orange from below by the lava fields; Ash Fall thickens it and
greys the sky.

The full sky is drawn on NeoForge for Minecraft 26.3. On every other version Cindara shows its colours with the vanilla sky shapes, and the world fog still follows the time of day and the weather.

## The Ember-kin

Cindara's **[villagers](Alien-Villagers)** are the stocky **Ember-kin**. They build hamlets and ruins from
cindrite, basalt and magma glass, and they trade cindrite, sulfur and heat-protection suit parts (only the helmet
until you reach their highest trust tier). They also buy ash. Their settlements appear only in newly generated land.
See [Alien Structures](Alien-Structures).

## Terraforming and fauna

On Cindara the [Terraformer](Terraformer) turns ashstone into cinder soil (**Rooted**). The steam vents cool and
water appears (**Hydrated**), and the land settles into the **savanna** you know from 1.x, with acacia-like Cinder
Trees and **Ember Strutters** (**Living**). The wild **Cinder Stalker** still roams the ash. See [Creatures](Creatures).

## Star Guide

**Worlds: Cindara**: land, mine Cindrite, build with Magma Glass, last two minutes under the open sky through an
ash fall, and root your first terraformed ground.

## Upgrading from 1.x

Your Cindara world is kept. Chunks you already explored stay exactly as they were: same blocks, same biome.
Only chunks generated for the first time use the new terrain, so there is a **visible seam** where old and new
terrain meet. This is expected. Nothing is regenerated or converted. See [Upgrading to 2.0](Upgrading-to-2.0).
