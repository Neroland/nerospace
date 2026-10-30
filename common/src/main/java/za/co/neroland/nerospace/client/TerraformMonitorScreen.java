package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.world.planet.TerraformRefusal;
import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTables;
import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.TerraformMonitorMenu;

/**
 * Screen for the Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6): the nearest Terraformer's stage
 * radii, hydration buffer and stall reason, plus the LOCAL column's stage — themed terraform green.
 */
public class TerraformMonitorScreen extends TexturedContainerScreen<TerraformMonitorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/terraform_monitor.png");
    private static final int ACCENT = 0xFF54D46A;     // green (terraform family)

    public TerraformMonitorScreen(TerraformMonitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int stage = this.menu.getLocalStage();
        label(g, Component.translatable("gui.nerospace.terraform_monitor.stage." + stage), 8, 18,
                stage >= 3 ? 0xFF9CF0C0 : (stage > 0 ? ACCENT : 0xFF7E8EA0));

        // Planet-specific readout (D7): why the terraformer refuses here, else what the next stage will plant.
        TerraformRefusal refusal = this.menu.getRefusal();
        if (refusal != TerraformRefusal.NONE) {
            label(g, Component.translatable(refusal.langKey()), 8, 29, 0xFFFF6A5E);
        } else {
            TerraformTable table = TerraformTables.byIndex(this.menu.getTableIndex());
            if (table != null) {
                label(g, Component.translatable(nextKey(table.langId(), stage)), 8, 29, 0xFFD8F5C8);
            }
        }

        if (!this.menu.isLinked()) {
            label(g, Component.translatable("gui.nerospace.terraform_monitor.no_link"), 8, 40, 0xFF7E8EA0);
            return;
        }
        label(g, Component.translatable("gui.nerospace.terraform_monitor.radii",
                this.menu.getRootedRadius(), this.menu.getHydrationRadius(), this.menu.getLifeRadius()),
                8, 40, 0xFFB7E8C2);
        label(g, Component.translatable("gui.nerospace.terraform_monitor.hydration",
                this.menu.getHydration()), 8, 51, 0xFF78D2F0);
        if (this.menu.isStalled()) {
            label(g, Component.translatable("gui.nerospace.terraformer.needs_glacite"), 8, 62, 0xFFFF6A5E);
        }
    }

    /** {@code gui.nerospace.terraform_monitor.next.<planet>.<stage 1..3>}, or {@code .done} once Living. */
    static String nextKey(String langId, int localStage) {
        String base = "gui.nerospace.terraform_monitor.next." + langId + ".";
        return localStage >= 3 ? base + "done" : base + (Math.max(0, localStage) + 1);
    }
}
