# Space-Suit Hazard Variants — Design (RELEASE_CHECKLIST §1)

Status: **SIGNED OFF (2026-06-06)** — ×4 drain only (no new damage path); variants as sidegrades
of T2 (orthogonal `HazardShield`, all-4-matching rule); recipes T2 piece + 4 cindrite/glacite;
radiation deferred past 1.0; names "Thermal Suit" / "Cryo Suit".

**IMPLEMENTED (2026-06-06)** — verified via the gradle MCP: `runData` + `build` green, `ecjCheck`
0 errors, gametest suite **27/27 green** (new: `hazard_shield_detection`,
`hazard_drain_multiplier`). Open for runClient: HUD badge/warning layout, worn variant art,
ember particles on Cindara, frost vignette on Glacira.

## 1. Concept

Two environmental hazards and two matching suit variants, completing the planet identities:

| Dimension | Hazard | Protecting variant | Source material |
|---|---|---|---|
| Cindara | **HEAT** | **Thermal Suit** (`oxygen_suit_heat_*`) | cindrite |
| Glacira | **COLD** | **Cryo Suit** (`oxygen_suit_cold_*`) | glacite |
| Greenxertz / Station | none | — | — |

**Radiation (third variant): recommend NO for 1.0.** There is no radiation source in the game —
no dimension or block emits it, so a radiation suit would protect against nothing. It becomes
meaningful only with a post-1.0 destination (e.g. the deferred asteroid field). The checklist's
"(radiation?)" line should be resolved as *deferred, by design* rather than left open.

## 2. What a hazard actually does

**A hazard multiplies the oxygen drain (×4) while exposed on its dimension; it does NOT add a new
damage path.** Rationale:

- It reuses the proven oxygen loop in `GreenxertzAtmosphere.onPlayerTick` — the hazard reads as
  "your suit's climate control burns through air fighting the environment", and lethality still
  arrives through the existing zero-O₂ suffocation. No new damage sources, no new death messages,
  no interaction with armour durability.
- It is save-compatible in spirit: Cindara has been visitable with a plain suit since Phase 7 —
  direct heat damage would retroactively wall off existing bases. A ×4 drain turns long unprotected
  excursions into short ones without killing anyone standing at their airlock.
- It applies to whichever drain path is active: suit drain (1→4 per check) and bare-lungs drain
  alike. Breathable zones (oxygen field, terraformed ground, pad radius) override hazards entirely
  — inside your base nothing changes.

Thematic feedback, one per hazard, both cheap:

- **Cold (Glacira):** while unprotected, the player's vanilla `ticksFrozen` climbs (the powder-snow
  frost vignette + shake), capped just below the fully-frozen threshold so vanilla freeze damage
  never double-dips — the visual is the warning, the O₂ bar is the cost. Cryo Suit (or any
  breathable zone) lets it thaw normally.
- **Heat (Cindara):** sparse smoke/ember particles around an unprotected player (server
  `sendParticles`, every ~2 s, vanilla particles only — the 26.1 custom-particle API stays
  untouched). No vignette analogue exists for heat without setting the player on fire (real damage
  + armour burn), which we don't want.

Numbers: `Tuning.BASE_HAZARD_DRAIN_MULTIPLIER = 4` (internalised constant, NOT a new config key —
per the config-refactor philosophy it already scales with `oxygenDrainMultiplier` because it
multiplies the scaled drains; `atmosphereDamageEnabled=false` still switches the whole system off).

## 3. How variants slot into `SuitTier` (the trade-off)

**Chosen shape: variants are sidegrades OF Tier 2 — capacity and protection are separate axes.**

- `SuitTier` (NONE/TIER_1/TIER_2) stays exactly what it is: the *capacity + refill-speed* ladder.
  Variant pieces count as tier-2-class in `pieceTier` (they are crafted from T2 pieces), so any
  full mix of T2/heat/cold pieces keeps the 600 tank and 2× airlock refill. Mixed-set rules are
  unchanged: any T1 piece in the set still demotes capacity to TIER_1.
- A new, orthogonal `HazardShield` (NONE/HEAT/COLD) requires **all four pieces of the same
  variant** — the variant analogue of the existing mixed-set rule. A heat helmet on a cold suit =
  T2 capacity, no shield. Detection is a second `pieceVariant()` sweep next to `suitTier()`, same
  pattern, armour-slot-only (client-checkable for the HUD with no sync).

Alternatives considered and rejected:

- *Linear T3/T4 tiers:* conflates "more air" with "heat-proof", forces an arbitrary heat-before-
  cold ordering, and breaks the clean two-axis read of the HUD badge.
