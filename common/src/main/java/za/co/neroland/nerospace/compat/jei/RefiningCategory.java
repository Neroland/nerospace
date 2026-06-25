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

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * JEI category for the Fuel Refinery's single in-code process: carbon (coal/charcoal) + blaze powder +
 * grid power → liquid rocket fuel. Inputs/outputs mirror {@link FuelRefineryBlockEntity}'s slot filters
 * and batch maths; the FE cost and cycle time derive from the refinery's constants + the live
 * {@link NerospaceConfig} machine-speed multiplier.
 */
public class RefiningCategory extends AbstractRecipeCategory<RefiningCategory.@NonNull RefiningRecipe> {

    public static final @NonNull IRecipeType<RefiningCategory.@NonNull RefiningRecipe> TYPE =
            IRecipeType.create(NerospaceCommon.MOD_ID, "refining", RefiningRecipe.class);

    /** The refinery's one recipe: any carbon stack + any catalyst stack → one tank batch. */
    public record RefiningRecipe(@NonNull List<ItemStack> carbon,
            @NonNull List<ItemStack> catalyst) {
        /** Mirrors {@link FuelRefineryBlockEntity}'s slot filters (coal/charcoal + blaze powder). */
        public static @NonNull RefiningRecipe standard() {
            return new RefiningRecipe(
                    NerospaceCommon.requireNonNull(List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL))),
                    NerospaceCommon.requireNonNull(List.of(new ItemStack(Items.BLAZE_POWDER))));
        }
    }

    public RefiningCategory(@NonNull IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.nerospace.category.refining"),
                guiHelper.createDrawableItemLike(ModBlocks.FUEL_REFINERY.get()), 90, 62);
    }

    @Override
    public void setRecipe(@NonNull IRecipeLayoutBuilder builder, RefiningRecipe recipe,
            @NonNull IFocusGroup focuses) {
        RefiningRecipe checkedRecipe = NerospaceCommon.requireNonNull(recipe);
        int mb = FuelRefineryBlockEntity.MB_PER_BATCH;
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(checkedRecipe.carbon());
        builder.addInputSlot(1, 21).setStandardSlotBackground().addItemStacks(checkedRecipe.catalyst());
        builder.addOutputSlot(66, 11).setStandardSlotBackground()
                .setFluidRenderer(Math.max(1, mb), false, 16, 16)
                .add(ModFluids.ROCKET_FUEL.get(), mb);
    }

    @Override
    public void createRecipeExtras(@NonNull IRecipeExtrasBuilder builder, RefiningRecipe recipe,
            @NonNull IFocusGroup focuses) {
        int ticks = NerospaceConfig.scaleInterval(FuelRefineryBlockEntity.WORK_TICKS,
                NerospaceConfig.machineSpeedMultiplier());
        builder.addAnimatedRecipeArrow(ticks).setPosition(28, 11);
        builder.addText(List.of(
                        Component.translatable("jei.nerospace.stat.energy_cost",
                                String.format(Locale.ROOT, "%,d",
                                        (long) ticks * FuelRefineryBlockEntity.FE_PER_TICK)),
                        Component.translatable("jei.nerospace.stat.time",
                                String.format(Locale.ROOT, "%.1f", ticks / 20.0))),
                        getWidth(), 20)
                .setPosition(0, 42)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF808080);
    }
}
