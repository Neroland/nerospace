# Nerospace — Roadmap

A public, high-level snapshot of where the mod stands and where it's heading (Minecraft 26.1 /
NeoForge, built standalone). **1.0.0 is the first full release** — the complete progression from
the first nerosium ore to a fully terraformed, rain-on-your-roof planet ships today.

📖 Full block-by-block docs live in the **[Wiki](../../wiki)**; release notes in
[CHANGELOG.md](CHANGELOG.md).

## ✅ Shipped in 1.0.0

- **Materials & machines** — the nerosium chain + Nerosium Grinder; Nerosteel, Xertz Quartz,
  Cindrite, Glacite; Combustion/Passive Generators; the Fuel Refinery; common-tag (`c:`) coverage
  across every material.
- **Logistics** — the **Universal Pipe** (energy + fluid + gas + items in one tube) with per-face
  modes, filters, upgrades, the Configurator; Battery / Fluid / Gas / Item storage endpoints;
  capabilities on every sensible machine face.
- **Space travel** — four rocket tiers with bespoke per-tier models; the 3×3 launch pad and the
  5×5 **Heavy Launch Complex** with boarding gantry; auto-fuelling Fuel Tanks; four destinations
  (Orbital Station, Greenxertz, Cindara, Glacira).
- **Player-founded stations** — Station Charters (anvil-renamed) founding named stations anchored
  by a Station Core, all selectable in the rocket UI.
- **Survival** — airless worlds with a per-block oxygen field (sealed rooms, leaks, airlock
  refills), two Oxygen Suit tiers plus **Thermal** and **Cryo** hazard variants, and an O₂ HUD.
- **Terraforming** — staged maturation (Rooted → Hydrated → Living) with a glacite-fed water
  cycle, real weather, mature biomes, the Terraform Monitor, and three breedable livestock
  species.
- **Creatures** — eight bespoke mobs with custom models, animations, and glow layers.
- **Progression** — the **Star Guide** interactive progression tree backed by a full advancement
  tree; the creative `/nerospace gallery` showcase.

## 🛠️ Next up (first post-1.0 updates)

- ~~JEI integration~~ **done** — with JEI installed, the grinder, fuel refinery and combustion
  generator show their own recipe categories (standard recipes/tags already worked out of the box).
- **EMI integration** as soon as it reaches 26.1.
- **[Modrinth](https://modrinth.com/mod/nerospace)** — wired into the same release pipeline as
  CurseForge; the listing goes live alongside the first published version.
- **Balance tuning from player feedback** — the config multipliers make this cheap; tell us what
  feels off.
- Bespoke audio (rocket launches, machines, planet ambience, creatures) to replace the current
  vanilla-alias sounds.

## 🔭 Later / exploring

- A radiation hazard + suit around a new destination (asteroid field?).
- More planets, more late-game terraforming toys, deeper station building.
- Cross-mod interoperability extras (already mostly free via standard capabilities/tags as other
  tech mods reach 26.1).

## Contributing / feedback

Ideas and bug reports are welcome — open a [GitHub issue](https://github.com/Neroland/nerospace/issues)
or join the [Discord](https://discord.gg/ArPXvYUzJG).
