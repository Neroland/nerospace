package za.co.neroland.nerospace.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.GrinderRecipes;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * EMI integration — the native counterpart of {@code compat.jei.NerospaceJeiPlugin}, with the same three
 * pages: grinding, fuel refining and combustion fuel values. Standard crafting/smelting recipes need no
 * code; EMI reads those itself.
 *
 * <p>Built against the community "EMI Unofficial Port" (official EMI has no Minecraft 26.x release), which
 * keeps the upstream {@code dev.emi.emi.api} package unchanged. This class uses only that API, so it lives
 * in {@code common}: NeoForge discovers it through {@link EmiEntrypoint}, Fabric through the {@code emi}
 * entrypoint in {@code fabric.mod.json}. EMI is a compile-time-only dependency — without it this class is
 * never loaded.</p>
 *
 * <p>With a native plugin present, EMI's JEI bridge (JEMI) skips JEI categories in the {@code nerospace}
 * namespace, so running JEI and EMI together does not show these pages twice.</p>
 */
@EmiEntrypoint
public class NerospaceEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory GRINDING =
            category("grinding", EmiStack.of(ModBlocks.NEROSIUM_GRINDER.get()));
    public static final EmiRecipeCategory REFINING =
            category("refining", EmiStack.of(ModBlocks.FUEL_REFINERY.get()));
    public static final EmiRecipeCategory COMBUSTION =
            category("combustion_fuel", EmiStack.of(ModBlocks.COMBUSTION_GENERATOR.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(GRINDING);
        registry.addCategory(REFINING);
        registry.addCategory(COMBUSTION);

        registry.addWorkstation(GRINDING, EmiStack.of(ModBlocks.NEROSIUM_GRINDER.get()));
        registry.addWorkstation(REFINING, EmiStack.of(ModBlocks.FUEL_REFINERY.get()));
        registry.addWorkstation(COMBUSTION, EmiStack.of(ModBlocks.COMBUSTION_GENERATOR.get()));

        // Same machine-side sources as the JEI plugin, so the two viewers can never disagree. Nothing here
        // may touch a compat.jei class: those extend JEI types and would fail to load when only EMI is
        // installed.
        for (GrinderRecipes.Grinding grinding : GrinderRecipes.all()) {
            registry.addRecipe(new GrindingEmiRecipe(grinding));
        }
        registry.addRecipe(new GrindingEmiRecipe(GrinderRecipes.meteor()));
        registry.addRecipe(new RefiningEmiRecipe());
        for (ItemStack fuel : CombustionGeneratorBlockEntity.knownFuels()) {
            registry.addRecipe(new CombustionFuelEmiRecipe(fuel, CombustionGeneratorBlockEntity.fuelValue(fuel)));
        }
    }

    private static EmiRecipeCategory category(String path, EmiStack icon) {
        return new EmiRecipeCategory(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path), icon);
    }

    /**
     * A synthetic recipe id. EMI's convention is a leading {@code /} in the path for recipes that do not
     * come from a datapack, so they never collide with real recipe ids.
     */
    static Identifier syntheticId(String path) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "/" + path);
    }
}
