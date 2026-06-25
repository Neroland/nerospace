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

import za.co.neroland.nerospace.machine.HydrationModuleBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Menu for the Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1): one glacite input slot plus three
 * synced data values (link state, the linked Terraformer's hydration units, the buffer cap).
 */
public class HydrationModuleMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final @org.jspecify.annotations.NonNull Container container;
    private final @org.jspecify.annotations.NonNull ContainerData data;

    public HydrationModuleMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(HydrationModuleBlockEntity.SIZE),
                new SimpleContainerData(HydrationModuleBlockEntity.DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public HydrationModuleMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, @org.jspecify.annotations.NonNull Container container, @org.jspecify.annotations.NonNull ContainerData data) {
        super(ModMenuTypes.HYDRATION_MODULE.get(), containerId);
        checkContainerSize(container, HydrationModuleBlockEntity.SIZE);
        checkContainerDataCount(data, HydrationModuleBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new InputSlot(container, HydrationModuleBlockEntity.INPUT_SLOT, 80, 46));
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
            if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (raw.is(ModTags.Items.HYDRATION_INPUT)) {
                if (!this.moveItemStackTo(raw, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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

    public boolean isLinked() {
        return this.data.get(0) != 0;
    }

    public int getHydration() {
        return this.data.get(1);
    }

    public int getHydrationCap() {
        return this.data.get(2);
    }

    private static class InputSlot extends Slot {
        InputSlot(@org.jspecify.annotations.NonNull Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModTags.Items.HYDRATION_INPUT);
        }
    }
}
