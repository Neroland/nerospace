package za.co.neroland.nerospace.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Fuel Refinery's one process on EMI: carbon (coal/charcoal) + blaze powder + grid power → liquid
 * rocket fuel. The inputs mirror {@link FuelRefineryBlockEntity}'s slot filters, as the JEI page does; the
 * FE cost and cycle time follow the live {@link NerospaceConfig} machine-speed multiplier.
 */
final class RefiningEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 60;

    RefiningEmiRecipe() {
        super(NerospaceEmiPlugin.REFINING, NerospaceEmiPlugin.syntheticId("refining/rocket_fuel"), WIDTH, HEIGHT);
        this.inputs = List.of(
                EmiIngredient.of(List.of(EmiStack.of(Items.COAL), EmiStack.of(Items.CHARCOAL))),
                EmiStack.of(Items.BLAZE_POWDER));
        this.outputs = List.of(fuelOutput());
    }

    /**
     * The refinery's output as a fluid stack. Same guard as the JEI page: on NeoForge a fluid stack is built
     * from the fluid's registry holder, whose data components may not be bound yet when the recipe viewer
     * reloads on the recipes packet — in that case show the rocket-fuel bucket instead of failing.
     */
    private static EmiStack fuelOutput() {
        Fluid fuel = ModFluids.ROCKET_FUEL.get();
        if (fuel.builtInRegistryHolder().areComponentsBound()) {
            return EmiStack.of(fuel, EmiStats.fluidAmount(FuelRefineryBlockEntity.MB_PER_BATCH));
        }
        return EmiStack.of(ModItems.ROCKET_FUEL_BUCKET.get());
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int ticks = NerospaceConfig.scaleInterval(FuelRefineryBlockEntity.WORK_TICKS,
                NerospaceConfig.machineSpeedMultiplier());

        widgets.addSlot(inputs.get(0), 4, 1);
        widgets.addSlot(inputs.get(1), 4, 21);
        widgets.addFillingArrow(28, 11, ticks * 50);
        widgets.addSlot(outputs.get(0), 62, 7).large(true).recipeContext(this);

        EmiStats.lines(widgets, WIDTH, 40, List.of(
                EmiStats.energyCost((long) ticks * FuelRefineryBlockEntity.FE_PER_TICK),
                EmiStats.seconds(ticks)));
    }
}
