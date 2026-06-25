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


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.machine.GrinderRecipes;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * JEI category for the Nerosium Grinder's in-code recipes ({@link GrinderRecipes}). Layout: input slot →
 * animated arrow → output slot, with the FE cost and processing time printed underneath. The numbers
 * derive from the grinder's own constants + the live {@link NerospaceConfig} machine-speed multiplier
 * (no {@code Tuning} class in the multiloader — values are inlined per machine).
 */
public class GrindingCategory extends AbstractRecipeCategory<GrinderRecipes.Grinding> {

    public static final IRecipeType<GrinderRecipes.Grinding> TYPE =
            IRecipeType.create(NerospaceCommon.MOD_ID, "grinding", GrinderRecipes.Grinding.class);

    public GrindingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.nerospace.category.grinding"),
                guiHelper.createDrawableItemLike(ModBlocks.NEROSIUM_GRINDER.get()), 90, 52);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrinderRecipes.Grinding recipe,
            IFocusGroup focuses) {
        GrinderRecipes.Grinding checkedRecipe = NerospaceCommon.requireNonNull(recipe);
        builder.addInputSlot(1, 5).setStandardSlotBackground().add(checkedRecipe.input());
        builder.addOutputSlot(66, 5).setOutputSlotBackground().add(checkedRecipe.output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GrinderRecipes.Grinding recipe,
            IFocusGroup focuses) {
        NerospaceCommon.requireNonNull(recipe);
        int ticks = NerospaceConfig.scaleInterval(NerosiumGrinderBlockEntity.MAX_PROGRESS,
                NerospaceConfig.machineSpeedMultiplier());
        builder.addAnimatedRecipeArrow(ticks).setPosition(28, 5);
        builder.addText(List.of(
                        Component.translatable("jei.nerospace.stat.energy_cost",
                                String.format(Locale.ROOT, "%,d",
                                        (long) ticks * NerosiumGrinderBlockEntity.ENERGY_PER_TICK)),
                        Component.translatable("jei.nerospace.stat.time",
                                String.format(Locale.ROOT, "%.1f", ticks / 20.0))),
                        getWidth(), 20)
                .setPosition(0, 32)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF808080);
    }
}
