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

- **State** — *Incoming* (still falling / not yet landed) or *Landed* (the crater is waiting).
- **Heading** — a compass direction (N, NE, E, …) toward the site.
- **Distance** — how far off it is, in metres.

It's a creative item for now (no survival recipe yet) — a survival craft is planned alongside the
scanner system below.

## Loot

Breaking the [Meteor Core](Meteor-Core) spills the meteor's contents (break-to-loot). The loot is
rolled **once** when the meteor lands and then fixed, so it's the same for everyone who reaches it —
no re-roll exploit. A meteor always carries a handful of **Alien Fragments**, plus weighted bonus
rolls of existing raw ores (Raw Nerosium, Raw Nerosteel, Xertz Quartz) and the rarer
**Alien Tech Scrap** and **Alien Core**.

| Item | Rarity | Role |
| --- | --- | --- |
| **Alien Fragment** | common (guaranteed) | Future **scanner** feedstock. |
| **Alien Tech Scrap** | uncommon | Future upgrade crafting. |
| **Alien Core** | rare | High-value scanner/upgrade gate. |

The three are grouped under the `nerospace:alien_materials` item tag for future recipes. The scanner
that turns them into upgrades is on the **[Roadmap](Roadmap)** / **[Future Features](Future-Features)**.

## Calling a meteor (creative)

The **Meteor Caller** is a creative-only tool: right-click any block to call a meteor down onto that
spot immediately, with freshly rolled loot — the same path natural spawning uses. It does nothing in
survival.

## Configuration

Meteor frequency, the warning window, target distance, crater size and loot generosity are all tunable
— see the **Meteor events** section of **[Configuration](Configuration)**. Natural spawning can be
turned off entirely (the Meteor Caller still works).

## Details

- Trigger: rare timer near players (≈ one per 2–3 play-hours by default, configurable).
- Dimensions: Overworld, Greenxertz, Cindara, Glacira (not the void station).
- Blocks: [Meteor Rock](Meteor-Rock), [Meteor Core](Meteor-Core) · Entity: `nerospace:falling_meteor`.
- Items: `alien_fragment`, `alien_tech_scrap`, `alien_core`, `meteor_tracker`, `meteor_caller`.
