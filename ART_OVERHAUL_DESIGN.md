# Art & Model Overhaul Design (RELEASE_CHECKLIST §2)

Status: **SIGNED OFF (2026-06-10) — IMPLEMENTED (phases A1–A4).** Verified: `runData` + `build`
green, `ecjCheck` 0 errors, gametests **37/37** (new: `machine_facing_placement`),
`check_assets.py` green. Contact sheets in `art/preview/` (blocks/items/gui/entities). Sign-off
deviations honoured/noted: soft review gates (Q7); hover tooltips replaced by always-visible inline
value labels (§5). Open for runClient: every visual (shapes, skins, suits, rocket stances, fluid,
gauges).

Shaped by the design Q&A with Dario (2026-06-10): **full §2
in one slice, phased internally**; palette = **full `--force` regeneration** with retuned ramps (no
hand-edited art to protect); block models = **shaped geometry for everything** (visual-only);
rocket = **per-tier geometry**; creatures = **richer anatomy-aware generators** + per-creature
bbmodel sources (legacy shared geometry retired); suits = **full generator redo**; GUI/fluid/gas =
**all four items** (animated gauges, fluid render in GUIs, `rocket_fuel` world visuals, O₂ pipe/tank
tint); review = **contact sheets in chat** before any runClient pass.

Hard constraints (from the prompt + house rules): generators stay **additive-only** (`--force` only
on the deliberate regeneration passes below); **zero missing-texture placeholders**; **no
hand-authored model/blockstate JSON** (datagen owns it — shaped models use the element-built
`ExtendedModelTemplateBuilder` pattern the pad plate and pipe already prove out).

## 1. Concept — same families, turned up

The mod's art identity is right; its rendering of that identity is flat. Every block face is the
same grey-purple `METAL` noise, every machine is an unoriented cube, the creatures' textures are
noise fills with eyes, and the GUIs are solid-colour bars. The overhaul keeps every family identity
and **raises saturation, contrast and silhouette**:

- **Palette:** deeper shadows, hotter highlights, more chroma in the mids — per-family ramps
  retuned in one place (`gen_textures.py`) and everything regenerated, so the whole mod shifts
  coherently and stays reproducible from the script.
- **Silhouette:** machines stop being anonymous cubes — every machine gets a FACING and a shaped
  body (inset panels, domes, vents, windows); tanks show their contents; the Star Guide becomes a
  real pedestal.
- **Life:** creatures get anatomy-aware skins (per-part shading, patterns, rim light); the rocket
  tiers earn distinct silhouettes; the suits look like suits on the player.
- **Feedback surfaces:** gauges animate, fluids render as themselves, O₂ reads cyan everywhere.

**Non-negotiable invariant:** all shape work is *visual only*. Collision/VoxelShapes do not change —
full-cube collision is what makes machines airtight in `OxygenField.canHold`'s
full-collision-cube fallback, and pad/gantry shapes are gameplay. A dome with a cube hitbox is an
accepted (and vanilla-common) trade.

## 2. Palette pass (phase A1) — per-family ramp retune + full regen

One table, one source of truth, in `gen_textures.py`. Directions below; exact hexes are tuned via
the contact sheets (§8) before the regen is committed:

| Family | Today (reads as) | Retune direction (proposed anchors) |
|---|---|---|
| Nerosium (red/purple) | muddy plum | deepen dark `(30,8,46)`, hotter magenta mid `(208,36,176)`, red `(248,56,64)`, glow `(255,150,224)` |
| Machine steel (`METAL`) | grey-purple mud — the single biggest dullness source | cooler blued steel `(40,44,58)→(76,84,106)→(108,120,148)`, highlight `(160,176,210)`; machines additionally get per-family **accent trims** (terraform green, O₂ cyan, fuel amber…) |
| Greenxertz (green/steel) | olive, low-chroma | greens up: `G_GREEN (52,190,92)`, `G_GREEN_L (120,244,140)`, steel keeps olive but +1 contrast step |
| Xertz quartz | pale, washed | keep pale but add a mint mid `(150,225,170)` so facets read |
| Cindara ember | good, slightly muted | darker dark `(20,8,6)`, hotter orange `(255,132,36)`, glow `(255,238,160)` |
| Glacite frost | good | small chroma push: `I_BLUE (48,138,224)`, `I_CYAN (110,220,252)` |
| Rocket steel + accents | flat white/grey | warmer hull white, visible panel-line grey, accent set (T1 red / T2 nerosium purple / T3 gold-green / T4 ice cyan) saturated one step |
| Livestock (loper/strutter/drift) | new, already tuned | inherit only the global contrast curve |

