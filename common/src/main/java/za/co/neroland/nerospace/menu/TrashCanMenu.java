package za.co.neroland.nerospace.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Trash Can menu — a single drop slot. Items placed in it are NOT trashed immediately: the trash can
 * holds the last-inserted stack and only voids it when the NEXT stack is inserted, so closing the screen
 * never trashes the held stack. The backing container (the block entity) always reports the slot empty,
 * so every insert is accepted and routed through the hold-then-void buffer.
 */
public class TrashCanMenu extends AbstractContainerMenu {

    private static final int TRASH_SLOT = 0;
    private final Container trash;

    /** Client constructor (dummy buffer; the server slot drives the real trashing). */
    public TrashCanMenu(int id, @org.jspecify.annotations.NonNull Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(1));
    }

    public TrashCanMenu(int id, @org.jspecify.annotations.NonNull Inventory playerInventory, Container trash) {
        super(ModMenuTypes.TRASH_CAN.get(), id);
        checkContainerSize(trash, 1);
        this.trash = trash;

        this.addSlot(new Slot(trash, 0, 80, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == TRASH_SLOT) {
            return ItemStack.EMPTY; // nothing to pull back out of the trash
        }
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            // Shift-click trashes the whole stack (held until the next insert, like a manual drop).
            this.trash.setItem(0, slot.getItem().copy());
            slot.set(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.trash.stillValid(player);
    }
}
