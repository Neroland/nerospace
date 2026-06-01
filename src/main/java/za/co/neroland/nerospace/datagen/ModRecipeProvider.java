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
import net.minecraft.world.item.crafting.CookingBookCategory;
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
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_raw_nerosium", this.has(ModItems.RAW_NEROSIUM))
                .save(this.output, "nerosium_ingot_from_smelting_raw_nerosium");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_NEROSIUM),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_raw_nerosium", this.has(ModItems.RAW_NEROSIUM))
                .save(this.output, "nerosium_ingot_from_blasting_raw_nerosium");

        // --- Smelting & blasting: ores -> ingot -----------------------------
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_nerosium_ore", this.has(ModBlocks.NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_smelting_nerosium_ore");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_nerosium_ore", this.has(ModBlocks.NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_blasting_nerosium_ore");

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_deepslate_nerosium_ore", this.has(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()))
                .save(this.output, "nerosium_ingot_from_smelting_deepslate_nerosium_ore");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
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

        // --- Machines -------------------------------------------------------
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModBlocks.NEROSIUM_GRINDER.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', ModItems.NEROSIUM_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_nerosium_ingot", this.has(ModItems.NEROSIUM_INGOT))
                .save(this.output);

        // Dust smelts/blasts back into an ingot (closes the processing loop).
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.NEROSIUM_DUST),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 200)
                .unlockedBy("has_nerosium_dust", this.has(ModItems.NEROSIUM_DUST))
                .save(this.output, "nerosium_ingot_from_smelting_nerosium_dust");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.NEROSIUM_DUST),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSIUM_INGOT, 0.7F, 100)
                .unlockedBy("has_nerosium_dust", this.has(ModItems.NEROSIUM_DUST))
                .save(this.output, "nerosium_ingot_from_blasting_nerosium_dust");

        // === Phase 3 — Greenxertz materials ================================

        // Nerosteel: smelt/blast raw and ore into ingots.
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_NEROSTEEL),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSTEEL_INGOT, 0.8F, 200)
                .unlockedBy("has_raw_nerosteel", this.has(ModItems.RAW_NEROSTEEL))
                .save(this.output, "nerosteel_ingot_from_smelting_raw_nerosteel");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_NEROSTEEL),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSTEEL_INGOT, 0.8F, 100)
                .unlockedBy("has_raw_nerosteel", this.has(ModItems.RAW_NEROSTEEL))
                .save(this.output, "nerosteel_ingot_from_blasting_raw_nerosteel");

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.NEROSTEEL_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSTEEL_INGOT, 0.8F, 200)
                .unlockedBy("has_nerosteel_ore", this.has(ModBlocks.NEROSTEEL_ORE.get()))
                .save(this.output, "nerosteel_ingot_from_smelting_nerosteel_ore");

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.NEROSTEEL_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.NEROSTEEL_INGOT, 0.8F, 100)
                .unlockedBy("has_nerosteel_ore", this.has(ModBlocks.NEROSTEEL_ORE.get()))
                .save(this.output, "nerosteel_ingot_from_blasting_nerosteel_ore");

        // Nerosteel storage block pack / unpack.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.NEROSTEEL_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.NEROSTEEL_INGOT)
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
                .save(this.output);

        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModItems.NEROSTEEL_INGOT, 9)
                .requires(ModBlocks.NEROSTEEL_BLOCK.get())
                .unlockedBy("has_nerosteel_block", this.has(ModBlocks.NEROSTEEL_BLOCK.get()))
                .save(this.output, "nerosteel_ingot_from_nerosteel_block");

        // Xertz quartz drops directly, but the ore can also be smelted (nether-quartz parity).
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.XERTZ_QUARTZ_ORE.get()),
                        RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.XERTZ_QUARTZ, 0.2F, 200)
                .unlockedBy("has_xertz_quartz_ore", this.has(ModBlocks.XERTZ_QUARTZ_ORE.get()))
                .save(this.output, "xertz_quartz_from_smelting_xertz_quartz_ore");

        // NOTE: the Greenxertz Navigator (Phase 3) is intentionally left WITHOUT a survival recipe in
        // Phase 4 — it is now a creative-only legacy item, replaced by the rocket flow below.

        // === Phase 4 — Rockets =============================================

        // Launch pad: a nerosteel frame around a nerosium core.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModBlocks.ROCKET_LAUNCH_PAD.get())
                .pattern("NNN")
                .pattern("NBN")
                .pattern("NNN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('B', ModBlocks.NEROSIUM_BLOCK.get())
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
                .save(this.output);

        // Fuel Tank: a nerosteel-framed glass tank around a fuel canister core.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModBlocks.FUEL_TANK.get())
                .pattern("NGN")
                .pattern("GCG")
                .pattern("NGN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('G', Items.GLASS)
                .define('C', ModItems.ROCKET_FUEL_CANISTER)
                .unlockedBy("has_rocket_fuel_canister", this.has(ModItems.ROCKET_FUEL_CANISTER))
                .save(this.output);

        // Oxygen Generator: a sealed nerosteel pump (glass dome, redstone, fuel canister core).
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModBlocks.OXYGEN_GENERATOR.get())
                .pattern("NGN")
                .pattern("RCR")
                .pattern("NNN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('G', Items.GLASS)
                .define('R', Items.REDSTONE)
                .define('C', ModItems.ROCKET_FUEL_CANISTER)
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
                .save(this.output);

        // Rocket fuel canister: blaze powder + coal + xertz quartz in an iron shell (yields 2).
        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModItems.ROCKET_FUEL_CANISTER, 2)
                .requires(Items.BLAZE_POWDER)
                .requires(Items.COAL)
                .requires(ModItems.XERTZ_QUARTZ)
                .requires(Items.IRON_INGOT)
                .unlockedBy("has_xertz_quartz", this.has(ModItems.XERTZ_QUARTZ))
                .save(this.output);

        // Tier 1 rocket: nerosteel hull + nerosium-grade core, a fuel canister, and a nerosteel engine.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.TOOLS, ModItems.ROCKET_TIER_1)
                .pattern(" N ")
                .pattern("NCN")
                .pattern("NBN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('C', ModItems.ROCKET_FUEL_CANISTER)
                .define('B', ModBlocks.NEROSTEEL_BLOCK.get())
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
                .save(this.output);

        // Tier 2 rocket: upgrades a Tier 1 with a larger tank + engine.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.TOOLS, ModItems.ROCKET_TIER_2)
                .pattern("NTN")
                .pattern("NCN")
                .pattern("NBN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('T', ModItems.ROCKET_TIER_1)
                .define('C', ModItems.ROCKET_FUEL_CANISTER)
                .define('B', ModBlocks.NEROSTEEL_BLOCK.get())
                .unlockedBy("has_rocket_tier_1", this.has(ModItems.ROCKET_TIER_1))
                .save(this.output);

        // Tier 3 rocket: upgrades a Tier 2, gated behind Station hull plating (reachable via the
        // Tier-1 Orbital Station). NOTE: cindrite is no longer a gate — it sits on Cindara, which a
        // Tier-3 rocket is what reaches, so gating Tier 3 on it would be circular.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.TOOLS, ModItems.ROCKET_TIER_3)
                .pattern("NTN")
                .pattern("DCD")
                .pattern("NBN")
                .define('N', ModItems.NEROSTEEL_INGOT)
                .define('T', ModItems.ROCKET_TIER_2)
                .define('C', ModItems.ROCKET_FUEL_CANISTER)
                .define('D', ModBlocks.STATION_WALL.get())
                .define('B', ModBlocks.NEROSTEEL_BLOCK.get())
                .unlockedBy("has_station_wall", this.has(ModBlocks.STATION_WALL.get()))
                .save(this.output);

        // === Phase 7 — Cindara materials ===================================

        // Cindrite storage block pack / unpack.
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.CINDRITE_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.CINDRITE)
                .unlockedBy("has_cindrite", this.has(ModItems.CINDRITE))
                .save(this.output);

        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.MISC, ModItems.CINDRITE, 9)
                .requires(ModBlocks.CINDRITE_BLOCK.get())
                .unlockedBy("has_cindrite_block", this.has(ModBlocks.CINDRITE_BLOCK.get()))
                .save(this.output, "cindrite_from_cindrite_block");

        // === Phase 7c — station building blocks ============================
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.STATION_FLOOR.get(), 8)
                .pattern("###")
                .pattern("# #")
                .pattern("###")
                .define('#', ModItems.NEROSTEEL_INGOT)
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
                .save(this.output);

        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM),
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.STATION_WALL.get(), 8)
                .pattern("###")
                .pattern("#I#")
                .pattern("###")
                .define('#', ModItems.NEROSTEEL_INGOT)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_nerosteel_ingot", this.has(ModItems.NEROSTEEL_INGOT))
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

        @Override
        public String getName() {
            return "Nerospace recipes";
        }
    }
}
