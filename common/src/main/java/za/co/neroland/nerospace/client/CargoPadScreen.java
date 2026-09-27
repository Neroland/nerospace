package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.route.FlightRequestResult;
import za.co.neroland.nerospace.api.route.FlightState;
import za.co.neroland.nerospace.api.route.ScheduleMode;
import za.co.neroland.nerospace.route.CargoPadMenu;

/**
 * The Cargo Pad console: the 3×9 hold on top, then destination / schedule / fuel / launch controls, then
 * the player inventory. Fully procedural hull (no texture). Everything shown comes from the menu's synced
 * data and the {@link ClientCargoPads} label list; every button is a server-validated menu button, and the
 * screen only greys out what the server will refuse anyway.
 */
public class CargoPadScreen extends TexturedContainerScreen<CargoPadMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFF3CC8E6;   // freight cyan
    private static final int FUEL = 0xFFF0703C;
    private static final int OK = 0xFF54D46A;
    private static final int WARN = 0xFFF5C542;
    private static final int BAD = 0xFFF06060;

    private static final int W = 176;
    private static final int H = 236;
    private static final int ROW_DEST = 76;
    private static final int ROW_SCHEDULE = 90;
    private static final int ROW_FUEL = 105;
    private static final int ROW_ACTIONS = 122;
    private static final int ROW_STATUS = 138;

    private SpaceButton prevDest;
    private SpaceButton nextDest;
    private SpaceButton clearDest;
    private SpaceButton scheduleButton;
    private SpaceButton intervalDown;
    private SpaceButton intervalUp;
    private SpaceButton returnButton;
    private SpaceButton publicButton;
    private SpaceButton launchButton;

    public CargoPadScreen(CargoPadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = 5;
        this.inventoryLabelY = 10_000; // hidden — the hull border says it all
    }

    @Override
    protected void init() {
        super.init();
        int l = this.leftPos;
        int t = this.topPos;
        this.prevDest = add(new SpaceButton(l + 8, t + ROW_DEST, 12, 12, Component.literal("<"), ACCENT,
                b -> send(CargoPadMenu.BUTTON_PREV_DEST)));
        this.nextDest = add(new SpaceButton(l + W - 34, t + ROW_DEST, 12, 12, Component.literal(">"), ACCENT,
                b -> send(CargoPadMenu.BUTTON_NEXT_DEST)));
        this.clearDest = add(new SpaceButton(l + W - 20, t + ROW_DEST, 12, 12, Component.literal("x"), BAD,
                b -> send(CargoPadMenu.BUTTON_CLEAR_DEST)));
        this.scheduleButton = add(new SpaceButton(l + 8, t + ROW_SCHEDULE, 104, 12, Component.empty(), ACCENT,
                b -> send(CargoPadMenu.BUTTON_CYCLE_SCHEDULE)));
        this.intervalDown = add(new SpaceButton(l + 116, t + ROW_SCHEDULE, 12, 12, Component.literal("-"), ACCENT,
                b -> send(CargoPadMenu.BUTTON_INTERVAL_DOWN)));
        this.intervalUp = add(new SpaceButton(l + W - 20, t + ROW_SCHEDULE, 12, 12, Component.literal("+"), ACCENT,
                b -> send(CargoPadMenu.BUTTON_INTERVAL_UP)));
        this.returnButton = add(new SpaceButton(l + 8, t + ROW_ACTIONS, 52, 12, Component.empty(), ACCENT,
                b -> send(CargoPadMenu.BUTTON_TOGGLE_RETURN)));
        this.publicButton = add(new SpaceButton(l + 63, t + ROW_ACTIONS, 44, 12, Component.empty(), WARN,
                b -> send(CargoPadMenu.BUTTON_TOGGLE_PUBLIC)));
        this.launchButton = add(new SpaceButton(l + 110, t + ROW_ACTIONS, W - 118, 12,
                Component.translatable("gui.nerospace.cargo_pad.launch"), OK,
                b -> send(CargoPadMenu.BUTTON_LAUNCH)));
    }

    private SpaceButton add(SpaceButton button) {
        this.addRenderableWidget(button);
        return button;
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, 0xFF0E1724);
        g.fill(l, t, l + W, t + 15, 0xFF14283C);
        g.fill(l, t + 15, l + W, t + 16, ACCENT);
        // Hold wells (locked slots beyond the server cap are drawn darker).
        int active = this.menu.activeSlots();
        for (int i = 0; i < 27; i++) {
            int x = l + CargoPadMenu.GRID_X + (i % 9) * 18;
            int y = t + CargoPadMenu.GRID_Y + (i / 9) * 18;
            g.fill(x - 1, y - 1, x + 17, y + 17, INK);
            g.fill(x, y, x + 16, y + 16, i < active ? 0xFF0A1018 : 0xFF07090C);
        }
        // Player inventory wells.
        for (int i = 0; i < 36; i++) {
            int col = i % 9;
            int row = i / 9;
            int x = l + CargoPadMenu.INV_X + col * 18;
            int y = t + CargoPadMenu.INV_Y + (row < 3 ? row * 18 : 58);
            g.fill(x - 1, y - 1, x + 17, y + 17, INK);
            g.fill(x, y, x + 16, y + 16, 0xFF0A1018);
        }
        g.fill(l + 6, t + ROW_DEST - 3, l + W - 6, t + ROW_DEST - 2, 0x33FFFFFF);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        boolean manage = this.menu.canManage();
        int destIndex = this.menu.destinationIndex();
        int destCount = this.menu.destinationCount();

        // --- Destination row -------------------------------------------------------------
        this.prevDest.active = manage && destCount > 0;
        this.nextDest.active = manage && destCount > 0;
        this.clearDest.active = manage && destIndex >= 0;
        String destLabel = destIndex >= 0 ? ClientCargoPads.label(this.menu.containerId, destIndex)
                : Component.translatable(destCount > 0 ? "gui.nerospace.cargo_pad.no_destination"
                        : "gui.nerospace.cargo_pad.no_pads").getString();
        label(g, Component.literal(clip(destLabel, 19)), 24, ROW_DEST + 2, destIndex >= 0 ? TITLE : SUBTLE);

        // --- Schedule row ----------------------------------------------------------------
        ScheduleMode mode = this.menu.scheduleMode();
        this.scheduleButton.active = manage && destIndex >= 0;
        this.scheduleButton.setMessage(switch (mode) {
            case MANUAL -> Component.translatable("gui.nerospace.cargo_pad.schedule.manual");
            case WHEN_FULL -> Component.translatable("gui.nerospace.cargo_pad.schedule.when_full");
            case EVERY_INTERVAL -> Component.translatable("gui.nerospace.cargo_pad.schedule.every", this.menu.intervalMinutes());
        });
        boolean interval = mode == ScheduleMode.EVERY_INTERVAL;
        this.intervalDown.active = manage && interval;
        this.intervalUp.active = manage && interval;
        this.intervalDown.visible = interval;
        this.intervalUp.visible = interval;
        if (interval) {
            labelCentered(g, Component.literal(this.menu.intervalMinutes() + "m"), 128, 28, ROW_SCHEDULE + 2, SUBTLE);
        }

        // --- Fuel row --------------------------------------------------------------------
        label(g, Component.translatable("gui.nerospace.cargo_pad.buffer"), 8, ROW_FUEL, 0xFFFFC9B0);
        hGauge(g, 44, ROW_FUEL, 40, 6, this.menu.fuelFrac(), FUEL);
        if (this.menu.rocketPresent()) {
            label(g, Component.translatable("gui.nerospace.cargo_pad.rocket"), 90, ROW_FUEL, 0xFFFFC9B0);
            hGauge(g, 128, ROW_FUEL, W - 136, 6, this.menu.rocketFuelFrac(), FUEL);
        } else {
            label(g, Component.translatable("gui.nerospace.cargo_pad.no_rocket"), 90, ROW_FUEL, SUBTLE);
        }
        int quoteFuel = this.menu.quoteFuel();
        if (destIndex >= 0 && quoteFuel > 0) {
            int secs = this.menu.quoteSeconds();
            boolean enough = this.menu.rocketFuel() >= quoteFuel;
            label(g, Component.translatable("gui.nerospace.cargo_pad.quote", quoteFuel, secs / 60, String.format("%02d", secs % 60)),
                    8, ROW_FUEL + 8, enough ? 0xFFB9C6D4 : WARN);
        } else {
            label(g, Component.translatable("gui.nerospace.cargo_pad.hold", this.menu.usedSlots(), this.menu.activeSlots()),
                    8, ROW_FUEL + 8, SUBTLE);
        }

        // --- Actions row -----------------------------------------------------------------
        this.returnButton.active = manage && destIndex >= 0;
        this.returnButton.setSelected(this.menu.returnEmpty());
        this.returnButton.setMessage(Component.translatable("gui.nerospace.cargo_pad.return_empty"));
        this.publicButton.active = manage;
        this.publicButton.setSelected(this.menu.isPublic());
        this.publicButton.setMessage(Component.translatable(this.menu.isPublic()
                ? "gui.nerospace.cargo_pad.public" : "gui.nerospace.cargo_pad.private"));
        this.launchButton.active = this.menu.launchReady();

        // --- Status line -----------------------------------------------------------------
        Component status;
        int colour;
        int verdict = this.menu.verdict();
        if (verdict == 1) {
            status = Component.translatable("gui.nerospace.cargo_pad.verdict.accepted");
            colour = OK;
        } else if (verdict >= 2) {
            FlightRequestResult.Denial[] denials = FlightRequestResult.Denial.values();
            int ordinal = Math.min(denials.length - 1, verdict - 2);
            status = Component.translatable("gui.nerospace.cargo_pad.denial." + denials[ordinal].name().toLowerCase(java.util.Locale.ROOT));
            colour = BAD;
        } else if (this.menu.padTier() < 2) {
            status = Component.translatable("gui.nerospace.cargo_pad.need_pad", this.menu.padTier());
            colour = WARN;
        } else if (this.menu.lastFlightState() >= 0) {
            FlightState state = FlightState.values()[Math.min(FlightState.values().length - 1, this.menu.lastFlightState())];
            status = state == FlightState.IN_FLIGHT
                    ? Component.translatable("gui.nerospace.cargo_pad.in_flight", this.menu.lastFlightSecondsLeft())
                    : Component.translatable("gui.nerospace.cargo_pad.state." + state.name().toLowerCase(java.util.Locale.ROOT));
            colour = state == FlightState.DROPPED ? BAD : state == FlightState.HOLDING ? WARN : 0xFFB9C6D4;
        } else {
            status = Component.translatable("gui.nerospace.cargo_pad.idle");
            colour = SUBTLE;
        }
        label(g, status, 8, ROW_STATUS, colour);
        int inbound = this.menu.inbound();
        if (inbound > 0) {
            labelCentered(g, Component.translatable("gui.nerospace.cargo_pad.inbound", inbound), W - 60, 52, ROW_STATUS, ACCENT);
        }
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
