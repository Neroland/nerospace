package za.co.neroland.nerospace.suit;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModDataComponents;

/**
 * One read path for a worn or held suit piece, legacy or new. Legacy ids are understood through
 * {@link LegacySuitConversion}; they do not have to be converted first.
 */
public final class SuitView {

    private SuitView() {
    }

    public static boolean isSuitStack(ItemStack stack) {
        return slot(stack).suit();
    }

    public static SuitProtection.Slot slot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SuitProtection.Slot.empty();
        }
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean rad = stack.has(ModDataComponents.RAD_LINING.get());
        if (LegacySuitConversion.isLegacyId(key)) {
            int max = Math.max(1, stack.getMaxDamage());
            boolean breached = SuitDurability.breached(stack.getDamageValue(), max);
            return SuitProtection.Slot.legacy(key, rad, breached);
        }
        if (stack.getItem() instanceof SuitPieceItem piece) {
            SuitLoadout loadout = SuitStacks.loadout(stack);
            int virtual = SuitDurability.max(piece.piece(), loadout.frameEnum());
            boolean breached = SuitDurability.breached(stack.getDamageValue(), virtual);
            return SuitProtection.Slot.modern(loadout, breached, rad || loadout.linings().contains(SuitIds.RAD_LINING));
        }
        return SuitProtection.Slot.empty();
    }

    public static SuitPiece piece(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof SuitPieceItem item) {
            return item.piece();
        }
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return LegacySuitConversion.piece(key);
    }

    public static SuitProtection.Reading worn(LivingEntity entity) {
        if (entity == null) {
            return SuitProtection.Reading.NONE;
        }
        return SuitProtection.read(slot(entity.getItemBySlot(EquipmentSlot.HEAD)),
                slot(entity.getItemBySlot(EquipmentSlot.CHEST)),
                slot(entity.getItemBySlot(EquipmentSlot.LEGS)),
                slot(entity.getItemBySlot(EquipmentSlot.FEET)));
    }

    public static int pieceCount(Player player) {
        if (player == null) {
            return 0;
        }
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (slot(player.getItemBySlot(slot)).suit() && !slot(player.getItemBySlot(slot)).breached()) {
                n++;
            }
        }
        return n;
    }

    public static boolean fullSuit(Player player) {
        return worn(player).suit() != za.co.neroland.nerospace.world.planet.HazardOrderRules.SuitClass.NONE;
    }

    public static int chestModules(Player player, String id) {
        if (player == null) {
            return 0;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        SuitProtection.Slot slot = slot(chest);
        if (!slot.suit() || slot.breached() || slot.loadout() == null) {
            return 0;
        }
        return slot.loadout().moduleCount(id);
    }
}
