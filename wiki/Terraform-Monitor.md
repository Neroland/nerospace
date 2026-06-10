# Terraform Monitor

The readout block for deeper terraforming: shows the nearest [Terraformer](Terraformer)'s stage
radii, hydration buffer and stall reason, and reports the **local ground's terraform stage** on a
comparator.

## Obtaining
**Craft** (shaped):
```
N G N
Q R Q
N N N
```
`N` = Nerosteel Ingot · `G` = Glass · `Q` = Xertz Quartz · `R` = Redstone

## How it works
- **Place it anywhere** on or near terraformed land. It links to the nearest Terraformer within
  **32 blocks** automatically (no wiring).
- **GUI readout:** the local column's stage (Dead / Rooted / Hydrated / Living), the linked
  machine's three stage radii, its hydration units, and a red "Needs glacite" warning when the
  water stage has stalled.
- **Comparator output = local stage:** 0 (dead), 5 (Rooted), 10 (Hydrated), 15 (Living). Open the
  ranch gates when the land turns Living, or alarm when terraforming reaches your base.

## Details
- ID: `nerospace:terraform_monitor` · Tool: pickaxe, iron tier · Drops: itself
