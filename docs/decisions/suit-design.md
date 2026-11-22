# Pathfinder Suit — design (Milestone S)

Locked by the owner on 2026-11-22. Ids stay `suit_*`. Display name is Pathfinder Suit. Version is 2.6.0-beta.1. All four pieces must carry a weave or lining. Flight defaults to every dimension and a server can restrict it. Flight needs Mk IV and Core `DEEP_SPACE`. Old suits convert on first hold or wear. A piece breaches and never breaks. Numbers are the brief's first pass. Charging is the Fabricator, Core `ItemEnergyLookup`, the Solar Skin, and the Suit Dock.

## Slot data

Rules live in `SuitSlotRules` and can be overlaid from `data/nerospace/suit_modules/*.json`. A broken file logs one line (the exception class only) and the built-in catalogue stays. Unknown module ids already saved on a stack are kept and shown as "Unknown module".

Weaves: Thermal (Mk II), Cryo (Mk II), Adaptive (Mk IV). They exclude each other. Linings: Rad Lining (Mk II), and `nerospace:pressure_lining` reserved for the later acid lining. Modules are listed on the wiki page Suit Modules. Hardpoint, EVA thrusters, tool link, mag clamps and the uplink visor are inert or data-only and have no recipe.

## Payloads

Not registered in this build. The server reads an input bitmask held on the in-memory flight state. A later pass should add `SuitStatusPayload`, `SuitTogglePayload` and `SuitInputPayload` on the existing `ModNetwork` lists, rate-limited by `SuitRateLimit` (20 per 20 ticks, excess counted as an int).

## Migration

`LegacySuitConversion` is the mapping table. `SuitProtection` classifies four slots the same way for a legacy set, the view of that set, and the converted set. `HazardParityTest` is the gate. Conversion replaces a stack in the player inventory on the server tick, keeps the count, and does not run for stacks that are not legacy ids.

## Privacy

No new saved store. The station flag `suit_flight_denied` is an optional boolean on the existing station entry and defaults to false, so an older station loads with flight allowed. In-memory maps are `WeakHashMap`s keyed by the player object.

## Space Programme amendments for the owner to apply

- The EVA pack and mag boots should be born as the EVA Thrusters and Mag Clamps modules on this suit, reusing `MovementAllowance`.
- The pressure lining should be a lining module (`nerospace:pressure_lining` is reserved).
- Liveries: `suit_loadout` already has `livery_color` and `livery_pattern`. A later livery pass can fill them. This tree has no livery palette yet.

## Review

Reviewed against the brief's balance, griefing, save, performance and privacy rules while the code was written. No open MAJOR in the design itself. Residual risks are listed in `docs/HANDOVER-2.6.0.md` and are implementation gaps (movement mixin, custom crafting serializer, payload registration), not undecided design.
