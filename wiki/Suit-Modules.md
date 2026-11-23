# Suit modules

Modules are installed only at the [Suit Fabricator](Suit-Fabricator). You toggle an installed module from the suit; you cannot swap them in the field.

A module works from the piece it sits in, and only while you wear a full suit that is not breached. Weaves and linings are the exception that needs all four pieces to carry the same one. See [Pathfinder Suit](Oxygen-Suit).

| Module | Piece | From | What it does |
|---|---|---|---|
| Life Support Tank | chest | Mk I | +450 air. Two copies. No energy. |
| O₂ Reclaimer | chest | Mk II | Suited oxygen drain ×0.67 each, never below half. Two copies. No energy. |
| Power Cell | chest | Mk II | +50% battery each. Two copies. |
| Solar Skin | chest | Mk II | Charges in sunlight. |
| Kinetic Shield | chest | Mk III | Absorbs up to 4 damage a hit, 400 FE a point. Off while breached. |
| Jump-Jet | chest | Mk III | A short hover. Heat-limited. |
| Flight Harness | chest | Mk IV | Sustained flight, after deep space. Replaces the Jump-Jet. |
| Night Visor | helmet | Mk I | Night vision, 1 FE a tick. |
| Survey Scanner | helmet | Mk II | Marks world-generated ore near you. Not chests, not blocks you placed. |
| Spore Filter | helmet | Mk I | Ignores Greenxertz spore nausea. |
| Stride Servos | legs | Mk II | Faster while sprinting. |
| Leap Servos | legs | Mk II | A higher jump, scaled by gravity. |
| Shock Absorbers | boots | Mk I | Less fall damage. Two copies remove it. |
| Glide Fins | boots | Mk II | Slow fall, toggled. |

The Hardpoint is registered and does nothing. It is not crafted and does not appear in the creative tab. Later milestones add an EVA thruster, mag clamps, a tool link and a pressure lining as more of these modules, not as new armour.

## Flight

Hover (Jump-Jet) and flight (Flight Harness) spend more energy in higher gravity, and never less than a quarter of the Earth rate, so the Moon is not free. The last 3% of the battery lowers you to the ground and stops flight until you recharge.

`suit.flight.dimensionMode` is `all`, `nerospace_only` or `none` (default `all`). `suit.flight.dimensionDenylist` adds more dimensions. A station can refuse flight inside its own box. Taking damage from another player locks flight for `suit.flight.combatLockTicks` (default 60).
