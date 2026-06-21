package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.TerraformerMenu;

/**
 * Screen for the Terraformer (grid-only): a power-buffer gauge (fed by pipes), the tier-upgrade slot,
 * and a live tier / per-stage frontier-radius readout, themed green.
 */
public class TerraformerScreen extends TexturedContainerScreen<TerraformerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/terraformer.png");
    private static final int ACCENT = 0xFF54D46A;     // green (terraform)

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
        segGauge(g, 8, 31, 160, 6, energyFrac, ACCENT);

        label(g, Component.translatable("gui.nerospace.terraformer.tier", this.menu.getTier()), 8, 48, ACCENT);
        label(g, Component.translatable("gui.nerospace.terraformer.stages", this.menu.getRadius(),
                this.menu.getHydrationRadius(), this.menu.getLifeRadius()), 8, 60, 0xFFB7E8C2);

        boolean active = this.menu.isActive();
        label(g, Component.translatable(active
                ? "gui.nerospace.terraformer.working"
                : "gui.nerospace.terraformer.idle"), 116, 20, active ? 0xFF9CF0C0 : 0xFF7E8EA0);

        label(g, Component.translatable("gui.nerospace.terraformer.hydration",
                this.menu.getHydration()), 116, 48, 0xFF78D2F0);
        if (this.menu.isHydrationStalled()) {
            label(g, Component.translatable("gui.nerospace.terraformer.needs_glacite"), 116, 60, 0xFFFF6A5E);
        }
    }
}
