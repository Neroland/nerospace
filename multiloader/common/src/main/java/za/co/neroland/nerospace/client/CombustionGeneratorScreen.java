package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.CombustionGeneratorMenu;

/** Screen for the Combustion Generator: a power-output gauge over the sci-fi hull panel. */
public class CombustionGeneratorScreen extends TexturedContainerScreen<CombustionGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/combustion_generator.png");
    private static final int ACCENT = 0xFFF0A83C; // amber (combustion)

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int energy = this.menu.energy();
        int max = this.menu.capacity();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float frac = max == 0 ? 0f : (float) energy / max;
        label(g, Component.literal("Power: " + pct + "%"), 8, 20, 0xFFFFE0B0);
        segGauge(g, 8, 31, 160, 6, frac, ACCENT);
    }
}
