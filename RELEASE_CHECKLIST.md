# Nerospace — 1.0.0 Release Checklist

Shaped by the release-planning Q&A (2026-06-05). Versioning goes **straight to 1.0.0** (no public
beta), published to **CurseForge** first, **Modrinth later**. No deadline — quality-driven; every
section below gates the release unless marked *(post-1.0)*. Keep this updated as items land; tick a
box only after the gradle-MCP build + gametests are green AND the in-game check passed.

---

## 1. Feature completion — new content for 1.0

### Big multiblock launch pad
- [x] Design doc for the larger pad (footprint, modules: fuel, gantry, etc.) —
      `LAUNCH_PAD_DESIGN.md`, signed off (ring OR Heavy for T3; gantry required; 12× fuel; flat
      plate model now)
- [x] Implement bigger footprint (5×5+) with functional modules — Heavy Launch Complex =
      5×5 + Launch Gantry (boards the rocket on right-click); fuel module = Fuel Tank at 12×
      (480 mB/t via `Tuning`); pad formation report on empty-hand right-click
- [x] Migrate/keep the 3×3 tier as the entry-level pad (no breaking of existing worlds) — T1/T2
      unchanged; T3 deploys on ring OR Heavy complex (both gametested)
- [x] Flatter/proper pad block model (replaces the near-full cube) — 3px plate model generated
      (matches the existing collision exactly); confirm the look + rocket stance in runClient
- [x] Gametests for new pad formation + gating (`heavy_complex_detection`,
      `tier3_deploys_on_heavy_complex`, `fuel_pump_rate_by_footprint` — 20/20 green)

### Star Guide (progression block) — NEW
- [x] Design: a block that holds the guidebook and shows an **interactive visual progression/quest
      tree** ("Star Guide") — `STAR_GUIDE_DESIGN.md`, signed off (advancements = completion truth,
      lectern-style pedestal + book-in-hand opens the same live GUI, next-step hologram)
- [x] Guidebook item (placeable on the Star Guide block) — install/return/break-drop gametested
- [x] Interactive GUI: progression chapters nerosium → machines → rocket → planets → terraforming
      — 7 chapters / 22 steps, node arcs, guide text, completed-pulse; confirmed in-game
      (screenshot 2026-06-05); row-wrap connector reworked to an L-route after the look
- [x] Quest/step completion tracking — per design sign-off: advancements are the source of truth
      (synced by vanilla); the per-player attachment carries seen-state. Gametested
      (`star_guide_progress_and_seen`, `star_guide_advancements_resolve`)

### Advancements
- [x] Full advancement tree mirroring the Star Guide progression (datagen) — 22 nodes, every guide
      step maps to one; custom `terraformed_ground` criterion fired from the atmosphere tick
- [ ] Advancement-triggered toasts for key milestones (first launch, first planet, terraformed)
      (all nodes have showToast; confirm the three milestone toasts visually in runClient)

### More planets / destinations
- [x] Design + implement at least one destination beyond Greenxertz / Cindara / Station —
      **Glacira** ice moon (`NEW_DESTINATION_DESIGN.md`, signed off 2026-06-05): dimension +
      biome + glacite ore/gem/block chain + Frost Strider mob + Glacira compass + Star Guide
      steps/advancements (22→25 nodes). Build + ecjCheck + gametests 25/25 green (incl.
      `glacira_dimension_loads`, `tier_destinations_cumulative`); confirm in runClient: sky/
      palette, arrival, Frost Strider look, glacite textures
- [x] Tier/recipe gating for the new destination(s) — **Tier 4 rocket** (24k tank / 8k per
      launch), recipe gated on cindrite (previous planet unlocks the next), deploys + launches
      ONLY on the Heavy Launch Complex (no ring shortcut — finally gives the Heavy pad a
      purpose); gametested (`tier4_requires_heavy_complex` incl. gantry-break grounding);
      confirm T4 stance/scale (2.8x) on the Heavy pad in runClient

### Multiple player-built stations
- [x] Design: how players found/register additional stations as rocket destinations —
      `MULTI_STATION_DESIGN.md`, signed off 2026-06-06: Station Charter (anvil rename = station
      name) + FOUND trajectory node; Station Core anchors/unregisters; single station dimension
      with x = 4096·(i+1) slots (cap 64, never reused); all tiers + all players; NO ownership
      data stored (POPIA/GDPR-clean)
