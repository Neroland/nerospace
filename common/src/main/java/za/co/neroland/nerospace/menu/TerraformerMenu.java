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

import za.co.neroland.nerospace.machine.TerraformerBlockEntity;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the Terraformer (grid-only): a tier-upgrade slot plus the synced stage/energy data. Power
 * arrives exclusively through the Universal Pipe network. Non-extended (entity ref stays server-side;
 * the client reads the synced {@link ContainerData}).
 */
public class TerraformerMenu extends AbstractContainerMenu {

    private static final int UPGRADE_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public TerraformerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TerraformerBlockEntity.SIZE),
                new SimpleContainerData(TerraformerBlockEntity.DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public TerraformerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(java.util.Objects.requireNonNull(ModMenuTypes.TERRAFORMER.get()), containerId);
        checkContainerSize(container, TerraformerBlockEntity.SIZE);
        checkContainerDataCount(data, TerraformerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new UpgradeSlot(container, TerraformerBlockEntity.UPGRADE_SLOT, 80, 46));
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
            if (index == UPGRADE_SLOT) {
                if (!this.moveItemStackTo(raw, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (raw.is(java.util.Objects.requireNonNull(ModItems.NEROSTEEL_INGOT.get())) || raw.is(java.util.Objects.requireNonNull(ModItems.CINDRITE.get()))) {
                if (!this.moveItemStackTo(raw, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) {
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

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getTier() {
        return this.data.get(2);
    }

    public int getRadius() {
        return this.data.get(3);
    }

    public int getHydration() {
        return this.data.get(4);
    }

    public int getHydrationCap() {
        return this.data.get(5);
    }

    public int getHydrationRadius() {
        return this.data.get(6);
    }

    public int getLifeRadius() {
        return this.data.get(7);
    }

    public boolean isHydrationStalled() {
        return this.data.get(8) != 0;
    }

    public boolean isActive() {
        return getEnergy() > 0;
    }

    private static class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(java.util.Objects.requireNonNull(ModItems.NEROSTEEL_INGOT.get())) || stack.is(java.util.Objects.requireNonNull(ModItems.CINDRITE.get()));
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
