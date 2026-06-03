package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.CombustionGeneratorMenu;

/** Screen for the Combustion Generator: a power-output gauge, fuel slot, and burn-progress gauge. */
public class CombustionGeneratorScreen extends TexturedContainerScreen<CombustionGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/combustion_generator.png");
    private static final int ACCENT = 0xFFF0A83C;     // amber (combustion)
    private static final int FLAME = 0xFFF0703C;      // burn

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
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
        hGauge(g, 8, 31, 160, 6, frac, ACCENT);

        boolean burning = this.menu.isBurning();
        label(g, Component.translatable(burning
                ? "gui.nerospace.generator.burning"
                : "gui.nerospace.generator.idle"), 102, 48, burning ? 0xFFF0B070 : 0xFF7E8EA0);
        hGauge(g, 102, 58, 58, 4, this.menu.getScaledBurn(1000) / 1000f, FLAME);
    }
}
