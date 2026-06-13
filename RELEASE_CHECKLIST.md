# Nerospace — 1.0.0 Release Checklist

Shaped by the release-planning Q&A (2026-06-05). Versioning goes **straight to 1.0.0** (no public
beta), published to **CurseForge** first, **Modrinth later**. No deadline — quality-driven; every
section below gates the release unless marked *(post-1.0)*. Keep this updated as items land; tick a
box only after the gradle-MCP build + gametests are green AND the in-game check passed.

---

## 1. Feature completion — new content for 1.0

### Big multiblock launch pad
- [x] Design for the larger pad (footprint, modules: fuel, gantry, etc.) — signed off (ring OR
      Heavy for T3; gantry required; 12× fuel; flat plate model now)
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
      tree** ("Star Guide") — signed off (advancements = completion truth,
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
      **Glacira** ice moon (signed off 2026-06-05): dimension +
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
      signed off 2026-06-06: Station Charter (anvil rename = station
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
      signed off 2026-06-10: staged maturation (Rooted → Hydrated →
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
- [x] Heat-resistant suit variant (Cindara/volcanic) — **Thermal Suit** (signed off 2026-06-06):
      T2 piece + 4 cindrite; Cindara heat = ×4 O₂ drain unprotected (no
      new damage path — zero-O₂ suffocation stays the killer), ember-puff feedback; gametested
      (`hazard_drain_multiplier`); confirm worn art + particles in runClient
- [x] Any other hazard variant per design — **Cryo Suit** (Glacira/cold): T2 piece + 4 glacite
      (the glacite hook from the Glacira destination design), frost-vignette feedback (capped
      `ticksFrozen`, never freeze damage). **Radiation: deferred past 1.0 by design** — no
      radiation source exists yet (revisit with an asteroid-field destination)
- [x] HUD badge + `suitTier` handling extended for variants — orthogonal `HazardShield`
      (all-4-matching rule; variant pieces stay Tier-2-class for capacity, mixed-set rules
      intact — gametested `hazard_shield_detection`); badge shows SUIT HEAT/COLD + red
      HEAT!/COLD! warning when exposed unprotected; Star Guide vacuum chapter +2 steps
      (tree 25→27). 27/27 gametests green; confirm badge layout in runClient

### Remaining small features
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

- [x] Palette pass in `tools/gen_textures.py` — every family ramp retuned (art overhaul §2, signed
      off 2026-06-10) + full deliberate `--force` regen; creature sheets decoupled from
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
- [x] Migrate existing config keys (breaking change documented in `wiki/Configuration.md`; dev
      config deleted/regenerates)
- [x] Document every key in the wiki (`wiki/Configuration.md`)
- [ ] Sanity-check extreme multiplier values don't break machines/atmosphere (code clamps scaled
      values to ≥1 and launch cost to tank size; confirm 0.1x/10x in runClient)

## 5. Balance

Full desk balance & compatibility audit signed off 2026-06-11; slice 1 applied + verified
(runData/check_assets/build/ecjCheck/39 gametests all green).

- [x] Recipe cost audit across the whole progression (pacing nerosium → T3 rocket → terraform) —
      balance audit §0–§1. **Fixed the circular-gate BLOCKER** (nerosteel + xertz were
      Greenxertz-only yet gated everything before Greenxertz): rare/deep overworld nerosteel added,
      xertz dropped from the fuel canister. Final pacing *feel* confirms in the §6 survival run.
- [ ] Oxygen drain/tank/refill rate tuning from playthrough feel — *play-feel, deferred to §6.*
      Numbers audited (`§4`): T1-suit 2.5 min flagged as borderline; lever is capacity, not drain.
- [x] Energy economy audit: generator output vs machine consumption at each game stage —
      balance audit §2. Sound; late-game Terraformer (≈288 FE/t) is intended energy/
      logistics tension. New Fuel Refinery ties fuel to the grid (≈2 FE/mB).
- [ ] Fuel economy: costs per launch per tier feel right — per-launch mB + 1:3 ratio kept (good);
      the *delivery* was the problem and is fixed (Fuel Tank canister auto-feed + **Fuel Refinery** +
      canister decoupled from xertz). Final "feel" confirms in §6.
- [ ] Post-release: keep tuning from player feedback (multipliers make this cheap)

## 6. Verification & testing

### Targeted runClient pass (every pending in-game item)
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

Compatibility audit §5; slice 1 applied + verified.

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
- [x] JEI integration (JEI 29.x reached 26.1, beta) — optional `compat/jei` plugin: grinding,
      fuel-refining and combustion-fuel categories + crafting-station catalysts; standard
      recipes/tags need no code. API is `compileOnly` + `localRuntime`, JEI declared `optional`
      in mods.toml — no hard dependency. **EMI** still awaits a 26.1 port (post-1.0).
- [x] No hard dependencies on any other mod (verify mods.toml) — only `neoforge` + `minecraft`
      required, `ordering="NONE"`; verified clean.

## 9. Distribution & legal

- [x] License file matching the chosen posture (Q&A 2026-06-11: source-available, private forks,
      modpacks allowed/encouraged on any platform, commercial use of legit copies OK, derivatives
      ask-first, attribution = CF + GitHub) — `LICENSE` drafted as custom ARR-with-permissions;
      **not lawyer-reviewed** (flagged clauses in the session notes; have it checked if in doubt)
- [x] License consistent in `LICENSE`, `neoforge.mods.toml` (via `mod_license` in
      gradle.properties) — *CurseForge/GitHub metadata fields are a human step: see §12.2–12.3*
- [x] Explicit **modpack permission statement** — LICENSE §1(b) + README License section +
      CurseForge description ("allowed and encouraged, any platform, official files, credit +
      links")
- [ ] `PRIVACY.md` current; Sentry opt-out documented and POPIA/GDPR scrubbing re-verified on the
      release build — *doc updated (sentry_test disclosure + policy-change note) and every claim
      code-audited against `NerospaceTelemetry` 2026-06-11; REMAINING: the end-to-end release-jar
      Sentry test (see §12.4)*
- [ ] Version set to 1.0.0; `publish.yml` release flow tested end-to-end (CurseForge upload) —
      *version bumped, workflow fixed (release-type from version suffix, CHANGELOG release notes,
      secret docs) and validated; REMAINING: add missing secrets `CURSE_FORGE_API_TOKEN` +
      `SENTRY_ORG`, then the publish itself IS the end-to-end test (see §12.1, §12.5 —
      ⚠️ pushing the gradle.properties bump with secrets in place publishes 1.0.0)*
- [ ] Modrinth listing + publish.yml target *(pulled forward from post-1.0)* — project created
      (slug `nerospace`, 2026-06-12); publish.yml uploads to Modrinth in the same run as
      CurseForge, and `.github/workflows/modrinth-description.yml` auto-syncs the page body from
      `art/modrinth_description.md`. REMAINING (human): re-add the `MODRINTH_API_TOKEN` secret
      (it did NOT save — verify with `gh secret list`) + the page fields/gallery
      (see §12.1, §12.2b); goes live when the first version clears Modrinth review

## 10. Marketing & community

- [ ] Screenshot/gallery set: rockets, launches, planets, machines, creatures, terraforming —
      *human-only; 10-shot list + specs in §12.6*
- [ ] Trailer/showcase video (launch + planets + terraforming) — *human-only; 9-beat script +
      tooling (OBS + DaVinci Resolve) in §12.7*
- [x] Final pass on `art/curseforge_description.md`; link the wiki — rewritten for 1.0 (features +
      Star Guide + `/nerospace gallery` per Q&A; wiki/Discord/issues links, modpack permission,
      telemetry blurb)
- [x] Wiki completeness pass — inventory audit vs registries 2026-06-11: 6 new pages (Glacite Ore,
      Block of Glacite, Launch Gantry, Station Charter, Station Core, Star Guide) + 11 pages
      updated (Home, Sidebar, Items, Oxygen-Suit, Rocket-Launch-Pad, Fuel-Tank, Creatures,
      Station-Wall, Roadmap, Future-Features) — *pushing `wiki/` to the GitHub wiki repo is a
      human step: see §12.3*
- [x] GitHub issue templates (bug report + feature request) — audited + refreshed for 1.0 (WIP
      framing dropped, current version placeholders, Discord contact link, new feature areas)
- [ ] Discord server created and linked from CurseForge/GitHub/README — *server EXISTS
      (discord.gg/ArPXvYUzJG) and is linked from README/roadmap/description/wiki/issue templates;
      REMAINING: the CurseForge + GitHub About fields (see §12.2, §12.8)*
- [x] `CHANGELOG.md` (Keep-a-Changelog) starting at 1.0.0 — created; publish.yml now extracts the
      version's section as CurseForge/GitHub release notes (update the date if release day ≠
      2026-06-11)
- [x] README + roadmap refreshed to reflect 1.0 (drop "early WIP" framing) — both rewritten (the
      roadmap now lives in the wiki, `wiki/Roadmap.md`); in-game
      `welcome.wip` join message replaced with a toned-down `welcome.intro` (Star Guide pointer)
      per Q&A; runData + build green 2026-06-11

## 11. Pre-launch

- [ ] Private playtests: a few friends on a dedicated server with the release candidate jar
- [ ] Fix-up pass from playtest feedback
- [ ] Fresh-world + existing-world (datafix/migration) load check on the final jar
- [ ] Final jar built from a tagged commit; gametests + manual smoke test on THAT jar
- [ ] Publish 1.0.0 🚀

## 12. Human-only release steps (external accounts, uploads, media)

Everything here needs **Dario** — external accounts, uploads, media. The in-repo work for §9–§10 is
done; the steps below are what to do, and in what order, at release time. The §9–§10 boxes point
here.

### 12.1 Secrets (GitHub Actions)

`publish.yml` and the description-sync workflows read these repo secrets — add/verify them before
the go-live push:

- [ ] `CURSE_FORGE_API_TOKEN` — CurseForge upload.
- [ ] `SENTRY_ORG` (+ the existing Sentry auth token) — release creation / telemetry.
- [ ] `MODRINTH_API_TOKEN` — Modrinth upload + page sync. ⚠️ It did NOT save previously — confirm
      with `gh secret list`.

### 12.2 CurseForge project settings

Project: <https://www.curseforge.com/minecraft/mc-mods/nerospace>

- [ ] **Description:** paste the rendered content of `art/curseforge_description.md` (it already
      contains the telemetry blurb, modpack permission, wiki/Discord/issue links).
- [ ] **License:** choose **Custom License**, name it "All Rights Reserved (modpacks allowed — see
      LICENSE)" and point it at <https://github.com/Neroland/nerospace/blob/main/LICENSE>.
- [ ] **External links:** Issues → GitHub issues URL, Wiki →
      <https://github.com/Neroland/nerospace/wiki>, Source → the repo.
- [ ] **Discord:** add `https://discord.gg/ArPXvYUzJG` (project social/Discord field).
- [ ] **Settings → allowed in modpacks:** ensure "Allow project to be added to modpacks" is ON — it
      must match the LICENSE text.
- [ ] **Gallery:** upload the screenshot set (§12.6).

### 12.2b Modrinth project settings

Project: <https://modrinth.com/mod/nerospace> (created 2026-06-12; the first uploaded version goes
through Modrinth moderation review before the page goes public).