Mechanics: ramps + painters updated → `gen_textures.py --force` regenerates **all block/item/GUI
textures** (creature/suit/rocket entity sheets regenerate under `--creatures` and the new
`--entities` flag in phase A3, not before). `gen_bbmodels.py` re-runs so the Blockbench sources
re-embed the new art. A new `tools/check_assets.py` cross-references every generated model JSON's
texture references against the PNGs on disk and exits non-zero on a miss — the automated half of
the "every model resolves" checklist box, wired into the verify step.

Item-icon painters get a quality pass in the same phase: ingots gain bar bevels + reflection line,
gems get faceting, dusts get pile shading, tools sharpen their silhouettes (`FLAT_HANDHELD_ITEM`
stays).

## 3. Block models (phase A2) — shaped everything, element-built, FACING

All via `ExtendedModelTemplateBuilder` element models in `ModModelProvider` (the pad-plate
pattern). Machines gain a horizontal **FACING** blockstate so fronts face the player on placement
(placement override in each block class; old placed machines load facing north — harmless, noted).
Per-face textures (`front/side/top`) replace `cube_all` on every machine.

| Block | Shape plan |
|---|---|
| Nerosium Grinder | hopper-mouth top inset, toothed front intake, side vents |
| Combustion Generator | firebox grill front (ember glow), chimney stub on top |
| Passive Generator | bevelled collector panel top, slim base |
| Oxygen Generator | domed top (the electrolysis bell), cyan side vents |
| Terraformer | front lens with the green core, soil tray skirt at the base |
| Hydration Module | inset frost-blue window showing melt level, finned sides |
| Terraform Monitor | angled screen face (element wedge), stand foot |
| Battery | terminal caps top, charge-bar strip on the sides |
| Item Store | drawer front with handle inset |
| Fluid / Gas / Fuel Tanks (+ creative) | steel frame + **window faces** (cutout render type) showing a content core; creative variants get a gold frame trim |
| Launch Gantry | open tower silhouette: corner posts + cross-brace + boarding arm |
| Star Guide | pedestal: base slab + column + slanted lectern top |
| Universal Pipe | keeps its multipart geometry; glass + arm textures retuned |
| Rocket Launch Pad | keeps the signed-off 3px plate; texture gets hazard-stripe edge + pad ring |
| Ores / storage blocks / Station Floor & Wall / Station Core | stay full cubes — their identity is the texture (storage blocks get strong family framing; station blocks get panel lines + hazard trims) |

Render types: window/cutout faces set via the template's `renderType` — still datagen, no hand
JSON. `check_assets.py` covers the new multi-texture references.

## 4. Entities (phase A3)

### 4.1 Creatures — retire the legacy shared geometry, source everything

- `model_sync.py` REGISTRY grows from 2 entries to 10: every per-creature Java model
  (XertzStalker, QuartzCrawler, Greenling, CinderStalker, FrostStrider, MeadowLoper, EmberStrutter,
  WoollyDrift) plus the rocket tiers (§4.2). Run `--to-bbmodel` to generate each creature's OWN
  `.bbmodel` source from its Java geometry.
- **Delete** `GreenxertzCreatureModel`'s shared LayerDefinition registration + the legacy
  shared-geometry `.bbmodel` mirrors (the class's animation base — `GreenxertzMobModel` — is
  untouched; it is the house anim system). Repo file deletions, flagged in the PR-style summary.
- Known limit honoured: model_sync is one-cube-per-bone, no rotation — Frost Strider's raked
  shards and any rotated parts stay Java-authoritative; their bbmodels carry the unrotated
  approximation (documented in the file header).
- **Anatomy-aware texture painters:** `_gen_creature` is rebuilt to paint per-part UV regions
  (each painter receives the creature's cube list — the same data model_sync parses from the Java —
  so body/head/limb/detail regions are shaded to the real layout): directional top-light shading,
  per-creature patterns (stalker facet veins, crawler plate seams, loper hide patches, strutter
  feather rows, drift wool curls), rim-light edge, redone eyes, recalibrated glow layers.
- Spawn eggs regenerate from the new palettes.

### 4.2 Rocket — per-tier geometry

`RocketModel` becomes a base builder + four tier LayerDefinitions (each model_sync-registered with
its own `rocket_tN.bbmodel`):

- **T1** — the classic: body, nose cone, 4 fins (current shape, better proportions + engine bell).
- **T2** — adds two side boosters (the LAUNCH/NEW_DESTINATION docs already describe T2 with
  boosters).