- *Parallel variant tracks from T1:* doubles the item count for an earlier entry point nobody
  needs — by the time you stand on Cindara/Glacira you can afford the T2 base.

## 4. Items + recipes

Eight new items (two four-piece sets), netherite-class like T2, same armour numbers:

- **Thermal Suit** piece = T2 piece + **4 cindrite** (shapeless or shaped `C / CTC / C`, matching
  the T1→T2 upgrade shape); repairs with `c:gems/cindrite`. Obtainable after your first short
  (unprotected, ×4-drain) Cindara excursions — uncomfortable but survivable, then comfortable.
  Not circular: the hazard slows you down, it doesn't gate entry.
- **Cryo Suit** piece = T2 piece + **4 glacite**; repairs with `c:gems/glacite`. Same logic on
  Glacira. (This is the glacite forward-hook promised in `NEW_DESTINATION_DESIGN.md` §3.)
- New `EquipmentAsset` keys + worn layers for both, ids `oxygen_suit_heat` / `oxygen_suit_cold`.

## 5. HUD badge

`OxygenHudLayer` badge extends from SUIT T1/T2 to show the *shield* when one is active:
`SUIT HEAT` (ember orange `0xFFF07830`) / `SUIT COLD` (ice cyan `0xFF78D2F0`), else the existing
`SUIT T2`/`SUIT T1`. One extra line: when standing on a hazard dimension *without* its shield, the
badge area also shows a small warning (`HEAT!`/`COLD!` in the LOW red) so the ×4 drain is never
mysterious. All client-side from armour slots + dimension — no new sync.

## 6. Textures (derived, `_emberize` pipeline)

T2 is already the ember recolour of T1, so the heat suit must go *further*, not repeat it:

- `gen_oxygen_suit_heat()` — recolour T2 art toward dark obsidian + bright ember seams (a second,
  stronger pass with a darker deep tone — reads "furnace plating" next to T2's warm trim).
- `gen_oxygen_suit_cold()` — `_frostize` (same algorithm, glacite palette: `I_BLUE`/`I_FROST`
  from the Glacira family) applied to the **T1** textures, so cold reads as the clean white-cyan
  opposite of the ember track.
- Both also derive the two worn `humanoid`/`humanoid_leggings` equipment layers, exactly like
  `gen_oxygen_suit_t2()`. Additive-only as always.

## 7. Star Guide + advancements

- `vacuum` chapter gains two steps after `oxygen_suit_t2` (chapter goes 4 → 6 steps, tree 25 → 27
  nodes): `thermal_suit` ("Forged for the Fire") and `cryo_suit` ("Dressed for the Deep Cold"),
  each a has-all-four-pieces advancement (`guide/thermal_suit`, `guide/cryo_suit`), both parented
  on `guide/oxygen_suit_t2`.
- Guide text explains the ×4 hazard drain explicitly (the system's only tutorialisation).

## 8. Slice scope (after sign-off)

1. `HazardShield` enum + `hazardShield(Player)` + per-dimension hazard map + drain multiplier in
   `GreenxertzAtmosphere`; freeze-vignette/ember-particle feedback.
2. 8 items + 2 armour materials + equipment assets + recipes + tags (+ item models, creative tab).
3. HUD badge + warning extension.
4. Textures via the generators (new heat/cold recolour passes).
5. Star Guide steps + advancements + lang (incl. subtitleless — no new sounds).
6. Gametests: shield detection (full heat → HEAT, mixed heat/cold → NONE shield but TIER_2
   capacity, T1 piece demotes capacity), hazard map per dimension, effective drain ×4 unprotected /
   ×1 shielded (public helper so tests can call it directly), existing suit tests stay green.
7. Verify via gradle MCP: `runData` + `build` + `ecjCheck` + full gametest suite; tick the §1
   hazard boxes that fully pass (HUD look + vignette/particles + worn art stay open for runClient).

## 9. Sign-off questions

1. Hazard model: ×4 O₂ drain + thematic feedback (freeze vignette / ember particles), **no new
   damage path** — OK, or do you want real heat/cold damage when unprotected?
2. Variant shape: sidegrades of T2 (capacity unchanged at 600), shield needs all 4 matching
   pieces — OK?
3. Recipes: T2 piece + 4 cindrite (Thermal) / + 4 glacite (Cryo) — OK?
4. Radiation: defer past 1.0 (no source exists) — OK?
5. Names: "Thermal Suit" / "Cryo Suit" (ids `oxygen_suit_heat_*` / `oxygen_suit_cold_*`) — OK?
