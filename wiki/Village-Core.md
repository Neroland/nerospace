# Village Core

The controller block at the heart of an alien village — claim it, stock it, and teach the village to
build itself.

## Overview

The **Village Core** is the hub of the alien-village system: it stores the owner, a construction
stockpile, the build queue, passive production, and the current quest. It appears in generated
**[structures](Alien-Structures)** and can be placed by hand.

## Obtaining

- Found at the centre of every **[hamlet, ruin and mega-city](Alien-Structures)**.
- Available in the Nerospace creative tab.

## Claiming & growing a village

1. **Claim** — right-click an unclaimed core to become its owner.
2. **Stock it** — right-click with **Block of Nerosteel** to fill the construction store.
3. **Teach the next building** — right-click as the owner. If the nearby **[villagers](Alien-Villagers)**

   trust you enough *and* the store has the materials, the core commits the cost and starts building.

4. **Watch it rise** — the building is placed **block-by-block over real time** with particles.

   Right-click again to read the % progress.

The core reads your standing as the **highest reputation tier among nearby villagers**, so trading with
the locals is what unlocks teaching.

### Build catalogue

| Building | Requires | Cost |
| --- | --- | --- |
| Hut | trust tier 2 | 32 Nerosteel |
| Workshop | trust tier 3 | 48 Nerosteel |

Buildings are taught in order as the village grows.

## Functional village

Once buildings exist, the core becomes a small engine:

- **Production** — completed buildings periodically yield goods (Hut → bread, Workshop → nerosteel).

  **Sneak-right-click** the core to collect the output and read the current task.

- **Quests** — the village posts a fetch task (e.g. *bring 8 Xertz Quartz*). Right-click the core with

  the requested item to hand it in for **emeralds + a village-wide trust bump**; a new task rolls.

- **Raids** — at night, claimed villages near a player are occasionally raided by hostile mobs.

  Toggle with `alienRaidsEnabled` (see **[Configuration](Configuration)**).

## Interactions at a glance

| Input | Result |
| --- | --- |
| Right-click (empty hand, unclaimed) | Claim the village |
| Right-click with Block of Nerosteel | Deposit into the construction store |
| Right-click (owner, empty hand) | Teach/raise the next building, or read progress |
| Sneak-right-click | Collect production + read the current quest |
| Right-click with the quest item | Hand in the quest for emeralds + trust |

## Details

- ID: `nerospace:village_core` · tool pickaxe · drops itself
- Saves owner, stockpile, built count, the active build job, production buffers, and the current quest.
