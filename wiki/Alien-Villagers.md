# Alien Villagers

The social aliens of the Nerospace planets — wary wanderers you win over to unlock trades and grow a
village.

## Overview

The **Alien Villager** (`nerospace:alien_villager`) is a peaceful, **wary-neutral** NPC: it strolls its
home biome, watches you, and edges away if you crowd it, but never attacks. Earn its trust and it
becomes a merchant and the key to the whole village system — trading, the
**[Village Core](Village-Core)**, and the alien **[structures](Alien-Structures)**.

The loop: *discover a settlement → earn trust → unlock trades → teach the village to build → it grows
into an engine → deeper structures (and a boss) open.*

## Where they live

- Spawn naturally on **Greenxertz** (and the mature `terraformed_meadow`).
- Sparse groups also spawn on **Cindara** and **Glacira**.
- Creative: the **Alien Villager Spawn Egg** (Nerospace tab).

## How they look

Every villager is visually unique without a new entity per look — a layered, seed-driven appearance:

- **Per-individual tint** — a stored colour seed gives each villager a slightly different shade, so no
  two are identical. Deterministic: a given villager always looks the same.
- **Per-planet skin** — Greenxertz green/steel, Cindara ember/red, Glacira frost/pale. Within
  Greenxertz, the mature meadow wears a lighter accessory set.
- **Mood warmth** — as your reputation with a village rises, its villagers' tint warms, so you can read
  trust at a glance.
- **Glowing eyes & shoulder crystals** — emissive, visible in the dark.

## Earning trust (reputation)

Each villager tracks a **per-player reputation score (0–100)** mapped to **6 tiers**:

| Tier | Name | Unlocks |
|---|---|---|
| T0 | Stranger | nothing — refuses to trade (wary head-shake) |
| T1 | Acquainted | basic trades |
| T2 | Trusted | mid trades · can teach **Tier-1 buildings** |
| T3 | Allied | rare trades · **Tier-2 buildings** |
| T4 | Honored | exclusive gear trades |
| T5 | Kin | best trades |

Reputation rises through:

- **Gifts** — right-click a villager while holding a valued item (**Xertz Quartz**, **Nerosium Ingot**,
  **Alien Fragment**, or **Emeralds**). It takes one, with happy-villager particles.
- **Trade volume** — every completed trade nudges trust up.
- **Quests** — handing a village task in to a **[Village Core](Village-Core)** bumps the trust of every
  nearby villager.

Reputation is keyed by Minecraft player UUID only — no names or interaction logs are sent anywhere
(POPIA/GDPR-safe).

## Trading

Right-click a villager at **tier 1+** to open the **vanilla trading screen**. Offers are tier-gated and
**cumulative** (a higher-trust villager also offers the lower tiers). Emeralds are the currency, so the
goods are useful in any modpack.

| Tier | Sample offers |
|---|---|
| T1 | sell 12 Xertz Quartz → 1 emerald · 1 emerald → 3 iron · 2 emeralds → 6 bread |
| T2 | 4 emeralds → Nerosium Ingot · 1 emerald + 8 Raw Nerosteel → 4 Nerosteel Ingots · sell Alien Fragments |
| T3 | 8 emeralds → diamond · 5 emeralds → Rocket Fuel Canister |
| T4 | 12 emeralds + 2 Alien Tech Scrap → Alien Core · 16 emeralds → **[Xertz Resonator](Alien-Gear)** |
| T5 | 18 emeralds → 3 diamonds · Alien Core + 24 emeralds → **[Grav Striders](Alien-Gear)** |

The top tiers are the only trade source of the exclusive **[Artificer gear](Alien-Gear)**.

## Details

- ID: `nerospace:alien_villager` · category CREATURE · size 0.6 × 1.95
- Variant (planet / home biome / colour seed) is synced and saved; reputation is saved per villager.
- Voice currently aliases the vanilla villager sounds.

See also: **[Village Core](Village-Core)** · **[Alien Structures](Alien-Structures)** ·
**[Alien Gear](Alien-Gear)** · **[Creatures](Creatures)**.
