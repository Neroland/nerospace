# Pathfinder Suit

One four-piece suit replaces the old Tier 1, Tier 2, Thermal and Cryo suits. You upgrade it by **frame** and fit it with **weaves**, **linings** and **modules**. It breathes Nerospace oxygen. Powered modules use Neroland Core energy (FE). Breathing and hazard shielding never use energy, so a flat battery cannot kill you.

## Pieces

| Piece | Id |
|---|---|
| Helmet | `nerospace:suit_helmet` |
| Chestplate | `nerospace:suit_chestplate` |
| Leggings | `nerospace:suit_leggings` |
| Boots | `nerospace:suit_boots` |

Life support needs all four pieces, and none of them breached. The set's frame is the **lowest** frame you are wearing.

## Frames

| Frame | Armour | Slots | Battery |
|---|---|---|---|
| Mk I — Pressure | today's Tier 1 | 1 module | 50,000 FE |
| Mk II — Cindrite | today's Tier 2 | 1 weave, 1 lining, 2 modules | 100,000 FE |
| Mk III — Titanium | Tier 2, +1 toughness | 1 weave, 2 linings, 2 modules (chest 3) | 250,000 FE |
| Mk IV — Voidglass | Tier 2, +2 toughness | 1 weave, 2 linings, 3 modules | 500,000 FE |

Craft **Frame Plating** and apply it at the [Suit Fabricator](Suit-Fabricator). The upgrade keeps everything already installed.

A full Mk II or higher set keeps out Mars-strength cold, the same as a Tier 2 suit did.

## Weaves and linings

A weave or lining shields you only when **all four** worn pieces carry it.

- **Thermal Weave** — heat. Made from Cindrite.
- **Cryo Weave** — all cold, including frigid worlds. Made from Glacite.
- **Adaptive Weave** — heat and cold together. Mk IV.
- **Rad Lining** — the existing item. Radiation, and it still sets `nerospace:rad_lining` so other mods that look for that component keep working.

See [Suit Modules](Suit-Modules) for the rest.

## Oxygen

A full suit holds 900 air, the same as before. Each Life Support Tank adds 450, up to 1,800. Each O₂ Reclaimer multiplies the suited drain by 0.67, and two of them never go below half. An unshielded hazard still multiplies drain by 4. The underwater rebreather is unchanged.

## Durability

A piece never breaks. At its last point of durability it is **Breached**: no armour, no modules, and it does not count toward the seal until you repair it at the fabricator or an anvil.

## Coming from the old suits

The sixteen old suit items still exist, so an old world loads. They still protect you. The first time you hold or wear one, it becomes the matching Pathfinder piece:

| Old suit | Becomes |
|---|---|
| Tier 1 | Mk I, no weave |
| Tier 2 | Mk II, no weave |
| Thermal | Mk II with Thermal Weave |
| Cryo | Mk II with Cryo Weave |
| Any of those with a Rad Lining | the same, plus a Rad Lining |

Enchantments, the custom name, the trim and the remaining durability come with it. The new chest starts empty of energy, which changes nothing the old suit could do. A suit left in a chest, on an armour stand or on a mob stays as it is until a player picks it up.

Servers can turn the conversion off with `suit.legacyConversion`. The old pieces still protect you either way.
