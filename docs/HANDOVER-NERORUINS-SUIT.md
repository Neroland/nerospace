# Handover for NeroRuins — suit loot

Nerospace 2.6.0 keeps the old suit ids, so a loot table that still lists `nerospace:oxygen_suit_helmet`, `nerospace:oxygen_suit_boots` or `nerospace:rad_lining` keeps resolving. Those pieces convert when a player picks them up.

When you next touch the oxygen-supply loot tag, prefer:

- `nerospace:suit_helmet`
- `nerospace:suit_boots`
- or the tag `#nerospace:suits/pieces`

`nerospace:rad_lining` is still a live item. It is the lining module.

Nothing in Nerospace reads NeroRuins.
