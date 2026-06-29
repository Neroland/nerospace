package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.FuelRefineryMenu;

/**
 * Screen for the Fuel Refinery: a power gauge, a refining-progress arrow between the carbon and
 * catalyst slots, and a fuel-output gauge with a millibucket readout.
 */
public class FuelRefineryScreen extends TexturedContainerScreen<FuelRefineryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/fuel_refinery.png");
    private static final int ACCENT = 0xFFF0A030;     // fuel orange
    private static final int FLAME = 0xFFF0703C;      // refining heat

    private ScreenSideConfig sideConfig;

    public FuelRefineryScreen(FuelRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.sideConfig = ScreenSideConfig.create("nerospace:fuel_refinery",
                ScreenSideConfig.ANCHOR_X, ScreenSideConfig.ANCHOR_Y);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        if (this.sideConfig != null) {
            this.sideConfig.render(g, this.leftPos, this.topPos, 0, 0);
        }
        int energy = this.menu.getEnergy();
        int max = this.menu.getMaxEnergy();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float frac = max == 0 ? 0f : (float) energy / max;

        label(g, Component.literal("Power: " + pct + "%"), 8, 20, 0xFFFFE0B0);
        segGauge(g, 8, 31, 160, 6, frac, ACCENT);

        // Refining-progress arrow between the two input slots.
        hGauge(g, 78, 40, 22, 4, this.menu.getScaledProgress(1000) / 1000f, FLAME);

        int cap = this.menu.getFuelCapacity();
        float fuelFrac = cap == 0 ? 0f : (float) this.menu.getFuel() / cap;
        fluidGauge(g, 8, 56, 160, 10, fuelFrac, ACCENT);
        label(g, Component.literal(this.menu.getFuel() + " / " + cap + " mB"), 8, 68, 0xFFB9C6D4);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.sideConfig != null
                && this.sideConfig.mouseClicked(event.x(), event.y(), event.button(), this.leftPos, this.topPos)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
