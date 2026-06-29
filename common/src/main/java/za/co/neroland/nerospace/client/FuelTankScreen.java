package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.FuelTankMenu;

/**
 * Screen for the Fuel Tank: a large sci-fi fuel gauge with a percentage + millibucket readout.
 * Filling is still done by right-clicking the block with a fuel bucket/canister.
 */
public class FuelTankScreen extends TexturedContainerScreen<FuelTankMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/fuel_tank.png");
    private static final int ACCENT = 0xFFF0A030;     // fuel orange

    private ScreenSideConfig sideConfig;

    public FuelTankScreen(FuelTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.sideConfig = ScreenSideConfig.create("nerospace:fuel_tank",
                ScreenSideConfig.ANCHOR_X, ScreenSideConfig.ANCHOR_Y);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        if (this.sideConfig != null) {
            this.sideConfig.render(g, this.leftPos, this.topPos, 0, 0);
        }
        int pct = this.menu.getFuelPercent();
        float frac = pct / 100f;

        label(g, Component.literal("Fuel: " + pct + "%"), 8, 20, 0xFFFFD9A0);
        fluidGauge(g, 8, 31, 160, 12, frac, ACCENT);
        label(g, Component.literal(this.menu.getFuel() + " / " + this.menu.getCapacity() + " mB"), 8, 50, 0xFFB9C6D4);
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
