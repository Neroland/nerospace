package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.PassiveGeneratorMenu;

/** Screen for the Passive Generator: a power-output gauge, a nerosium core slot, and a core-life gauge. */
public class PassiveGeneratorScreen extends TexturedContainerScreen<PassiveGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/passive_generator.png");
    private static final int ACCENT = 0xFFB327A0;     // nerosium magenta
    private static final int CORE = 0xFFE03A3A;       // core glow

    public PassiveGeneratorScreen(PassiveGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        label(g, Component.translatable("gui.nerospace.generator.output", pct), 8, 20, 0xFFF0CFEA);
        hGauge(g, 8, 31, 160, 6, frac, ACCENT);

        boolean active = this.menu.hasCore();
        label(g, Component.translatable(active
                ? "gui.nerospace.generator.core_active"
                : "gui.nerospace.generator.core_empty"), 102, 48, active ? 0xFFF09CC0 : 0xFF7E8EA0);
        hGauge(g, 102, 58, 58, 4, this.menu.getScaledCore(1000) / 1000f, CORE);
    }
}
