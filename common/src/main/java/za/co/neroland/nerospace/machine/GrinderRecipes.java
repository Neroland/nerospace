package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModItems;

/** In-code grinding recipes (ores/raw -> 2 dust; ingot -> 1 dust). Isolated for a later datapack swap. */
public final class GrinderRecipes {

    private GrinderRecipes() {
    }

    /** One input → output pairing, for display/integration (JEI). */
    public record Grinding(ItemStack input, ItemStack output) {
    }

    /** @return every grinding pairing, derived through {@link #getResult} so the display can't drift. */
    public static List<Grinding> all() {
        List<Grinding> recipes = new ArrayList<>();
        for (Item item : List.of(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get(),
                ModItems.RAW_NEROSIUM.get(), ModItems.NEROSIUM_INGOT.get())) {
            ItemStack input = new ItemStack(NerospaceCommon.requireNonNull(item));
            ItemStack output = getResult(input);
            if (!output.isEmpty()) {
                recipes.add(new Grinding(input, output));
            }
        }
        return NerospaceCommon.requireNonNull(List.copyOf(recipes));
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
