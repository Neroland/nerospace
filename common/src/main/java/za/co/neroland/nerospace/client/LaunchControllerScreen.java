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
import za.co.neroland.nerospace.client.ui.Glyphs;
import za.co.neroland.nerospace.client.ui.PendingSelection;
import za.co.neroland.nerospace.menu.LaunchControllerMenu;
import za.co.neroland.nerospace.rocket.DestinationSelection;
import za.co.neroland.nerospace.rocket.DestinationTrace;
import za.co.neroland.nerospace.rocket.Destinations;

/**
 * The Launch Controller console with two modes. <b>Build mode</b>: pick a target pad tier, load Launch
 * Pads / Station Wall / a Launch Gantry, preview the layout as a hologram, and Build lays it out in the
 * world. <b>Launch mode</b>: read the docked rocket's fuel/oxygen/power, pick its destination, and launch
 * it straight from the controller. Fully procedural hull (no texture). Every control is a glyph or a label that
 * is cut with an ellipsis and carries its full text in a tooltip, so no translation can overlap another.
 */
public class LaunchControllerScreen extends TexturedContainerScreen<LaunchControllerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFF54D46A; // build green
    private static final int FUEL = 0xFFF0703C;
    private static final int O2 = 0xFF3CC8E6;
    private static final int POWER = 0xFFF5C542;
    private static final int HULL = 0xFF0D1119;
    private static final int HEADER = 0xFF14283C;
    private static final int WELL = 0xFF0A1018;
    private static final int VALUE = 0xFFB9C6D4;
    private static final int LABEL = 0xFFCFE7FF;
    private static final Glyphs.Glyph[] SLOT_GLYPHS = { Glyphs.Glyph.PAD, Glyphs.Glyph.WALL, Glyphs.Glyph.GANTRY };
    private static final String[] SLOT_KEYS = { "pad", "wall", "gantry" };

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
    private IconButton prevDestButton;
    private IconButton nextDestButton;
    private SpaceButton launchButton;
    /** The optimistic destination: a step shows at once, the synced index settles it. */
    private final PendingSelection pending = new PendingSelection();
    @Nullable
    private Component status;
    private long statusUntil;
    /** Mask/selection the destination tooltip was last built for (rebuilt only when they change). */
    private long tooltipKey = Long.MIN_VALUE;
    /** State the Build button's tooltip was last built for. */
    private String buildTipKey = "";

    public LaunchControllerScreen(LaunchControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = 4;
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
        this.modeButton = new SpaceButton(this.leftPos + 116, this.topPos + 2, 56, 12,
                Component.empty(), POWER, btn -> sendButton(LaunchControllerMenu.BUTTON_TOGGLE_MODE));
        this.addRenderableWidget(this.modeButton);
        Component preview = Component.translatable("gui.nerospace.launch_controller.preview");
        this.previewButton = new SpaceButton(this.leftPos + 8, this.topPos + 114, 76, 12, preview, O2,
                btn -> sendButton(LaunchControllerMenu.BUTTON_TOGGLE_HOLOGRAM));
        this.previewButton.setTooltip(Tooltip.create(preview));
        this.addRenderableWidget(this.previewButton);
        this.buildButton = new SpaceButton(this.leftPos + 92, this.topPos + 114, 76, 12,
                Component.translatable("gui.nerospace.launch_controller.build"), ACCENT,
                btn -> sendButton(LaunchControllerMenu.BUTTON_BUILD));
        this.addRenderableWidget(this.buildButton);
        // Destination: ◀ [name] ▶ as glyph buttons — the name steps forward too; the wheel and Up/Down also step.
        this.prevDestButton = new IconButton(this.leftPos + 8, this.topPos + 96, 14, 12, Glyphs.Glyph.ARROW_L,
                Component.translatable("gui.nerospace.launch_controller.prev_dest"), ACCENT, btn -> stepDestination(-1));
        this.addRenderableWidget(this.prevDestButton);
        this.destButton = new SpaceButton(this.leftPos + 24, this.topPos + 96, 128, 12,
                Component.empty(), ACCENT, btn -> stepDestination(1));
        this.addRenderableWidget(this.destButton);
        this.nextDestButton = new IconButton(this.leftPos + 154, this.topPos + 96, 14, 12, Glyphs.Glyph.ARROW_R,
                Component.translatable("gui.nerospace.launch_controller.next_dest"), ACCENT, btn -> stepDestination(1));
        this.addRenderableWidget(this.nextDestButton);
        this.tooltipKey = Long.MIN_VALUE;
        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + 110, W - 16, 14,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT,
                btn -> sendButton(LaunchControllerMenu.BUTTON_LAUNCH));
        this.addRenderableWidget(this.launchButton);
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, HULL);
        g.fill(l, t, l + W, t + 16, HEADER);
        g.fill(l, t + 16, l + W, t + 17, ACCENT);
        if (this.menu.isLaunchMode()) {
            well(g, 6, 19, 164, 74);
        } else {
            well(g, 6, 34, 164, 34); // materials well
            for (int sx : SLOT_X) {
                slotWell(g, sx, SLOT_Y);
            }
        }
    }

    /** A recessed well: WELL body with an INK top/left edge. */
    private void well(GuiGraphicsExtractor g, int dx, int dy, int w, int h) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x, y, x + w, y + h, INK);
        g.fill(x + 1, y + 1, x + w, y + h, WELL);
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
        Component mode = Component.translatable(launch
                ? "gui.nerospace.launch_controller.mode_launch"
                : "gui.nerospace.launch_controller.mode_build");
        if (!mode.equals(this.modeButton.getMessage())) {
            this.modeButton.setMessage(mode);
            this.modeButton.setTooltip(Tooltip.create(mode));
        }
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
        boolean canBuild = this.menu.canBuild();
        this.buildButton.active = canBuild;
        String buildTip = canBuild + "," + this.menu.neededPads() + "," + this.menu.neededWall() + ","
                + this.menu.neededGantry();
        if (!buildTip.equals(this.buildTipKey)) {
            this.buildTipKey = buildTip;
            this.buildButton.setTooltip(Tooltip.create(canBuild
                    ? Component.translatable("gui.nerospace.launch_controller.build") : neededText()));
        }

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
        // Material slot glyphs above their wells (localisation-proof; the tooltip names them).
        for (int i = 0; i < SLOT_X.length; i++) {
            Glyphs.draw(g, SLOT_GLYPHS[i], this.leftPos + SLOT_X[i] + 4, this.topPos + 35, SUBTLE);
        }
        // What's still needed for the selected tier: glyph + "xN" triplets, the sentences in the tooltip.
        int[] needed = { this.menu.neededPads(), this.menu.neededWall(), this.menu.neededGantry() };
        for (int i = 0; i < 3; i++) {
            int x = 8 + 52 * i;
            Glyphs.draw(g, SLOT_GLYPHS[i], this.leftPos + x, this.topPos + 64, LABEL);
            label(g, Component.literal("x" + needed[i]), x + 10, 64, needed[i] > 0 ? LABEL : SUBTLE);
        }
        // The tier the pad actually forms, so a hand-built pad shows up here too.
        label(g, SpaceButton.fit(this.font, Component.translatable("gui.nerospace.launch_controller.built_tier",
                this.menu.builtTier()), 160), 8, 74, SUBTLE);

        // Resource hub levels (fed by pipes/cables → pumped into the docked rocket): glyph, bar, value.
        hubRow(g, 0, Glyphs.Glyph.FUEL, this.menu.fuelFrac(), FUEL);
        hubRow(g, 1, Glyphs.Glyph.O2, this.menu.oxygenFrac(), O2);
        hubRow(g, 2, Glyphs.Glyph.POWER, this.menu.powerFrac(), POWER);
    }

    private void hubRow(GuiGraphicsExtractor g, int k, Glyphs.Glyph glyph, float frac, int colour) {
        int y = 86 + 10 * k;
        Glyphs.draw(g, glyph, this.leftPos + 8, this.topPos + y, colour);
        hGauge(g, 20, y + 1, 120, 5, frac, colour);
        label(g, Component.literal(Math.round(Math.max(0F, Math.min(1F, frac)) * 100) + "%"), 146, y, VALUE);
    }

    /** The three "still needed" sentences (build-mode tooltip). */
    private Component neededText() {
        return Component.translatable("gui.nerospace.launch_controller.needed.pads", this.menu.neededPads())
                .append("\n").append(Component.translatable("gui.nerospace.launch_controller.needed.wall",
                        this.menu.neededWall()))
                .append("\n").append(Component.translatable("gui.nerospace.launch_controller.needed.gantry",
                        this.menu.neededGantry()));
    }

    /** Tooltips over the glyph-only rows (slot glyphs, needed triplets, hub rows). */
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractContents(g, mouseX, mouseY, partialTick);
        if (this.menu.isLaunchMode()) {
            return;
        }
        int mx = mouseX - this.leftPos;
        int my = mouseY - this.topPos;
        Component tip = null;
        for (int i = 0; i < SLOT_X.length; i++) {
            if (mx >= SLOT_X[i] && mx < SLOT_X[i] + 16 && my >= 34 && my < 44) {
                tip = Component.translatable("gui.nerospace.launch_controller." + SLOT_KEYS[i]);
            }
        }
        if (mx >= 8 && mx < 168 && my >= 64 && my < 73) {
            tip = neededText();
        }
        String[] hub = { "fuel", "oxygen", "power" };
        for (int k = 0; k < 3; k++) {
            int y = 86 + 10 * k;
            if (mx >= 8 && mx < 168 && my >= y && my < y + 9) {
                tip = Component.translatable("gui.nerospace.launch_controller.hub." + hub[k]);
            }
        }
        if (tip != null) {
            g.setTooltipForNextFrame(this.font.split(tip, 200), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, SpaceButton.fit(this.font, this.title, 108), this.titleLabelX, this.titleLabelY,
                TITLE, false);
    }

    private void drawLaunchMode(GuiGraphicsExtractor g, boolean rocket) {
        if (!rocket) {
            label(g, Component.translatable("gui.nerospace.launch_controller.no_rocket"), 8, 42, SUBTLE);
            return;
        }
        label(g, Component.translatable("gui.nerospace.rocket.title", this.menu.rocketTier()), 10, 22, TITLE);
        label(g, Component.translatable("gui.nerospace.rocket.fuel_label"), 10, 33, 0xFFFFC9B0);
        hGauge(g, 10, 44, 156, 6, this.menu.rocketFuelFrac(), FUEL);
        label(g, Component.translatable("gui.nerospace.rocket.oxygen_label"), 10, 53, 0xFFBFEFFF);
        hGauge(g, 10, 64, 156, 6, this.menu.rocketOxygenFrac(), O2);
        label(g, Component.translatable("gui.nerospace.rocket.power_label"), 10, 73, 0xFFF6DC8A);
        hGauge(g, 10, 84, 156, 6, this.menu.rocketPowerFrac(), POWER);

        long now = System.currentTimeMillis();
        int synced = this.menu.rocketDestIndex();
        if (this.pending.reconcile(synced, this.menu.lastOutcomeCode(), now) == PendingSelection.Result.REFUSED) {
            showStatus(Component.translatable(DestinationSelection.refusalKey(this.pending.refusal())), now);
        }
        int shown = this.pending.shown(synced);
        ResourceKey<Level> key = Destinations.byIndex(shown);
        this.destButton.setMessage(Component.translatable("gui.nerospace.launch_controller.destination",
                key == null ? Component.literal("—") : Destinations.displayName(key)));
        // Worlds whose Core gate is still closed for this viewer are never offered (a creative viewer has none).
        int mask = this.menu.rocketDestMask() & ~this.menu.gateMask();
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
        int predicted = DestinationSelection.step(mask, this.menu.gateMask(), current, dir, Destinations.all().size());
        DestinationTrace.log("controller screen {} step {} from {} mask {} -> {}", this.menu.containerId, dir,
                current, mask, predicted);
        if (predicted == current) {
            showStatus(Component.translatable("gui.nerospace.dest.reason.only_one"), now);
            return;
        }
        this.pending.begin(predicted, synced, this.menu.lastOutcomeCode(), now);
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
        int y = this.topPos + 96;
        if (destControlsLive() && scrollY != 0.0D && mouseX >= x && mouseX < x + 160 && mouseY >= y
                && mouseY < y + 12) {
            playClick();
            stepDestination(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Up/Down step the destination; Enter launches when ready (launch mode). */
    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
    *///?}
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
        //? if >=1.21.9 {
        return super.keyPressed(event);
        //?} else {
        /*return super.keyPressed(event.key(), event.scancode(), event.modifiers());
        *///?}
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
