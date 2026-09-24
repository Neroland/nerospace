package za.co.neroland.nerospace.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;

/**
 * One Combustion Generator fuel on EMI: the item, a burning flame for its burn time, and the total FE it
 * yields. Fuels and burn values come from the generator's own lookup
 * ({@link CombustionGeneratorBlockEntity#knownFuels()} / {@link CombustionGeneratorBlockEntity#fuelValue});
 * FE uses the live {@link NerospaceConfig} energy-rate multiplier.
 */
final class CombustionFuelEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 44;

    private final int burnTicks;

    CombustionFuelEmiRecipe(ItemStack fuel, int burnTicks) {
        super(NerospaceEmiPlugin.COMBUSTION, NerospaceEmiPlugin.syntheticId(
                "combustion_fuel/" + BuiltInRegistries.ITEM.getKey(fuel.getItem()).getPath()), WIDTH, HEIGHT);
        this.burnTicks = burnTicks;
        this.inputs = List.of(EmiStack.of(fuel));
    }

    @Override
    public boolean supportsRecipeTree() {
        // A fuel value is not a way to make anything; keep it out of EMI's recipe tree.
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int fePerTick = NerospaceConfig.scale(CombustionGeneratorBlockEntity.FE_PER_TICK,
                NerospaceConfig.energyRateMultiplier());

        widgets.addSlot(inputs.get(0), 18, 2).recipeContext(this);
        widgets.addTexture(EmiTexture.EMPTY_FLAME, 56, 4);
        widgets.addAnimatedTexture(EmiTexture.FULL_FLAME, 56, 4, burnTicks * 50, false, true, true);

        EmiStats.lines(widgets, WIDTH, 24, List.of(
                EmiStats.energyGenerated((long) burnTicks * fePerTick),
                EmiStats.seconds(burnTicks)));
    }
}
