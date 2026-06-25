package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.PassiveGeneratorMenu;

/** Screen for the Passive Generator: a power-output gauge over the sci-fi hull panel. */
public class PassiveGeneratorScreen extends TexturedContainerScreen<PassiveGeneratorMenu> {

    private static final Identifier TEXTURE =
            NerospaceCommon.id("textures/gui/passive_generator.png");
    private static final int ACCENT = 0xFFB327A0; // nerosium magenta

    public PassiveGeneratorScreen(PassiveGeneratorMenu menu, Inventory playerInventory, Component title) {
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
        label(g, Component.literal("Power: " + pct + "%"), 8, 20, 0xFFF0CFEA);
        segGauge(g, 8, 31, 160, 6, frac, ACCENT);
    }
}
