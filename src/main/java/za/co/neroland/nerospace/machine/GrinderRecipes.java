package za.co.neroland.nerospace.machine;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;

/**
 * In-code grinding recipes for the Nerosium Grinder.
 *
 * <p><strong>Design note:</strong> Phase 2's definition of done only requires the machine to
 * process an input over time, persist, and expose energy via the capability — not a data-driven
 * recipe type. 26.1's custom {@code Recipe} interface is heavy ({@code CommonInfo},
 * {@code BookInfo}, {@code RecipeDisplay}, {@code PlacementInfo}, {@code RecipeBookCategory}), so
 * to keep this slice buildable and focused, grinding is defined here in code. This is deliberately
 * isolated behind a single {@link #getResult(ItemStack)} lookup so it can be swapped for a
 * datapack-driven {@code RecipeType} later without touching the block entity.</p>
 */
public final class GrinderRecipes {

    private GrinderRecipes() {
    }

    /**
     * @return the grinding result for the given input, or {@link ItemStack#EMPTY} if the input is
     *         not grindable. The returned stack is a fresh instance the caller may mutate.
     */
    public static ItemStack getResult(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // Ores and raw material grind into 2 dust.
        if (input.is(ModItems.NEROSIUM_ORE_ITEM.get())
                || input.is(ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get())
                || input.is(ModItems.RAW_NEROSIUM.get())) {
            return new ItemStack(ModItems.NEROSIUM_DUST.get(), 2);
        }
        // Ingots grind into 1 dust.
        if (input.is(ModItems.NEROSIUM_INGOT.get())) {
            return new ItemStack(ModItems.NEROSIUM_DUST.get(), 1);
        }
        return ItemStack.EMPTY;
    }

    /** @return whether the given input has a grinding result. */
    public static boolean isGrindable(ItemStack input) {
        return !getResult(input).isEmpty();
    }
}
