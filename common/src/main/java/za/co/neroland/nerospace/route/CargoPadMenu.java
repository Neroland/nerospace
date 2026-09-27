package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.api.route.RouteQuote;
import za.co.neroland.nerospace.api.route.ScheduleMode;
import za.co.neroland.nerospace.network.CargoPadSyncPayload;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * The Cargo Pad menu: the 27-slot hold, the player inventory, a synced readout (fuel, rocket, quote,
 * schedule, verdicts) and the button ids the screen sends. Non-extended, like every Nerospace menu: the
 * client displays synced data and routes buttons back through {@link #clickMenuButton}, where the server
 * menu holds the block entity and the viewer.
 *
 * <p>The destination list is <em>per viewer</em> — the pads this player may route to — so it is computed
 * here on the server, refreshed every second, and its labels pushed with {@link CargoPadSyncPayload}.
 * Every control is owner-gated on the server; the screen only greys buttons out.</p>
 */
public class CargoPadMenu extends AbstractContainerMenu {

    public static final int BUTTON_LAUNCH = 0;
    public static final int BUTTON_PREV_DEST = 1;
    public static final int BUTTON_NEXT_DEST = 2;
    public static final int BUTTON_CLEAR_DEST = 3;
    public static final int BUTTON_CYCLE_SCHEDULE = 4;
    public static final int BUTTON_INTERVAL_DOWN = 5;
    public static final int BUTTON_INTERVAL_UP = 6;
    public static final int BUTTON_TOGGLE_RETURN = 7;
    public static final int BUTTON_TOGGLE_PUBLIC = 8;

    public static final int DATA_COUNT = 21;
    /** Hold grid origin (matches the screen). */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int INV_X = 8;
    public static final int INV_Y = 154;

    private static final int HOLD_START = 0;
    private static final int HOLD_END = CargoPadBlockEntity.SLOTS;
    private static final int PLAYER_INV_START = HOLD_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;
    private static final int REFRESH_INTERVAL = 20;

    private final Container container;
    private final ContainerData data;
    @Nullable
    private final CargoPadBlockEntity pad;
    @Nullable
    private final UUID viewer;
    /** Server-side: the viewer's destination list, refreshed every second. */
    private List<PadHandle> visible = List.of();
    private int[] lastSentIds = new int[0];
    private int refreshTick;
    @Nullable
    private RouteQuote quoteCache;
    private int quoteFor = Integer.MIN_VALUE;

    /** Client constructor. */
    public CargoPadMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CargoPadBlockEntity.SLOTS),
                new SimpleContainerData(DATA_COUNT), null, null);
    }

    /** Server constructor. */
    public CargoPadMenu(int containerId, Inventory playerInventory, CargoPadBlockEntity pad, Player viewer) {
        this(containerId, playerInventory, pad, null, pad, viewer.getUUID());
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    private CargoPadMenu(int containerId, Inventory playerInventory, Container container, @Nullable ContainerData data,
            @Nullable CargoPadBlockEntity pad, @Nullable UUID viewer) {
        super(ModMenuTypes.CARGO_PAD.get(), containerId);
        checkContainerSize(container, CargoPadBlockEntity.SLOTS);
        this.container = container;
        this.pad = pad;
        this.viewer = viewer;
        this.data = data != null ? data : serverData();
        checkContainerDataCount(this.data, DATA_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new HoldSlot(container, row * 9 + col, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        this.addStandardInventorySlots(playerInventory, INV_X, INV_Y);
        this.addDataSlots(this.data);

        if (pad != null && playerInventory.player instanceof ServerPlayer serverPlayer) {
            refreshVisible(serverPlayer.level().getServer(), true, serverPlayer);
        }
    }

    // --- Server side --------------------------------------------------------------------------

    private ContainerData serverData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                CargoPadBlockEntity be = pad;
                if (be == null) {
                    return 0;
                }
                MinecraftServer server = be.getLevel() == null ? null : be.getLevel().getServer();
                RouteRegistry registry = server == null ? null : RouteRegistry.get(server);
                RouteRecord route = registry == null || be.padId() < 0 ? null : registry.routeForOrigin(be.padId());
                CargoRocketEntity rocket = index >= 2 && index <= 4 ? be.dockedRocket() : null;
                return switch (index) {
                    case 0 -> be.fuelAmount() / 10;
                    case 1 -> be.fuelCapacity() / 10;
                    case 2 -> rocket != null ? 1 : 0;
                    case 3 -> rocket == null ? 0 : Math.min(32_000, rocket.getFuel());
                    case 4 -> rocket == null ? 0 : Math.min(32_000, rocket.fuelCapacity());
                    case 5 -> quote(server, registry, be, route) == null ? 0 : Math.min(32_000, quoteCache.fuelMb());
                    case 6 -> quote(server, registry, be, route) == null ? 0 : Math.min(32_000, quoteCache.travelSeconds());
                    case 7 -> route == null ? -1 : indexOf(route.destinationPadId());
                    case 8 -> visible.size();
                    case 9 -> route == null ? 0 : route.mode().ordinal();
                    case 10 -> route == null ? RouteRecord.DEFAULT_INTERVAL_TICKS / 1_200 : route.intervalTicks() / 1_200;
                    case 11 -> route != null && route.returnEmpty() ? 1 : 0;
                    case 12 -> be.verdict();
                    case 13 -> be.activeSlots();
                    case 14 -> be.managedBy(viewer) ? 1 : 0;
                    case 15 -> registry == null || be.padId() < 0 ? 0 : Math.min(999, registry.inboundCount(be.padId()));
                    case 16 -> be.padTier();
                    case 17 -> {
                        PadRecord record = be.record();
                        yield record != null && record.isPublic() ? 1 : 0;
                    }
                    case 18 -> be.usedSlots();
                    case 19 -> {
                        FlightRecord last = lastFlight(registry, be);
                        yield last == null ? 0 : 1 + last.state().ordinal();
                    }
                    case 20 -> {
                        FlightRecord last = lastFlight(registry, be);
                        if (last == null || server == null || last.state().isTerminal()) {
                            yield 0;
                        }
                        long left = last.arrivesAt() - server.overworld().getGameTime();
                        yield (int) Math.max(0, Math.min(32_000, (left + 19) / 20));
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Read-only from the client.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Nullable
    private RouteQuote quote(@Nullable MinecraftServer server, @Nullable RouteRegistry registry, CargoPadBlockEntity be,
            @Nullable RouteRecord route) {
        if (server == null || registry == null || route == null) {
            this.quoteCache = null;
            return null;
        }
        int key = route.destinationPadId() * 31 + be.usedSlots() * 7 + (route.returnEmpty() ? 1 : 0);
        if (this.quoteCache == null || key != this.quoteFor || this.refreshTick % REFRESH_INTERVAL == 0) {
            this.quoteCache = CargoLaunch.quote(server, registry, be.padId(), route.destinationPadId(), be.manifestCopy(),
                    route.returnEmpty());
            this.quoteFor = key;
        }
        return this.quoteCache;
    }

    /** The most recent flight that left this pad (live or retained), or {@code null}. */
    @Nullable
    private static FlightRecord lastFlight(@Nullable RouteRegistry registry, CargoPadBlockEntity be) {
        if (registry == null || be.padId() < 0) {
            return null;
        }
        FlightRecord last = null;
        for (FlightRecord flight : registry.allFlights()) {
            if (flight.originPadId() == be.padId() && !flight.returnLeg()) {
                last = flight;
            }
        }
        return last;
    }

    private int indexOf(int padId) {
        for (int i = 0; i < this.visible.size(); i++) {
            if (this.visible.get(i).id() == padId) {
                return i;
            }
        }
        return -1;
    }

    private void refreshVisible(MinecraftServer server, boolean force, ServerPlayer player) {
        CargoPadBlockEntity be = this.pad;
        if (be == null || this.viewer == null) {
            return;
        }
        List<PadHandle> all = RouteRegistry.get(server).padsVisibleTo(server, this.viewer);
        List<PadHandle> out = new ArrayList<>(all.size());
        for (PadHandle handle : all) {
            if (handle.id() != be.padId()) {
                out.add(handle);
            }
        }
        this.visible = List.copyOf(out);
        int[] ids = new int[this.visible.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = this.visible.get(i).id();
        }
        if (force || !java.util.Arrays.equals(ids, this.lastSentIds)) {
            this.lastSentIds = ids;
            ModNetwork.sendToPlayer(player, CargoPadSyncPayload.of(this.containerId, this.visible));
        }
    }

    @Override
    public void broadcastChanges() {
        CargoPadBlockEntity be = this.pad;
        if (be != null && be.getLevel() != null && ++this.refreshTick % REFRESH_INTERVAL == 0) {
            MinecraftServer server = be.getLevel().getServer();
            if (server != null && this.viewer != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(this.viewer);
                if (player != null) {
                    refreshVisible(server, false, player);
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        CargoPadBlockEntity be = this.pad;
        if (be == null || be.getLevel() == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        MinecraftServer server = be.getLevel().getServer();
        if (server == null) {
            return false;
        }
        if (id == BUTTON_LAUNCH) {
            be.launchNow(serverPlayer);
            return true;
        }
        // Everything below reconfigures the pad: owner (or anyone, while unowned) only.
        if (!be.managedBy(serverPlayer.getUUID()) || be.padId() < 0) {
            return true;
        }
        RouteRegistry registry = RouteRegistry.get(server);
        RouteRecord route = registry.routeForOrigin(be.padId());
        switch (id) {
            case BUTTON_PREV_DEST, BUTTON_NEXT_DEST -> {
                refreshVisible(server, false, serverPlayer);
                if (this.visible.isEmpty()) {
                    registry.clearRoute(be.padId());
                    return true;
                }
                int current = route == null ? -1 : indexOf(route.destinationPadId());
                int step = id == BUTTON_NEXT_DEST ? 1 : -1;
                int next = Math.floorMod(current + step, this.visible.size());
                registry.setRoute(be.padId(), this.visible.get(next).id());
            }
            case BUTTON_CLEAR_DEST -> registry.clearRoute(be.padId());
            case BUTTON_CYCLE_SCHEDULE -> {
                if (route != null) {
                    registry.updateSchedule(route.id(), route.mode().next(), route.intervalTicks());
                }
            }
            case BUTTON_INTERVAL_DOWN, BUTTON_INTERVAL_UP -> {
                if (route != null) {
                    int delta = id == BUTTON_INTERVAL_UP ? RouteRecord.INTERVAL_STEP_TICKS : -RouteRecord.INTERVAL_STEP_TICKS;
                    registry.updateSchedule(route.id(), route.mode(), RouteRecord.clampInterval(route.intervalTicks() + delta));
                }
            }
            case BUTTON_TOGGLE_RETURN -> {
                if (route != null) {
                    registry.setReturnEmpty(route.id(), !route.returnEmpty());
                }
            }
            case BUTTON_TOGGLE_PUBLIC -> {
                PadRecord record = be.record();
                if (record != null) {
                    registry.setPadPublic(record.id(), !record.isPublic());
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            moved = raw.copy();
            if (index < HOLD_END) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(raw, HOLD_START, activeSlots(), false)) {
                return ItemStack.EMPTY;
            }
            if (raw.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (raw.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, raw);
        }
        return moved;
    }

    // --- Screen helpers (synced) -------------------------------------------------------------

    public int fuel() {
        return this.data.get(0) * 10;
    }

    public int fuelCapacity() {
        return this.data.get(1) * 10;
    }

    public float fuelFrac() {
        int cap = this.data.get(1);
        return cap == 0 ? 0F : Math.min(1F, this.data.get(0) / (float) cap);
    }

    public boolean rocketPresent() {
        return this.data.get(2) == 1;
    }

    public int rocketFuel() {
        return this.data.get(3);
    }

    public int rocketFuelCapacity() {
        return this.data.get(4);
    }

    public float rocketFuelFrac() {
        int cap = this.data.get(4);
        return cap == 0 ? 0F : Math.min(1F, this.data.get(3) / (float) cap);
    }

    public int quoteFuel() {
        return this.data.get(5);
    }

    public int quoteSeconds() {
        return this.data.get(6);
    }

    public int destinationIndex() {
        return this.data.get(7);
    }

    public int destinationCount() {
        return this.data.get(8);
    }

    public ScheduleMode scheduleMode() {
        return ScheduleMode.byOrdinal(this.data.get(9));
    }

    public int intervalMinutes() {
        return this.data.get(10);
    }

    public boolean returnEmpty() {
        return this.data.get(11) == 1;
    }

    /** 0 none, 1 accepted, 2 + denial ordinal. */
    public int verdict() {
        return this.data.get(12);
    }

    public int activeSlots() {
        int n = this.data.get(13);
        return n <= 0 ? CargoPadBlockEntity.SLOTS : n;
    }

    public boolean canManage() {
        return this.data.get(14) == 1;
    }

    public int inbound() {
        return this.data.get(15);
    }

    public int padTier() {
        return this.data.get(16);
    }

    public boolean isPublic() {
        return this.data.get(17) == 1;
    }

    public int usedSlots() {
        return this.data.get(18);
    }

    /** -1 when this pad has never launched, else the {@code FlightState} ordinal of its latest flight. */
    public int lastFlightState() {
        return this.data.get(19) - 1;
    }

    public int lastFlightSecondsLeft() {
        return this.data.get(20);
    }

    /** Whether the launch button should light: rocket docked, destination set, cargo aboard, fuel ≥ quote. */
    public boolean launchReady() {
        return rocketPresent() && destinationIndex() >= 0 && usedSlots() > 0 && quoteFuel() > 0
                && rocketFuel() >= quoteFuel() && padTier() >= CargoLaunch.REQUIRED_PAD_TIER;
    }

    /** Hold slots beyond the server cap are visible but locked. */
    private final class HoldSlot extends Slot {
        HoldSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() < activeSlots();
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < activeSlots();
        }
    }
}
