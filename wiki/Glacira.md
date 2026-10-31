# Glacira

Glacira is a **Tier 4** destination: blue ice plains, frozen methane lakes and slick ice spires under a pale sun, with
aurora on the long nights.

| | |
|---|---|
| Dimension | `nerospace:glacira` |
| Rocket | Tier 4 |
| Gravity | 0.4 × Overworld |
| Day | 72 000 ticks (three Overworld days), with long dawn and dusk twilights |
| Weather | Diamond dust, on average every two Overworld days, only at night or twilight, lasting about 3 to 10 minutes |
| Hazard | Cold (Cryo Suit, all four pieces) |
| Villagers | Frostbound |
| Native life | Frost Strider; Woolly Drifts on terraformed ground |

## Biomes

- **Glacira Ice Plains**: Permafrost over deep packed ice. Ice faces show Packed Cryo-Ice. You'll find the odd Frozen Geyser, a few Frost Crystal Clusters, and Aurora Stone veins at the surface.
- **Methane Shore**: low ground and frozen lakes. Methane Ice lines the lake beds and runs through the rock beneath.
- **Glacira Spires**: high, steep peaks of Packed Cryo-Ice. Frost Crystal fields grow here, and there is more glacite below.

Glacite ore generates in the stone under every Glacira biome. Low gravity covers all three biomes.

## Blocks and items

| Block / item | What makes it special |
|---|---|
| Permafrost | Frozen ground. A Hydration Module can't thaw it. Only a Terraformer that reaches its final stage turns it into soil. |
| Packed Cryo-Ice | As fast as blue ice, and it never melts at any light level. Needs Silk Touch to collect. Craft it from blue ice and a Frost Shard. |
| Aurora Stone | Its veins cycle through aurora colours. It lights up (level 8) only during Glacira's night. |
| Frozen Geyser | Vents steam every 8 seconds and launches whoever stands on it about seven blocks up. You take no fall damage for the next 3 seconds. |
| Frost Crystal Cluster | Glows (light 3 to 9 by size). It grows one stage each time a diamond-dust storm ends, if it can see the sky. Silk Touch keeps its size. Otherwise it drops 1 to 4 Frost Shards. |
| Methane Ice | Needs an iron pickaxe and drops 2 to 4 Methane Crystals (Fortune applies). Fire, lava, a campfire or a torch within two blocks sets it off. The explosion hurts but does not break blocks unless the server enables `blocks.methaneIceBreaksBlocks`. |
| Frost Shard | Crafts into light blue dye and into Packed Cryo-Ice. |
| Methane Crystal | Fuel: it burns in the Combustion Generator (2 400 ticks, one and a half times coal) and makes torches. |
| Glacite | Unchanged: the Cryo Suit and terraforming material. |

## Hazards and protection

Glacira is airless and **frigid**. Without a full four-piece **Cryo Suit** your suit's oxygen drains four times as
fast, with a creeping frost vignette as a warning. A plain Tier 2 suit, which is enough for Mars, is not enough
here. Glacira has no harmful radiation, so you do not need Rad Lining
here. Mind the Methane Ice around torches and campfires. See [Oxygen Suit](Oxygen-Suit).

## Clock and weather

A Glacira day lasts 72 000 ticks (three Overworld days), with long twilights at dawn and dusk; the HUD clock dial
marks both and names them *Twilight*. Aurora Stone only glows at night.

**Diamond dust** only forms at night or in twilight. While it lasts, sparkling ice crystals hang in the air and open
water under the sky slowly freezes over (a few blocks per chunk per storm). When the storm ends, every Frost Crystal
Cluster that can see the sky grows one stage. See [Planet Clock and Weather](Planet-Clock-and-Weather).

**Beds** work at night (from just after sunset until part-way through the dawn twilight): when enough players on
Glacira are asleep, the clock jumps to sunrise. Diamond dust keeps falling through it. See
[Sleeping](Planet-Clock-and-Weather#sleeping).

**Open water freezes** even without diamond dust: Glacira is cold, so water under the open sky turns to ice like in a
snowy Overworld biome, unless the block light at the water is 10 or more. Sugar cane planted by that water breaks
when it freezes. Keep a light source within a few blocks of the water, or farm under a roof.

## Sky

A pale, cold sky with a small white sun ringed by a faint 22° halo with sun dogs, which brightens sharply in
diamond dust. Two little moons, Rime and Hoar, show real phases as they cross. Twilight is long and mauve. On clear
nights three layers of green-to-teal **aurora curtains** fold, ripple and drift slowly across the northern sky over
the days; during diamond dust they brighten and a rose fringe rides their upper edge. Shooting stars cross the night
sky.

The full sky is drawn on NeoForge for Minecraft 26.3. On every other version Glacira shows its colours with the vanilla sky shapes, and the world fog still follows the time of day and the weather.

## The Frostbound

Glacira's **[villagers](Alien-Villagers)** are the fur-wrapped **Frostbound**. They build hamlets and ruins
from glacite and ice, and they trade glacite, methane ice and cold-protection suit parts (only the helmet until you
reach their highest trust tier). They also buy frost shards. Their settlements appear only in newly generated land.
See [Alien Structures](Alien-Structures).

## Terraforming and fauna

On Glacira the [Terraformer](Terraformer) roots the ground (**Rooted**) and brings meltwater (**Hydrated**), but the
frozen surface holds: **Permafrost never melts until the final stage**. Hydration skips it, and only **Living**
ground thaws it. The land settles into the **tundra** you know from 1.x, with spruces and **Woolly Drifts**. The
wild **Frost Strider** still stalks the ice. See [Creatures](Creatures).

## Star Guide

**Worlds: Glacira**: land, mine Glacite, build with Aurora Stone, last two minutes under the open sky in diamond
dust, and root your first terraformed ground.

## Upgrading from 1.x

Glacira keeps its dimension id, its biome and its glacite. Chunks you explored in 1.x stay exactly as they were. Only
land that has never been generated gets the new terrain, so where old and new ground meet you may see a seam in the
surface blocks. The shape of the land lines up across it: Glacira keeps its 1.x terrain shape. Terraformers you
already built carry on from their current stage. See [Upgrading to 2.0](Upgrading-to-2.0).
