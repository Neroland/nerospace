package za.co.neroland.nerospace.suit;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModDataComponents;

/**
 * Server tick for a worn suit: legacy conversion, the breach clamp, and flushing the chest's energy onto its
 * component at most every 10 ticks. Runs from the same player tick as oxygen.
 */
public final class SuitTicker {

    private SuitTicker() {
    }

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }
        if (NerospaceConfig.suitLegacyConversion()) {
            convertInventory(player);
        }
        clampWorn(player);
        SuitFlight.tick(player);
        flush(player);
    }

    /** Replace a legacy stack in place. Idempotent. Never called for a client world. */
    public static ItemStack convert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!LegacySuitConversion.isLegacyId(key)) {
            return stack;
        }
        int max = Math.max(1, stack.getMaxDamage());
        LegacySuitConversion.Converted converted = LegacySuitConversion.convert(
                new LegacySuitConversion.LegacyPiece(key, stack.has(ModDataComponents.RAD_LINING.get()),
                        stack.getDamageValue(), max));
        if (converted == null) {
            return stack;
        }
        ItemStack next = new ItemStack(SuitItems.pieceItem(converted.piece()), stack.getCount());
        copyComponents(stack, next);
        SuitStacks.setLoadout(next, converted.loadout());
        next.set(DataComponents.MAX_DAMAGE, SuitDurability.ITEM_MAX);
        next.set(DataComponents.DAMAGE, converted.damage());
        if (converted.loadout().linings().contains(SuitIds.RAD_LINING)) {
            next.set(ModDataComponents.RAD_LINING.get(), Unit.INSTANCE);
        }
        SuitCoreState core = SuitStacks.core(next);
        if (converted.piece() == SuitPiece.CHEST && core.energy() != 0) {
            SuitStacks.setCore(next, core.withEnergy(0));
        }
        return next;
    }

    private static void copyComponents(ItemStack from, ItemStack to) {
        var enchantments = from.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            to.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        var name = from.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            to.set(DataComponents.CUSTOM_NAME, name);
        }
        var trim = from.get(DataComponents.TRIM);
        if (trim != null) {
            to.set(DataComponents.TRIM, trim);
        }
        var dyed = from.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            to.set(DataComponents.DYED_COLOR, dyed);
        }
    }

    private static void convertInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ItemStack next = convert(stack);
            if (next != stack) {
                inventory.setItem(slot, next);
            }
        }
    }

    private static void clampWorn(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof SuitPieceItem piece)) {
                continue;
            }
            int virtual = SuitDurability.max(piece.piece(), SuitStacks.loadout(stack).frameEnum());
            int clamped = SuitDurability.clamp(stack.getDamageValue(), virtual);
            if (clamped != stack.getDamageValue()) {
                stack.set(DataComponents.DAMAGE, clamped);
            }
        }
    }

    private static void flush(ServerPlayer player) {
        if (player.level().getGameTime() % 10 != 0) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof SuitPieceItem piece) || piece.piece() != SuitPiece.CHEST) {
            return;
        }
        SuitFlight.Live live = SuitFlight.live(player);
        if (live == null) {
            return;
        }
        SuitStacks.setCore(chest, new SuitCoreState(live.energy, live.rcs, live.heat));
    }
}
