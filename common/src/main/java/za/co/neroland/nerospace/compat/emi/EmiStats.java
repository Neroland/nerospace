package za.co.neroland.nerospace.compat.emi;

import java.util.List;
import java.util.Locale;

import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;

import za.co.neroland.nerospace.platform.Services;

/**
 * Shared bits for the EMI pages: the grey stat lines under each recipe (reusing the JEI pages' translation
 * keys, so both viewers read the same) and the fluid-unit conversion EMI expects.
 */
final class EmiStats {

    /** Same grey the JEI pages use. */
    private static final int STAT_COLOR = 0xFF808080;
    private static final int LINE_HEIGHT = 10;

    private EmiStats() {
    }

    static Component energyCost(long fe) {
        return Component.translatable("jei.nerospace.stat.energy_cost", number(fe));
    }

    static Component energyGenerated(long fe) {
        return Component.translatable("jei.nerospace.stat.energy_generated", number(fe));
    }

    static Component seconds(int ticks) {
        return Component.translatable("jei.nerospace.stat.time", String.format(Locale.ROOT, "%.1f", ticks / 20.0));
    }

    /** Centred grey lines, one under the other, starting at {@code y}. */
    static void lines(WidgetHolder widgets, int width, int y, List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            widgets.addText(lines.get(i), width / 2, y + i * LINE_HEIGHT, STAT_COLOR, false)
                    .horizontalAlign(TextWidget.Alignment.CENTER);
        }
    }

    /**
     * Converts millibuckets (Nerospace's internal fluid unit on every loader) to the unit EMI counts fluids
     * in: millibuckets on NeoForge/Forge, droplets (81 per millibucket) on Fabric.
     */
    static long fluidAmount(int millibuckets) {
        return "Fabric".equals(Services.PLATFORM.getPlatformName()) ? millibuckets * 81L : millibuckets;
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
