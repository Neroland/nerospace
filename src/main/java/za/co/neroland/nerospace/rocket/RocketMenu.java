package za.co.neroland.nerospace.rocket;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the in-rocket UI (Phase 8b). Carries the player inventory, a single fuel-intake slot
 * (drop a fuel bucket/canister to fuel the rocket — the door to hopper automation), and five synced
 * data values describing the rocket (fuel, capacity, tier, launch-readiness, destination index).
 *
 * <p>Buttons route through {@link #clickMenuButton} so no custom packet is needed: Launch, the legacy
 * cycle button, and direct destination selection ({@code SELECT_DEST_BASE + index}) are all handled
 * server-side, where the menu holds a reference to the {@link RocketEntity}.</p>
 */
public class RocketMenu extends AbstractContainerMenu {

    public static final int BUTTON_LAUNCH = 0;
    public static final int BUTTON_CYCLE_DEST = 1;
    /** Select destination {@code n} via button id {@code SELECT_DEST_BASE + n}. */
    public static final int SELECT_DEST_BASE = 100;

    private static final int DATA_COUNT = 5;
    private static final int FUEL_SLOT_INDEX = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36; // exclusive

    private static final int FUEL_SLOT_X = 148;
    private static final int FUEL_SLOT_Y = 52;

    private final ContainerData data;
    private final Container fuelContainer;
    @Nullable
    private final RocketEntity rocket;

    /** Client constructor (referenced by the menu type); resolves the rocket from its synced id. */
    public RocketMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolveRocket(playerInventory, buffer.readVarInt()),
                new SimpleContainerData(DATA_COUNT));
    }

    /** Server constructor (and client, via the resolved rocket). */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public RocketMenu(int containerId, Inventory playerInventory, @Nullable RocketEntity rocket, ContainerData data) {
        super(ModMenuTypes.ROCKET.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.rocket = rocket;
        this.data = data;
        this.fuelContainer = rocket != null ? rocket.getFuelInput() : new SimpleContainer(1);

        this.addSlot(new FuelSlot(this.fuelContainer, 0, FUEL_SLOT_X, FUEL_SLOT_Y));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Nullable
    private static RocketEntity resolveRocket(Inventory inventory, int entityId) {
        return inventory.player.level().getEntity(entityId) instanceof RocketEntity r ? r : null;
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
        return current.isAlive() && !current.isRemoved()
                && player.distanceTo(current) < 8.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            moved = raw.copy();
            if (index == FUEL_SLOT_INDEX) {
                // Fuel slot -> player inventory.
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (RocketEntity.isFuelContainer(raw)) {
                // Player inventory -> fuel slot.
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
        // Prefer the resolved entity: its synced data is already correct when the screen inits,
        // while menu data slots are zero until the first broadcast — building the destination
        // row from slot data made every rocket look Tier 1 for the opening frames.
        RocketEntity current = this.rocket;
        return current != null ? current.getTier() : RocketTier.byOrdinal(this.data.get(2));
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

    /** @return fuel scaled to {@code pixels}. */
    public int getScaledFuel(int pixels) {
        int capacity = getCapacity();
        return capacity != 0 ? getFuel() * pixels / capacity : 0;
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
