package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.client.ui.PlanetAccents;
import za.co.neroland.nerospace.client.ui.PlanetCard;
import za.co.neroland.nerospace.client.ui.PlanetCardData;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketMenu;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The in-rocket flight console (004 §6, "list + card"): a vertical list of compact planet cards down the left
 * (three visible, scrolled by the mouse wheel or the two chevrons), the instrument well (fuel, oxygen, power,
 * status, trip cost) top-right, the selected destination's standard planet card under it, and the dock/pad
 * cycler and Launch button bottom-left. Hull 212 × 244 and every slot coordinate are unchanged.
 *
 * <p>Procedural (no PNG): Stage 6 redraws the card art; the layout, fields and states here are final. The
 * player is NOT aboard while the console is open; pressing Launch is what boards them.</p>
 */
public class RocketScreen extends TexturedContainerScreen<RocketMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFFE0506A; // rocket red
    private static final int FUEL = 0xFFF0703C;   // fuel orange-red
    private static final int O2 = 0xFF3CC8E6;     // oxygen cyan
    private static final int POWER = 0xFFF5C542;  // power amber
    private static final int GOOD = 0xFF54D46A;   // ready green
    private static final int HULL = 0xFF0D1119;
    private static final int WELL = 0xFF0A1018;

    private static final int W = 212;
    /** Base hull height with the vanilla 3 main-inventory rows; grows 18px per extra modded row. */
    private static final int BASE_H = 244;

    // Destination list column (panel-relative): three compact cards at y 20 / 52 / 84.
    private static final int LIST_X = 8;
    private static final int LIST_Y = 20;
    private static final int VISIBLE = 3;
    private static final int CHEVRON_Y = 118;
    // Instrument well.
    private static final int RX = 76;
    private static final int GW = 92;
    // Selected destination's standard card.
    private static final int CARD_X = 108;
    private static final int CARD_Y = 106;
    // Stacked control rows (values kept from 1.x).
    private static final int DOCK_ROW_Y = 128;
    private static final int LAUNCH_ROW_Y = 144;
    private static final int ROW_W = 96;

    private SpaceButton launchButton;
    private SpaceButton stationButton;
    private SpaceButton padButton;
    private SpaceButton upButton;
    private SpaceButton downButton;

    /** Index of the first visible list entry. */
    private int scroll;
    private boolean scrolledToSelection;
    private int mouseX;
    private int mouseY;
    /** Destination indices shown in the list this frame, in {@link Destinations} order. */
    private final List<Integer> entries = new ArrayList<>();
    /** Locked destinations a station Observatory reveals this frame (shown as LOCKED_TIER cards, 005 §6.6). */
    private final List<Integer> revealed = new ArrayList<>();
    /** A revealed locked destination the player clicked to preview in the standard card, or −1. */
    private int previewIndex = -1;

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W,
                BASE_H + (RocketMenu.mainRows(playerInventory) - 3) * 18);
        this.titleLabelY = -100;        // header drawn manually in extractForeground
        this.inventoryLabelY = 10_000;  // hide the redundant inventory label
    }

    @Override
    protected void init() {
        super.init();
        this.upButton = new SpaceButton(this.leftPos + LIST_X, this.topPos + CHEVRON_Y, 28, 8,
                Component.literal("▲"), ACCENT, b -> scrollBy(-1));
        this.addRenderableWidget(this.upButton);
        this.downButton = new SpaceButton(this.leftPos + LIST_X + 32, this.topPos + CHEVRON_Y, 28, 8,
                Component.literal("▼"), ACCENT, b -> scrollBy(1));
        this.addRenderableWidget(this.downButton);

        // Dock cycler (Orbital Station) and pad cycler (every other destination) share one row, 96 wide.
        this.stationButton = new SpaceButton(this.leftPos + 8, this.topPos + DOCK_ROW_Y, ROW_W, 12,
                Component.empty(), ACCENT, b -> sendButton(RocketMenu.BUTTON_CYCLE_STATION));
        this.addRenderableWidget(this.stationButton);
        this.padButton = new SpaceButton(this.leftPos + 8, this.topPos + DOCK_ROW_Y, ROW_W, 12,
                Component.empty(), O2, b -> sendButton(RocketMenu.BUTTON_CYCLE_PAD));
        this.addRenderableWidget(this.padButton);
        if (this.minecraft != null && this.minecraft.player != null && !RocketEntity.hasCharter(this.minecraft.player)) {
            // Client hint only (005 §8): "New Station" is skipped without a charter; the server re-checks.
            this.stationButton.setTooltip(Tooltip.create(Component.translatable("item.nerospace.station_charter.needed")));
        }

        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + LAUNCH_ROW_Y, ROW_W, 16,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT, b -> sendButton(RocketMenu.BUTTON_LAUNCH));
        this.addRenderableWidget(this.launchButton);
        this.scrolledToSelection = false;
    }

    // --- Procedural hull -----------------------------------------------------

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        int h = this.imageHeight; // grows with extra modded inventory rows
        g.fill(l - 2, t - 2, l + W + 2, t + h + 2, INK);     // outer rim
        g.fill(l, t, l + W, t + h, HULL);                    // hull body
        g.fill(l, t, l + W, t + 16, 0xFF14283C);             // header band
        g.fill(l, t + 16, l + W, t + 17, ACCENT);            // accent rule under the header
        well(g, l + LIST_X, t + LIST_Y, 64, 96);             // destination list column
        well(g, l + RX, t + 20, 96, 84);                     // instrument well
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (i == 0) {
                fuelSlotWell(g, slot.x, slot.y);
            } else {
                slotWell(g, slot.x, slot.y);
            }
        }
        rivet(g, l + 3, t + 3);
        rivet(g, l + W - 5, t + 3);
        rivet(g, l + 3, t + h - 5);
        rivet(g, l + W - 5, t + h - 5);
    }

    private void well(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
        g.fill(x, y, x + w, y + h, WELL);
    }

    private void rivet(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFF35506A);
    }

    // --- Live readouts -------------------------------------------------------

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        label(g, Component.translatable("gui.nerospace.rocket.title", this.menu.getTier().level()), 10, 4, TITLE);

        // Instrument well: three compact gauges, status and trip cost.
        int fpct = this.menu.getFuelPercent();
        label(g, Component.translatable("gui.nerospace.rocket.fuel_line", fpct), RX + 2, 22, 0xFFFFC9B0);
        fluidGauge(g, RX + 2, 31, GW, 6, fpct / 100f, FUEL);
        int opct = this.menu.getOxygenPercent();
        label(g, Component.translatable("gui.nerospace.rocket.oxygen_line", opct), RX + 2, 44, 0xFFBFEFFF);
        fluidGauge(g, RX + 2, 53, GW, 6, opct / 100f, O2);
        int ppct = this.menu.getPowerPercent();
        label(g, Component.translatable("gui.nerospace.rocket.power_line", ppct), RX + 2, 66, 0xFFF6DC8A);
        segGauge(g, RX + 2, 75, GW, 6, ppct / 100f, POWER);

        boolean padValid = this.menu.isPadValid();
        boolean ready = this.menu.isLaunchable();
        String statusKey;
        int statusColor;
        if (!padValid) {
            statusKey = "gui.nerospace.rocket.pad_blocked";
            statusColor = ACCENT;
        } else if (!ready) {
            statusKey = "gui.nerospace.rocket.needs_fuel";
            statusColor = 0xFFF0A83C;
        } else {
            statusKey = "gui.nerospace.rocket.pad_ready";
            statusColor = GOOD;
        }
        label(g, Component.translatable(statusKey), RX + 2, 86, statusColor);
        label(g, Component.translatable("gui.nerospace.rocket.cost", this.menu.getFuelCost()), RX + 2, 95, 0xFFCFE7FF);

        // Destination list: reachable destinations in Destinations order, plus the "you are here" node.
        rebuildEntries();
        int selected = this.menu.getDestinationIndex();
        if (!this.scrolledToSelection) {
            int at = this.entries.indexOf(selected);
            if (at >= 0) {
                this.scroll = Math.max(0, Math.min(at, this.entries.size() - VISIBLE));
            }
            this.scrolledToSelection = true;
        }
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.entries.size() - VISIBLE)));
        PlanetCardData hovered = null;
        for (int row = 0; row < VISIBLE; row++) {
            int i = this.scroll + row;
            if (i >= this.entries.size()) {
                break;
            }
            int x = this.leftPos + LIST_X;
            int y = this.topPos + LIST_Y + row * PlanetCard.COMPACT_H;
            PlanetCardData data = cardData(this.entries.get(i));
            boolean hover = PlanetCard.hit(x, y, true, this.mouseX, this.mouseY);
            PlanetCard.draw(g, this.font, x, y, data, true, hover, now);
            if (hover) {
                hovered = data;
            }
        }
        boolean scrollable = this.entries.size() > VISIBLE;
        if (this.upButton != null && this.downButton != null) {
            this.upButton.visible = scrollable;
            this.downButton.visible = scrollable;
            this.upButton.active = scrollable && this.scroll > 0;
            this.downButton.active = scrollable && this.scroll + VISIBLE < this.entries.size();
        }

        // The selected destination's standard card (or a clicked Observatory preview).
        ResourceKey<Level> selectedKey = Destinations.byIndex(selected);
        if (this.previewIndex >= 0 && this.revealed.contains(this.previewIndex)) {
            PlanetCard.draw(g, this.font, this.leftPos + CARD_X, this.topPos + CARD_Y, cardData(this.previewIndex),
                    false, false, now);
        } else if (selectedKey != null && (this.menu.getDestinationMask() & (1 << selected)) != 0) {
            PlanetCard.draw(g, this.font, this.leftPos + CARD_X, this.topPos + CARD_Y, cardData(selected), false,
                    false, now);
        }

        // Dock / pad cycler.
        boolean stationDest = this.menu.isStationDestination();
        boolean padDest = this.menu.isPadDestination();
        if (this.stationButton != null) {
            this.stationButton.visible = stationDest;
            this.stationButton.active = stationDest;
            if (stationDest) {
                this.stationButton.setMessage(Component.translatable("gui.nerospace.rocket.dock",
                        ClientStations.name(this.menu.getStationSlot())));
            }
        }
        if (this.padButton != null) {
            this.padButton.visible = padDest;
            this.padButton.active = padDest;
            if (padDest) {
                this.padButton.setMessage(Component.translatable("gui.nerospace.rocket.pad", padLabel(selectedKey)));
            }
        }

        if (this.launchButton != null) {
            this.launchButton.active = ready;
            this.launchButton.setSelected(ready && ((now / 500L) & 1L) == 0L);
        }
        if (hovered != null) {
            g.setComponentTooltipForNextFrame(this.font, PlanetCard.tooltip(hovered), this.mouseX, this.mouseY);
        }
    }

    private void rebuildEntries() {
        this.entries.clear();
        int mask = this.menu.getDestinationMask();
        int here = hereIndex();
        List<ResourceKey<Level>> all = Destinations.all();
        for (int i = 0; i < all.size(); i++) {
            if ((mask & (1 << i)) != 0 || i == here) {
                this.entries.add(i);
            }
        }
        this.revealed.clear();
        this.revealed.addAll(revealedLocked(mask, here, this.menu.getTier().level(), this.menu.getRevealCount()));
        this.entries.addAll(this.revealed);
    }

    /**
     * The next {@code count} locked destinations in tier order (unlock tier, then destination index): planets this
     * rocket's tier cannot reach yet, never the current world. A gate-locked world (Nyxara) is included and stays
     * unselectable. Planet data only.
     */
    static List<Integer> revealedLocked(int mask, int here, int rocketTier, int count) {
        List<Integer> out = new ArrayList<>();
        if (count <= 0) {
            return out;
        }
        List<int[]> locked = new ArrayList<>(); // {unlockTier, index}
        List<ResourceKey<Level>> all = Destinations.all();
        for (int i = 0; i < all.size(); i++) {
            PlanetDefinition planet = Planets.get(all.get(i));
            if ((mask & (1 << i)) == 0 && i != here && planet != null && planet.unlockTier() > rocketTier) {
                locked.add(new int[] { planet.unlockTier(), i });
            }
        }
        locked.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        for (int k = 0; k < Math.min(count, locked.size()); k++) {
            out.add(locked.get(k)[1]);
        }
        return out;
    }

    private int hereIndex() {
        return this.minecraft != null && this.minecraft.level != null
                ? Destinations.indexOf(this.minecraft.level.dimension()) : -1;
    }

    /** Fills a card from client state only (004 §5): planet traits, the synced clock/weather, the pad name. */
    private PlanetCardData cardData(int index) {
        ResourceKey<Level> key = Destinations.byIndex(index);
        if (key == null) {
            key = Level.OVERWORLD;
        }
        PlanetDefinition planet = Planets.get(key);
        PlanetTraits traits = planet != null ? planet.traits() : null;
        Level level = this.minecraft != null ? this.minecraft.level : null;
        boolean current = level != null && level.dimension().equals(key);
        int mask = this.menu.getDestinationMask();
        boolean reachable = (mask & (1 << index)) != 0;
        boolean here = current && !reachable;
        boolean gate = (this.menu.getGateMask() & (1 << index)) != 0;
        boolean tierLocked = this.revealed.contains(index); // an Observatory preview: full LOCKED_TIER card
        PlanetCardData.State state = PlanetCardData.stateFor(here, gate && !tierLocked, tierLocked,
                index == this.menu.getDestinationIndex());
        double gravity = traits != null ? traits.defaultGravity() : 1.0D;
        Hazard hazard = traits != null ? traits.hazard() : Hazard.NONE;
        boolean rad = traits != null && traits.radiation() > 0;
        boolean fixed = traits != null && traits.fixedClock();
        boolean hasDay = traits == null || !fixed;
        float phase = current ? (float) ClientPlanetClock.phase(level)
                : (planet != null && fixed ? (float) planet.pinnedPhase() : 0.5F);
        WeatherEvent weather = current ? ClientPlanetWeather.event(level) : WeatherEvent.NONE;
        float intensity = current ? ClientPlanetWeather.intensity(level) : 0.0F;
        OxygenManager.SuitTier suit = traits != null && traits.airless() ? OxygenManager.SuitTier.TIER_1
                : OxygenManager.SuitTier.NONE;
        return new PlanetCardData(key, Destinations.displayName(key), gravity, hazard, suit, rad, weather, intensity,
                phase, fixed, hasDay, chosenPad(key), state, planet != null ? planet.unlockTier() : 1);
    }

    private Optional<Component> chosenPad(ResourceKey<Level> key) {
        if (key.equals(za.co.neroland.nerospace.registry.ModDimensions.STATION_LEVEL)) {
            return Optional.of(Component.translatable("gui.nerospace.card.dock",
                    ClientStations.name(this.menu.getStationSlot())));
        }
        String dimId = key.identifier().toString();
        if (ClientPads.count(dimId) == 0) {
            return Optional.empty(); // no pad there: a Landing Pod will be placed
        }
        return Optional.of(Component.translatable("gui.nerospace.card.pad", padLabel(key)));
    }

    private String padLabel(@Nullable ResourceKey<Level> key) {
        int idx = this.menu.getPadIndex();
        if (idx < 0 || key == null) {
            return Component.translatable("gui.nerospace.card.pad_auto").getString();
        }
        String name = ClientPads.name(key.identifier().toString(), idx);
        return name != null ? name : this.menu.getPadName();
    }

    // --- Input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mask = this.menu.getDestinationMask();
            for (int row = 0; row < VISIBLE; row++) {
                int i = this.scroll + row;
                if (i >= this.entries.size()) {
                    break;
                }
                int x = this.leftPos + LIST_X;
                int y = this.topPos + LIST_Y + row * PlanetCard.COMPACT_H;
                if (PlanetCard.hit(x, y, true, event.x(), event.y())) {
                    int index = this.entries.get(i);
                    if (this.revealed.contains(index)) {
                        this.previewIndex = this.previewIndex == index ? -1 : index; // preview only, never selectable
                        return true;
                    }
                    this.previewIndex = -1;
                    boolean gate = (this.menu.getGateMask() & (1 << index)) != 0;
                    if ((mask & (1 << index)) != 0 && !gate && Integer.bitCount(mask) > 1) {
                        sendButton(RocketMenu.SELECT_DEST_BASE + index);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = this.leftPos + LIST_X;
        int y = this.topPos + LIST_Y;
        if (mouseX >= x && mouseX < x + PlanetCard.COMPACT_W && mouseY >= y
                && mouseY < y + VISIBLE * PlanetCard.COMPACT_H && scrollY != 0.0D) {
            scrollBy(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void scrollBy(int delta) {
        this.scroll = Math.max(0, Math.min(this.scroll + delta, Math.max(0, this.entries.size() - VISIBLE)));
    }

    // --- Slot wells ----------------------------------------------------------

    private void slotWell(GuiGraphicsExtractor g, int dx, int dy) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + 17, y + 17, INK);
        g.fill(x, y, x + 16, y + 16, WELL);
    }

    /** The fuel-intake recess: framed in fuel orange; pulses with a small arrow while empty. */
    private void fuelSlotWell(GuiGraphicsExtractor g, int dx, int dy) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        boolean empty = !this.menu.slots.get(0).hasItem();
        boolean lit = !empty || ((System.currentTimeMillis() / 500L) & 1L) == 0L;
        int rim = lit ? FUEL : dim(FUEL, 0.45F);
        g.fill(x - 2, y - 2, x + 18, y + 18, rim);
        g.fill(x - 1, y - 1, x + 17, y + 17, INK);
        g.fill(x, y, x + 16, y + 16, 0xFF1A1208);
        if (empty) {
            int ax = x + 8;
            g.fill(ax - 3, y - 7, ax + 3, y - 5, rim);
            g.fill(ax - 2, y - 5, ax + 2, y - 3, rim);
            g.fill(ax - 1, y - 3, ax + 1, y - 2, rim);
        }
    }

    private static int dim(int argb, float f) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int gg = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    /** Accent of the selected destination (kept for callers that tint by planet). */
    public static int accentFor(@Nullable ResourceKey<Level> key) {
        return PlanetAccents.of(key);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
