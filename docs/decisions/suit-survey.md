# Suit survey (Milestone S, stage S0)

Checked against the tree at 2.4.2 before this branch. Where the brief and the source disagree, the source wins.

## What the source actually is

- The version axis is **27 cells**: Minecraft 1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2, 26.3, each on Fabric, Forge and NeoForge (`gradle.properties` `mc_versions`, `settings.gradle`). `AGENTS.md` still says nine 26.x cells. That file is stale.
- `mod_version` was **2.4.2**. Milestone A (liveries, 2.5.0) is not in this tree. The owner locked S as **2.6.0-beta.1** anyway, so the version moves from 2.4.2 to 2.6.0-beta.1 and 2.5.0 is skipped.
- Neroland Core pin is **1.20.1**. The published `nerolandcore-neoforge-26.3-1.20.1.jar` contains `energy.ItemEnergyLookup` and `ItemEnergyProvider`. Core's changelog lists them under 1.20.1. The class javadoc still says "Core 1.21.0". No Core release is required.
- There is no `RegistryIdFreezeTest` or `AdvancementIdFreezeTest` in `common/src/test`. The freezes that exist are `StarGuideLayoutFreezeTest` and `ConfigSchemaCompatTest`. This milestone extends those and adds suit tests, rather than inventing a freeze file the tree does not have.

## Suit readers

Code that decides what a worn suit does, all now routed through `SuitView` where they read a piece:

- `OxygenManager` — full suit, piece count, oxygen capacity and drain, `suitTier`, `suitClass`, `hazardShield`, `shields`, `radLined`, `isRadLined`.
- `PlanetHazards` — calls `OxygenManager.suitClass` and `radLined`.
- `BlockSuits` — sealed boots, heat boots, sealed helmet.
- `api.NerospaceSuits` — radiation queries, plus the new read methods.
- `client.OxygenHud` — tier and shield badges from `OxygenManager`.
- `link.NerospaceLinkSnapshots.lifeSupport` — `suit_tier` and `hazard_shield`, plus additive frame, energy and module fields. `SCHEMA_VERSION` stays 4.
- `village.AlienTrades` — culture tables sell `suit_*` with that world's weave or Rad Lining preinstalled. A saved villager offer keeps its old output and converts when the stack reaches a player.
- `progression.StarGuide` vacuum chapter — step ids kept, icons moved, three steps appended.
- Guide advancements `guide/oxygen_suit`, `oxygen_suit_t2`, `thermal_suit`, `cryo_suit` — criterion names kept.
- NeroQuests `breathe_easy` — collects the four new pieces.
- Tags `#nerospace:suits/radiation_capable` (old twelve kept, four new added), `#nerospace:suits/pieces` and the per-slot tags.
- Recipes: the sixteen `oxygen_suit_*` recipe files stay. Tier 1 results are now `suit_*`. The twelve rad-lining smithing recipes install the lining on `suit_*` and still set `nerospace:rad_lining`.

## Armour, assets, inventory tick, recipes

- **1.21.5+** armour is a plain `Item` whose properties call `humanoidArmor`. The Pathfinder piece does the same and sets durability to 100,000 so one hit cannot break it. Breach is the virtual durability (material multiplier × the vanilla slot factor) minus one.
- **1.21.2–1.21.4** armour is `ArmorItem(material, type, properties)`. `SuitPieceItem` extends `ArmorItem` on those lines.
- **1.21.1** has no equipment-asset component. The 1.21.1 registration still needs the legacy `ArmorMaterial` holder constructor. The shared texture is `textures/entity/equipment/*/suit.png`, copied from the Tier 1 sheet. Per-frame tint is the documented fallback and is not a separate asset on that line.
- **1.21.2+** equipment assets: `assets/nerospace/equipment/suit.json`. One asset for the first pass. Per-frame asset swapping via the equippable component is recorded as follow-up; the item id is one, the model is one.
- `inventoryTick` is ` (ItemStack, Level, Entity, int, boolean)` before 1.21.5 and `(ItemStack, ServerLevel, Entity, EquipmentSlot)` from 1.21.5. Conversion does not use it. It runs from the existing server player tick, which is the first server tick a stack spends in a player's inventory, and it never runs for a client world. Armour stands, mobs and chests are not scanned.
- Vanilla `crafting_transmute` is not used. 1.21.1 does not have it, and the shaped Tier 1 recipes do not need to copy components (they create a new Mk I piece). Frame upgrades that must keep enchantments go through the Suit Fabricator, which copies the stack. The old Tier 2 / Thermal / Cryo shaped recipes still output the legacy items; those convert on the next player tick. That is weaker than a component-copying crafting recipe. It is recorded as a residual risk.

## Movement checks

Vanilla kicks a flying player from `ServerGamePacketListenerImpl.handleMovePlayer` with `multiplayer.disconnect.flying`, and a player who moves too far with `multiplayer.disconnect.moved_too_quickly`. The method name is stable across these lines. The field that counts airborne ticks is not. `MovementAllowance` is the tested service: off unless flight is active, and `withinCap` rejects anything above the suit's own speed. A mixin with `require = 0` is the intended hook and is not in this branch yet, because a wrong accessor crashes the game and the mixin target was not javap'd per cell. Flight still zeroes fall distance and sets velocity on the server. The kick suppression is a residual risk until that mixin is confirmed on each cell.

## Advancement criteria

`AdvancementProgress` stores the time a criterion was obtained under the criterion's name. That map has been the save format since advancements existed, and nothing in 1.21.1 through 26.3 replaced it. Keeping `has_suit`, `has_thermal_suit` and the other names keeps a completed advancement completed. The conditions now also accept the new items through an extra OR criterion, so a player who never had the old suit can still earn the step.

## Numbers used

The brief's first-pass table, locked by the owner. Worked cruise at 60 FE/t × max(g, 0.25), Mk IV with two Power Cells (1,000,000 FE) before the 3% reserve:

- Earth 1.0 g: 60 FE/t, about 16,166 ticks, about 13.5 minutes.
- Mars 0.38 g: about 23 FE/t, about 35 minutes.
- Luna, floored at 0.25 g: 15 FE/t, about 54 minutes.

Oxygen: bare 300 / drain 30, suit 900 / drain 3, tank +450 (max 2), reclaimer ×0.67 (max 2, floor ×0.5), hazard ×4. A legacy suit with no modules is still 900 and 3.
