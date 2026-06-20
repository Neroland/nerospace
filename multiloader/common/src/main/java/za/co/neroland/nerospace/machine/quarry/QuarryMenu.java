package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the quarry controller. Layout: slot 0 = frame casing, then the output buffer; followed by
 * the player inventory. Status (energy, state, fluid, depth) is synced through {@link ContainerData}.
 *
 * <p>Cross-loader port note: upgrade modules are deferred, so there are no module slots here.</p>
 */
public class QuarryMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 1 + QuarryControllerBlockEntity.OUTPUT_SLOTS;

    private final Container container;
    private final ContainerData data;

    /** Client constructor. */
    public QuarryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(QuarryControllerBlockEntity.DATA_COUNT));
    }

    /** Server constructor. */
    @SuppressWarnings("this-escape")
    public QuarryMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.QUARRY_CONTROLLER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        this.container = container;
        this.data = data;

        this.addSlot(new FrameSlot(container, QuarryControllerBlockEntity.FRAME_SLOT, 8, 20));

        int outStart = 1;
        for (int i = 0; i < QuarryControllerBlockEntity.OUTPUT_SLOTS; i++) {
            int row = i / 6;
            int col = i % 6;
            this.addSlot(new OutputSlot(container, outStart + i, 8 + col * 18, 42 + row * 18));
        }

        this.addStandardInventorySlots(playerInventory, 8, 126);
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
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack raw = slot.getItem();
        moved = raw.copy();
        int playerStart = MACHINE_SLOTS;
        int playerEnd = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(raw, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (raw.is(ModItems.FRAME_CASING.get())) {
                if (!this.moveItemStackTo(raw, QuarryControllerBlockEntity.FRAME_SLOT,
                        QuarryControllerBlockEntity.FRAME_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
        return moved;
    }

    // --- Screen helpers ---------------------------------------------------------

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public QuarryControllerBlockEntity.State getState() {
        return QuarryControllerBlockEntity.State.values()[
                Math.floorMod(this.data.get(2), QuarryControllerBlockEntity.State.values().length)];
    }

    public int getFluid() {
        return this.data.get(3);
    }

    public int getMaxFluid() {
        return this.data.get(4);
    }

    public int getCurrentY() {
        return this.data.get(5);
    }

    public int getRefY() {
        return this.data.get(6);
    }

    // --- Slot kinds -------------------------------------------------------------

    private static final class FrameSlot extends Slot {
        FrameSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.FRAME_CASING.get());
        }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
