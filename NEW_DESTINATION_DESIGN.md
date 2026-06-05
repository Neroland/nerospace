# New Destination Design — "Glacira" (RELEASE_CHECKLIST §1: More planets / destinations)

Status: **SIGNED OFF (2026-06-05)** — Glacira ice moon; Option A (Tier 4 rocket, Heavy-pad-only
deploy); **Frost Strider mob included in slice 1** (Dario's call — Glacira ships with its mob from
day one, like Cindara).

**Slice 1 IMPLEMENTED (2026-06-05)** — verified via the gradle MCP: `runData` + `build` green,
`ecjCheck` 0 errors, gametest suite **25/25 green** (new: `tier4_requires_heavy_complex`,
`tier_destinations_cumulative`, `glacira_dimension_loads`). Open for runClient: sky/palette,
arrival platform behaviour, T4 stance on the Heavy pad, Frost Strider look. Harness note: the
gametest server composes only vanilla level stems, so the dimension test asserts strict parity
with Cindara rather than absolute presence.

## 1. Concept

**Glacira** — a frozen ice moon, the outermost and final destination. It completes the arc:

| Destination | Theme | Palette family | Tier |
|---|---|---|---|
| Orbital Station | void / built | steel | T1 |
| Greenxertz | lush alien | green / steel | T2 |
| Cindara | volcanic heat | ember red / orange | T3 |
| **Glacira** | **frozen cold** | **ice cyan / blue / white** | **T4 (proposed — see §5)** |

Why an ice world over the alternatives considered:

- **Ice world (chosen):** thematic mirror of Cindara (heat ↔ cold) — the pair motivates both planned
  suit hazard variants (checklist §1 "Space-suit hazard variants"). Its palette (cyan/white) is the
  one family colour not yet taken. Water-ice is the natural feedstock for the planned "deeper
  terraforming" water cycle, so its resource has a future beyond slice 1. Generation is a straight
  reuse of the Cindara pattern (lowest slice-1 risk).
- **Asteroid field (rejected for now):** floating-island generation would need `NoiseGeneratorSettings.END`
  or custom noise — a new, untested generation path; arrival logic (heightmap landing) also breaks
  over void gaps. Better as a post-1.0 destination.
- **Crystal cavern world (rejected):** an underground-focused world fights the heightmap arrival +
  surface-platform pattern, and crystal aesthetics overlap Xertz Quartz / the Geode Skitterer.

Name note: *Glacira* deliberately echoes *Cindara* (cinder→Cindara, glacier→Glacira). Open to
alternatives at sign-off.

## 2. Dimension generation (reuse the Cindara pattern)

Exactly the `ModDimensions.bootstrapStem` / `ModBiomes` pattern Cindara uses:

- `GLACIRA_STEM` / `GLACIRA_LEVEL` keys (path `glacira`) in `ModDimensions`.
- `LevelStem` = `ModDimensionTypes.SPACE` dimension type (END starfield, no sun — registry
  reference, same join-packet rule as Cindara) + `NoiseBasedChunkGenerator` with
  `FixedBiomeSource(ModBiomes.GLACIRA)` over `NoiseGeneratorSettings.OVERWORLD`.
- `ModBiomes.glacira(...)`: `hasPrecipitation(false)` (airless — no snowfall), `temperature(-0.5F)`,
  cave carvers like Cindara, `UNDERGROUND_ORES` feature for the new ore. Special effects carry the
  look: pale-cyan sky/fog, ice-blue water, frosted white-cyan grass/foliage colour overrides
  (the same trick that makes Cindara read as ash).
- Honest limitation (same as Cindara): surface blocks come from the overworld surface rules, so the
  "ice" reads through biome tinting, not real snow/ice blocks. True packed-ice/snow surfaces need
  custom `NoiseGeneratorSettings` surface rules — **deferred to the art-overhaul pass (§2 of the
  checklist), not slice 1.** Flagging in case you want it in scope.

