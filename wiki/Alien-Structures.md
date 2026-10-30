# Alien Structures

The settlements and ruins of the alien planets — procedurally generated from the alien decoration set,
spaced apart so they never clutter the world.

## Overview

Three structures generate on the surface, all built from the **[alien decoration blocks](Alien-Decoration)**
with a futuristic look: tile podiums, brick walls with **glowing crystal window bands**, taller **lit
corner pillars**, **tapered roofs** and **crystal spires**. Each holds a **[Village Core](Village-Core)**.

Hamlets and Ruins appear on every world with **[villagers](Alien-Villagers)**: Greenxertz, Cindara, Glacira and
the Nyxara Twilight Band. Mega-Cities appear only on **Greenxertz** and in **Nyxara's Twilight Band**. Each
culture builds the same shapes in its own materials:

| Planet | Walls | Floors and pillars | Windows and lights |
| --- | --- | --- | --- |
| Greenxertz | alien bricks | alien tile, alien pillars | alien crystal, alien lamps |
| Cindara | Block of Cindrite | polished basalt, Basalt Spire | Magma Glass, shroomlights |
| Glacira | Block of Glacite | packed ice, Permafrost | blue ice, sea lanterns |
| Nyxara | Nyx Stone | polished blackstone, purpur pillars | tinted glass, Void Crystal |

Structures only appear in newly generated land.

## Hamlet

A small alien outpost — the common find and your "starter village".

- A glowing tile plaza with a lamp border.
- Two futuristic towers.
- A central **[Village Core](Village-Core)** on a lit crystal podium.

## Ancient Ruin

A large, half-buried, weathered alien hall — uncommon.

- Built from **cracked alien brick** with collapsed walls and a broken roof (sunken into the ground).
- A dead crystal core at the centre.
- A **loot vault** (chest) of rare alien goods: Alien Core, Alien Tech Scrap, Alien Fragments, Nerosium

  Ingots and emeralds.

## Mega-City

The massive end-state alien settlement — very rare, and a real expedition.

- A pillared, lit, **crenellated curtain wall** (41×41) with four gates.
- A tile plaza of futuristic towers around a **central keep**.
- The keep holds a **grand vault** — Alien Cores, **both** pieces of **[Artificer gear](Alien-Gear)**,

  diamonds and emeralds — guarded by the **[Ruin Warden](Creatures#ruin-warden)** boss.

## Spacing & density

To stop structures clustering (and to cap how many appear in an area), all three share a deterministic
**region grid**:

- The world is divided into **16×16-chunk cells** (≈256 blocks).
- Each cell gets **at most one** structure, chosen by a weighted hash: ~26% Hamlet, ~8% Ruin, ~2%

  Mega-City, and the rest **empty** for breathing room. Each newer world mixes its own planet value into

  the hash, so its structures do not line up with another world's. Greenxertz, Cindara and Glacira keep the

  original grid, so nothing moves in existing worlds.

- The structure sits at a deterministic anchor chunk inside the cell, kept off the edge so footprints

  don't straddle boundaries.

The result is even spacing and a hard density cap, regardless of the placement RNG.

## Details

- Custom worldgen features listed in each villager planet's biomes.
- A Mega-City is centred on its chunk, so the whole 41×41 city is always built. Before 2.0, a city near a
  chunk edge could lose part of its wall.
- IDs: `nerospace:hamlet`, `nerospace:ruin`, `nerospace:mega_city`.
