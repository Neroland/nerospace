# Nerospace — 1.0.0 Release Checklist

Shaped by the release-planning Q&A (2026-06-05). Versioning goes **straight to 1.0.0** (no public
beta), published to **CurseForge** first, **Modrinth later**. No deadline — quality-driven; every
section below gates the release unless marked *(post-1.0)*. Keep this updated as items land; tick a
box only after the gradle-MCP build + gametests are green AND the in-game check passed.

---

## 1. Feature completion — new content for 1.0

### Big multiblock launch pad
- [ ] Design doc for the larger pad (footprint, modules: fuel, gantry, etc.)
- [ ] Implement bigger footprint (5×5+) with functional modules
- [ ] Migrate/keep the 3×3 tier as the entry-level pad (no breaking of existing worlds)
- [ ] Flatter/proper pad block model (replaces the near-full cube)
- [ ] Gametests for new pad formation + gating

### Star Guide (progression block) — NEW
- [ ] Design: a block that holds the guidebook and shows an **interactive visual progression/quest
      tree** ("Star Guide")
- [ ] Guidebook item (placeable on the Star Guide block)
- [ ] Interactive GUI: progression chapters nerosium → machines → rocket → planets → terraforming
- [ ] Quest/step completion tracking (per-player attachment, synced)

### Advancements
- [ ] Full advancement tree mirroring the Star Guide progression (datagen)
- [ ] Advancement-triggered toasts for key milestones (first launch, first planet, terraformed)

### More planets / destinations
- [ ] Design + implement at least one destination beyond Greenxertz / Cindara / Station
- [ ] Tier/recipe gating for the new destination(s)

### Multiple player-built stations
- [ ] Design: how players found/register additional stations as rocket destinations
- [ ] Implement station creation + destination selection in the rocket UI

### Deeper terraforming
- [ ] Design pass: what "deeper" means (stages? biomes? water cycle?)
- [ ] Implement chosen scope; keep existing Terraformer saves working

### Space-suit hazard variants
- [ ] Heat-resistant suit variant (Cindara/volcanic)
- [ ] Any other hazard variant per design (radiation?)
- [ ] HUD badge + `suitTier` handling extended for variants

### Remaining small features (from FUTURE_WORK)
- [x] ~~Hopper/pipe feeding of the Oxygen Generator fuel slot~~ N/A — stale item: the gas rework
      made the O₂ Generator grid-powered electrolysis (no fuel slot; energy arrives via pipes)
- [x] Item cap on the rocket intake (`Capabilities.Item.ENTITY`) — landed in the suit-station
      batch (entity cap + pad proxy, `pad_proxies_rocket_intake` gametest)
- [x] Rocket UI: painted background panel, graphical fuel gauge, trajectory arc — landed in the
      machine-GUI batch (`rocket.png` panel via `TexturedContainerScreen`, `hGauge`, dotted arc)
- [x] Doors/trapdoors/glass as valid sealed-room boundaries — in `OxygenField.canHold` (closed
      door/trapdoor = wall, open = leak, glass = airtight); `isLeaky` tag-order bug fixed +
      `oxygen_sealing_boundaries` gametest added (13/13 green)

## 2. Art & model overhaul

Goal: bespoke models for **all blocks / items / entities / fluids / gasses**, and a **more colourful
palette** (current art reads dull). Keep family identities (nerosium red/purple, Greenxertz
green/steel, rocket tiers steel + accent) but raise saturation/contrast.

- [ ] Palette pass in `tools/gen_textures.py` — brighter, more saturated families; regenerate
      with `--force` only on deliberately re-rendered assets
- [ ] Custom (non-trivial-cube) block models where it matters: machines, pad, pipes, tanks,
      Star Guide
- [ ] Custom item models / improved icons
- [ ] Worn suit armour art for both tiers (replace placeholders) + hazard variants
- [ ] Per-creature Blockbench sources; retire legacy shared `GreenxertzCreatureModel` + `.bbmodel`s
- [ ] Nicer per-creature textures (beyond the script repaint)
- [ ] Rocket entity model (true model, not the tuned-blind transform) — confirm pad stance + rider seat
- [ ] Fluid visuals: `IClientFluidTypeExtensions` for `rocket_fuel` (still/flow) + particle texture
- [ ] Gas visuals (O₂ in pipes/tanks)
- [ ] Animated/segmented machine gauges, real fluid render in GUIs, tooltips
- [ ] Every model resolves in-game — zero missing-texture placeholders

## 3. Audio

- [ ] 1.0 ships with the current vanilla-alias `sounds.json` (acceptable per Q&A)
- [ ] Subtitles present for every alias
- [ ] *(post-1.0)* Bespoke `.ogg` audio: rocket launch, machines, planet ambience, creatures

## 4. Config refactor

Goal: shrink the config file while keeping modpack flexibility — **base numbers + multiplier keys**.

