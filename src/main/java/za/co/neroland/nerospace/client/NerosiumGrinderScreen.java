package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.NerosiumGrinderMenu;

/**
 * Screen for the Nerosium Grinder (Phase 9b): sci-fi panel with an energy gauge and a grind-progress
 * gauge between the input and output slots.
 */
public class NerosiumGrinderScreen extends TexturedContainerScreen<NerosiumGrinderMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/nerosium_grinder.png");
    private static final int ACCENT = 0xFFD23A8C;     // nerosium magenta

    public NerosiumGrinderScreen(NerosiumGrinderMenu menu, Inventory playerInventory, Component title) {
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

        label(g, Component.literal("Power: " + pct + "%"), 8, 18, 0xFFE9C2DC);
        segGauge(g, 8, 28, 160, 4, energyFrac, ACCENT);

        // Grind progress arrow between input (56,35) and output (116,35).
        hGauge(g, 78, 40, 22, 6, this.menu.getScaledProgress(1000) / 1000f, 0xFF9CF06A);
    }
}
