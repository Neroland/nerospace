package za.co.neroland.nerospace.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Recipes for the nerosium material chain: smelting/blasting raw and ore into ingots, 3x3
 * packing/unpacking for both storage blocks, and a nerosium pickaxe.
 */
public class ModRecipeProvider extends RecipeProvider {

    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // --- Smelting & blasting: raw nerosium -> ingot ---------------------
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_NEROSIUM),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_raw_nerosium", this.has(ModItems.RAW_NEROSIUM))
                .save(this.output, "nerosium_ingot_from_smelting_raw_nerosium");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_NEROSIUM),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_raw_nerosium", this.has(ModItems.RAW_NEROSIUM))
                .save(this.output, "nerosium_ingot_from_blasting_raw_nerosium");

        // --- Smelting & blasting: ores -> ingot -----------------------------
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_nerosium_ore", this.has(ModBlocks.NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_smelting_nerosium_ore");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_nerosium_ore", this.has(ModBlocks.NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_blasting_nerosium_ore");

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_deepslate_nerosium_ore", this.has(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_smelting_deepslate_nerosium_ore");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_deepslate_nerosium_ore", this.has(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_blasting_deepslate_nerosium_ore");

        // --- Storage block packing / unpacking ------------------------------
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.NEROSIUM_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.NEROSIUM_INGOT)
                .unlockedBy("has_nerosium_ingot", this.has(ModItems.NEROSIUM_INGOT))
                .save(this.output);

        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModItems.NEROSIUM_INGOT, 9)
                .requires(ModBlocks.NEROSIUM_BLOCK.get())
                .unlockedBy("has_nerosium_block", this.has(ModBlocks.NEROSIUM_BLOCK.get()))
                .save(this.output, "nerosium_ingot_from_nerosium_block");

        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.RAW_NEROSIUM_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.RAW_NEROSIUM)
                .unlockedBy("has_raw_nerosium", this.has(ModItems.RAW_NEROSIUM))
                .save(this.output);

        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModItems.RAW_NEROSIUM, 9)
                .requires(ModBlocks.RAW_NEROSIUM_BLOCK.get())
                .unlockedBy("has_raw_nerosium_block", this.has(ModBlocks.RAW_NEROSIUM_BLOCK.get()))
                .save(this.output, "raw_nerosium_from_raw_nerosium_block");

        // --- Tools ----------------------------------------------------------
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.TOOLS, ModItems.NEROSIUM_PICKAXE)
                .pattern("###")
                .pattern(" | ")
                .pattern(" | ")
                .define('#', ModItems.NEROSIUM_INGOT)
                .define('|', Items.STICK)
                .unlockedBy("has_nerosium_ingot", this.has(ModItems.NEROSIUM_INGOT))
                .save(this.output);
    }

    /** Runner registered with the data generator; constructs the provider per the lookup future. */
    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }
    }
}
