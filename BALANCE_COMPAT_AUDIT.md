# Nerospace — Balance & Compatibility Audit

Audit of RELEASE_CHECKLIST.md **§5 (Balance)** and **§8 (Compatibility)**, run before the §6
runClient/survival passes. Goal: settle the numbers and the cross-mod surface so the playthrough
tests *feel*, not arithmetic.

**Design intent locked for this audit (Q&A 2026-06-11):**
- **Pacing:** moderate — ~4–6 h to first launch, ~12–15 h to first terraform.
- **Grind pillar:** *logistics* (automation & transport). Costs stay modest; throughput is the
  challenge. Do **not** inflate mining or energy walls.
- **Tags:** aggressive cross-mod compatibility, with one hard rule — **planet-gated materials
  (cindrite, glacite, and the Greenxertz pair) must stay locked to our own items** so no other mod
  can shortcut the planet trip. A renewable, non-travel source is a *future* feature; tag inputs
  should be authored so that future source slots in for free.
- **Flagged in play:** rocket fuel per launch felt wrong.

All numbers are **base** values from [`Tuning.java`](src/main/java/za/co/neroland/nerospace/Tuning.java)
(pack multipliers default 1.0). Source of truth for each figure is cited inline.

---

## 0. Headline finding — the progression is circular (BLOCKER)

This must be read first; it reframes every cost table below.

**Nerosteel and xertz quartz generate *only* on Greenxertz.** Verified three ways:
- The only overworld biome modifier is `add_nerosium_ore`
  ([`ModBiomeModifiers.java:35`](src/main/java/za/co/neroland/nerospace/world/ModBiomeModifiers.java),
  targets `Tags.Biomes.IS_OVERWORLD`, injects **nerosium only**).
- `nerosteel_ore_placed` and `xertz_quartz_ore_placed` are referenced by exactly one biome:
  [`greenxertz.json`](src/generated/resources/data/nerospace/worldgen/biome/greenxertz.json) (the
  7th feature step).
- The Orbital Station (Tier-1 destination) is an **empty void** — a flat generator with no layers
  ([`ModDimensions.java:102`](src/main/java/za/co/neroland/nerospace/registry/ModDimensions.java));
  the rocket places a crafted Station Floor platform, so it yields no ore.

Nerosteel has **no crafting/alloying recipe** — the only routes are smelting nerosteel ore or its
raw drop ([`ModRecipeProvider.java:152`](src/main/java/za/co/neroland/nerospace/datagen/ModRecipeProvider.java)).

Now trace the gate. The Star Guide's intended order
([`StarGuide.java:55`](src/main/java/za/co/neroland/nerospace/progression/StarGuide.java)) is
*machines → power grid → rocketry → Station → Greenxertz*. But:

| Item the player needs early | Requires | Where that material lives |
|---|---|---|
| Combustion Generator (2nd guide step) | 6× nerosteel ingot | **Greenxertz** |
| Universal Pipe / Battery / Passive Gen | nerosteel | **Greenxertz** |
| Rocket Fuel Canister | xertz quartz | **Greenxertz** |
| Rocket Launch Pad | 8× nerosteel + nerosium block | **Greenxertz** |
| Tier 1 Rocket | 14× nerosteel + canister | **Greenxertz** |
| Tier 2 Rocket (reaches Greenxertz) | T1 + 15× nerosteel + canister | **Greenxertz** |

