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
- **Auto-fuelling:** when a rocket is on an adjacent launch pad, the tank pumps fuel into it. A
  complete **3×3 launch pad** pumps **4× faster** (160 vs 40 mB/tick) — a reason to build the full pad.
- **Comparator:** emits a redstone signal scaled to its fill level.

## Details
- ID: `nerospace:fuel_tank` · Tool: pickaxe, iron tier · Drops: itself