- [ ] **Whole project page: automated** — `.github/workflows/modrinth-description.yml` PATCHes
      everything in one run: the body (from `art/modrinth_description.md`), the icon
      (`art/logo/nerospace_logo_400.png`; the 1024px master is over Modrinth's 256 KiB cap), and the
      pinned metadata — title, summary, categories (technology/worldgen/adventure), client+server =
      required, issue/source/wiki/Discord links, and the **Custom license** (`LicenseRef-Custom` →
      the LICENSE on GitHub; the field AutoMod's "Missing License" check reads). Make sure the §12.1
      secrets exist, then trigger it once and **resubmit for review**.
- [ ] **Gallery:** same screenshot set as CurseForge (§12.6) — the only manual page item.
- [ ] File uploads themselves are automatic — `publish.yml` targets CurseForge **and** Modrinth in
      the same run.

### 12.3 GitHub repo metadata

- [ ] Repo **About**: description ("Space-progression mod for NeoForge — rockets, oxygen survival,
      player stations, terraforming"), website = the CurseForge page, topics (`minecraft`,
      `neoforge`, `minecraft-mod`, `space`, `terraforming`).
- [ ] **Publish the wiki:** the GitHub wiki is a separate git repo. From a scratch folder, clone
      `https://github.com/Neroland/nerospace.wiki.git`, copy the repo's `wiki/*.md` over it, commit,
      push. (`_Sidebar.md` becomes the sidebar automatically.) Re-do this whenever `wiki/` changes —
      the in-repo `wiki/` folder is the source of truth.
- [ ] **Discussions:** the issue templates link to GitHub Discussions — enable the feature
      (Settings → General → Features) if it isn't already.
- [ ] GitHub does not auto-detect custom licenses; the README License section + LICENSE file are
      already in place, nothing else needed.

### 12.4 Release-jar telemetry verification (§9 PRIVACY sign-off)

On the **final release candidate jar** (not a dev run):

1. Launch a normal client with the jar in `mods/`, create/join a world.
2. `/give @s nerospace:sentry_test`, place it. Chat should confirm the event was sent.
3. Check the Sentry dashboard (environment **production**, release `nerospace@1.0.0`) for the
   synthetic "Sentry test block" event — confirm **no IP, no username, no home-dir paths**.
4. Set `telemetryEnabled = false` in `config/nerospace-common.toml`, reload/restart, place the block
   again — chat must say telemetry is disabled and **nothing** must arrive in Sentry.
5. Re-enable telemetry, delete the block. Done — tick the §9 PRIVACY box's "release build" clause.

### 12.5 Go-live order (the push that publishes)

⚠️ `publish.yml` fires on any push to `main` that touches `gradle.properties`. The version is
already bumped to 1.0.0 in the working tree, so **the publish happens on whatever push lands that
change** — sequence accordingly:

1. Finish §6–§8 + §11 gates (playtests, performance, final jar checks).
2. Add the secrets from §12.1.
3. If release day isn't 2026-06-11, update the date in `CHANGELOG.md`'s `## [1.0.0]` heading.
4. Commit everything; push to `main`. The workflow: builds → creates the Sentry release → uploads to
   CurseForge **and Modrinth** (as **release**-type files, changelog section as release notes) →
   creates the GitHub release + `v1.0.0` tag.
5. Watch the run (`gh run watch`); CurseForge files sit in moderation briefly, and Modrinth also
   reviews the project itself on its first version.
6. After approval: check the file page renders, then run §12.4 against the *published* jar if you
   didn't already on the identical RC.
7. If a publish half-fails **after** the tag was created: delete the `v1.0.0` tag/release and re-run
   the workflow (documented at the top of `publish.yml`).

### 12.6 Screenshot / gallery shot list

CurseForge gallery: upload **1920×1080** (16:9) PNGs; the first image becomes the card thumbnail —
make it the money shot. Take them fullscreen 1080p+ with HUD hidden (F1) unless the HUD *is* the
subject. Suggested set (≈10):

1. **Hero:** Tier 4 rocket on the Heavy Launch Complex at dusk, gantry beside it, fuel tank line
   visible. (Thumbnail.)
2. Tier 1 rocket lifting off — particles mid-launch.
3. The four rockets side by side on pads (creative lineup; shows per-tier geometry).
4. Greenxertz vista with a Xertz Stalker + Quartz Crawler in frame.
5. Cindara: Cinder Stalker in a lava-lit basin, player in Thermal Suit with HUD badge visible (keep
   HUD on — show "SUIT HEAT").
6. Glacira: Frost Strider against the starfield, player in Cryo Suit.
7. Machine room: grinder + generators + batteries laced with Universal Pipes carrying visible
   items/streams; GUIs open in a second shot if needed.
8. Star Guide GUI open mid-progression (chapters + glowing completed nodes).
9. Terraforming before/after split: barren ground vs Living-stage land with rain, trees, livestock
   (Meadow Loper herd).
10. A founded station: platform + Station Core + rocket parked, nameplate/UI showing the custom
    station name.

Cheapest route: a creative world + `/nerospace gallery` for the lineup shots, a staged terraform
world for 9–10.

### 12.7 Trailer / showcase video outline

Target: **60–90 s**, 1080p60, hosted on **YouTube** (link it on the CurseForge page; CF supports a
featured video URL). Tooling: DaVinci Resolve for the edit; install **OBS Studio** (free) for
capture — game capture at 1080p60; record voice-over separately or use text cards only.

Script (each beat ≈ 6–10 s, cut on the beat of the music):

1. **Cold open** — black, "One ore starts it." → pickaxe breaks nerosium ore.
2. Grinder + pipes montage — dust pours, energy streams pulse, GUIs animate.
3. First launch — Tier 1 on the 3×3 pad, liftoff, cut to the Orbital Station.
4. Worlds montage — Greenxertz pan → Cindara embers + Stalker lunge → Glacira frost vignette
   ("SUIT COLD" flash). Card: "Four worlds. None of them want you alive."
5. Survival beat — sealed base, O₂ HUD draining, airlock refill.
6. Heavy Launch Complex — gantry boarding, Tier 4 launch. Card: "Go heavy."
7. Station founding — anvil rename "Haven-1", FOUND node click, new platform reveal.
8. Terraforming crescendo — timelapse of Rooted → Hydrated → Living, first rain, livestock herd.
   Card: "Make it rain. Literally."
9. Outro — Star Guide tree zooms out to the logo + "Nerospace 1.0 — on CurseForge now" +
   Discord/wiki URLs.

Capture tips: F1 for clean shots, no night-vision gamma, `/gamerule doDaylightCycle false` while
framing; record everything at 1.5× the length you need so Resolve has trim room.

### 12.8 Discord wiring

Server exists: `https://discord.gg/ArPXvYUzJG` (permanent invite, grants the Minecraft role). The
repo/description links are already wired in. Remaining:

- [ ] Add the invite to the CurseForge project (§12.2) and the GitHub About sidebar.
- [ ] Channels worth having before the announcement: `#announcements`, `#support`, `#bug-reports`
      (pinned link to the GitHub issue forms), `#showcase`, `#modpack-makers`.
- [ ] Pin the wiki + `PRIVACY.md` links in `#support`.
- [ ] Post the 1.0.0 announcement in `#announcements` when the CurseForge file clears moderation.
