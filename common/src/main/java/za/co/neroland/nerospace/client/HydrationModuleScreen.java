package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.HydrationModuleMenu;

/**
 * Screen for the Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1): the glacite input slot, the
 * linked Terraformer's hydration-unit gauge and the link status, themed glacite cyan.
 */
public class HydrationModuleScreen extends TexturedContainerScreen<HydrationModuleMenu> {

    private static final Identifier TEXTURE =
            NerospaceCommon.id("textures/gui/hydration_module.png");
    private static final int ACCENT = 0xFF78D2F0;     // glacite cyan (water stage)

    public HydrationModuleScreen(HydrationModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int hydration = this.menu.getHydration();
        int cap = this.menu.getHydrationCap();
        float frac = cap == 0 ? 0f : (float) hydration / cap;

        label(g, Component.translatable("gui.nerospace.hydration_module.buffer", hydration, cap),
                8, 20, 0xFFCFE7FF);
        fluidGauge(g, 8, 31, 160, 6, frac, ACCENT);

        boolean linked = this.menu.isLinked();
        label(g, Component.translatable(linked
                ? "gui.nerospace.hydration_module.linked"
                : "gui.nerospace.hydration_module.no_link"), 8, 48, linked ? 0xFF9CF0C0 : 0xFFFF6A5E);
    }
}
