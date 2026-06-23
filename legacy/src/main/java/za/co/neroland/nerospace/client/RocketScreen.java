package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The interactive in-rocket UI (Phase 9b + multi-station rework): a sci-fi panel with a fuel gauge
 * (intake slot beside it), an interactive trajectory row (one {@link SpaceButton} per reachable
 * planet, the chosen one lit), a station row (a cycler through founded player stations plus the
 * FOUND node when the rider carries a Station Charter — MULTI_STATION_DESIGN.md §4), and a Launch
 * button. Built from custom widgets + gauges drawn on the hull panel (26.1 render model).
 */
public class RocketScreen extends TexturedContainerScreen<RocketMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFFE0506A;     // rocket red
    private static final int FUEL = 0xFFF0703C;       // fuel orange-red
    private static final int STATION = 0xFF6AC8E0;    // station cyan

    private SpaceButton launchButton;
    private SpaceButton stationButton;
    private SpaceButton foundButton;
    private final List<SpaceButton> destinationButtons = new ArrayList<>();

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
        // The Launch row now occupies the strip above the inventory; drop the redundant label.
        this.inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        // Planet row: up to four tier destinations at a compact pitch.
        List<ResourceKey<Level>> destinations = this.menu.getTier().destinations();
        int x = this.leftPos + 28;
        for (int i = 0; i < destinations.size(); i++) {
            final int index = i;
            SpaceButton node = new SpaceButton(x, this.topPos + 36, 34, 14,
                    Component.literal(shortName(Destinations.name(destinations.get(i)))), ACCENT,
                    b -> onSelectDestination(index));
            node.active = destinations.size() > 1;
            this.addRenderableWidget(node);
            this.destinationButtons.add(node);
            x += 36;
        }

        // Station row: one cycler through all founded stations + the FOUND node when chartered.
        this.stationButton = new SpaceButton(this.leftPos + 8, this.topPos + 52, 112, 14,
                stationLabel(), STATION, b -> onCycleStation());
        this.stationButton.active = !this.menu.getStations().isEmpty();
        this.addRenderableWidget(this.stationButton);

        if (this.menu.canFound()) {
            this.foundButton = new SpaceButton(this.leftPos + 124, this.topPos + 52, 44, 14,
                    Component.translatable("gui.nerospace.rocket.found"), STATION, b -> onSelectFound());
            this.addRenderableWidget(this.foundButton);
        } else {
            this.foundButton = null;
        }

        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + 68, 160, 14,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT, b -> onLaunch());
        this.addRenderableWidget(this.launchButton);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int pct = this.menu.getFuelPercent();
        label(g, Component.literal("Fuel: " + pct + "%   " + this.menu.getFuel() + " / " + this.menu.getCapacity() + " mB"),
                8, 17, 0xFFFFC9B0);
        fluidGauge(g, 8, 27, 130, 5, pct / 100f, FUEL);
        label(g, Component.literal("PAD >"), 8, 39, 0xFF9FB4C8);

        // Reflect live selection / launch-readiness onto the widgets.
        int station = this.menu.getStationSelection();
        int selected = this.menu.getDestinationIndex();
        for (int i = 0; i < this.destinationButtons.size(); i++) {
            this.destinationButtons.get(i).setSelected(station == RocketEntity.STATION_NONE && i == selected);
        }
        if (this.stationButton != null) {
            this.stationButton.setMessage(stationLabel());
            this.stationButton.setSelected(station >= 0);
        }
        if (this.foundButton != null) {
            this.foundButton.setSelected(station == RocketEntity.STATION_FOUND);
        }
        if (this.launchButton != null) {
            this.launchButton.active = this.menu.isLaunchable();
        }

        // A dotted trajectory arc from the pad to the selected node (planet, station, or FOUND).
        SpaceButton target = null;
        if (station == RocketEntity.STATION_FOUND && this.foundButton != null) {
            target = this.foundButton;
        } else if (station >= 0 && this.stationButton != null) {
            target = this.stationButton;
        } else if (selected >= 0 && selected < this.destinationButtons.size()) {
            target = this.destinationButtons.get(selected);
        }
        if (target != null) {
            int x0 = this.leftPos + 26;
            int y0 = this.topPos + 45;
            int x1 = target.getX() + target.getWidth() / 2;
            int y1 = target.getY();
            int segments = 16;
            for (int i = 0; i <= segments; i++) {
                float t = i / (float) segments;
                int ax = Math.round(x0 + (x1 - x0) * t);
                int ay = Math.round(y0 + (y1 - y0) * t - Mth.sin(t * Mth.PI) * 11.0F);
                g.fill(ax, ay, ax + 2, ay + 2, ACCENT);
            }
        }
    }

    /** The station cycler's label: the selected station, the count, or the empty notice. */
    private Component stationLabel() {
        int station = this.menu.getStationSelection();
        if (station >= 0) {
            return Component.translatable("gui.nerospace.rocket.station_node",
                    truncate(this.menu.stationName(station), 14));
        }
        if (this.menu.getStations().isEmpty()) {
            return Component.translatable("gui.nerospace.rocket.stations_none");
        }
        return Component.literal("STATIONS (" + this.menu.getStations().size() + ")");
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String shortName(String full) {
        return switch (full) {
            case "Orbital Station" -> "Station";
            case "Greenxertz" -> "Xertz";
            default -> full;
        };
    }

    private void onSelectDestination(int index) {
        sendButton(RocketMenu.SELECT_DEST_BASE + index);
    }

    private void onCycleStation() {
        sendButton(RocketMenu.BUTTON_CYCLE_STATION);
    }

    private void onSelectFound() {
        sendButton(RocketMenu.BUTTON_SELECT_FOUND);
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
