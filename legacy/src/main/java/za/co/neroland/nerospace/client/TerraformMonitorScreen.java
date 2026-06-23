package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.TerraformMonitorMenu;

/**
 * Screen for the Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6): the nearest Terraformer's stage
 * radii, hydration buffer and stall reason, plus the LOCAL column's stage — themed terraform green.
 */
public class TerraformMonitorScreen extends TexturedContainerScreen<TerraformMonitorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/terraform_monitor.png");
    private static final int ACCENT = 0xFF54D46A;     // green (terraform family)

    public TerraformMonitorScreen(TerraformMonitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int stage = this.menu.getLocalStage();
        label(g, Component.translatable("gui.nerospace.terraform_monitor.stage." + stage), 8, 20,
                stage >= 3 ? 0xFF9CF0C0 : (stage > 0 ? ACCENT : 0xFF7E8EA0));

        if (!this.menu.isLinked()) {
            label(g, Component.translatable("gui.nerospace.terraform_monitor.no_link"), 8, 36, 0xFF7E8EA0);
            return;
        }
        label(g, Component.translatable("gui.nerospace.terraform_monitor.radii",
                this.menu.getRootedRadius(), this.menu.getHydrationRadius(), this.menu.getLifeRadius()),
                8, 36, 0xFFB7E8C2);
        label(g, Component.translatable("gui.nerospace.terraform_monitor.hydration",
                this.menu.getHydration()), 8, 48, 0xFF78D2F0);
        if (this.menu.isStalled()) {
            label(g, Component.translatable("gui.nerospace.terraformer.needs_glacite"), 8, 60, 0xFFFF6A5E);
        }
    }
}
