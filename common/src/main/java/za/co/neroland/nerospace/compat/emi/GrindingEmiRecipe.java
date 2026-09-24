package za.co.neroland.nerospace.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.core.registries.BuiltInRegistries;

import za.co.neroland.nerolandcore.meteor.MeteorMaterialTags;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.machine.GrinderRecipes;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;

/**
 * One Nerosium Grinder process on EMI's grinding page. Mirrors {@code compat.jei.GrindingCategory}: input →
 * animated arrow → output, with the FE cost and processing time underneath. The numbers are computed when
 * the page is drawn, so they follow the live {@link NerospaceConfig} machine-speed multiplier.
 */
final class GrindingEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 50;

    private final EmiIngredient output;

    GrindingEmiRecipe(GrinderRecipes.Grinding grinding) {
        super(NerospaceEmiPlugin.GRINDING, NerospaceEmiPlugin.syntheticId(idPath(grinding)), WIDTH, HEIGHT);
        this.inputs = List.of(EmiStack.of(grinding.input()));
        if (grinding.meteor()) {
            // Random meteor-block path: the output is resolved live by Neroland Core's Meteor Material
            // Registry, so show the whole neroland:meteor/grindable pool (every installed Nero mod's
            // entries) as one cycling tag slot.
            this.output = EmiIngredient.of(MeteorMaterialTags.GRINDABLE);
            this.outputs = List.copyOf(this.output.getEmiStacks());
        } else {
            EmiStack result = EmiStack.of(grinding.output());
            this.output = result;
            this.outputs = List.of(result);
        }
    }

    private static String idPath(GrinderRecipes.Grinding grinding) {
        String input = BuiltInRegistries.ITEM.getKey(grinding.input().getItem()).getPath();
        return grinding.meteor() ? "grinding/meteor/" + input : "grinding/" + input;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int ticks = NerospaceConfig.scaleInterval(NerosiumGrinderBlockEntity.MAX_PROGRESS,
                NerospaceConfig.machineSpeedMultiplier());

        widgets.addSlot(inputs.get(0), 4, 4);
        widgets.addFillingArrow(28, 5, ticks * 50);
        widgets.addSlot(output, 62, 0).large(true).recipeContext(this);

        EmiStats.lines(widgets, WIDTH, 30, List.of(
                EmiStats.energyCost((long) ticks * NerosiumGrinderBlockEntity.ENERGY_PER_TICK),
                EmiStats.seconds(ticks)));
    }
}