- **T3** — ring skirt + stretched nose, gold-green trim panels.
- **T4** — heavy: widened core read + four boosters, ice-cyan glow bands.

`RocketRenderer` bakes four layers and picks by tier (scale stays `visualScale()` — stance numbers
unchanged so pad alignment is not re-tuned). Tier textures repainted with panel lines, window band
(cutout preserved), accent livery + glow stripes.

### 4.3 Suits — worn layers + icons

The four worn sets (T1, T2, Thermal, Cryo) get real suit reads on `humanoid` /
`humanoid_leggings` layers: visor glass with glint, backpack tank, chest console, shoulder/knee
seals; identity accents (T1 steel-red, T2 gold, Thermal ember-orange piping, Cryo frost-cyan
piping). Item icons redrawn to match. All generator functions, regenerated under a deliberate flag.

## 5. Fluids, gas & GUI (phase A4)

- **`rocket_fuel` world visuals:** `IClientFluidTypeExtensions` via `RegisterClientExtensionsEvent`
  — generated still/flow texture strips (+`.mcmeta` animation metadata, written by the generator;
  mcmeta is texture metadata, not model JSON, so the no-hand-JSON rule is satisfied), amber tint +
  fog. 26.1 event/interface names javap-probed before coding.
- **O₂ gas visuals:** cyan tint on Universal Pipe gas streams (`UniversalPipeRenderer` already
  draws streams — it gains per-content tinting) and a faint animated shimmer band on the Gas Tank's
  window face.
- **Animated gauges:** `TexturedContainerScreen` gains a segmented gauge helper (tick marks,
  smooth fill animation, low-level pulse) + hover tooltips with exact values; every machine screen
  migrates off the flat `hGauge`.
- **Fluid render in GUIs:** tank/machine gauges fill with the actual fluid/gas sprite + tint
  (fuel amber, O₂ cyan, water blue) instead of solid colour.
- **GUI panels:** all machine screens move onto `gen_gui_machine_panel`-generated 256×256 sheets
  (per-machine slot sockets + accent), replacing the assorted committed panels — one consistent
  hull style across the mod (the Star Guide's bespoke panel stays).

## 6. How it reuses the existing tools

- `gen_textures.py` — single palette source; all painters live here; `--force` for the deliberate
  regen passes; new `--entities` flag scopes the creature/suit/rocket sheets like `--creatures`.
- `gen_bbmodels.py` — re-embeds regenerated textures into the block/item Blockbench sources.
- `model_sync.py` — registry expansion (10 entries) keeps every entity's bbmodel ↔ Java pair
  converging at build time, exactly as today.
- `render_models.py` → superseded by a parser-driven previewer: contact sheets are rendered from
  the SAME cube data `model_sync` parses out of the Java, killing the hand-kept-mirror drift the
  file header warns about. Block contact sheets render datagen element JSON in the same 3/4
  projection.
- `ModModelProvider` — all shapes are element templates + per-face mappings; blockstates stay
  datagen-owned (FACING variants via the standard horizontal-dispatch helpers).
- Verification — gradle MCP per CLAUDE.md: `runData` → `build` → `ecjCheck` → full gametest suite
  per phase, plus `check_assets.py` for texture resolution.

## 7. Trade-offs considered and rejected

1. **Hand-painting as the primary art pass** — rejected: not reproducible, blocks agent iteration;
   the generators stay the source of truth and Blockbench remains the overlay surface (sources are
   regenerated for exactly that purpose).
2. **32× textures** — rejected: doubles every painter, breaks visual parity with vanilla's 16×
   world; contrast/saturation, not resolution, is what's missing.
3. **Custom model loaders / OBJ** — rejected: element templates cover every shape above with zero
   runtime cost or loader risk, and keep the no-hand-JSON rule trivially true.
4. **Matching VoxelShapes to the new silhouettes** — rejected: collision is the oxygen-sealing and
   pad-gameplay contract (§1 invariant).
5. **An animation library (GeckoLib etc.)** — rejected: hard dependency (CLAUDE.md forbids), and
   `GreenxertzMobModel`'s walk/idle system already animates everything we ship.
6. **Connected/CTM station textures** — deferred: needs a loader feature; panel-line tiles get most
   of the look.
7. **Texture-only rocket tiers** — Dario chose geometry; cost contained by the shared base builder.
8. **Keeping `render_models.py`'s hand mirrors** — rejected: drift-prone duplicate of the Java;
   replaced by parsing (§6).

## 8. Review workflow — contact sheets gate each phase

After each phase's regen, I render `art/preview/` contact sheets (blocks atlas, items atlas,
per-creature turnarounds, rocket tier line-up, suit front/back, GUI panel strip) and show them in
chat. **Each phase's art is approved (or redirected) on the sheet before the next phase starts**;
runClient at the end is confirmation, not discovery. Sheets are deterministic script output —
re-rendering after a palette tweak is one command.