- [x] Redesign config: internal base values (`Tuning.java`), five exposed multipliers
      (`oxygenDrain`, `oxygenCapacity`, `energyRate`, `fuelCost`, `machineSpeed`, clamped 0.1–10)
- [x] Migrate existing config keys (breaking change documented in FUTURE_WORK.md +
      `wiki/Configuration.md`; dev config deleted/regenerates)
- [x] Document every key in the wiki (`wiki/Configuration.md`)
- [ ] Sanity-check extreme multiplier values don't break machines/atmosphere (code clamps scaled
      values to ≥1 and launch cost to tank size; confirm 0.1x/10x in runClient)

## 5. Balance

- [ ] Recipe cost audit across the whole progression (pacing nerosium → T3 rocket → terraform)
- [ ] Oxygen drain/tank/refill rate tuning from playthrough feel
- [ ] Energy economy audit: generator output vs machine consumption at each game stage
- [ ] Fuel economy: costs per launch per tier feel right
- [ ] Post-release: keep tuning from player feedback (multipliers make this cheap)

## 6. Verification & testing

### Targeted runClient pass (every pending FUTURE_WORK item)
- [ ] Fuel Tank auto-fuel + 3×3 speed bonus + pumping particles/sound
- [ ] Pad gating messages (3×3, T3 Station Wall ring) + grounding on pad break
- [ ] Oxygen drain/refill/suffocation + sealed rooms + airlock refill
- [ ] O₂ HUD gauge + suit badge + vanilla bubble suppression
- [ ] Suits T1/T2 (tank sizes, mixed-set = T1)
- [ ] Machine GUIs (all themed screens, readouts, slots)
- [ ] Machine cap-feed in-world (hopper + pipe into every machine)
- [ ] Rocket UI (fuel readout, trajectory, destination cycling, launch)
- [ ] Mob idle/ambient + walk animations + glow layers in-game
- [ ] Terraformer + oxygen field end-to-end

### Survival playthrough
- [ ] Full start-to-terraform survival playthrough (no creative shortcuts) — log friction/balance notes

### Multiplayer
- [ ] Dedicated-server pass (`runServer` + 2 clients): oxygen sync, rocket rides, pipe networks,
      GUIs, world join (registry sync), suit HUD per player

### Automated
- [ ] Gametest suite green (`gradlew runGameTestServer`) including tests for all new 1.0 features
- [ ] `ecjCheck` clean
- [ ] `runData` + `build` green via gradle MCP

## 7. Performance (release gate — "it needs to be performant")

- [ ] Stress-test large pipe networks (hundreds of pipes/machines)
- [ ] Stress-test oxygen field: large sealed rooms, multiple players, terraformed areas
- [ ] Spark/profiler pass on a busy dedicated server; fix anything hot
- [ ] Client FPS check around pipe rendering / particles / many mobs

## 8. Compatibility (standalone, plays well with others)

- [ ] Common ore/material tags (`c:` conventions) on all ores/ingots/raw materials
- [ ] Conditional recipes / tag-based inputs where another mod's materials could substitute
- [ ] Capabilities exposed everywhere sensible (already the convention — audit for gaps)
- [ ] JEI/EMI integration *(when they reach 26.1 — otherwise first post-1.0 patch)*
- [ ] No hard dependencies on any other mod (verify mods.toml)

## 9. Distribution & legal

- [ ] License file matching the chosen posture: **open source, forks allowed, no redistribution**
      (custom licence — review wording carefully; consider having it checked)
- [ ] License consistent in `LICENSE`, `neoforge.mods.toml`, CurseForge/GitHub metadata
- [ ] Explicit **modpack permission statement** (allowed? CurseForge-distributed packs only?)
- [ ] `PRIVACY.md` current; Sentry opt-out documented and POPIA/GDPR scrubbing re-verified on the
      release build
- [ ] Version set to 1.0.0; `publish.yml` release flow tested end-to-end (CurseForge upload)
- [ ] *(post-1.0)* Modrinth listing + publish.yml target

## 10. Marketing & community

- [ ] Screenshot/gallery set: rockets, launches, planets, machines, creatures, terraforming
- [ ] Trailer/showcase video (launch + planets + terraforming)
- [ ] Final pass on `art/curseforge_description.md`; link the wiki
- [ ] Wiki completeness pass — every block/item/creature/system has a current page; new 1.0
      features documented
- [ ] GitHub issue templates (bug report + feature request)
- [ ] Discord server created and linked from CurseForge/GitHub/README
- [ ] `CHANGELOG.md` (Keep-a-Changelog) starting at 1.0.0
- [ ] README/ROADMAP refreshed to reflect 1.0 (drop "early WIP" framing)

## 11. Pre-launch

- [ ] Private playtests: a few friends on a dedicated server with the release candidate jar
- [ ] Fix-up pass from playtest feedback
- [ ] Fresh-world + existing-world (datafix/migration) load check on the final jar
- [ ] Final jar built from a tagged commit; gametests + manual smoke test on THAT jar
- [ ] Publish 1.0.0 🚀
