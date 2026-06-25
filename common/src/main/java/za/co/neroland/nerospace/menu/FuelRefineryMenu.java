package za.co.neroland.nerospace.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Menu for the Fuel Refinery: a carbon slot + a catalyst slot, plus energy/fuel/progress data. */
public class FuelRefineryMenu extends AbstractContainerMenu {

    private static final int CARBON_SLOT = 0;
    private static final int CATALYST_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final @org.jspecify.annotations.NonNull Container container;
    private final @org.jspecify.annotations.NonNull ContainerData data;

    public FuelRefineryMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(FuelRefineryBlockEntity.SIZE),
                new SimpleContainerData(FuelRefineryBlockEntity.DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public FuelRefineryMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, @org.jspecify.annotations.NonNull Container container, @org.jspecify.annotations.NonNull ContainerData data) {
        super(ModMenuTypes.FUEL_REFINERY.get(), containerId);
        checkContainerSize(container, FuelRefineryBlockEntity.SIZE);
        checkContainerDataCount(data, FuelRefineryBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.addSlot(new FilterSlot(container, FuelRefineryBlockEntity.CARBON_SLOT, 56, 35));
        this.addSlot(new FilterSlot(container, FuelRefineryBlockEntity.CATALYST_SLOT, 104, 35));
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
        if (slot.hasItem()) {
            ItemStack raw = slot.getItem();
            moved = raw.copy();
            if (index == CARBON_SLOT || index == CATALYST_SLOT) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (raw.is(Items.BLAZE_POWDER)) {
                if (!this.moveItemStackTo(raw, CATALYST_SLOT, CATALYST_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (raw.is(Items.COAL) || raw.is(Items.CHARCOAL)) {
                if (!this.moveItemStackTo(raw, CARBON_SLOT, CARBON_SLOT + 1, false)) {
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

    public int getFuel() {
        return this.data.get(2);
    }

    public int getFuelCapacity() {
        return this.data.get(3);
    }

    public int getScaledProgress(int pixels) {
        int max = this.data.get(5);
        int cur = this.data.get(4);
        return (max != 0 && cur != 0) ? cur * pixels / max : 0;
    }

    private static class FilterSlot extends Slot {
        FilterSlot(@org.jspecify.annotations.NonNull Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
