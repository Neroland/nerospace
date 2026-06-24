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

import za.co.neroland.nerospace.module.UpgradeModuleItem;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the quarry controller. Combined inventory: slot 0 = frame casing, 1..M = module cards, then
 * the output buffer; followed by the player inventory. Status is synced through {@link ContainerData}.
 */
public class QuarryMenu extends AbstractContainerMenu {

    private static final int TIER1_MODULE_SLOTS = 1;

    private final Container container;
    private final ContainerData data;
    private final int machineSlots;
    private final int moduleSlots;

    /** Client constructor (Tier-1 layout). */
    public QuarryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleContainer(1 + TIER1_MODULE_SLOTS + QuarryControllerBlockEntity.OUTPUT_SLOTS),
                new SimpleContainerData(QuarryControllerBlockEntity.DATA_COUNT),
                TIER1_MODULE_SLOTS);
    }

    /** Server constructor. */
    @SuppressWarnings("this-escape")
    public QuarryMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, int moduleSlots) {
        super(ModMenuTypes.QUARRY_CONTROLLER.get(), containerId);
        this.container = container;
        this.data = data;
        this.moduleSlots = moduleSlots;
        this.machineSlots = container.getContainerSize();

        this.addSlot(new FrameSlot(container, QuarryControllerBlockEntity.FRAME_SLOT, 8, 20));
        for (int i = 0; i < moduleSlots; i++) {
            this.addSlot(new ModuleSlot(container, 1 + i, 26 + i * 18, 20));
        }

        int outStart = 1 + moduleSlots;
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
        int playerStart = this.machineSlots;
        int playerEnd = this.machineSlots + 36;

        if (index < this.machineSlots) {
            if (!this.moveItemStackTo(raw, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (raw.is(ModItems.FRAME_CASING.get())) {
                if (!this.moveItemStackTo(raw, QuarryControllerBlockEntity.FRAME_SLOT,
                        QuarryControllerBlockEntity.FRAME_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (UpgradeModuleItem.isModule(raw)) {
                if (!this.moveItemStackTo(raw, 1, 1 + this.moduleSlots, false)) {
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

    private static final class ModuleSlot extends Slot {
        ModuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return UpgradeModuleItem.isModule(stack);
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
