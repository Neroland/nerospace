# Fuel Tank

Stores rocket fuel and auto-fuels a rocket sitting on an adjacent launch pad.

## Overview
The Fuel Tank is the first piece of launch-pad automation: it holds a large buffer of **Rocket Fuel**
and, each tick, pumps it into a rocket standing on a connected **[Rocket Launch Pad](Rocket-Launch-Pad)**
— so you don't have to hand-fill rockets with canisters.

## Obtaining
**Craft** (shaped):
```
N G N
G C G
N G N
```
`N` = Nerosteel Ingot · `G` = Glass · `C` = Rocket Fuel Canister

## How it works
- **Capacity:** 32,000 mB of rocket fuel.
- **Filling:** right-click with a **Rocket Fuel Bucket** or **Canister** to add fuel (empty bucket is
  returned); a fluid pipe can fill it via the fluid capability. Empty-hand right-click prints the
  current fuel level.
- **Canister auto-feed:** hoppers and [Universal Pipes](Universal-Pipe) can insert **Rocket Fuel
  Canisters** through the tank's item capability — each is converted to **1,000 mB** of fuel on tick.
  Pair it with a [Fuel Refinery](Fuel-Refinery) (or a canister line) for fully automated fuelling.
- **Auto-fuelling:** when a rocket is on an adjacent launch pad, the tank pumps fuel into it. A
  complete **3×3 launch pad** pumps **4× faster** (160 vs 40 mB/tick) — and since the multiblock
  gating pass a rocket can only deploy on a full 3×3 anyway. On a
  **[Heavy Launch Complex](Launch-Gantry)** (5×5 + gantry) the rate rises to **480 mB/t** (12×),
  filling a Tier 4's 24,000 mB tank in under a minute.
- **Full automation:** the pad itself also proxies the rocket's **fuel-intake slot** as an item
  capability, so hoppers/[Universal Pipes](Universal-Pipe) can deliver Rocket Fuel Buckets and
  Canisters straight into the rocket — a Fuel Tank + an item line makes launch prep hands-free.
- **Comparator:** emits a redstone signal scaled to its fill level.

## Details
- ID: `nerospace:fuel_tank` · Tool: pickaxe, iron tier · Drops: itself
