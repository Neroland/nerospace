package za.co.neroland.nerospace.rocket;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the in-rocket UI. Carries the player inventory, a single fuel-intake slot (drop a fuel
 * bucket/canister to fuel the rocket), and five synced data values describing the rocket (fuel,
 * capacity, tier, launch-readiness, destination index).
 *
 * <p>Buttons route through {@link #clickMenuButton} so no custom packet is needed: Launch, the cycle
 * button, and direct destination selection ({@code SELECT_DEST_BASE + index}) are all handled
 * server-side, where the menu holds a reference to the {@link RocketEntity}.</p>
 *
 * <p>Cross-loader note: this is a plain (non-extended) menu. The client never needs the entity — it
 * displays from the synced {@link ContainerData} and routes buttons to the server menu — so the
 * server opens it with the vanilla {@code openMenu(MenuProvider)} and we avoid the loader-divergent
 * extended-menu API. The multi-station founding rows are deferred with that subsystem.</p>
 */
public class RocketMenu extends AbstractContainerMenu {

    public static final int BUTTON_LAUNCH = 0;
    public static final int BUTTON_CYCLE_DEST = 1;
    /** Cycles which founded station the Orbital Station destination docks at (origin → each station → origin). */
    public static final int BUTTON_CYCLE_STATION = 2;
    /** Select destination {@code n} via button id {@code SELECT_DEST_BASE + n}. */
    public static final int SELECT_DEST_BASE = 100;

    private static final int DATA_COUNT = 6;
    private static final int FUEL_SLOT_INDEX = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36; // exclusive

    private static final int FUEL_SLOT_X = 148;
    private static final int FUEL_SLOT_Y = 17;

    private final ContainerData data;
    private final Container fuelContainer;
    @Nullable
    private final RocketEntity rocket;

    /** Client constructor (referenced by the menu type); rocket state arrives via the synced data. */
    public RocketMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public RocketMenu(int containerId, Inventory playerInventory, @Nullable RocketEntity rocket, ContainerData data) {
        super(java.util.Objects.requireNonNull(ModMenuTypes.ROCKET.get()), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.rocket = rocket;
        this.data = data;
        this.fuelContainer = java.util.Objects.requireNonNull(
                rocket != null ? rocket.getFuelInput() : new SimpleContainer(1));

        this.addSlot(new FuelSlot(this.fuelContainer, 0, FUEL_SLOT_X, FUEL_SLOT_Y));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        RocketEntity current = this.rocket; // local copy so the null check holds for the analyzer
        if (current == null) {
            return false;
        }
        if (id == BUTTON_LAUNCH) {
            current.startLaunch();
            return true;
        }
        if (id == BUTTON_CYCLE_DEST) {
            current.cycleDestination();
            return true;
        }
        if (id == BUTTON_CYCLE_STATION) {
            current.cycleStation();
            return true;
        }
        if (id >= SELECT_DEST_BASE) {
            current.setDestinationIndex(id - SELECT_DEST_BASE);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        RocketEntity current = this.rocket;
        if (current == null) {
            return true;
        }
        return current.isAlive() && !current.isRemoved() && player.distanceTo(current) < 8.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack raw = slot.getItem();
            moved = raw.copy();
            if (index == FUEL_SLOT_INDEX) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (RocketEntity.isFuelContainer(raw)) {
                if (!this.moveItemStackTo(raw, FUEL_SLOT_INDEX, FUEL_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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

    // --- Screen helpers -----------------------------------------------------

    public int getFuel() {
        return this.data.get(0);
    }

    public int getCapacity() {
        return this.data.get(1);
    }

    public RocketTier getTier() {
        return RocketTier.byOrdinal(this.data.get(2));
    }

    public boolean isLaunchable() {
        return this.data.get(3) != 0;
    }

    public int getDestinationIndex() {
        return this.data.get(4);
    }

    /** Current fuel as a 0–100 percentage of the tier capacity. */
    public int getFuelPercent() {
        int capacity = getCapacity();
        return capacity == 0 ? 0 : Math.min(100, getFuel() * 100 / capacity);
    }

    /** Display name of the currently selected destination. */
    public String getDestinationName() {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key =
                getTier().destination(getDestinationIndex());
        return key == null ? "—" : Destinations.name(key);
    }

    public boolean hasMultipleDestinations() {
        return getTier().destinations().size() > 1;
    }

    // --- Orbital-station selection (shown only when the Orbital Station is the destination) ----------

    /** Selected founded-station slot, or {@code -1} for the shared origin platform. */
    public int getStationSlot() {
        return this.data.get(5);
    }

    /** Whether the currently selected destination is the Orbital Station dimension. */
    public boolean isStationDestination() {
        return ModDimensions.STATION_LEVEL.equals(
                java.util.Objects.requireNonNull(getTier().destination(getDestinationIndex())));
    }

    /**
     * Display label for the selected docking target. The custom charter name lives server-side (it can't
     * ride the int-only {@link ContainerData}), so the client shows the stable founding-order label.
     */
    public String getStationName() {
        int slot = getStationSlot();
        return slot < 0 ? "Origin Platform" : "Station " + (slot + 1);
    }

    /** Fuel-intake slot: only rocket fuel buckets/canisters may be placed. */
    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RocketEntity.isFuelContainer(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 16;
        }
    }
}