- [x] Implement station creation + destination selection in the rocket UI — `StationRegistry`
      SavedData (overworld) + Charter/Core content + founding flow in `completeLaunch` (platform +
      bound Core + `founded_station` advancement, Star Guide rocketry step, tree 27→28) + rocket
      UI station row (cycler + FOUND node, slot-id-stable selection, menu-open snapshot sync) +
      graceful missing-station fallback; fuel slot moved beside the gauge; also fixed the latent
      T4 tank cap (physical tank was sized to T3). Gametested (`station_registry_roundtrip`,
      `station_core_break_unregisters`, `rocket_station_selection` — 30/30 green); confirm the
      reworked rocket UI layout + an actual founding flight in runClient

### Deeper terraforming
- [x] Design pass: what "deeper" means (stages? biomes? water cycle?) —
      `DEEPER_TERRAFORM_DESIGN.md`, signed off 2026-06-10: staged maturation (Rooted → Hydrated →
      Living), glacite-fed water cycle via the Hydration Module (must touch), per-planet mature
      biomes with real weather, three breedable livestock species, Terraform Monitor readout
- [x] Implement chosen scope; keep existing Terraformer saves working — trailing stage frontiers
      (additive NBT/SavedData/attachment only; legacy land = stage 1, locked by the
      `terraform_legacy_save_compat` gametest), water-table fill + Hydration Module,
      3 mature biomes + runtime trees + precipitation, Meadow Loper / Ember Strutter / Woolly
      Drift (first `Animal`s: breeding, drops, herd seeding, biome spawns), Terraform Monitor
      (comparator = local stage), Star Guide terraforming chapter 2→5 steps + 3 advancements
      (tree 28→31). Build + ecjCheck green, gametests **36/36** (new:
      `hydration_module_feeds_terraformer`, `terraform_water_table_fill`,
      `terraform_stage_progression`, `terraform_legacy_save_compat`,
      `terraform_creature_breeding`, `terraform_monitor_readout`). Verify in runClient: water
      fill look, mature palettes + rain/snow, livestock models/animations, the two new GUIs,
      milestone toasts

### Space-suit hazard variants
- [x] Heat-resistant suit variant (Cindara/volcanic) — **Thermal Suit** (`SUIT_HAZARD_DESIGN.md`,
      signed off 2026-06-06): T2 piece + 4 cindrite; Cindara heat = ×4 O₂ drain unprotected (no
      new damage path — zero-O₂ suffocation stays the killer), ember-puff feedback; gametested
      (`hazard_drain_multiplier`); confirm worn art + particles in runClient
- [x] Any other hazard variant per design — **Cryo Suit** (Glacira/cold): T2 piece + 4 glacite
      (the glacite hook from `NEW_DESTINATION_DESIGN.md` §3), frost-vignette feedback (capped
      `ticksFrozen`, never freeze damage). **Radiation: deferred past 1.0 by design** — no
      radiation source exists yet (revisit with an asteroid-field destination)
- [x] HUD badge + `suitTier` handling extended for variants — orthogonal `HazardShield`
      (all-4-matching rule; variant pieces stay Tier-2-class for capacity, mixed-set rules
      intact — gametested `hazard_shield_detection`); badge shows SUIT HEAT/COLD + red
      HEAT!/COLD! warning when exposed unprotected; Star Guide vacuum chapter +2 steps
      (tree 25→27). 27/27 gametests green; confirm badge layout in runClient

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

- [x] Palette pass in `tools/gen_textures.py` — every family ramp retuned (`ART_OVERHAUL_DESIGN.md`
      §2, signed off 2026-06-10) + full deliberate `--force` regen; creature sheets decoupled from
      `--force` (own `--creatures` scope); confirm the brighter read in runClient
- [x] Custom (non-trivial-cube) block models — ALL machines shaped + FACING (element-built datagen
      templates, collision untouched = oxygen sealing intact), tank frames with visible content
      cores, gantry tower, Star Guide pedestal, monitor wedge; gametested
      (`machine_facing_placement`); confirm shapes in runClient
- [x] Custom item models / improved icons — gem facets + crown sparkle, ingot polish, fresh suit
      icons, new drop-item art
- [x] Worn suit armour art for both tiers + hazard variants — T1 painted onto the real humanoid
      layers (visor/tank/console/seals), T2/Thermal/Cryo re-derive from it; confirm worn look in
      runClient
- [x] Per-creature Blockbench sources; legacy shared `GreenxertzCreatureModel` + mirrors RETIRED —
      model_sync registry now 12 entries (8 creatures + 4 rocket tiers), `render_models.py`'s
      hand mirrors replaced by parser-driven `render_entity_previews.py`
