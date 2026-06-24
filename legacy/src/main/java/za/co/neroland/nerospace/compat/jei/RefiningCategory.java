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
import net.minecraft.world.item.Items;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * JEI category for the Fuel Refinery's single in-code process: carbon (coal/charcoal) + blaze
 * powder + grid power → liquid rocket fuel. Inputs/outputs mirror
 * {@link FuelRefineryBlockEntity}'s slot filters and batch maths; the FE cost and cycle time are
 * live {@link Tuning} values, so config multipliers are reflected.
 */
public class RefiningCategory extends AbstractRecipeCategory<RefiningCategory.RefiningRecipe> {

    public static final IRecipeType<RefiningRecipe> TYPE =
            IRecipeType.create(Nerospace.MODID, "refining", RefiningRecipe.class);

    /** The refinery's one recipe: any carbon stack + any catalyst stack → one tank batch. */
    public record RefiningRecipe(List<ItemStack> carbon, List<ItemStack> catalyst) {
        /** Mirrors {@link FuelRefineryBlockEntity}'s slot filters (coal/charcoal + blaze powder). */
        public static RefiningRecipe standard() {
            return new RefiningRecipe(
                    List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL)),
                    List.of(new ItemStack(Items.BLAZE_POWDER)));
        }
    }

    public RefiningCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.nerospace.category.refining"),
                guiHelper.createDrawableItemLike(ModBlocks.FUEL_REFINERY.get()), 90, 62);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RefiningRecipe recipe, IFocusGroup focuses) {
        int mb = Tuning.fuelRefineryMbPerBatch();
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.carbon());
        builder.addInputSlot(1, 21).setStandardSlotBackground().addItemStacks(recipe.catalyst());
        builder.addOutputSlot(66, 11).setStandardSlotBackground()
                .setFluidRenderer(Math.max(1, mb), false, 16, 16)
                .add(ModFluids.ROCKET_FUEL.get(), mb);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RefiningRecipe recipe, IFocusGroup focuses) {
        int ticks = Tuning.fuelRefineryWorkTicks();
        builder.addAnimatedRecipeArrow(ticks).setPosition(28, 11);
        builder.addText(List.of(
                        Component.translatable("jei.nerospace.stat.energy_cost",
                                String.format(Locale.ROOT, "%,d", (long) ticks * Tuning.fuelRefineryFePerTick())),
                        Component.translatable("jei.nerospace.stat.time",
                                String.format(Locale.ROOT, "%.1f", ticks / 20.0))),
                        getWidth(), 20)
                .setPosition(0, 42)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF808080);
    }
}
