package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.LaunchControllerMenu;

/**
 * The Launch Controller console: pick a target pad tier, load Launch Pads / Station Wall / a Launch
 * Gantry into the three slots, and Build lays the formation out in the world. Fully procedural hull
 * (no texture), matching the rocket console styling.
 */
public class LaunchControllerScreen extends TexturedContainerScreen<LaunchControllerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFF54D46A; // build green

    private static final int W = 176;
    private static final int H = 186;
    // Material slots (must match LaunchControllerMenu).
    private static final int[] SLOT_X = {44, 70, 96};
    private static final int SLOT_Y = 40;

    private final List<SpaceButton> tierButtons = new ArrayList<>();
    private SpaceButton previewButton;
    private SpaceButton buildButton;

    public LaunchControllerScreen(LaunchControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = 6;
        this.inventoryLabelY = 10_000; // hide
    }

    @Override
    protected void init() {
        super.init();
        this.tierButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int tier = i + 1;
            SpaceButton b = new SpaceButton(this.leftPos + 8 + i * 40, this.topPos + 18, 36, 14,
                    Component.literal("T" + tier), ACCENT, btn -> sendButton(tier));
            this.addRenderableWidget(b);
            this.tierButtons.add(b);
        }
        this.previewButton = new SpaceButton(this.leftPos + 8, this.topPos + 70, W - 16, 12,
                Component.translatable("gui.nerospace.launch_controller.preview"), 0xFF3CC8E6,
                btn -> sendButton(LaunchControllerMenu.BUTTON_TOGGLE_HOLOGRAM));
        this.addRenderableWidget(this.previewButton);
        this.buildButton = new SpaceButton(this.leftPos + 8, this.topPos + 84, W - 16, 15,
                Component.translatable("gui.nerospace.launch_controller.build"), ACCENT,
                btn -> sendButton(LaunchControllerMenu.BUTTON_BUILD));
        this.addRenderableWidget(this.buildButton);
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, 0xFF0E1724);
        g.fill(l, t, l + W, t + 16, 0xFF14283C);
        g.fill(l, t + 16, l + W, t + 17, ACCENT);
        for (int sx : SLOT_X) {
            slotWell(g, sx, SLOT_Y);
        }
    }

    private void slotWell(GuiGraphicsExtractor g, int dx, int dy) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + 17, y + 17, INK);
        g.fill(x, y, x + 16, y + 16, 0xFF0A1018);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int tier = this.menu.targetTier();
        for (int i = 0; i < this.tierButtons.size(); i++) {
            this.tierButtons.get(i).setSelected(i + 1 == tier);
        }

        // Slot captions.
        label(g, Component.translatable("gui.nerospace.launch_controller.pad"), SLOT_X[0] - 2, SLOT_Y - 10, SUBTLE);
        label(g, Component.translatable("gui.nerospace.launch_controller.wall"), SLOT_X[1] - 2, SLOT_Y - 10, SUBTLE);
        label(g, Component.translatable("gui.nerospace.launch_controller.gantry"), SLOT_X[2] - 4, SLOT_Y - 10, SUBTLE);

        // Onboard resource hub levels (fed by pipes/cables → pumped into the rocket).
        label(g, Component.literal("F"), 112, 40, 0xFFFFC9B0);
        hGauge(g, 120, 40, 48, 5, this.menu.fuelFrac(), 0xFFF0703C);
        label(g, Component.literal("O"), 112, 48, 0xFFBFEFFF);
        hGauge(g, 120, 48, 48, 5, this.menu.oxygenFrac(), 0xFF3CC8E6);
        label(g, Component.literal("P"), 112, 56, 0xFFF6DC8A);
        hGauge(g, 120, 56, 48, 5, this.menu.powerFrac(), 0xFFF5C542);

        // Needed readout (what the chosen tier still wants).
        label(g, Component.translatable("gui.nerospace.launch_controller.needed",
                        this.menu.neededPads(), this.menu.neededWall(), this.menu.neededGantry()),
                8, 60, 0xFFCFE7FF);

        if (this.previewButton != null) {
            this.previewButton.setSelected(this.menu.isHologram());
        }
        boolean canBuild = this.menu.canBuild();
        if (this.buildButton != null) {
            this.buildButton.active = canBuild;
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
