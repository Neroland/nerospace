package za.co.neroland.nerospace.rocket;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the in-rocket UI (Phase 4). Carries only the player inventory plus four synced data
 * values describing the rocket (fuel, capacity, tier, launch-readiness). The Launch button is wired
 * through {@link #clickMenuButton} so no custom packet is needed: the click is handled server-side,
 * where the menu holds a reference to the {@link RocketEntity}.
 */
public class RocketMenu extends AbstractContainerMenu {

    public static final int BUTTON_LAUNCH = 0;
    public static final int BUTTON_CYCLE_DEST = 1;
    private static final int DATA_COUNT = 5;

    private final ContainerData data;
    @Nullable
    private final RocketEntity rocket;

    /** Client constructor (referenced by the menu type); resolves the rocket from its synced id. */
    public RocketMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolveRocket(playerInventory, buffer.readVarInt()),
                new SimpleContainerData(DATA_COUNT));
    }

    /** Server constructor. */
    public RocketMenu(int containerId, Inventory playerInventory, @Nullable RocketEntity rocket, ContainerData data) {
        super(ModMenuTypes.ROCKET.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.rocket = rocket;
        this.data = data;

        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Nullable
    private static RocketEntity resolveRocket(Inventory inventory, int entityId) {
        return inventory.player.level().getEntity(entityId) instanceof RocketEntity r ? r : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.rocket == null) {
            return false;
        }
        if (id == BUTTON_LAUNCH) {
            this.rocket.startLaunch();
            return true;
        }
        if (id == BUTTON_CYCLE_DEST) {
            this.rocket.cycleDestination();
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
        // No machine slots to shuttle to/from; nothing to quick-move.
        return ItemStack.EMPTY;
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

    /** Display name of the currently selected destination, for the cycle button label. */
    public String getDestinationName() {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key =
                getTier().destination(getDestinationIndex());
        return key == null ? "—" : Destinations.name(key);
    }

    public boolean hasMultipleDestinations() {
        return getTier().destinations().size() > 1;
    }

    /** @return fuel scaled to {@code pixels} (e.g. height of the fuel gauge). */
    public int getScaledFuel(int pixels) {
        int capacity = getCapacity();
        return capacity != 0 ? getFuel() * pixels / capacity : 0;
    }
}
