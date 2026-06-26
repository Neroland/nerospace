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
 * The Launch Controller console with two modes. <b>Build mode</b>: pick a target pad tier, load Launch
 * Pads / Station Wall / a Launch Gantry, preview the layout as a hologram, and Build lays it out in the
 * world. <b>Launch mode</b>: read the docked rocket's fuel/oxygen/power, pick its destination, and launch
 * it straight from the controller. Fully procedural hull (no texture).
 */
public class LaunchControllerScreen extends TexturedContainerScreen<LaunchControllerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFF54D46A; // build green
    private static final int FUEL = 0xFFF0703C;
    private static final int O2 = 0xFF3CC8E6;
    private static final int POWER = 0xFFF5C542;

    private static final int W = 176;
    private static final int H = 206;
    // Material slots (must match LaunchControllerMenu).
    private static final int[] SLOT_X = {44, 70, 96};
    private static final int SLOT_Y = 44;

    private final List<SpaceButton> tierButtons = new ArrayList<>();
    private SpaceButton modeButton;
    private SpaceButton previewButton;
    private SpaceButton buildButton;
    private SpaceButton destButton;
    private SpaceButton launchButton;

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
        this.modeButton = new SpaceButton(this.leftPos + W - 52, this.topPos + 3, 48, 11,
                Component.empty(), POWER, btn -> sendButton(LaunchControllerMenu.BUTTON_TOGGLE_MODE));
        this.addRenderableWidget(this.modeButton);
        this.previewButton = new SpaceButton(this.leftPos + 8, this.topPos + 98, W - 16, 12,
                Component.translatable("gui.nerospace.launch_controller.preview"), O2,
                btn -> sendButton(LaunchControllerMenu.BUTTON_TOGGLE_HOLOGRAM));
        this.addRenderableWidget(this.previewButton);
        this.buildButton = new SpaceButton(this.leftPos + 8, this.topPos + 112, W - 16, 12,
                Component.translatable("gui.nerospace.launch_controller.build"), ACCENT,
                btn -> sendButton(LaunchControllerMenu.BUTTON_BUILD));
        this.addRenderableWidget(this.buildButton);
        this.destButton = new SpaceButton(this.leftPos + 8, this.topPos + 90, W - 16, 12,
                Component.empty(), ACCENT, btn -> sendButton(LaunchControllerMenu.BUTTON_CYCLE_DEST));
        this.addRenderableWidget(this.destButton);
        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + 104, W - 16, 12,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT,
                btn -> sendButton(LaunchControllerMenu.BUTTON_LAUNCH));
        this.addRenderableWidget(this.launchButton);
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, 0xFF0E1724);
        g.fill(l, t, l + W, t + 16, 0xFF14283C);
        g.fill(l, t + 16, l + W, t + 17, ACCENT);
        if (!this.menu.isLaunchMode()) {
            for (int sx : SLOT_X) {
                slotWell(g, sx, SLOT_Y);
            }
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
        boolean launch = this.menu.isLaunchMode();
        this.modeButton.setMessage(Component.translatable(launch
                ? "gui.nerospace.launch_controller.mode_launch"
                : "gui.nerospace.launch_controller.mode_build"));
        this.modeButton.setSelected(launch);

        // Build-mode widgets.
        int tier = this.menu.targetTier();
        for (int i = 0; i < this.tierButtons.size(); i++) {
            SpaceButton b = this.tierButtons.get(i);
            b.visible = !launch;
            b.setSelected(i + 1 == tier);
        }
        this.previewButton.visible = !launch;
        this.previewButton.setSelected(this.menu.isHologram());
        this.buildButton.visible = !launch;
        this.buildButton.active = this.menu.canBuild();

        // Launch-mode widgets.
        boolean rocket = this.menu.rocketPresent();
        this.destButton.visible = launch && rocket;
        this.launchButton.visible = launch && rocket;

        if (!launch) {
            drawBuildMode(g);
        } else {
            drawLaunchMode(g, rocket);
        }
    }

    private void drawBuildMode(GuiGraphicsExtractor g) {
        // Material slot labels, centred above their wells.
        label(g, Component.translatable("gui.nerospace.launch_controller.pad"), SLOT_X[0], SLOT_Y - 11, SUBTLE);
        label(g, Component.translatable("gui.nerospace.launch_controller.wall"), SLOT_X[1], SLOT_Y - 11, SUBTLE);
        label(g, Component.translatable("gui.nerospace.launch_controller.gantry"), SLOT_X[2] - 4, SLOT_Y - 11, SUBTLE);

        // What's still needed for the selected tier.
        label(g, Component.translatable("gui.nerospace.launch_controller.needed",
                        this.menu.neededPads(), this.menu.neededWall(), this.menu.neededGantry()),
                8, SLOT_Y + 20, 0xFFCFE7FF);

        // Resource hub levels (fed by pipes/cables → pumped into the docked rocket) — full-width bars.
        int gx = 46;
        int gw = W - gx - 8;
        label(g, Component.literal("Fuel"), 8, 73, 0xFFFFC9B0);
        hGauge(g, gx, 73, gw, 5, this.menu.fuelFrac(), FUEL);
        label(g, Component.literal("O2"), 8, 81, 0xFFBFEFFF);
        hGauge(g, gx, 81, gw, 5, this.menu.oxygenFrac(), O2);
        label(g, Component.literal("Power"), 8, 89, 0xFFF6DC8A);
        hGauge(g, gx, 89, gw, 5, this.menu.powerFrac(), POWER);
    }

    private void drawLaunchMode(GuiGraphicsExtractor g, boolean rocket) {
        if (!rocket) {
            label(g, Component.translatable("gui.nerospace.launch_controller.no_rocket"), 8, 42, SUBTLE);
            return;
        }
        label(g, Component.translatable("gui.nerospace.rocket.title", this.menu.rocketTier()), 8, 22, TITLE);
        label(g, Component.translatable("gui.nerospace.rocket.fuel_label"), 8, 32, 0xFFFFC9B0);
        hGauge(g, 8, 41, W - 16, 6, this.menu.rocketFuelFrac(), FUEL);
        label(g, Component.translatable("gui.nerospace.rocket.oxygen_label"), 8, 50, 0xFFBFEFFF);
        hGauge(g, 8, 59, W - 16, 6, this.menu.rocketOxygenFrac(), O2);
        label(g, Component.literal("POWER"), 8, 68, 0xFFF6DC8A);
        hGauge(g, 8, 77, W - 16, 6, this.menu.rocketPowerFrac(), POWER);

        this.destButton.setMessage(Component.literal("» " + this.menu.rocketDestName()));
        this.launchButton.active = this.menu.rocketLaunchable();
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
