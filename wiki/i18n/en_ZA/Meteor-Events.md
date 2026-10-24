# Meteor Events

Falling meteors are a **world event**: rare crashes that seed the Overworld (and the planets) with
alien materials and a jump-start of off-world ores — no rocket required to get your first taste of
what's out there.

## Overview

Every so often, near an active player, a meteor is scheduled to fall. After a short warning window it
streaks down from the sky on a fiery arc, craters into the ground, and leaves a small **crater of
[Meteor Rock](Meteor-Rock)** around a glowing **[Meteor Core](Meteor-Core)** — the "box" that holds
the RNG loot. The crash is deliberately modest (a few-block crater, no wide explosion, no fire) so it
won't grief your builds.

Impacts happen **as you get close** to the site: meteors are scheduled far out and only fall once the
area is loaded, so following the warning toward the site is part of the hunt.

## The Meteor Tracker

The **Meteor Tracker** is an early-warning compass. While you hold it, it shows the nearest tracked
meteor in the action bar:

- **State** — _Incoming_ (still falling / not yet landed) or _Landed_ (the crater is waiting).
- **Heading** — a compass direction (N, NE, E, …) toward the site.
- **Distance** — how far off it is, in metres.

You can craft it in survival (shaped):

```text
. N .
R C R
. N .
```

`N` = Nerosteel Ingot · `R` = Redstone · `C` = Compass

## Loot

Breaking the [Meteor Core](Meteor-Core) spills the meteor's contents (break-to-loot). The loot is
rolled **once** when the meteor lands and then fixed, so it's the same for everyone who reaches it —
no re-roll exploit. A meteor always carries a handful of **Alien Fragments**, plus weighted bonus
rolls of existing raw ores (Raw Nerosium, Raw Nerosteel, Xertz Quartz) and the rarer
**Alien Tech Scrap** and **Alien Core**.

| Item                 | Rarity                                 | Role                                             |
| -------------------- | -------------------------------------- | ------------------------------------------------ |
| **Alien Fragment**   | common (guaranteed) | Future **scanner** feedstock.    |
| **Alien Tech Scrap** | uncommon                               | Future upgrade crafting.         |
| **Alien Core**       | rare                                   | High-value scanner/upgrade gate. |

The three are grouped under the `nerospace:alien_materials` item tag for future recipes. The scanner
that turns them into upgrades is on the **[Roadmap](Roadmap)** / **[Future Features](Future-Features)**.

## Calling a meteor (creative)

The **Meteor Caller** is a creative-only tool: right-click any block to call a meteor down onto that
spot immediately, with freshly rolled loot — the same path natural spawning uses. It does nothing in
survival.

## Configuration

Meteor pacing is fixed. There are no meteor config keys. The fixed values are:

- about one natural meteor every **9,000 seconds** (2.5 hours), near an active player;
- a **30 second** warning before it falls;
- it lands **200 to 500 blocks** away;
- up to **4** meteor sites at once per dimension.

See **[Configuration](Configuration)** for the keys that do exist.

## Details

- Trigger: rare timer near players (about one every 9,000 seconds; fixed).
- Dimensions: Overworld, Greenxertz, Cindara, Glacira (not the void station).
- Gravity: meteors descend more slowly on low-[gravity](Planet-Gravity) worlds.
- Blocks: [Meteor Rock](Meteor-Rock), [Meteor Core](Meteor-Core) · Entity: `nerospace:falling_meteor`.
- Items: `alien_fragment`, `alien_tech_scrap`, `alien_core`, `meteor_tracker`, `meteor_caller`.
