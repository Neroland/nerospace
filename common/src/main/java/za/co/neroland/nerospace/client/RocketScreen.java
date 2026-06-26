package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The in-rocket flight console: a large, fully procedural sci-fi panel (no texture asset). The left
 * half is an animated viewport — a scrolling starfield, the selected destination rendered as a glowing
 * planet, and the rocket flying a dotted trajectory toward it. The right half is the instrument cluster:
 * a fuel gauge, an oxygen (life-support) gauge with its loading readout, the fuel-intake slot, and a
 * pad-readiness light. A trajectory row of planet nodes selects the destination, an overlaid dock cycler
 * appears for the Orbital Station, and a wide Launch button (which pulses when ready) boards the player
 * and lifts off.
 *
 * <p>Built entirely from {@code fill}s on the {@link TexturedContainerScreen} hull helpers + custom
 * widgets, so it animates every frame without a PNG. The player is NOT aboard while the console is open;
 * pressing Launch is what boards them (see {@code RocketMenu.boardAndLaunch}).</p>
 */
public class RocketScreen extends TexturedContainerScreen<RocketMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFFE0506A; // rocket red
    private static final int FUEL = 0xFFF0703C;   // fuel orange-red
    private static final int O2 = 0xFF3CC8E6;     // oxygen cyan
    private static final int POWER = 0xFFF5C542;  // power amber
    private static final int GOOD = 0xFF54D46A;   // ready green

    private static final int W = 212;
    private static final int H = 244;

    // Viewport (panel-relative).
    private static final int VX = 8;
    private static final int VY = 20;
    private static final int VW = 92;
    private static final int VH = 86;

    // Stacked control rows below the viewport / instruments (panel-relative Y).
    private static final int DEST_ROW_Y = 110;   // destination node row
    private static final int DOCK_ROW_Y = 127;   // dock cycler (only for the Orbital Station)
    private static final int LAUNCH_ROW_Y = 142; // launch button
    // Right instrument column (panel-relative).
    private static final int RX = 108;
    private static final int GW = 72;
    // Fuel-intake slot recess (must match RocketMenu's FUEL_SLOT_X/Y).
    private static final int SLOT_X = 186;
    private static final int SLOT_Y = 30;

    /** The full global destination order; the synced mask decides which nodes are visible. */
    private static final List<ResourceKey<Level>> ALL_DESTINATIONS = Destinations.all();

    private SpaceButton launchButton;
    private SpaceButton stationButton;
    private final List<SpaceButton> destinationButtons = new ArrayList<>();

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = -100;        // header drawn manually in extractForeground
        this.inventoryLabelY = 10_000;  // hide the redundant inventory label
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        // Trajectory node row: one node per destination in the global order, centred under the panel.
        int count = ALL_DESTINATIONS.size();
        int nodeW = 36;
        int gap = 2;
        int rowW = count * nodeW + (count - 1) * gap;
        int x = this.leftPos + Math.max(8, (W - rowW) / 2);
        for (int i = 0; i < count; i++) {
            final int index = i;
            SpaceButton node = new SpaceButton(x, this.topPos + DEST_ROW_Y, nodeW, 15,
                    Component.literal(shortName(Destinations.name(ALL_DESTINATIONS.get(i)))), ACCENT,
                    b -> onSelectDestination(index));
            this.addRenderableWidget(node);
            this.destinationButtons.add(node);
            x += nodeW + gap;
        }

        // Dock cycler: its own full-width row (only shown for the Orbital Station destination), so the
        // long "Dock: <name>" label has room and never spills over the instrument column.
        this.stationButton = new SpaceButton(this.leftPos + 8, this.topPos + DOCK_ROW_Y, W - 16, 11,
                Component.empty(), ACCENT, b -> onCycleStation());
        this.addRenderableWidget(this.stationButton);

        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + LAUNCH_ROW_Y, W - 16, 16,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT, b -> onLaunch());
        this.addRenderableWidget(this.launchButton);
    }

    // --- Procedural hull -----------------------------------------------------

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);     // outer rim
        g.fill(l, t, l + W, t + H, 0xFF0E1724);              // hull body
        g.fill(l, t, l + W, t + 18, 0xFF14283C);             // header band
        g.fill(l, t + 18, l + W, t + 19, ACCENT);            // accent rule under the header
        well(g, l + VX, t + VY, VW, VH);                     // viewport recess
        well(g, l + RX - 2, t + 22, W - RX - 6, 82);         // instrument cluster backing (106..204)
        slotWell(g, SLOT_X, SLOT_Y);                         // fuel-intake recess (under the slot item)
        rivet(g, l + 3, t + 3);
        rivet(g, l + W - 5, t + 3);
        rivet(g, l + 3, t + H - 5);
        rivet(g, l + W - 5, t + H - 5);
    }

    private void well(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
        g.fill(x, y, x + w, y + h, 0xFF0A1018);
    }

    private void rivet(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFF35506A);
    }

    // --- Live readouts -------------------------------------------------------

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();

        label(g, Component.translatable("gui.nerospace.rocket.title", this.menu.getTier().level()),
                10, 6, TITLE);

        drawViewport(g, now);

        // Three compact tank gauges: fuel, oxygen (life support), and power.
        int fpct = this.menu.getFuelPercent();
        label(g, Component.translatable("gui.nerospace.rocket.fuel_line", fpct), RX, 22, 0xFFFFC9B0);
        fluidGauge(g, RX, 31, GW, 6, fpct / 100f, FUEL);

        int opct = this.menu.getOxygenPercent();
        label(g, Component.translatable("gui.nerospace.rocket.oxygen_line", opct), RX, 44, 0xFFBFEFFF);
        fluidGauge(g, RX, 53, GW, 6, opct / 100f, O2);

        int ppct = this.menu.getPowerPercent();
        label(g, Component.translatable("gui.nerospace.rocket.power_line", ppct), RX, 66, 0xFFF6DC8A);
        segGauge(g, RX, 75, GW, 6, ppct / 100f, POWER);

        // Status light — report the ACTUAL blocker: an insufficient pad, an empty tank, or all-clear.
        boolean padValid = this.menu.isPadValid();
        boolean ready = this.menu.isLaunchable();
        String statusKey;
        int statusColor;
        if (!padValid) {
            statusKey = "gui.nerospace.rocket.pad_blocked";
            statusColor = ACCENT;
        } else if (!ready) {
            statusKey = "gui.nerospace.rocket.needs_fuel";
            statusColor = 0xFFF0A83C; // amber: pad's fine, just needs fuelling
        } else {
            statusKey = "gui.nerospace.rocket.pad_ready";
            statusColor = GOOD;
        }
        label(g, Component.translatable(statusKey), RX, 90, statusColor);

        // Destination node states from the live tier mask.
        int mask = this.menu.getDestinationMask();
        int reachable = Integer.bitCount(mask);
        int selected = this.menu.getDestinationIndex();
        for (int i = 0; i < this.destinationButtons.size(); i++) {
            SpaceButton node = this.destinationButtons.get(i);
            boolean visible = (mask & (1 << i)) != 0;
            node.visible = visible;
            node.active = visible && reachable > 1;
            node.setSelected(i == selected);
        }

        // Dock cycler (only when the Orbital Station is the chosen destination).
        if (this.stationButton != null) {
            boolean stationDest = this.menu.isStationDestination();
            this.stationButton.visible = stationDest;
            this.stationButton.active = stationDest;
            if (stationDest) {
                this.stationButton.setMessage(Component.translatable("gui.nerospace.rocket.dock",
                        ClientStations.name(this.menu.getStationSlot())));
            }
        }

        // Launch button: enabled when ready, with a soft ready-pulse on its border.
        if (this.launchButton != null) {
            this.launchButton.active = ready;
            this.launchButton.setSelected(ready && ((now / 500L) & 1L) == 0L);
        }
    }

    /** The animated viewport: scrolling starfield, the destination planet, and the rocket's trajectory. */
    private void drawViewport(GuiGraphicsExtractor g, long now) {
        int x0 = this.leftPos + VX;
        int y0 = this.topPos + VY;

        // Scrolling starfield.
        for (int i = 0; i < 30; i++) {
            int sx = x0 + 3 + Math.floorMod(hash(i, 0x9E3779B1), VW - 6);
            int scroll = (int) (now / 40L) + i * 7;
            int sy = y0 + 3 + Math.floorMod(hash(i, 0x85EBCA77) + scroll, VH - 6);
            int twinkle = (int) ((now / 200L) + i) & 3;
            int c = twinkle == 0 ? 0xFFFFFFFF : (twinkle == 1 ? 0xFFBFD4EC : 0xFF6E84A0);
            g.fill(sx, sy, sx + 1, sy + 1, c);
        }

        // Destination planet.
        int selected = this.menu.getDestinationIndex();
        int pc = planetColor(selected);
        int cx = x0 + VW / 2;
        int cy = y0 + 30;
        disc(g, cx, cy, 15, dim(pc, 0.35F)); // halo
        disc(g, cx, cy, 12, pc);
        g.fill(cx - 6, cy - 7, cx - 1, cy - 2, light(pc)); // specular highlight

        // Rocket flying a dotted trajectory up toward the planet, bobbing gently.
        int bob = Math.round(Mth.sin(now / 300.0F) * 2.0F);
        int rx = cx;
        int ry = y0 + VH - 16 + bob;
        int segments = 12;
        for (int i = 1; i < segments; i++) {
            float s = i / (float) segments;
            int ax = Math.round(rx + (cx - rx) * s);
            int ay = Math.round(ry + (cy - ry) * s);
            if (((i + (now / 150L)) & 1L) == 0L) {
                g.fill(ax, ay, ax + 1, ay + 1, ACCENT);
            }
        }
        drawRocketIcon(g, rx, ry, now);

        // Destination name along the viewport floor.
        if (selected >= 0 && selected < ALL_DESTINATIONS.size()) {
            labelCentered(g, Component.literal(Destinations.name(ALL_DESTINATIONS.get(selected))),
                    VX, VW, VY + VH - 10, TITLE);
        }
    }

    private void drawRocketIcon(GuiGraphicsExtractor g, int cx, int by, long now) {
        g.fill(cx - 2, by - 8, cx + 2, by, 0xFFD9DEE6);     // body
        g.fill(cx - 1, by - 11, cx + 1, by - 8, ACCENT);    // nose
        g.fill(cx - 4, by - 3, cx - 2, by, ACCENT);         // left fin
        g.fill(cx + 2, by - 3, cx + 4, by, ACCENT);         // right fin
        int flame = (int) ((now / 100L) % 3L);              // flickering exhaust
        g.fill(cx - 1, by, cx + 1, by + 2 + flame, FUEL);
    }

    /** Filled circle (scanline fills); centre absolute. */
    private void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private void slotWell(GuiGraphicsExtractor g, int dx, int dy) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + 17, y + 17, INK);
        g.fill(x, y, x + 16, y + 16, 0xFF0A1018);
    }

    private static int hash(int i, int salt) {
        int h = i * 374761393 + salt;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    private static int planetColor(int index) {
        return switch (index) {
            case 0 -> 0xFF4A90D9; // Home
            case 1 -> 0xFF9FB4C8; // Orbital Station
            case 2 -> 0xFF54D46A; // Greenxertz
            case 3 -> 0xFFE0662C; // Cindara
            case 4 -> 0xFF9FE8FF; // Glacira
            default -> 0xFF9FB4C8;
        };
    }

    private static int dim(int argb, float f) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int gg = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int light(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 90);
        int gg = Math.min(255, ((argb >> 8) & 0xFF) + 90);
        int b = Math.min(255, (argb & 0xFF) + 90);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static String shortName(String full) {
        return switch (full) {
            case "Home" -> "Home";
            case "Orbital Station" -> "Station";
            case "Greenxertz" -> "Xertz";
            case "Cindara" -> "Cind";
            case "Glacira" -> "Glac";
            default -> full;
        };
    }

    private void onSelectDestination(int index) {
        sendButton(RocketMenu.SELECT_DEST_BASE + index);
    }

    private void onCycleStation() {
        sendButton(RocketMenu.BUTTON_CYCLE_STATION);
    }

    private void onLaunch() {
        sendButton(RocketMenu.BUTTON_LAUNCH);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