- [x] Nicer per-creature textures — anatomy-aware painters (per-part box-UV shading + per-creature
      patterns, parsed from the Java cube lists); confirm skins in runClient
- [x] Rocket entity model — per-tier GEOMETRY (T1 classic+bell / T2 boosters / T3 ring skirt /
      T4 heavy quad), per-tier hull textures with panel lines; stance numbers untouched — confirm
      pad stance + rider seat in runClient
- [x] Fluid visuals — rocket_fuel renders via `RegisterFluidModelsEvent` + animated still/flow
      strips (+ particle texture, found missing by `check_assets.py`); confirm placed fluid in
      runClient (26.1 note: `IClientFluidTypeExtensions` no longer carries textures)
- [x] Gas visuals — pipe gas streams now O₂ cyan; Gas Tank shows its cyan content core through the
      new frame model
- [x] Animated/segmented machine gauges + fluid render in GUIs — `segGauge` (ticked cells, pulsing
      edge) on every energy bar, `fluidGauge` (two-tone wave + meniscus) on fuel/O₂/hydration;
      exact values stay on inline labels (deliberate deviation: no hover tooltips — values are
      always visible instead); all standard machine GUI panels unified on the shared generator
- [x] Every model resolves — automated by `tools/check_assets.py` (model↔texture↔blockstate
      cross-check, green); the in-game half stays a runClient check

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

Full desk audit in `BALANCE_COMPAT_AUDIT.md` (signed off 2026-06-11); slice 1 applied + verified
(runData/check_assets/build/ecjCheck/39 gametests all green).

- [x] Recipe cost audit across the whole progression (pacing nerosium → T3 rocket → terraform) —
      `BALANCE_COMPAT_AUDIT.md` §0–§1. **Fixed the circular-gate BLOCKER** (nerosteel + xertz were
      Greenxertz-only yet gated everything before Greenxertz): rare/deep overworld nerosteel added,
      xertz dropped from the fuel canister. Final pacing *feel* confirms in the §6 survival run.
- [ ] Oxygen drain/tank/refill rate tuning from playthrough feel — *play-feel, deferred to §6.*
      Numbers audited (`§4`): T1-suit 2.5 min flagged as borderline; lever is capacity, not drain.
- [x] Energy economy audit: generator output vs machine consumption at each game stage —
      `BALANCE_COMPAT_AUDIT.md` §2. Sound; late-game Terraformer (≈288 FE/t) is intended energy/
      logistics tension. New Fuel Refinery ties fuel to the grid (≈2 FE/mB).
- [ ] Fuel economy: costs per launch per tier feel right — per-launch mB + 1:3 ratio kept (good);
      the *delivery* was the problem and is fixed (Fuel Tank canister auto-feed + **Fuel Refinery** +
      canister decoupled from xertz). Final "feel" confirms in §6.
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

Audit in `BALANCE_COMPAT_AUDIT.md` §5; slice 1 applied + verified.

- [x] Common ore/material tags (`c:` conventions) on all ores/ingots/raw materials — completeness
      pass: added the missing `c:ores/cindrite`, `c:ores/glacite`, `c:storage_blocks/{cindrite,glacite}`
      subtags + published xertz quartz under `c:gems/quartz` (output-side).
- [x] Conditional recipes / tag-based inputs where another mod's materials could substitute —
      aggressive tag-input pass: vanilla commodities (iron/gold/redstone/glass/cobblestone/chest/
      sticks) routed through broad `c:` tags; our own materials through our narrow `c:` subtags so the
      future renewable source slots in for free **and the planet-ore gates still hold** (a narrow
      subtag holds only our item — no other mod's gem can satisfy a Cindara/Glacira/Greenxertz recipe).
- [x] Capabilities exposed everywhere sensible (already the convention — audit for gaps) — §5.3
      sweep: added the **Fuel Tank** canister Item cap + the **Fuel Refinery**'s Energy/Item/Fluid
      caps. Terraformer/Hydration *fluid* caps were deliberately **not** added — a water-fluid intake
      would bypass the Glacira/glacite gate (documented in the audit; revisit only if that gate is
      consciously weakened).
- [ ] JEI/EMI integration *(when they reach 26.1 — otherwise first post-1.0 patch)*
- [x] No hard dependencies on any other mod (verify mods.toml) — only `neoforge` + `minecraft`
      required, `ordering="NONE"`; verified clean.

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