## 3. Signature resource — Glacite

A gem-style chain mirroring cindrite (ore → gem → storage block):

- **Glacite Ore** (`glacite_ore`) — generated like `CINDRITE_ORE` (ConfiguredFeature `Feature.ORE`,
  vein size ~8, `CountPlacement.of(7)`, triangle −48..48, `BiomeFilter.biome()`), Glacira-only.
- **Glacite** (`glacite`) — gem item, dropped by the ore (fortune-aware loot like cindrite).
- **Block of Glacite** (`glacite_block`) — storage block, 3×3 both ways.
- Tags: `c:ores/glacite`, `c:gems/glacite`, `c:storage_blocks/glacite` + mineable/needs-tool
  (iron-tier pick, matching cindrite) — fits checklist §8 (common tags).

**What glacite gates (forward hooks, not slice 1):**

1. **Cold-hazard suit variant** — the mirror of the planned heat-resistant Cindara suit
   (checklist §1 "Space-suit hazard variants"): T2 suit piece + glacite = insulated variant. Slots
   straight into the existing `SuitTier`/`pieceTier` pattern.
2. **Deeper terraforming water cycle** — glacite (water-ice crystal) as the Terraformer's water-stage
   input when that design lands.
3. Immediately in slice 1 it gates nothing onward (deliberate — cindrite originally had the same
   shape, and the T4 recipe must not be circular; see §5).

Texture/palette: new `GLACITE` family in `tools/gen_textures.py` — e.g. `G_DEEP (10,30,55)`,
`G_BLUE (60,130,200)`, `G_CYAN (120,210,240)`, `G_FROST (200,240,255)`, `G_WHITE` glow highlights —
mirroring `EMBER_RAMP`'s structure; `gen_glacite_ore/_block/gen_glacite()` + entries in
`gen_bbmodels.py` BLOCKS/ITEMS. Distinct from every existing family (closest neighbour is the cyan
O₂ GUI theme, which is UI-only, not a material family).

## 4. Mob — Frost Strider (in slice 1, per sign-off)

