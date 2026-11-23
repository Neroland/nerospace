# Suit Fabricator

The Suit Fabricator (`nerospace:suit_fabricator`) is where a Pathfinder Suit is fitted.

Put a suit piece in the left slot and a module, weave, lining or frame plating in the right slot, then use the install control. Removing a module puts it back in your inventory. Nothing is created or destroyed except the one item you meant to move.

It also:

- converts a legacy suit piece as soon as you put it in the slot
- applies Mk II, Mk III and Mk IV frame plating, in order, keeping what is already installed
- charges a chest piece from its energy buffer

Every face accepts Core energy (the input preset). The charge rate is `suit.fabricatorChargeRate` (default 2,000 FE per tick).

## Suit Dock

The Suit Dock (`nerospace:suit_dock`) is a floor pad. A player standing on it charges the suit chest they are wearing, from the pad's own energy buffer. Every face accepts Core energy. The rate is `suit.dockChargeRate` (default 500 FE per tick).

Recipe: eight Nerosteel Ingots around one Redstone dust.
