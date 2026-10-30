package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ui.PendingSelection;
import za.co.neroland.nerospace.menu.LaunchControllerMenu;
import za.co.neroland.nerospace.rocket.DestinationSelection;
import za.co.neroland.nerospace.rocket.DestinationTrace;
import za.co.neroland.nerospace.rocket.Destinations;

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
    private SpaceButton prevDestButton;
    private SpaceButton nextDestButton;
    private SpaceButton launchButton;
    /** The optimistic destination: a step shows at once, the synced index settles it (004 §6). */
    private final PendingSelection pending = new PendingSelection();
    @Nullable
    private Component status;
    private long statusUntil;
    /** Mask/selection the destination tooltip was last built for (rebuilt only when they change). */
    private long tooltipKey = Long.MIN_VALUE;

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
                    Component.translatable("gui.nerospace.launch_controller.tier_button", tier), ACCENT, btn -> sendButton(tier));
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
        // Destination: ◀ [name] ▶ — the name steps forward too; the mouse wheel and Up/Down also step.
        this.prevDestButton = new SpaceButton(this.leftPos + 8, this.topPos + 90, 14, 12,
                Component.literal("◀"), ACCENT, btn -> stepDestination(-1));
        this.addRenderableWidget(this.prevDestButton);
        this.destButton = new SpaceButton(this.leftPos + 24, this.topPos + 90, W - 48, 12,
                Component.empty(), ACCENT, btn -> stepDestination(1));
        this.addRenderableWidget(this.destButton);
        this.nextDestButton = new SpaceButton(this.leftPos + W - 22, this.topPos + 90, 14, 12,
                Component.literal("▶"), ACCENT, btn -> stepDestination(1));
        this.addRenderableWidget(this.nextDestButton);
        this.tooltipKey = Long.MIN_VALUE;
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
        this.prevDestButton.visible = launch && rocket;
        this.nextDestButton.visible = launch && rocket;
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
        // The tier the pad actually forms, so a hand-built pad shows up here too.
        label(g, Component.translatable("gui.nerospace.launch_controller.built_tier", this.menu.builtTier()),
                8, SLOT_Y + 30, SUBTLE);

        // Resource hub levels (fed by pipes/cables → pumped into the docked rocket) — full-width bars.
        int gx = 46;
        int gw = W - gx - 8;
        label(g, Component.translatable("gui.nerospace.launch_controller.hub.fuel"), 8, 73, 0xFFFFC9B0);
        hGauge(g, gx, 73, gw, 5, this.menu.fuelFrac(), FUEL);
        label(g, Component.translatable("gui.nerospace.launch_controller.hub.oxygen"), 8, 81, 0xFFBFEFFF);
        hGauge(g, gx, 81, gw, 5, this.menu.oxygenFrac(), O2);
        label(g, Component.translatable("gui.nerospace.launch_controller.hub.power"), 8, 89, 0xFFF6DC8A);
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
        label(g, Component.translatable("gui.nerospace.rocket.power_label"), 8, 68, 0xFFF6DC8A);
        hGauge(g, 8, 77, W - 16, 6, this.menu.rocketPowerFrac(), POWER);

        long now = System.currentTimeMillis();
        int synced = this.menu.rocketDestIndex();
        if (this.pending.reconcile(synced, now) == PendingSelection.Result.REFUSED) {
            showStatus(Component.translatable("gui.nerospace.dest.reason.refused"), now);
        }
        int shown = this.pending.shown(synced);
        ResourceKey<Level> key = Destinations.byIndex(shown);
        this.destButton.setMessage(Component.translatable("gui.nerospace.launch_controller.destination",
                key == null ? Component.literal("—") : Destinations.displayName(key)));
        int mask = this.menu.rocketDestMask();
        boolean choice = Integer.bitCount(mask) > 1;
        this.prevDestButton.active = choice;
        this.nextDestButton.active = choice;
        refreshDestTooltip(mask, shown);
        this.launchButton.active = this.menu.rocketLaunchable() && !this.pending.active();
        if (this.status != null && now < this.statusUntil) {
            drawNotice(g, this.status);
        }
    }

    /** The destination tooltip: every destination in range, the selected one marked, and the controls. */
    private void refreshDestTooltip(int mask, int shown) {
        long key = ((long) mask << 32) | (shown & 0xFFFFFFFFL);
        if (key == this.tooltipKey) {
            return;
        }
        this.tooltipKey = key;
        MutableComponent text = Component.translatable("gui.nerospace.launch_controller.dest_list");
        for (int i = 0; i < Destinations.all().size(); i++) {
            if ((mask & (1 << i)) != 0) {
                text.append("\n").append(Component.literal(i == shown ? "▶ " : "   "))
                        .append(Destinations.displayName(Destinations.all().get(i)));
            }
        }
        text.append("\n").append(Component.translatable("gui.nerospace.launch_controller.dest_hint"));
        this.destButton.setTooltip(Tooltip.create(text));
    }

    /**
     * Steps the docked rocket's destination: predicted and shown at once, then the server steps it (skipping
     * worlds whose Core gate is closed for this player) and the synced index settles the display.
     */
    private void stepDestination(int dir) {
        long now = System.currentTimeMillis();
        int synced = this.menu.rocketDestIndex();
        int mask = this.menu.rocketDestMask();
        int current = this.pending.shown(synced);
        int predicted = DestinationSelection.step(mask, 0, current, dir, Destinations.all().size());
        DestinationTrace.log("controller screen {} step {} from {} mask {} -> {}", this.menu.containerId, dir,
                current, mask, predicted);
        if (predicted == current) {
            showStatus(Component.translatable("gui.nerospace.dest.reason.only_one"), now);
            return;
        }
        this.pending.begin(predicted, synced, now);
        this.status = null;
        sendButton(dir < 0 ? LaunchControllerMenu.BUTTON_PREV_DEST : LaunchControllerMenu.BUTTON_CYCLE_DEST);
    }

    private void showStatus(Component reason, long now) {
        this.status = Component.translatable("gui.nerospace.dest.cant_select", reason);
        this.statusUntil = now + 2500L;
    }

    private boolean destControlsLive() {
        return this.menu.isLaunchMode() && this.menu.rocketPresent();
    }

    /** The mouse wheel over the destination row steps the destination. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = this.leftPos + 8;
        int y = this.topPos + 90;
        if (destControlsLive() && scrollY != 0.0D && mouseX >= x && mouseX < x + W - 16 && mouseY >= y
                && mouseY < y + 12) {
            playClick();
            stepDestination(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Up/Down step the destination; Enter launches when ready (launch mode). */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (destControlsLive()) {
            if (event.isUp() || event.isDown()) {
                playClick();
                stepDestination(event.isUp() ? -1 : 1);
                return true;
            }
            if (event.isConfirmation() && this.launchButton.active) {
                playClick();
                sendButton(LaunchControllerMenu.BUTTON_LAUNCH);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void playClick() {
        if (this.minecraft != null) {
            net.minecraft.client.gui.components.AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
