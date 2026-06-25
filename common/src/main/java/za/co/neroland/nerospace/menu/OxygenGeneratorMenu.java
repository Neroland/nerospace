package za.co.neroland.nerospace.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Oxygen Generator menu — no machine slots (power comes in and oxygen goes out over the Universal Pipe
 * network); just four synced values (energy, energy capacity, oxygen, oxygen capacity) for the screen's
 * gauges, plus the player inventory.
 */
public class OxygenGeneratorMenu extends AbstractContainerMenu {

    private final @org.jspecify.annotations.NonNull ContainerData data;
    private final ContainerLevelAccess access;

    /** Client constructor (referenced by the {@code MenuType}); dummy data syncs from the server. */
    public OxygenGeneratorMenu(int id, @org.jspecify.annotations.NonNull Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainerData(4), ContainerLevelAccess.NULL);
    }

    public OxygenGeneratorMenu(int id, @org.jspecify.annotations.NonNull Inventory playerInventory, @org.jspecify.annotations.NonNull ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.OXYGEN_GENERATOR.get(), id);
        this.data = data;
        this.access = access;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.OXYGEN_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine slots
    }
}
