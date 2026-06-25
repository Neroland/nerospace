package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketMenu;
import za.co.neroland.nerospace.rocket.RocketTier;

/**
 * The interactive in-rocket UI: a sci-fi panel with a fuel gauge (intake slot beside it), an
 * interactive trajectory row (one {@link SpaceButton} per reachable planet, the chosen one lit), and
 * a Launch button. Built from custom widgets + gauges drawn on the hull panel (26.1 render model).
 *
 * <p>Cross-loader port note: the multi-station founding row (a station cycler + the FOUND node) is
 * deferred with that subsystem, so this screen shows the planet trajectory + launch only. Destination
 * buttons are built for the full destination order and enabled per the live (synced) tier.</p>
 */
public class RocketScreen extends TexturedContainerScreen<RocketMenu> {

    private static final Identifier TEXTURE =
            NerospaceCommon.id("textures/gui/rocket.png");
    private static final int ACCENT = 0xFFE0506A;     // rocket red
    private static final int FUEL = 0xFFF0703C;       // fuel orange-red

    /** The full destination order (a prefix of which is reachable per tier). */
    private static final List<ResourceKey<Level>> ALL_DESTINATIONS = RocketTier.TIER_4.destinations();

    private SpaceButton launchButton;
    private SpaceButton stationButton;
    private final List<SpaceButton> destinationButtons = new ArrayList<>();

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 10_000; // hide the redundant inventory label
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        // Planet row: one node per destination in the global order; enabled per the live tier.
        int x = this.leftPos + 28;
        for (int i = 0; i < ALL_DESTINATIONS.size(); i++) {
            final int index = i;
            String destinationName = NerospaceCommon.requireNonNull(Destinations.name(
                    NerospaceCommon.requireNonNull(ALL_DESTINATIONS.get(i))));
            SpaceButton node = new SpaceButton(x, this.topPos + 36, 34, 14,
                    Component.literal(shortName(destinationName)), ACCENT,
                    b -> onSelectDestination(index));
            this.addRenderableWidget(node);
            this.destinationButtons.add(node);
            x += 36;
        }

        // Station dock cycler: shown only when the Orbital Station is the chosen destination.
        this.stationButton = new SpaceButton(this.leftPos + 8, this.topPos + 52, 160, 12,
                Component.empty(), ACCENT, b -> onCycleStation());
        this.addRenderableWidget(this.stationButton);

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

        int reachable = this.menu.getTier().destinations().size();
        int selected = this.menu.getDestinationIndex();
        for (int i = 0; i < this.destinationButtons.size(); i++) {
            SpaceButton node = NerospaceCommon.requireNonNull(this.destinationButtons.get(i));
            node.active = i < reachable && reachable > 1;
            node.visible = i < reachable;
            node.setSelected(i == selected);
        }
        if (this.stationButton != null) {
            boolean stationDest = this.menu.isStationDestination();
            this.stationButton.visible = stationDest;
            this.stationButton.active = stationDest;
            if (stationDest) {
                this.stationButton.setMessage(
                        Component.literal("Dock: " + ClientStations.name(this.menu.getStationSlot())));
            }
        }
        if (this.launchButton != null) {
            this.launchButton.active = this.menu.isLaunchable();
        }

        // A dotted trajectory arc from the pad to the selected node.
        if (selected >= 0 && selected < this.destinationButtons.size()) {
            SpaceButton target = NerospaceCommon.requireNonNull(this.destinationButtons.get(selected));
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

    private static String shortName(
            String full) {
        return NerospaceCommon.requireNonNull(switch (full) {
            case "Orbital Station" -> "Station";
            case "Greenxertz" -> "Xertz";
            default -> full;
        });
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
        MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
