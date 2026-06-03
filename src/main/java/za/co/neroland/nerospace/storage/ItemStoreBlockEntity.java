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
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Item Store: a 27-slot container that pipes (and hoppers) can fill and empty on every side.
 * Right-click opens a chest-style GUI.
 */
public class ItemStoreBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SIZE = 27;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final ItemStacksResourceHandler handler = new ItemStacksResourceHandler(this.items) {
        @Override
        protected void onContentsChanged(int index, ItemStack oldStack) {
            ItemStoreBlockEntity.this.setChanged();
        }
    };

    public ItemStoreBlockEntity(BlockPos pos, BlockState state) {
        super(za.co.neroland.nerospace.registry.ModBlockEntities.ITEM_STORE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return this.handler;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
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
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.items.set(slot, stack);
        setChanged();
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
        this.items.clear();
        setChanged();
    }
}