**Frost Strider** — hostile, the cold mirror of the Cinder Stalker: same `Monster` + goals +
`ModEntities` builder pattern (freeze immunity — `canFreeze()` override — as the analogue of
Cindara's `.fireImmune()`), spawn via `MobSpawnSettings.SpawnerData` in the Glacira biome, geometry
via the `GreenxertzMobModel` layered-cube pattern (tall ice-shard strider silhouette, distinct from
the four existing creatures), glow layer + texture via the existing scripts, vanilla-alias sounds +
subtitles, spawn egg via `NerospaceSpawnEggItem`.

## 5. Tier gating — the trade-off to decide

**Option A (recommended): a new Tier 4 rocket.**

- `RocketTier.TIER_4(4, 24_000, 8_000, [STATION, GREENXERTZ, CINDARA, GLACIRA])` — doubling
  continues the existing capacity/cost progression; `fuelCostMultiplier`/`Tuning` clamps apply as-is.
- New `rocket_tier_4` item + recipe **gated on cindrite** (mined on Cindara → same "previous planet
  unlocks the next" depth as T2 suit / T3's original cindrite idea, and not circular).
- **Deploy gating: T4 requires the Heavy Launch Complex specifically** (5×5 + gantry), not the
  Station Wall ring. This gives the Heavy pad a real purpose — today T3 accepts ring OR Heavy, so
  the Heavy complex is strictly optional. `RocketItem`/`RocketEntity.canLaunch` checks become
  tier-aware (T3 = ring OR heavy, T4 = heavy only).
- Texture/accent per the palette rule: steel + **ice-cyan accent + glow** (T1 red, T2 purple,
  T3 gold/green).
- Cost: new item/recipe/textures/bbmodel/lang, tier-aware pad checks, +2–3 gametests. The rocket
  entity/menu/UI are already tier-generic, so no new entity work.

**Option B: extend TIER_3's destination list.**

- One-line change + UI picks it up automatically (destination cycling is list-driven).
- Cost: trivial. Risk: flattens progression — a T3 owner gets the new planet for free, no new goal
  between Cindara and terraforming, the Heavy pad stays purposeless, and checklist §1's
  "Tier/recipe gating for the new destination(s)" box has nothing meaningful to tick.

Recommendation: **A**. It is the substance of the checklist's gating line and finally pays off the
Heavy Launch Complex.

## 6. Travel, arrival, compass

- `Destinations.name()` gains Glacira; arrival uses the existing planet path in
  `RocketEntity.completeLaunch()` (heightmap landing at the player's XZ) — **full parity with
  Cindara, no new arrival code.**
- Creative-only **Glacira Compass** (`glacira_compass`), matching the existing three.
- Oxygen: add `GLACIRA_LEVEL` to the `PLANETS` set in `GreenxertzAtmosphere` — airless like the
  rest. (A *cold drain modifier* — faster suit O₂ drain on Glacira — is a nice hook but belongs to
  the hazard-variant work, not slice 1.)

## 7. Star Guide + advancements

- Extend the existing **`new_worlds` chapter** (no new chapter) with two steps, mirroring
  cindara/cindrite exactly:
  - `rocket_tier_4` (icon: T4 rocket item, advancement `nerospace:guide/rocket_tier_4` — craft
    criterion, parent `cindrite`) *(Option A only)*
  - `glacira` (icon: glacite, advancement `nerospace:glacira` —
    `ChangeDimensionTrigger.changedDimensionTo(GLACIRA_LEVEL)`, `AdvancementType.GOAL`, parent
    `rocket_tier_4`)
  - `glacite` (icon: glacite block, advancement `nerospace:guide/glacite` — inventory criterion,
    parent `glacira`)
- Tree grows 22 → 25 nodes (24 under Option B — no T4 node); lang via `ModLanguageProvider`
  (step titles + guide text keys follow the `gui.nerospace.star_guide.step.*` convention).
- Placement keeps the terraforming chapter last — Glacira is the "edge of the map" before the
  terraform finale.

## 8. Slice 1 scope — parity with Cindara 7a, mob included

1. Dimension + biome + dimension wiring (`ModDimensions`, `ModBiomes`, datagen registry set).
2. Glacite ore/gem/block + worldgen + loot + recipes (block↔gem) + tags + lang.
3. `RocketTier` TIER_4 + `rocket_tier_4` item + cindrite recipe + Heavy-only deploy gating
   (per Option A) + `Destinations` + Glacira compass.
4. Textures via `gen_textures.py` (new GLACITE palette + T4 rocket) + `gen_bbmodels.py` entries.
5. Star Guide steps + advancements + lang.
6. Frost Strider mob: entity + spawns + model + renderer + glow + sounds aliases + spawn egg +
   textures (§4).
7. Gametests: T4 destination list / per-tier destination gating, `tier4_requires_heavy_complex`,
   glacira + guide advancement resolution (extend `star_guide_advancements_resolve`), travel-parity
   where the existing patterns allow.
8. Verify: `runData` → `build` → `ecjCheck` → full gametest suite via the gradle MCP. In-game look
   checks (sky/palette/arrival/T4 stance on the Heavy pad, Frost Strider look) stay open for
   runClient.

Deferred (explicitly out of slice 1): cold hazard + insulated suit (checklist hazard-variants
item), true ice surface rules (art overhaul), terraforming water-cycle use of glacite
(deeper-terraforming design).

## 9. Sign-off record (2026-06-05)

1. Concept: **Glacira ice moon** — approved.
2. Tier gating: **Option A** (Tier 4 rocket, Heavy Launch Complex required) — approved.
3. Mob: **Frost Strider in slice 1** — approved.
