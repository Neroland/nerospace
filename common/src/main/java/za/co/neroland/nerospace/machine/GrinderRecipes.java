package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;

/** In-code grinding recipes (ores/raw -> 2 dust; ingot -> 1 dust). Isolated for a later datapack swap. */
public final class GrinderRecipes {

    private GrinderRecipes() {
    }

    /**
     * One grinder process, for display/integration (JEI). A fixed recipe pairs one {@code input} with a
     * deterministic {@code output}. The special {@code meteor} entry instead represents the random
     * meteor-block path (resolved through Neroland Core's Meteor Material Registry at runtime): its
     * {@code output} is empty and the JEI category renders the {@code neroland:meteor/grindable} pool.
     */
    public record Grinding(ItemStack input, ItemStack output, boolean meteor) {

        public Grinding(ItemStack input, ItemStack output) {
            this(input, output, false);
        }
    }

    /** The display entry for the random meteor-block grind (output is resolved live from Core). */
    public static Grinding meteor() {
        return new Grinding(new ItemStack(ModItems.METEOR_ROCK_ITEM.get()), ItemStack.EMPTY, true);
    }

    /** @return every grinding pairing, derived through {@link #getResult} so the display can't drift. */
    public static List<Grinding> all() {
        List<Grinding> recipes = new ArrayList<>();
        for (Item item : List.of(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get(),
                ModItems.RAW_NEROSIUM.get(), ModItems.NEROSIUM_INGOT.get())) {
            ItemStack input = new ItemStack(item);
            ItemStack output = getResult(input);
            if (!output.isEmpty()) {
                recipes.add(new Grinding(input, output));
            }
        }
        return List.copyOf(recipes);
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
