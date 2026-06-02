package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.TerraformerMenu;

/**
 * Screen for the Terraformer (terraform design §2.4): a power-buffer gauge, fuel + tier-upgrade slots,
 * and a live tier / frontier-radius readout, themed green (the terraforming accent).
 */
public class TerraformerScreen extends TexturedContainerScreen<TerraformerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/terraformer.png");
    private static final int ACCENT = 0xFF54D46A;     // green (terraform)
    private static final int FLAME = 0xFFF0A83C;      // burn

    public TerraformerScreen(TerraformerMenu menu, Inventory playerInventory, Component title) {
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

        label(g, Component.translatable("gui.nerospace.terraformer.power", pct), 8, 20, 0xFFCFE7FF);
        hGauge(g, 8, 31, 160, 6, energyFrac, ACCENT);

        label(g, Component.translatable("gui.nerospace.terraformer.tier", this.menu.getTier()), 8, 48, ACCENT);
        label(g, Component.translatable("gui.nerospace.terraformer.radius", this.menu.getRadius()), 8, 60, 0xFFB7E8C2);

        boolean burning = this.menu.isBurning();
        label(g, Component.translatable(burning
                ? "gui.nerospace.terraformer.working"
                : "gui.nerospace.terraformer.idle"), 116, 20, burning ? 0xFF9CF0C0 : 0xFF7E8EA0);
    }
}
