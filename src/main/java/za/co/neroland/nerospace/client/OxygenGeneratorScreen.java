package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.OxygenGeneratorMenu;

/**
 * Screen for the Oxygen Generator (Phase 9b): a sci-fi panel with a live power-buffer gauge, a fuel
 * slot, and a burn-progress gauge with a Burning/Idle status — all drawn on the hull panel.
 */
public class OxygenGeneratorScreen extends TexturedContainerScreen<OxygenGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/oxygen_generator.png");
    private static final int ACCENT = 0xFF3CC8E6;     // cyan (oxygen)
    private static final int FLAME = 0xFFF0A83C;      // burn

    public OxygenGeneratorScreen(OxygenGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int energy = this.menu.getEnergy();
        int max = this.menu.getMaxEnergy();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float energyFrac = max == 0 ? 0f : (float) energy / max;

        label(g, Component.translatable("gui.nerospace.oxygen_generator.power", pct), 8, 20, 0xFFCFE7FF);
        hGauge(g, 8, 31, 160, 6, energyFrac, ACCENT);

        boolean burning = this.menu.isBurning();
        label(g, Component.translatable(burning
                ? "gui.nerospace.oxygen_generator.burning"
                : "gui.nerospace.oxygen_generator.idle"), 102, 48, burning ? 0xFF9CF0C0 : 0xFF7E8EA0);
        hGauge(g, 102, 58, 58, 4, this.menu.getScaledBurn(1000) / 1000f, FLAME);
    }
}
