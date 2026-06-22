package za.co.neroland.nerospace.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.machine.GrinderRecipes;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * JEI integration. Standard crafting/smelting recipes and tags need no code — JEI reads them from the
 * recipe manager — so this plugin only surfaces the three in-code machine processes JEI cannot see on
 * its own: grinding, fuel refining and combustion fuel values.
 *
 * <p>Cross-loader port: NeoForge-only (the JEI API is a {@code compileOnly} NeoForge dependency), so it
 * lives in the {@code neoforge} source set. JEI is a soft dependency — this class is only classloaded by
 * JEI's own {@code @JeiPlugin} annotation scan, so the mod runs unchanged without JEI installed.</p>
 */
@JeiPlugin
public class NerospaceJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new GrindingCategory(guiHelper),
                new RefiningCategory(guiHelper),
                new CombustionFuelCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GrindingCategory.TYPE, GrinderRecipes.all());
        registration.addRecipes(RefiningCategory.TYPE, List.of(RefiningCategory.RefiningRecipe.standard()));
        registration.addRecipes(CombustionFuelCategory.TYPE, CombustionFuelCategory.allFuels());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(GrindingCategory.TYPE, ModBlocks.NEROSIUM_GRINDER.get());
        registration.addCraftingStation(RefiningCategory.TYPE, ModBlocks.FUEL_REFINERY.get());
        registration.addCraftingStation(CombustionFuelCategory.TYPE, ModBlocks.COMBUSTION_GENERATOR.get());
    }
}
