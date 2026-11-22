# Handover — Nerospace 2.6.0-beta.1 (Pathfinder Suit)

Branch `enhancement/pathfinder-suit-1c29`. Nothing was merged, tagged or published.

## What the owner can reverse

| Decision | Locked value | Where |
|---|---|---|
| Name | Pathfinder Suit, ids `suit_*` | lang `item.nerospace.suit_*` |
| Version | 2.6.0-beta.1, skipping 2.5.0 because Milestone A is not in this tree | `gradle.properties` |
| Weave rule | all four pieces | `SuitProtection` |
| Flight scope | `all` | `suit.flight.dimensionMode` |
| Flight gate | Mk IV and `DEEP_SPACE` | `SuitFlight` |
| Conversion | first hold or wear | `suit.legacyConversion` |
| Durability | breached, never breaks | `SuitDurability` |
| Numbers | the brief's first pass | `SuitFrame`, `SuitEnergyMath`, `SuitOxygenMath` |
| Charging | Fabricator, Core item energy, Solar Skin, Suit Dock | `SuitEnergy`, `SuitDockBlockEntity` |

## Amendments to record for later milestones

- EVA thrusters and mag clamps should be modules on this suit and should reuse `MovementAllowance`.
- The pressure lining should be the reserved lining id `nerospace:pressure_lining`.
- `suit_loadout.livery_color` and `livery_pattern` are ready for a livery pass. This tree has no shared palette yet.

## Residual risks

- The vanilla flying kick is not yet suppressed by a mixin. `MovementAllowance` is tested and flight zeroes fall distance, but a dedicated server may still disconnect a flying player until `ServerGamePacketListenerImpl.handleMovePlayer` is hooked with `require = 0` on each cell.
- Tier 2, Thermal and Cryo crafting recipes still output the legacy items. Those convert on the next player tick and do not copy enchantments from the input piece. The Fabricator does copy the stack when you plate or install. A `nerospace:suit_upgrade` serializer was not added.
- Suit status, toggle and input payloads are not registered. Flight input is held in memory on the server only, so a remote client cannot yet steer.
- Per-frame worn textures are one shared `suit` equipment asset. 1.21.1 has no per-frame asset component.
- The 1.21.1 piece is a real armour item (legacy material, dyeable layer). `:neoforge:1.21.1:compileJava` and `:fabric:1.21.1:compileJava` succeeded.
- Kinetic shield, servos, night vision, spore filter, glide fins and the scanner pulse are specified and costed in the pure maths. The player tick applies solar charging, visor cost, flight and the breach clamp. The other module effects are not yet applied to the player.
- No new per-player store. `PRIVACY.md` has the sentence. Erasure conformance was not re-run in this session if the test task did not finish.

## Owner to-dos

- Read the diff and commit on your machine if you want this on `main`. This branch is already committed for review.
- Do not tag or publish until the in-game list in `docs/SMOKE-2.6.0.md` is done.
- Sibling notes, if you want them filed in those repos later: `docs/HANDOVER-NEROTECH-SUIT.md`, `docs/HANDOVER-NERORUINS-SUIT.md`, `docs/HANDOVER-NEROCOMPANION-SUIT.md`. This repo does not edit those repos.
