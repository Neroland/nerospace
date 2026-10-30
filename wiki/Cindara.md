# Cindara

Cindara is a **Tier 3** destination: ember-cracked ash under a red giant that eclipses into dusk.

| | |
|---|---|
| Dimension | `nerospace:cindara` |
| Gravity | 0.8 × Overworld |
| Day | two Overworld days (48 000 ticks); a late-afternoon eclipse runs straight into dusk |
| Hazard | Heat (Tier 2 suit, or the Thermal Suit) |
| Weather | Ash Fall, about every day and a half, lasting 2.5 to 7.5 minutes |
| Villagers | Ember-kin |

## Biomes

- **Cindara Ash Wastes**: the original Cindara biome. Ashstone plains with the odd sulfur patch and basalt spire.
- **Ember Plains**: Ashstone shot through with glowing Ember-Cracked Ashstone, and pools of lava. Cindrite is richer here (about 1.5 ×).
- **Sulfur Vents**: yellow Sulfur Crust fields between tall Basalt Spires.

## Blocks and items

| Block / item | What makes it special |
|---|---|
| Ashstone | Soft: any pickaxe mines it instantly. Drops 2 Ash Dust (Silk Touch drops the block). |
| Ember-Cracked Ashstone | The cracks glow. Standing on it without heat-rated boots burns you. Water next to it quenches it into Ashstone. Needs a stone pickaxe. |
| Basalt Spire | Natural columns that ember strikes never hit. Needs an iron pickaxe. |
| Magma Glass | Glows (light 7). The only transparent block lava cannot destroy. Breaks without Silk Touch. Craft: 8 Glass around a Magma Block → 8. |
| Ash Layer | Builds up like snow during Ash Fall (the weather adds up to four layers; you can stack it to eight). Three or more layers slow you down. It only melts next to fire or lava. Shovel it for Ash Dust (one per layer). |
| Sulfur Crust | Cindara's ore. Drops 2 to 4 Sulfur (Fortune applies). If fire touches it, it sets nearby flammable blocks alight and burns down to Ashstone. |
| Sulfur | Two Sulfur and one coal or charcoal craft into two Gunpowder. |
| Ash Dust | Two craft into Ashstone; one crafts into an Ash Layer. |
| Cindrite Ore | Unchanged. Generates at the same rate as before, more often in the Ember Plains. |

Deep underground, new Cindara land also has the usual large copper and iron ore veins.

## The Ember-kin

Cindara's **[villagers](Alien-Villagers)** are the stocky **Ember-kin**. They build hamlets and ruins from
cindrite, basalt and magma glass, and they trade cindrite, sulfur and heat-protection suit parts. They also buy
ash. Their settlements appear only in newly generated land.

## Weather: Ash Fall and ember strikes

During Ash Fall, grey ash settles on open ground and piles into Ash Layers. In the strongest part of a storm,
ember strikes (lightning) hit open ground under the sky near players, at most one near each player every five
seconds (servers can space them out further with `weather.emberStrikeIntervalTicks`). They hurt and ignite creatures
but never set blocks alight. A Basalt Spire is never struck. Ash Fall never starts close to the eclipse.

## Upgrading from 1.x

Your Cindara world is kept. Chunks you already explored stay exactly as they were: same blocks, same biome.
Only chunks generated for the first time use the new terrain, so there is a **visible seam** where old and new
terrain meet. This is expected. Nothing is regenerated or converted.

## Terraforming

On Cindara the Terraformer turns ashstone into cinder soil (Rooted). The steam vents cool and water appears
(Hydrated), and the land settles into the **savanna** you know from 1.x, with acacia-like Cinder Trees and Ember
Strutters (Living).

**Worlds: Cindara** in the Star Guide: land, mine Cindrite, build with Magma Glass, last two minutes under the
open sky through an ash fall, and root your first terraformed ground.
