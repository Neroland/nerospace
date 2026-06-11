package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.FuelRefineryMenu;

/**
 * Screen for the Fuel Refinery: a power gauge, a refining-progress arrow between the carbon and
 * catalyst slots, and a fuel-output gauge with a millibucket readout.
 */
public class FuelRefineryScreen extends TexturedContainerScreen<FuelRefineryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/fuel_refinery.png");
    private static final int ACCENT = 0xFFF0A030;     // fuel orange
    private static final int FLAME = 0xFFF0703C;      // refining heat

    public FuelRefineryScreen(FuelRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int energy = this.menu.getEnergy();
        int max = this.menu.getMaxEnergy();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float frac = max == 0 ? 0f : (float) energy / max;

        label(g, Component.translatable("gui.nerospace.generator.output", pct), 8, 20, 0xFFFFE0B0);
        segGauge(g, 8, 31, 160, 6, frac, ACCENT);

        // Refining-progress arrow between the two input slots.
        hGauge(g, 78, 40, 22, 4, this.menu.getScaledProgress(1000) / 1000f, FLAME);

        int cap = this.menu.getFuelCapacity();
        float fuelFrac = cap == 0 ? 0f : (float) this.menu.getFuel() / cap;
        fluidGauge(g, 8, 56, 160, 10, fuelFrac, ACCENT);
        label(g, Component.literal(this.menu.getFuel() + " / " + cap + " mB"), 8, 68, 0xFFB9C6D4);
    }
}
