# Roadmap

A public, high-level view of where Nerospace is and where it's going. **1.0.0 is the first full
release** — the complete progression from the first nerosium ore to a terraformed planet ships in it.
(Release notes live in the repo's `CHANGELOG.md`.)

## ✅ Shipped in 1.0.0

**Materials & machines**
- Nerosium material chain (ore → raw → ingot → dust) and the **Nerosium Grinder** (double ore yield).
- Planetary materials: **Nerosteel** + **Xertz Quartz** (Greenxertz), **Cindrite** (Cindara),
  **Glacite** (Glacira).
- The **Fuel Refinery** (coal + blaze powder + power → pipeable rocket fuel), storage blocks,
  station blocks, themed machine GUIs with animated gauges + comparator output.
- Common `c:` tags and tag-based recipes throughout, capability automation on every machine face.

**Space travel**
- **Rockets Tier 1–4** with bespoke per-tier models and an interactive in-rocket UI.
- The **3×3 Launch Pad** and the **Heavy Launch Complex** (5×5 + Launch Gantry) with formation
  reports; Tier 3 takes the Station Wall ring **or** the Heavy complex, Tier 4 needs the Heavy
  complex.
- Auto-fuelling **Fuel Tanks** (up to 480 mB/t) and hands-free fuel feeding through pad blocks.
- Destinations: **Orbital Station**, **Greenxertz**, **Cindara**, **Glacira** — plus
  **player-founded stations** via the Station Charter (up to 64 per world).

**Survival & atmosphere**
- Airless dimensions with a per-block **oxygen field** (sealed rooms, leaks, doors/glass as
  boundaries), the grid-powered electrolysis **Oxygen Generator**, airlock refills, and an O₂ HUD.
- The **Oxygen Suit** in two tiers plus **Thermal** and **Cryo** hazard variants (Cindara heat /
  Glacira cold = ×4 drain unprotected).

**Terraforming & life**
- The **Terraformer** with staged maturation — **Rooted → Hydrated → Living** — a glacite-fed water
  cycle (Hydration Module), mature per-planet biomes with real weather, and the Terraform Monitor.
- Eight bespoke **creatures**: five natives + three breedable livestock species on Living ground.

**Progression & tooling**
- The **Star Guide** interactive progression tree (7 chapters / 31 steps) backed by a full
  advancement tree; the creative `/nerospace gallery` showcase; a 36+-test gametest suite.

  
**JEI integration** 
- With JEI installed, the grinder, fuel refinery and combustion generator show their own recipe categories (standard recipes/tags already worked out of the box).

## 🛠️ Next up (first post-1.0 updates)

- **EMI integration** as soon as it reaches 26.1.
- **Balance tuning from player feedback** — the config multipliers make this cheap.
- **Bespoke audio** to replace the vanilla-alias placeholders.

## 🔭 Planned / exploring

See **[Future Features](Future-Features)**.

## ⏳ Deferred

- **Cross-mod integration** (e.g. Mekanism) waits until those mods port to Minecraft 26.1. Nerospace
  is standalone by design — tags + NeoForge capabilities mean most integration comes free later.
