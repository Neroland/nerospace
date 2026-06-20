package za.co.neroland.nerospace.machine;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;

/** In-code grinding recipes (ores/raw -> 2 dust; ingot -> 1 dust). Isolated for a later datapack swap. */
public final class GrinderRecipes {

    private GrinderRecipes() {
    }

    public static ItemStack getResult(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.is(ModItems.NEROSIUM_ORE_ITEM.get())
                || input.is(ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get())
                || input.is(ModItems.RAW_NEROSIUM.get())) {
            return new ItemStack(ModItems.NEROSIUM_DUST.get(), 2);
        }
        if (input.is(ModItems.NEROSIUM_INGOT.get())) {
            return new ItemStack(ModItems.NEROSIUM_DUST.get(), 1);
        }
        return ItemStack.EMPTY;
    }
}