## 9. Slice 1 scope (numbered, phased — verify green between phases)

**Phase A1 — palette + regen + guardrails:**
1. Retune all family ramps + upgrade block/item painters; add machine accent-trim system.
2. `--force` regen of block/item/GUI textures; `gen_bbmodels.py` re-embed.
3. `tools/check_assets.py` (model-JSON ↔ PNG cross-check) + contact-sheet renderer (parser-driven).
4. Contact sheets → Dario gate. Verify: runData/build/ecjCheck/gametests + check_assets.

**Phase A2 — block shapes + facing:**
5. FACING + placement on all machines; element-built shaped models + per-face textures (§3 table);
   tank window cutouts; pad/gantry/Star Guide shapes.
6. Gametest: machine FACING placement sanity (placed block faces the placer). Contact sheets →
   gate. Verify green.

**Phase A3 — entities:**
7. model_sync registry ×10; per-creature bbmodels generated; legacy shared model class +
   registration + mirror bbmodels deleted; render previews replace hand mirrors.
8. Anatomy-aware creature painters + glow recalibration + eggs (`--creatures` regen).
9. Rocket per-tier LayerDefinitions + renderer dispatch + tier textures; stance numbers untouched.
10. Suit worn-layer + icon redo. Contact sheets (turnarounds, tier line-up, suit sheet) → gate.
    Verify green (existing rocket/pad/creature gametests must stay green untouched).

**Phase A4 — fluids/gas/GUI:**
11. `rocket_fuel` client extensions (+ generated still/flow strips + mcmeta); O₂ pipe-stream tint +
    gas-tank shimmer.
12. Segmented animated gauges + tooltips in `TexturedContainerScreen`; fluid/gas sprite fills;
    all machine GUI panels regenerated onto the shared generator. Contact sheets → gate.
13. Full verify: runData → build → ecjCheck → full gametest suite + check_assets, all green; tick
    the §2 boxes that fully pass; flag all look checks "Verify in runClient"; wiki touch-ups where
    pages show old art descriptions.

## 10. Deferred (explicitly out of slice 1)

- Bespoke `.ogg` audio (§3, post-1.0 by Q&A).
- Connected textures / CTM for station builds.
- 32× or HD texture packs; shader/emissive (PBR) maps.
- VoxelShape changes to match silhouettes (deliberate invariant, §1).
- JEI/EMI icon work (blocked on 26.1 ports, checklist §8).
- Biome-coloured machine trim variants; seasonal/easter-egg liveries.
- In-GUI 3D block previews and item tooltips beyond gauge values.

## 11. Sign-off questions

1. **Palette directions** (§2 table) — approve the per-family directions, with exact hexes settled
   on the A1 contact sheets?
2. **FACING on machines** — OK that already-placed machines in old worlds load facing north
   (blockstate default; no save break, purely cosmetic)?
3. **Tank windows** — cutout window faces showing a content core on all six tank blocks (creative
   variants gold-trimmed) — approve?
4. **Rocket tier silhouettes** (§4.2: T1 classic / T2 twin boosters / T3 ring skirt / T4 heavy
   quad) — approve?
5. **Legacy retirement** — deleting `GreenxertzCreatureModel`'s layer registration + the legacy
   shared-geometry bbmodel mirrors (and replacing `render_models.py`'s hand mirrors with the
   parser-driven previewer) — approve the repo deletions?
6. **GUI panel unification** — regenerating ALL machine GUI panels onto the shared generator style
   (replacing the assorted committed panels) — approve?
7. **Review gates** — hard gate per phase (I stop on each contact sheet until you approve) vs soft
   (show the sheet and continue; you veto asynchronously)?
8. **Phasing** — A1 palette → A2 shapes → A3 entities → A4 fluids/GUI — approve the order?

## 11. Sign-off Answers:

1. I like bright red and purple, so the palette directions are approved.
2. FACING on machines is approved, and the fact that already-placed machines will load facing north is acceptable.
3. Tank windows with cutout faces showing a content core are approved, and the gold-trimmed creative variants sound great.
4. The rocket tier silhouettes are approved as described.
5. Yes, you can retire all and any changes.
6. The GUI panel unification by regenerating all machine GUI panels onto the shared generator style is approved.
7. I trust you to make the decisions, we can make the changes later
8. Approved.
