package za.co.neroland.nerospace.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Passive Generator menu: 1 core slot + player inventory + synced energy/core data. */
public class PassiveGeneratorMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = PassiveGeneratorBlockEntity.SIZE;
    private final Container container;
    private final ContainerData data;

    public PassiveGeneratorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(4));
    }

    public PassiveGeneratorMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(java.util.Objects.requireNonNull(ModMenuTypes.PASSIVE_GENERATOR.get()), id);
        checkContainerSize(container, MACHINE_SLOTS);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, PassiveGeneratorBlockEntity.CORE_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PassiveGeneratorBlockEntity.isCore(stack);
            }
        });
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

    public int energy() {
        return this.data.get(0);
    }

    public int capacity() {
        return this.data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = invStart + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, invStart, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
