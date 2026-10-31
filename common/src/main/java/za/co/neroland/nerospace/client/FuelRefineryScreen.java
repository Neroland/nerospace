package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.FuelRefineryMenu;

/**
 * Screen for the Fuel Refinery: a power gauge, a refining-progress arrow between the carbon and
 * catalyst slots, and a fuel-output gauge with a millibucket readout.
 */
public class FuelRefineryScreen extends TexturedContainerScreen<FuelRefineryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/fuel_refinery.png");
    private static final int ACCENT = 0xFFF0A030;     // fuel orange
    private static final int FLAME = 0xFFF0703C;      // refining heat

    private ScreenSideConfig sideConfig;

    public FuelRefineryScreen(FuelRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.sideConfig = ScreenSideConfig.create("nerospace:fuel_refinery",
                ScreenSideConfig.ANCHOR_X, ScreenSideConfig.ANCHOR_Y);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        if (this.sideConfig != null) {
            this.sideConfig.render(g, this.leftPos, this.topPos, 0, 0);
        }
        int energy = this.menu.getEnergy();
        int max = this.menu.getMaxEnergy();
        int pct = max == 0 ? 0 : energy * 100 / max;
        float frac = max == 0 ? 0f : (float) energy / max;

        label(g, Component.translatable("gui.nerospace.machine.power", pct), 8, 20, 0xFFFFE0B0);
        segGauge(g, 8, 31, 160, 6, frac, ACCENT);

        // Refining-progress arrow between the two input slots.
        hGauge(g, 78, 40, 22, 4, this.menu.getScaledProgress(1000) / 1000f, FLAME);

        int cap = this.menu.getFuelCapacity();
        float fuelFrac = cap == 0 ? 0f : (float) this.menu.getFuel() / cap;
        fluidGauge(g, 8, 56, 160, 10, fuelFrac, ACCENT);
        label(g, Component.literal(this.menu.getFuel() + " / " + cap + " mB"), 8, 68, 0xFFB9C6D4);
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        super.drawPanel(g);
        // 2.0 slots (raw ilmenite, titanium out, oxidiser): frames drawn under the items (the panel texture predates them).
        slotFrame(g, FuelRefineryMenu.ILMENITE_X, FuelRefineryMenu.EXTRA_SLOT_Y);
        slotFrame(g, FuelRefineryMenu.TITANIUM_X, FuelRefineryMenu.EXTRA_SLOT_Y);
        slotFrame(g, FuelRefineryMenu.OXIDISER_X, FuelRefineryMenu.EXTRA_SLOT_Y);
    }

    /** An 18x18 slot frame around the 16x16 slot whose top-left is (x, y). */
    private void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        int sx = this.leftPos + x - 1;
        int sy = this.topPos + y - 1;
        g.fill(sx, sy, sx + 18, sy + 18, INK);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, TROUGH);
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
    *///?}
        if (this.sideConfig != null
                && this.sideConfig.mouseClicked(event.x(), event.y(), event.button(), this.leftPos, this.topPos)) {
            return true;
        }
        //? if >=1.21.9 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(event.x(), event.y(), event.button());
        *///?}
    }
}
