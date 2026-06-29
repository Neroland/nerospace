package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.OxygenGeneratorMenu;

/**
 * Screen for the Oxygen Generator: a power-buffer gauge (fed by pipes) and the internal oxygen-tank
 * gauge over the sci-fi hull panel. No slots — power and oxygen both move through the pipe network.
 */
public class OxygenGeneratorScreen extends TexturedContainerScreen<OxygenGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/oxygen_generator.png");
    private static final int ACCENT = 0xFF3CC8E6; // cyan (power)
    private static final int OXYGEN = 0xFF54D46A; // gas-layer green

    private ScreenSideConfig sideConfig;

    public OxygenGeneratorScreen(OxygenGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.sideConfig = ScreenSideConfig.create("nerospace:oxygen_generator",
                ScreenSideConfig.ANCHOR_X, ScreenSideConfig.ANCHOR_Y);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        if (this.sideConfig != null) {
            this.sideConfig.render(g, this.leftPos, this.topPos, 0, 0);
        }
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int pct = maxEnergy == 0 ? 0 : energy * 100 / maxEnergy;
        float energyFrac = maxEnergy == 0 ? 0f : (float) energy / maxEnergy;
        label(g, Component.literal("Power: " + pct + "%"), 8, 20, 0xFFCFE7FF);
        segGauge(g, 8, 31, 160, 6, energyFrac, ACCENT);

        int oxygen = this.menu.getOxygen();
        int maxOxygen = this.menu.getMaxOxygen();
        float o2Frac = maxOxygen == 0 ? 0f : (float) oxygen / maxOxygen;
        label(g, Component.literal("Oxygen: " + oxygen + " / " + maxOxygen + " mB"), 8, 46, 0xFFC8F0D2);
        fluidGauge(g, 8, 57, 160, 6, o2Frac, OXYGEN);
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
