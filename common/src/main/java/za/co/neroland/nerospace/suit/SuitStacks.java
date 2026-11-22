package za.co.neroland.nerospace.suit;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModDataComponents;

/** Read and write the suit components. Missing components are the empty Mk I / empty core. */
public final class SuitStacks {

    private SuitStacks() {
    }

    public static SuitLoadout loadout(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SuitLoadout.EMPTY;
        }
        SuitLoadout stored = stack.get(ModDataComponents.SUIT_LOADOUT.get());
        return stored == null ? SuitLoadout.EMPTY : stored;
    }

    public static void setLoadout(ItemStack stack, SuitLoadout loadout) {
        stack.set(ModDataComponents.SUIT_LOADOUT.get(), loadout == null ? SuitLoadout.EMPTY : loadout);
        if (loadout != null && loadout.linings().contains(SuitIds.RAD_LINING)) {
            stack.set(ModDataComponents.RAD_LINING.get(), net.minecraft.util.Unit.INSTANCE);
        }
    }

    public static SuitCoreState core(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SuitCoreState.EMPTY;
        }
        SuitCoreState stored = stack.get(ModDataComponents.SUIT_CORE.get());
        return stored == null ? SuitCoreState.EMPTY : stored;
    }

    public static void setCore(ItemStack stack, SuitCoreState core) {
        stack.set(ModDataComponents.SUIT_CORE.get(), core == null ? SuitCoreState.EMPTY : core);
    }
}
