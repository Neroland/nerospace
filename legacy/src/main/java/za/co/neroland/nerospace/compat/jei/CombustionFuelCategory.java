package za.co.neroland.nerospace.compat.jei;

import java.util.List;
import java.util.Locale;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * JEI category listing what the Combustion Generator burns and for how much. Fuels and burn values
 * come from {@link CombustionGeneratorBlockEntity#knownFuels()} /
 * {@link CombustionGeneratorBlockEntity#fuelValue(ItemStack)}; the FE total uses the live
 * {@link Tuning} per-tick rate, so config multipliers are reflected.
 */
public class CombustionFuelCategory extends AbstractRecipeCategory<CombustionFuelCategory.CombustionFuel> {

    public static final IRecipeType<CombustionFuel> TYPE =
            IRecipeType.create(Nerospace.MODID, "combustion_fuel", CombustionFuel.class);

    /** One accepted fuel and its burn duration in ticks. */
    public record CombustionFuel(ItemStack fuel, int burnTicks) {
    }

    /** Every accepted fuel, paired with its burn value straight from the generator's lookup. */
    public static List<CombustionFuel> allFuels() {
        return CombustionGeneratorBlockEntity.knownFuels().stream()
                .map(stack -> new CombustionFuel(stack, CombustionGeneratorBlockEntity.fuelValue(stack)))
                .toList();
    }

    public CombustionFuelCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.nerospace.category.combustion"),
                guiHelper.createDrawableItemLike(ModBlocks.COMBUSTION_GENERATOR.get()), 90, 52);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CombustionFuel recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 5).setStandardSlotBackground().add(recipe.fuel());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CombustionFuel recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlame(recipe.burnTicks()).setPosition(30, 7);
        builder.addText(List.of(
                        Component.translatable("jei.nerospace.stat.energy_generated",
                                String.format(Locale.ROOT, "%,d",
                                        (long) recipe.burnTicks() * Tuning.combustionGeneratorFePerTick())),
                        Component.translatable("jei.nerospace.stat.time",
                                String.format(Locale.ROOT, "%.1f", recipe.burnTicks() / 20.0))),
                        getWidth(), 20)
                .setPosition(0, 32)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF808080);
    }
}
