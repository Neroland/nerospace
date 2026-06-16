# Meteor Core

The glowing block at the centre of a crater — the meteor's "box" of loot.

## Overview

Every fallen meteor seats a Meteor Core at the bottom of its crater (see
**[Meteor Events](Meteor-Events)**). It holds the meteor's RNG loot, rolled **once** when the meteor
lands and then fixed, so the contents are identical for everyone who reaches it.

## Obtaining

**Not craftable.** A Meteor Core is placed by a meteor impact. **Break it to claim the loot**
(break-to-loot): the stored stacks spill out where it stood — a handful of **Alien Fragments** plus
weighted bonus rolls of raw ores and the rarer **Alien Tech Scrap** / **Alien Core**.

## Use

- Smash it open for the meteor's contents — the jump-start of alien materials and off-world ores.
- The alien items feed a future **scanner / upgrade** system (see [Roadmap](Roadmap)).

## Details

- ID: `nerospace:meteor_core` · Tool: pickaxe · No loot table — drops its stored contents on break.
- Emits light (level 10). Loot is configurable via the **Meteor events** keys in
  [Configuration](Configuration).
