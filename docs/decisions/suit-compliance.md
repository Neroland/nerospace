# Suit compliance (Milestone S)

- No new `SavedData`. Suit state is item components `nerospace:suit_loadout` and `nerospace:suit_core`. Neither field is a name, a UUID or an owner.
- In memory, weakly keyed by the player object and dropped on logout: `SuitFlight.LIVE` (energy mirror, heat, input flags, combat lock). `MovementAllowance` (speed cap while flight is on). `OxygenManager.SHOWN` was already there.
- `suit_flight_denied` is an optional boolean on the existing station entry. Default false. An older station file loads with it off.
- `PRIVACY.md` gains a Pathfinder Suit row: no store, nothing new for erasure or `/nerospace privacy export`.
- New log lines: one warn when a suit-module JSON file fails to parse. It names the exception class only.
- Link additions are on the owner's own life-support snapshot: frame name, energy, energy percent, module ids. No schema version bump. No other player's suit is read.
- The scanner rule (`SuitScannerRules`) accepts a block only when it is an ore, generated, not a container and not player-placed. The pulse itself is not wired to the world yet, so it cannot read anything else.
- Items carry no identifier. Conversion copies enchantments, custom name, trim and dye, which the player already put on the old item.
- Originality: no other mod is named in the new lang, wiki, assets or changelog. Textures are generated in this repo (steel shell, cyan charge mark). The fabricator screen reuses Nerospace's own oxygen-generator panel.
