# Handover for NeroTech — suit charging

Nerospace 2.6.0-beta.1 registers the Pathfinder chest piece (`nerospace:suit_chestplate`) with Neroland Core's `ItemEnergyLookup`. A charger that asks `ItemEnergyLookup.find(stack)` can fill the suit without a Nerospace dependency and without a Nerospace-specific adapter.

Core 1.20.1 already ships that lookup. A comment that says Core has no item-energy API is out of date.

Optional, not required: the tag `nerospace:suit_components/circuit` is the slot where a NeroTech circuit could stand in for a Frame Circuit later. Nerospace does not read NeroTech.

No player data is involved. The energy value lives on the item.
