package za.co.neroland.nerospace.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the Oxygen Generator (gas-layer rework): no machine slots — the machine runs on piped
 * power and pipes its oxygen out — just four synced values (energy, energy cap, oxygen, oxygen cap)
 * for the screen's gauges, plus the player inventory.
 */
public class OxygenGeneratorMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client constructor (referenced by the {@code MenuType}). */
    public OxygenGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(4), ContainerLevelAccess.NULL);
    }

    /** Server constructor. */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public OxygenGeneratorMenu(int containerId, Inventory playerInventory, ContainerData data,
            ContainerLevelAccess access) {
        super(ModMenuTypes.OXYGEN_GENERATOR.get(), containerId);
        checkContainerDataCount(data, 4);
        this.data = data;
        this.access = access;

        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.OXYGEN_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine slots
    }

    // --- Screen helpers -----------------------------------------------------

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getOxygen() {
        return this.data.get(2);
    }

    public int getMaxOxygen() {
        return this.data.get(3);
    }

    public boolean isProducing() {
        return getEnergy() >= za.co.neroland.nerospace.Tuning.oxygenGeneratorFePerMb()
                && getOxygen() < getMaxOxygen();
    }
}
