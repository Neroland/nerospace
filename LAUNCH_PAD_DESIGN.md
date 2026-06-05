# Big multiblock launch pad — design (RELEASE_CHECKLIST §1)

Grow the launch pad from the canonical 3×3 into a **5×5 Heavy Launch Complex** with functional
modules, without breaking a single existing world or recipe.

## 1. Current state (what we build on)

A pad is a horizontally-connected cluster of `rocket_launch_pad` blocks at one Y level
(`LaunchPadMultiblock.connectedPads`, flood-fill capped at 25). A complete aligned **3×3** is the
canonical pad: required to deploy any rocket, re-checked at launch, 4× Fuel Tank pump
(160 vs 40 mB/t), and the pad proxies the rocket's item intake. **Tier 3** additionally needs the
16-block **Station Wall ring** (the 5×5 border at pad level). The Fuel Tank is adjacent machinery,
not part of the footprint.

## 2. Footprint tiers

| Tier | Footprint | Unlocks |
|---|---|---|
| **Pad** (existing) | full aligned 3×3 | deploy + launch T1/T2 (T3 with Station-Wall ring), 4× fuel pump |
| **Heavy Launch Complex** (new) | full aligned **5×5** of pad blocks **+ ≥1 Gantry module** | 8× fuel pump, gantry auto-board, T3 without the ring (see Q1), launch countdown hooks (later) |

- Detection generalises the existing geometry: `isFullSquare(pads, size)` returning the min-corner
  (the 3×3 path delegates to it; existing gametests keep passing). `MAX_PADS` rises 25 → 64 so a
  sloppy pad field can still contain an aligned 5×5.
- A 5×5 trivially contains 3×3s, so every Heavy complex is automatically a valid basic pad —
  nothing about T1/T2 changes, **zero migration**.

## 3. Modules

- **Fuel module = the existing Fuel Tank**, no new block. A Fuel Tank adjacent to a Heavy complex
  pumps at the new heavy rate (proposed **8× base = 320 mB/t**, wired as a `Tuning` base constant ×
  `machineSpeedMultiplier`, like the existing 40/160). Rationale: players already understand the
  tank; the complex makes it better.
- **Gantry module = new block `launch_gantry`** (steel tower segment, ~2 blocks visual height via
  the art pass; slice 1 ships a cube). Placement: at pad level, horizontally adjacent to the 5×5
  (same convention as the Fuel Tank). Function in slice 1: it **completes** the Heavy complex
  (structure requirement) and **right-click boards** the rocket on the pad (auto-board QoL — no
  more pixel-hunting the entity). Later slices: launch-countdown FX anchor, crew/suit airlock hook.
- Module scan: `heavyComplex(level, pads)` finds the 5×5 corner, then checks the 20 border-adjacent
  cells at pad level for gantries/tanks — pure geometry in `LaunchPadMultiblock`, no block entity.

## 4. Tier gating (the T3 question)

Proposed: **T3 deploys on EITHER** the legacy 3×3 + Station-Wall ring **or** a Heavy complex
(5×5 + gantry, no ring needed). Existing T3 sites keep working untouched; the Heavy complex is the
forward-looking path and frees the ring for what it visually is (a blast wall, not a tech gate).
Alternatives flagged in Q1.

## 5. Formation feedback

- Deploy attempts keep the existing precise failure messages, extended: "needs a 5×5 + gantry" when
  a T3 deploy targets a bare 5×5 (and Q1 option B/C variants).
- NEW: **empty-hand right-click on any pad block prints a formation report** (like the Fuel Tank's
  fuel readout): cluster size, largest formed square (none / 3×3 / 5×5), gantry present, fuel
  module connected, what the next missing piece is.

## 6. Slice plan

- **Slice 1 (this batch):** generalized detection + `MAX_PADS` raise, Heavy complex recognition
  (5×5 + gantry), formation report interaction, T3 alternate gate per Q1, heavy fuel rate via
  `Tuning`, `launch_gantry` block + item + datagen + texture (cube), gametests (valid/invalid 5×5,
  gantry requirement, T3 gating both paths, heavy pump rate, formation report fixture reuse).
- **Slice 2 (art pass §2):** flatter pad block model + gantry tower model — **deferred** (see Q4:
  the pad's collision height changes rocket/entity Y placement and is art-pass territory).
- **Slice 3:** launch countdown FX from the gantry, crew boarding animations, module extensions
  (oxygen umbilical?).

> **Sign-off (2026-06-05):** Q1 = ring OR Heavy both deploy T3 (zero breakage). Q2 = gantry
> required (bare 5×5 is just a big basic pad). Q3 = **12× base = 480 mB/t** heavy fuel rate.
> Q4 = **slab-profile pad model ships in slice 1** (accept the placement churn now).

## 7. Open questions (sign-off)

- **Q1 — T3 home:** (A, recommended) ring OR Heavy complex both deploy T3; (B) Heavy complex
  becomes the only T3 pad (breaking — existing ringed sites stop working); (C) ring stays the only
  gate, Heavy complex is pure bonus.
- **Q2 — bare 5×5:** is a 5×5 with NO gantry just a big basic pad (recommended — gantry is what
  makes it "Heavy"), or should the 5×5 alone already grant the heavy fuel rate?
- **Q3 — heavy fuel rate:** 8× base (320 mB/t, fills a T3 tank in ~37s)? Or steeper (12×)?
- **Q4 — flatter pad model:** defer to the §2 art pass (recommended — collision/Y-placement churn),
  or attempt a slab-profile pad in slice 1?
