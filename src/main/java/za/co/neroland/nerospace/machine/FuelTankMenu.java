package za.co.neroland.nerospace.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the Fuel Tank: no machine slots (it holds a fluid, not items), just the player inventory
 * and two synced data values (fuel, capacity) for the screen's readout.
 */
public class FuelTankMenu extends AbstractContainerMenu {

    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public FuelTankMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(2));
    }

    /** Server constructor. */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public FuelTankMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.FUEL_TANK.get(), containerId);
        checkContainerDataCount(data, 2);
        this.data = data;
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine slots to shuttle to/from
    }

    // --- Screen helpers -----------------------------------------------------

    public int getFuel() {
        return this.data.get(0);
    }

    public int getCapacity() {
        return this.data.get(1);
    }

    public int getFuelPercent() {
        int cap = getCapacity();
        return cap == 0 ? 0 : Math.min(100, getFuel() * 100 / cap);
    }
}
