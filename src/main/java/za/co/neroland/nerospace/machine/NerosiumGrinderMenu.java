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

/**
 * Menu for the Nerosium Grinder. Slot layout: input (0), output (1), then the standard 36 player
 * inventory slots. Progress and energy are synced via {@link ContainerData}.
 */
public class NerosiumGrinderMenu extends AbstractContainerMenu {

    private static final int DATA_SLOT_COUNT = NerosiumGrinderBlockEntity.SIZE; // 2
    private static final int PLAYER_INVENTORY_END = DATA_SLOT_COUNT + 36;

    private final Container container;
    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public NerosiumGrinderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NerosiumGrinderBlockEntity.SIZE), new SimpleContainerData(4));
    }

    /** Server constructor. */
    public NerosiumGrinderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.NEROSIUM_GRINDER.get(), containerId);
        checkContainerSize(container, NerosiumGrinderBlockEntity.SIZE);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, NerosiumGrinderBlockEntity.INPUT_SLOT, 56, 35));
        this.addSlot(new OutputSlot(container, NerosiumGrinderBlockEntity.OUTPUT_SLOT, 116, 35));

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
            if (index < DATA_SLOT_COUNT) {
                // Machine slot -> player inventory.
                if (!this.moveItemStackTo(raw, DATA_SLOT_COUNT, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> input slot only.
                if (!this.moveItemStackTo(raw, NerosiumGrinderBlockEntity.INPUT_SLOT, NerosiumGrinderBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
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

    /** @return grinding progress scaled to {@code pixels} (e.g. width of the progress arrow). */
    public int getScaledProgress(int pixels) {
        int prog = this.data.get(0);
        int max = this.data.get(1);
        return (max != 0 && prog != 0) ? prog * pixels / max : 0;
    }

    /** @return stored energy scaled to {@code pixels} (e.g. height of the energy bar). */
    public int getScaledEnergy(int pixels) {
        int stored = this.data.get(2);
        int capacity = this.data.get(3);
        return (capacity != 0) ? stored * pixels / capacity : 0;
    }

    public int getEnergy() {
        return this.data.get(2);
    }

    public int getMaxEnergy() {
        return this.data.get(3);
    }

    /** Output slot: items can be taken but never inserted by the player. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
