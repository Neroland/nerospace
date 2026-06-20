package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public FuelTankScreen(FuelTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int pct = this.menu.getFuelPercent();
        float frac = pct / 100f;

        label(g, Component.literal("Fuel: " + pct + "%"), 8, 20, 0xFFFFD9A0);
        fluidGauge(g, 8, 31, 160, 12, frac, ACCENT);
        label(g, Component.literal(this.menu.getFuel() + " / " + this.menu.getCapacity() + " mB"), 8, 50, 0xFFB9C6D4);
    }
}