**You cannot obtain nerosteel or xertz quartz without reaching Greenxertz, and you cannot reach
Greenxertz without nerosteel and xertz quartz.** Everything past the nerosium pickaxe and grinder is
walled off. This was not caught because §6 ("Full start-to-terraform survival playthrough — no
creative shortcuts") is still unchecked; piecemeal playtests used creative/`/give`.

**Verdict: BLOCKER — must fix before any survival pass.** This is a design decision, not a number
tune, so it leads the sign-off questions (§6). Recommended fix (Option A, least invasive): add a
**rare, deep overworld generation** of nerosteel ore and move xertz quartz off the early-game
critical path (the canister is the only pre-Greenxertz xertz consumer — see §3). Greenxertz then
becomes the *abundant* source rather than the *only* source, the "Greenxertz material" theme
survives, and no recipes churn. Alternatives in §6.

---

## 1. Progression cost table

Material acquisition (worldgen + processing), then per-stage gating recipes. "Logistics pillar"
means these costs are deliberately read as *modest* — the friction is meant to be moving and
automating, not out-mining the numbers.

### 1.1 Raw material economics

| Material | Dimension | Vein | Veins/chunk | Y-range | Mine → | Processing ratio |
|---|---|---|---|---|---|---|
| Nerosium (+ deepslate) | Overworld | 9 | 8 | −24…56 | 1 raw | raw → 1 ingot; **ore → grinder → 2 dust → 2 ingot** (2×) |
| Nerosteel | Greenxertz* | 9 | 10 | −32…72 | 1 raw | raw/ore → 1 ingot (no grinder doubling) |
| Xertz quartz | Greenxertz* | 14 | 12 | 0…110 | 1 gem | direct drop (ore also smelts → 1) |
| Cindrite | Cindara | 8 | 7 | −48…48 | 1 gem | direct drop (no smelt) |
| Glacite | Glacira | 8 | 7 | −48…48 | 1 gem | direct drop (no smelt) |

*\*Greenxertz-only is the §0 blocker.* Sources:
[`ModConfiguredFeatures.java`](src/main/java/za/co/neroland/nerospace/world/ModConfiguredFeatures.java),
[`ModPlacedFeatures.java`](src/main/java/za/co/neroland/nerospace/world/ModPlacedFeatures.java),
loot tables under `src/generated/resources/data/nerospace/loot_table/blocks/`,
[`GrinderRecipes.java`](src/main/java/za/co/neroland/nerospace/machine/GrinderRecipes.java).

**Verdict — grinder doubling asymmetry: tune.** Only nerosium benefits from the grinder (2×).
Nerosteel, the backbone material of every machine, has no doubling path, so the only machine in the
mod is irrelevant to its most-used input. For a logistics mod this is a missed automation hook.
*Recommend:* add nerosteel ore/raw → 2 nerosteel dust → ingot to `GrinderRecipes` (and a dust item
+ smelt recipe), matching nerosium. Low risk, reinforces the "build a grinder line" loop. (Gems
intentionally stay un-doubled — they're the planet-gated currency.)

### 1.2 Gating recipes by stage (priced in ingots/gems; vanilla commodities listed)

Pattern sources: [`ModRecipeProvider.java`](src/main/java/za/co/neroland/nerospace/datagen/ModRecipeProvider.java).

**Stage 0 — Nerosium (overworld, intended day-one):**
| Recipe | Mod-material cost | Vanilla cost |
|---|---|---|
| Star Guide book | 1 raw nerosium | 1 book |
| Star Guide block | 1 raw nerosium | 1 planks + 3 cobblestone |
| Nerosium pickaxe | 3 nerosium ingot | 2 stick |
| Nerosium Grinder | 3 nerosium ingot | 1 furnace + 3 cobblestone |

*Verdict: fine.* Cheap, reachable, paced right for the opening hour.

**Stage 1 — Power grid & logistics (currently nerosteel → §0 blocked):**
| Recipe | Mod-material cost | Vanilla cost | Yield |
|---|---|---|---|
| Combustion Generator | 6 nerosteel | furnace + redstone | 1 |
| Passive Generator | 6 nerosteel + 1 nerosium block (9 ingot) | redstone | 1 |
| Universal Pipe | 8 nerosteel | 1 glass | **8** |
| Battery | 4 nerosteel + 1 nerosium ingot | 4 redstone | 1 |
| Fluid Tank | 4 nerosteel | 4 glass | 1 |
| Gas Tank | 12 nerosteel (incl. fluid tank) | — | 1 |
| Item Store | 8 nerosteel | chest | 1 |
| Pipe Filter | 6 nerosteel | iron bars | **4** |
| Configurator | 2 nerosteel | redstone | 1 |
| Speed Upgrade | 4 nerosteel | 4 redstone + gold | 1 |
| Capacity Upgrade | 4 nerosteel + 4 xertz quartz | chest | 1 |

*Verdict: costs fine for logistics pacing (pipes 1 nerosteel each, filters cheap) — but unbuildable
pre-Greenxertz (§0). The Capacity Upgrade's xertz-quartz requirement double-gates it behind
Greenxertz; acceptable once §0 is fixed.*

**Stage 2 — Rocketry (nerosteel + xertz, Greenxertz-gated):**
| Recipe | Mod-material cost | Vanilla cost | Yield |
|---|---|---|---|
| Rocket Fuel Canister | 1 xertz quartz | blaze powder + coal + iron | **2** |
| Launch Pad | 8 nerosteel + 1 nerosium block | — | 1 |
| Launch Gantry | 6 nerosteel + 1 station wall | iron bars | 1 |
| Station Floor | 8 nerosteel | — | **8** |
| Station Wall | 8 nerosteel | iron | **8** |
| Tier 1 Rocket | 14 nerosteel (5 + block) + 1 canister | — | 1 |
| Tier 2 Rocket | T1 + 15 nerosteel + 1 canister | — | 1 |
| Station Charter | 8 wall + 1 floor (≈9 nerosteel-equiv) | — | 1 |

*Verdict: nerosteel quantities are the heaviest sink in the mod (T1+T2 ≈ 30 nerosteel + 2 launch
pads worth). At 1 nerosteel/ore and ~10 veins/chunk this is ~2–3 chunks of focused mining per
rocket tier — consistent with the 4–6 h first-launch target, **once §0 makes nerosteel reachable.**
Fine.*

**Stage 3 — Station-tier (T3, suits, oxygen):**
| Recipe | Mod-material cost | Vanilla cost |
|---|---|---|
| Tier 3 Rocket | T2 + 15 nerosteel + 2 station wall + 1 canister | — |
| Oxygen Generator | 6 nerosteel + 1 canister | glass + redstone |
| Oxygen Suit (4 pcs) | 22 nerosteel + 1 canister | glass |

*Verdict: fine.* Station Wall gate on T3 is a clean "build an orbital base first" beat.

**Stage 4 — Cindara (cindrite):**
| Recipe | Mod-material cost |
|---|---|
| Tier 4 Rocket | T3 + 6 nerosteel + 2 cindrite + canister + block |
| T2 Suit (4 pcs) | 16 cindrite + T1 suit |
| Thermal Suit (4 pcs) | 16 cindrite + T2 suit |

*Verdict: fine.* Cindrite (8-vein, 7/chunk) is the rarest material; 16 per suit-set is a real Cindara
expedition. Matches "previous planet unlocks the next".

**Stage 5–6 — Glacira & terraforming:**
| Recipe | Mod-material cost | Vanilla cost |
|---|---|---|
| Cryo Suit (4 pcs) | 16 glacite + T2 suit | — |
| Hydration Module | 8 nerosteel + 4 glacite | 1 fluid tank |
| Terraformer | 13 nerosteel + 1 oxygen generator | 4 dirt |
| Terraform Monitor | 6 nerosteel + 2 xertz quartz | glass + redstone |

*Verdict: fine.* Terraformer-as-capstone cost (oxygen gen + 13 nerosteel) reads right for the
12–15 h terraform target.

---

## 2. Energy economy (output vs consumers, by stage)

Generators: Combustion **60 FE/t**, Passive **10 FE/t** (no fuel). Energy pipe throughput
**4,000 FE/t** (never the bottleneck). Battery **200,000 FE**. Buffers: combustion 50k, passive 20k,
grinder 10k, oxygen gen 10k, terraformer 100k.

| Consumer | Draw | Per-operation cost |
|---|---|---|
| Nerosium Grinder | 30 FE/t for 100 t/item | **3,000 FE → 2 dust** (1,500 FE/ingot) |
| Oxygen Generator | 2 FE/mB × 5 mB/t = **10 FE/t** | makes 5 mB/t, emits 2 mB/t to field |
| Terraformer stage 1 | up to 48 col × 12 FE / 8 t = **72 FE/t** | 12 FE/block |
| Terraformer stage 2 | **144 FE/t** | 24 FE/block |
| Terraformer stage 3 | **288 FE/t** | 48 FE/block |
| Airlock refill | (gas, not FE) 20 air/check, 5 mB O₂/air | — |

Combustion fuel values ([`CombustionGeneratorBlockEntity.java:100`](src/main/java/za/co/neroland/nerospace/machine/CombustionGeneratorBlockEntity.java)):
coal/charcoal 1,600 t → **96,000 FE**; coal block 16,000 t → 960k FE; blaze rod 144k FE;
**Rocket Fuel Canister 240k FE**.

**Stage analysis & verdicts:**
- **Early (1 combustion + 1 grinder):** 60 supply vs 30 demand. 1 coal = 32 grind ops. *Fine —
  comfortable surplus, coal-limited, paced right.*
- **Passive Generator (10 FE/t, fuel-free):** powers ⅓ of a grinder forever. *Fine* as an idle/AFK
  trickle; intentionally too weak to run a base, which pushes players to combustion + logistics.
- **Mid (grinder + oxygen gen):** 40 FE/t demand vs 60 supply. *Fine.*
- **Late (terraformer at full throughput):** 288 FE/t at stage 3 needs ~5 combustion generators *or*
  battery buffering + piping. *Verdict: fine and on-theme* — this is exactly the logistics/energy
  challenge the player should build toward; the 100k terraformer buffer + 4,000 FE/t pipes make it a
  routing problem, not a wall. **Flag for §6 feel:** confirm 5-generator demand doesn't read as
  tedious vs satisfying.
- **Canister as combustion fuel (240k FE):** technically usable but never economical (a canister
  costs blaze + coal + xertz + iron for 2; burning one nets less than its inputs). *Verdict: leave —
  harmless flexibility, not an exploit.* Minor note only.

---

## 3. Fuel-per-launch economics (player-flagged)

Chain ([`RocketEntity.java:80`](src/main/java/za/co/neroland/nerospace/rocket/RocketEntity.java),
[`FuelTankBlockEntity.java:43`](src/main/java/za/co/neroland/nerospace/machine/FuelTankBlockEntity.java)):
**1 canister = 1,000 mB** liquid `rocket_fuel`. There is **no machine recipe** that produces the
fluid — it originates *only* from converting canisters/buckets. The Fuel Tank is filled by **manual
right-click** with a canister; it then pumps to a padded rocket at 40 / 160 / 480 mB/t
(partial / 3×3 / Heavy).

| Tier | Tank (mB) | Launch (mB) | Launches/tank | Canisters/launch | Raw mats/launch |
|---|---|---|---|---|---|
| T1 | 3,000 | 1,000 | 3 | 1 | ½ each: blaze powder, coal, xertz, iron |
| T2 | 6,000 | 2,000 | 3 | 2 | 1 each |
| T3 | 12,000 | 4,000 | 3 | 4 | 2 each |
| T4 | 24,000 | 8,000 | 3 | 8 | 4 each |

**Why it feels wrong — three concrete problems, not the mB count:**
1. **No automation path.** The Fuel Tank exposes only a *Fluid* capability, but **nothing produces
   the fluid into a pipe**, and the tank has **no item slot** for canisters — so it cannot be fed by
   hopper or pipe (see capability gap §5.3.1). The entire fuel loop is hand-craft canisters +
   right-click. That directly contradicts the logistics grind pillar: the one place automation
   should shine is manual-only. *(The rocket entity itself **can** be pipe-fed canisters via the pad
   proxy — [`ModCapabilities.java:134`](src/main/java/za/co/neroland/nerospace/registry/ModCapabilities.java) —
   so the capability model is half-built; the Fuel Tank machine is the gap.)*
2. **Xertz-quartz gate.** The canister needs xertz quartz → **no rocket can be fuelled before
   Greenxertz**, compounding §0. After §0 this still means "no fuel until planet 2".
3. **Flat scaling.** 1,000 mB/canister regardless of tier → T4 = 8 hand-crafted canisters/launch =
   tedious clicking, not a cost.

**Verdict:** the per-launch **mB amount and 1:3 tank ratio are good — keep them.** What needs to
change is the *delivery*: (a) make the Fuel Tank canister-feedable so fuel is automatable (the
real fix for "felt wrong"); (b) decouple fuel from the xertz gate (drop xertz from the canister, or
satisfy it via the §0 overworld-xertz decision); optionally (c) add a cheap bulk-fuel machine recipe
(coal + blaze + energy → liquid fuel, pipeable) as the logistics-grade source. (a)+(b) are slice-1;
(c) is a candidate or defer. Direction is a sign-off question (§6).

---

## 4. Oxygen drain / tank / refill

From [`Tuning.java:26`](src/main/java/za/co/neroland/nerospace/Tuning.java) (suit checks ≈ every 0.5 s):

| Quantity | Value | Real-time |
|---|---|---|
| Player air (bare) | 300, drain 2/t | **7.5 s** unprotected |
| T1 suit | 300 cap, drain 1/check | **~2.5 min** |
| T2 suit | 600 cap, drain 1/check | **~5 min** |
| Hazard (no matching variant) | ×4 drain | T1 ≈ 38 s / T2 ≈ 75 s on hostile planet |
| Airlock refill | 20 air/check (T2: ×2) | 300 in ~7.5 s; 600 in ~15 s |
| Suffocation | 1 half-heart / 2 s at 0 air | — |

**Verdicts:**
- Bare-lungs 7.5 s — *fine* (intended "get a suit" pressure).
- T1 2.5 min — *borderline; flag for §6.* Fine for base-building near airlocks; may feel short for
  exploration. Tune only if the playthrough confirms friction (lever: capacity, not drain).
- T2 5 min + 15 s refill — *fine.*
- Hazard ×4 — *fine* (drain pressure, no new damage path, per `SUIT_HAZARD_DESIGN.md`).
- All five multipliers clamp scaled values ≥1 — *fine* (extreme-config safety verified in code).

---

## 5. Compatibility audit

### 5.1 Conventional (`c:`) tag coverage

Present and correct: `c:ingots` (+ `/nerosium`, `/nerosteel`), `c:dusts` (+ `/nerosium`),
`c:raw_materials` (+ `/nerosium`, `/nerosteel`), `c:ores` (broad — all six),
`c:storage_blocks` (broad — all five), `c:gems` (broad — quartz/cindrite/glacite) with
`/xertz_quartz`, `/cindrite`, `/glacite`. Sources:
[`ModItemTagProvider.java`](src/main/java/za/co/neroland/nerospace/datagen/ModItemTagProvider.java),
[`ModBlockTagProvider.java`](src/main/java/za/co/neroland/nerospace/datagen/ModBlockTagProvider.java),
[`ModTags.java`](src/main/java/za/co/neroland/nerospace/registry/ModTags.java).

**Gaps (inconsistent per-material subtags):**
| Missing tag | Side | Note |
|---|---|---|
| `c:ores/cindrite` | block **+ item** | absent entirely (nerosium/nerosteel/xertz/glacite have theirs) |
| `c:ores/glacite` | **item** | block tag `ORES_GLACITE` exists; item subtag missing |
| `c:storage_blocks/cindrite` | block **+ item** | absent entirely |
| `c:storage_blocks/glacite` | **item** | block tag exists; item subtag missing |

*Verdict: add all four (+ matching `ModTags` keys).* Broad-tag discoverability already works; this is
about per-material consistency so tech mods keying on `c:ores/glacite` etc. find ours.

**Aggressive output-side opportunity:** add xertz quartz to **`c:gems/quartz`** so quartz-consuming
recipes in other mods accept it. *Safe* because it only widens what *others* can use — it does **not**
change our recipe inputs, so the Greenxertz gate holds. *Recommend: do it.*

### 5.2 Tag-based recipe inputs (the aggressive pass, gate-preserving)

Every recipe currently hard-codes exact items. The rule that satisfies "aggressive compat" **and**
"lock the planet ores":

- **Vanilla commodities → broad tags** (cross-mod): `Items.IRON_INGOT → c:ingots/iron`,
  `GOLD_INGOT → c:ingots/gold`, `REDSTONE → c:dusts/redstone`, `IRON_BARS → c:bars` (or keep),
  `CHEST → c:chests/wooden`, `GLASS → c:glass_blocks/cheap` (NeoForge) — applied to grinder, pipes,
  battery, generators, tanks, upgrades, suits, station blocks. Lets any mod's equivalents qualify.
- **Our own materials → our narrow `c:` subtag** (`c:ingots/nerosium`, `c:ingots/nerosteel`,
  `c:gems/cindrite`, `c:gems/glacite`, `c:gems/xertz_quartz`). Today each subtag contains *only our
  item*, so using it as an input is behaviourally identical to the exact item **— the gate holds —**
  while the **future renewable source** (the user's planned non-travel sourcing) slots in for free by
  joining the same tag. This is the key future-proofing move.
- **Do NOT** input planet materials via broad parent tags (`c:gems/quartz`, `c:gems`): that would let
  nether quartz or another mod's gem bypass Greenxertz/Cindara/Glacira. Output-side only (§5.1).

*Verdict: implement the dual rule above.* It is the maximal compat that respects the planet locks.

### 5.3 Capability gaps

Sweep cross-referenced [`ModCapabilities.java`](src/main/java/za/co/neroland/nerospace/registry/ModCapabilities.java)
against every block entity. Most are correct (grinder sided I/O, both generators' fuel/core slots,
all tanks, pipe Energy/Fluid/Gas, rocket Fluid+Item, pad proxy). Three gaps:

1. **Fuel Tank — no Item capability (HIGH, logistics-critical).**
   [`FuelTankBlockEntity.java`](src/main/java/za/co/neroland/nerospace/machine/FuelTankBlockEntity.java)
   exposes only Fluid via `getTank()`, with no canister item slot. Combined with "no machine makes
   the fluid", fuel cannot be automated. *Recommend: add a canister input slot + Item capability that
   converts inserted canisters to 1,000 mB.* This is the concrete fix behind §3.
2. **Terraformer — hydration buffer not a Fluid capability.** Hydration is an `int` (melted glacite
   units) fed only by an adjacent Hydration Module via `acceptHydration()`.
   **REVISED after sign-off (gate conflict):** a water-`Fluid` intake would let infinitely-renewable
   `minecraft:water` convert straight to hydration units, **bypassing the Glacira/glacite gate** that
   `DEEPER_TERRAFORM_DESIGN.md` set up deliberately ("vanilla ice would bypass the Glacira gate").
   That contradicts the signed-off hard rule (planet materials stay locked), which **outranks** the
   Q3 "full sweep". *Decision: NOT implemented — kept deferred.* The gate-safe automation hook (pipe
   **glacite items** into the Hydration Module) already exists as an Item capability. Revisit only if
   the Glacira gate is consciously weakened, or alongside the future renewable-sourcing feature.
3. **Hydration Module — no Fluid output capability.** The module melts glacite *items* into `int`
   units pushed to the adjacent Terraformer — there is **no fluid** to output. A Fluid output would
   mean inventing a hydration fluid for no real interop gain. *Decision: NOT implemented — the Item
   input capability is the correct, gate-safe surface.*

Non-gaps confirmed: Battery (Energy only — correct), Terraform Monitor / Station Core / Star Guide
(metadata only — correct), creative variants (all expose their type).

### 5.4 `mods.toml` dependency check

[`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml): only `neoforge`
(`[${neo_version},)`) and `minecraft` (`${minecraft_version_range}`) as **required**, both
`ordering="NONE"`, `side="BOTH"`. **No hard dependency on any other mod.** *Verdict: PASS (§8 last
box).* Minor follow-ups (not balance): `license="${mod_license}"` must resolve to the §9 custom
licence; `displayURL`/`issueTrackerURL` point to `github.com/Neroland/nerospace` — confirm the org
slug is correct. Both belong to §9, noted here only.

---

> **Slice 1 status: APPLIED & VERIFIED (2026-06-11).** runData → check_assets → build → ecjCheck →
> gametests all green (**39/39**, +2 new: `fuel_tank_canister_autofeed`, `fuel_refinery_produces_fuel`).
> §5 boxes (recipe-cost + energy-economy audit) and §8 boxes (c: tags, tag-inputs, capabilities,
> no-hard-deps) ticked in `RELEASE_CHECKLIST.md`; oxygen/fuel *play-feel* left flagged for §6. Not
> committed/pushed.

## 6. Slice 1 scope (SIGNED OFF 2026-06-11)

Decisions: **Q1 = A** (overworld nerosteel + drop xertz from canister); **Q2 = automation +
bulk-fuel machine**; **Q3 = full capability sweep**; **Q4 = no grinder change now, dedicated
ore-doubling machine deferred to a later slice**.

1. **Fix the circular gate (§0, BLOCKER) — Option A.** Add a rare/deep **overworld** nerosteel
   placed feature + biome modifier (scarcer than Greenxertz: ~count 4, deep Y so it reads as
   "tier-2 alloy you dig for"). Greenxertz stays the abundant source.
2. **Decouple fuel from the xertz gate (§3).** Drop xertz quartz from the Rocket Fuel Canister
   recipe (canister → blaze powder + coal + iron, still yields 2).
3. **Fuel automation (§3).** Add a canister **input slot + Item capability** to the Fuel Tank
   (inserted canisters convert to 1,000 mB); per-launch mB unchanged. Update its menu/screen for the
   slot.
4. **Bulk-fuel machine (§3c) — NEW "Fuel Refinery".** Consumes coal + blaze powder + grid energy →
   liquid `rocket_fuel` into an internal tank, exposed via Fluid cap (pipeable). Craftable
   pre-launch from nerosteel/nerosium (no xertz). Full content: block/BE/menu/screen/recipe/model/
   texture/blockstate/loot/lang/tags/capability/creative-tab/Tuning.
5. **Capability sweep (§5.3).** Fuel Tank Item cap (with #3) — DONE. Terraformer Fluid intake +
   Hydration Module Fluid output **revised to NOT-implemented**: both conflict with the Glacira gate
   the hard rule protects (see §5.3) — the gate-safe glacite-item input already exists. This is the
   one place the literal Q3 answer was overridden by the Q-tags hard rule; flagged for the user.
6. **Tag completeness (§5.1).** Add `c:ores/cindrite`, `c:ores/glacite` (item), `c:storage_blocks/
   cindrite`, `c:storage_blocks/glacite` (item) + `ModTags` keys; add xertz quartz to `c:gems/quartz`.
7. **Aggressive tag-input pass (§5.2).** Vanilla commodities → broad `c:` tags; our materials → our
   narrow `c:` subtags (gate-preserving, renewable-ready). Recipes via datagen only.
8. **Gametests.** `overworld_nerosteel_reachable` (guards §0 from regressing),
   `fuel_tank_canister_autofeed`, `fuel_refinery_produces_fuel`, `combustion_generator_output_rate`,
   `rocket_launch_fuel_consumed`.
9. **Wiki.** Update [`Configuration.md`](wiki/Configuration.md) and any page whose numbers move
   (nerosteel sourcing, fuel automation, the new machine).
10. **Verify.** `runData → check_assets → build → ecjCheck → full gametest suite`, all green; tick the
    §5/§8 boxes that fully pass; leave play-feel items (T1 suit duration, terraformer generator
    count) flagged for §6.

## 7. Deferred

- **Dedicated ore-doubling machine** (Q4) — a future machine doubles *all* ores (incl. nerosteel,
  and a renewable lens on planet gems). Grinder stays nerosium-only for now.
- **Renewable, non-travel sourcing** of planet materials — explicitly a future feature; §7's
  narrow-subtag inputs are authored so it slots in without recipe churn.
- **EMI** integration — awaiting a 26.1 port (§8). JEI integration shipped once JEI 29.x reached
  26.1: see the `compat/jei` plugin (grinding / fuel-refining / combustion-fuel categories).
- **§6 play-feel tunes:** T1 suit duration, terraformer late-game generator count — decide after the
  survival playthrough, cheap to adjust via multipliers/bases.
- `mods.toml` licence/URL polish → §9.

---

## 8. Sign-off questions — STOP

No numbers, tags, capabilities, or recipes change until these are answered.

**Q1 — Circular-gate fix (§0).** Which approach?
- **(A) Recommended:** add rare/deep **overworld nerosteel** generation; move xertz quartz off the
  early path (drop it from the canister recipe). Greenxertz stays the abundant source; no recipe
  re-gating; theme preserved.
- **(B)** Re-gate early recipes (combustion gen, pipes, battery, launch pad, T1 rocket, canister)
  onto **nerosium/iron**, making nerosteel a strict Tier-2+ upgrade material. Bigger recipe churn but
  keeps nerosteel 100% Greenxertz.
- **(C)** Give the **Orbital Station** a nerosteel source *and* make the first rocket + pad
  nerosium-buildable (so the T1 Station trip is the nerosteel unlock). Most thematic, most work.

**Q2 — Rocket fuel (§3).** Confirm: keep per-launch mB and 1:3 ratio as-is, and fix "felt wrong"
purely via **automation** (Fuel Tank canister-feed) + decoupling from xertz? And do you want the
optional **bulk-fuel machine** (coal + blaze + energy → pipeable liquid fuel) in slice 1, or
deferred?

**Q3 — Capability scope (§5.3).** Implement the **Terraformer Fluid intake** now (water-stage
automation), or keep slice 1 tight and defer it (Fuel Tank Item cap only)?

**Q4 — Grinder nerosteel doubling (§1.1).** Add it (new dust item + recipes + art), or leave
nerosteel single-yield to keep slice 1 smaller?
