package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;

/** Screen for the Nerosium Grinder: a power gauge and a grind-progress arrow over the sci-fi hull panel. */
public class NerosiumGrinderScreen extends TexturedContainerScreen<NerosiumGrinderMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/nerosium_grinder.png");
    private static final int ACCENT = 0xFFD23A8C; // nerosium magenta

    private ScreenSideConfig sideConfig;

    public NerosiumGrinderScreen(NerosiumGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.sideConfig = ScreenSideConfig.create("nerospace:nerosium_grinder",
                ScreenSideConfig.ANCHOR_X, ScreenSideConfig.ANCHOR_Y);
    }

    /** Output buffer slot positions (panel-relative), matching {@link NerosiumGrinderMenu}. */
    private static final int[][] OUTPUT_WELLS = { { 116, 35 }, { 134, 35 }, { 116, 53 }, { 134, 53 } };

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        super.drawPanel(g);
        // Recessed wells behind the four output buffer slots (the texture has no cells there).
        for (int[] xy : OUTPUT_WELLS) {
            int x = this.leftPos + xy[0];
            int y = this.topPos + xy[1];
            g.fill(x - 1, y - 1, x + 17, y + 17, INK);
            g.fill(x, y, x + 16, y + 16, TROUGH);
        }
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        if (this.sideConfig != null) {
            this.sideConfig.render(g, this.leftPos, this.topPos, 0, 0);
        }
        int energy = this.menu.energy();
        int max = this.menu.capacity();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float frac = max == 0 ? 0f : (float) energy / max;
        label(g, Component.literal("Power: " + pct + "%"), 8, 18, 0xFFE9C2DC);
        segGauge(g, 8, 28, 160, 4, frac, ACCENT);

        int maxProg = this.menu.maxProgress();
        float pf = maxProg == 0 ? 0f : (float) this.menu.progress() / maxProg;
        hGauge(g, 78, 40, 22, 6, pf, 0xFF9CF06A);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.sideConfig != null
                && this.sideConfig.mouseClicked(event.x(), event.y(), event.button(), this.leftPos, this.topPos)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
