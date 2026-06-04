package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.machine.MachineItemHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Item Store: a 27-slot container that pipes (and hoppers) can fill and empty on every side.
 * Right-click opens a chest-style GUI.
 */
public class ItemStoreBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SIZE = 27;

    /**
     * The authoritative inventory ({@link MachineItemHandler}): the capability surface AND the
     * backing store of the Container/GUI — never a parallel copy (see that class's javadoc).
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler handler = new MachineItemHandler(SIZE, this::setChanged);

    public ItemStoreBlockEntity(BlockPos pos, BlockState state) {
        super(za.co.neroland.nerospace.registry.ModBlockEntities.ITEM_STORE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return this.handler;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Same NBT layout as before (ContainerHelper "Items" list) via a copy of the handler store.
        ContainerHelper.saveAllItems(output, this.handler.copyToList());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        NonNullList<ItemStack> loaded = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, loaded);
        for (int i = 0; i < SIZE; i++) {
            this.handler.setStack(i, loaded.get(i));
        }
    }

    /** Spill the inventory like a chest when the block goes away. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            net.minecraft.world.Containers.dropContents(this.level, pos, this);
            clearContent();
        }
    }

    // --- MenuProvider ---------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.item_store");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    // --- Container --------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.handler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.handler.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.handler.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.handler.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.handler.setStack(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.handler.clearStore();
    }
}
