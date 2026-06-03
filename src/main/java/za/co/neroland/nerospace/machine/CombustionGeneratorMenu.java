package za.co.neroland.nerospace.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Menu for the Combustion Generator: a fuel slot + synced energy/burn data for the gauges. */
public class CombustionGeneratorMenu extends AbstractContainerMenu {

    private static final int FUEL_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public CombustionGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CombustionGeneratorBlockEntity.SIZE),
                new SimpleContainerData(4));
    }

    public CombustionGeneratorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.COMBUSTION_GENERATOR.get(), containerId);
        checkContainerSize(container, CombustionGeneratorBlockEntity.SIZE);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        this.addSlot(new FuelSlot(container, CombustionGeneratorBlockEntity.FUEL_SLOT, 80, 46));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
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
            if (index == FUEL_SLOT) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (OxygenGeneratorBlockEntity.fuelValue(raw) > 0) {
                if (!this.moveItemStackTo(raw, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public boolean isBurning() {
        return this.data.get(2) > 0;
    }

    public int getScaledBurn(int pixels) {
        int max = this.data.get(3);
        int cur = this.data.get(2);
        return (max != 0 && cur != 0) ? cur * pixels / max : 0;
    }

    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return OxygenGeneratorBlockEntity.fuelValue(stack) > 0;
        }
    }
}
