package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.InfiniteResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Creative Item Store: an endless source of one configured item (and a void for inserts). Right-click
 * with an item to choose it; sneak-right-click to clear.
 */
public class CreativeItemStoreBlockEntity extends BlockEntity {

    private ItemResource source = ItemResource.EMPTY;
    /** Endless source of {@link #source}; empty handler until an item is configured. */
    private ResourceHandler<ItemResource> handler = EmptyResourceHandler.instance();

    public CreativeItemStoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_ITEM_STORE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return this.handler;
    }

    public ItemResource source() {
        return this.source;
    }

    public void setSource(ItemResource source) {
        this.source = source;
        // InfiniteResourceHandler rejects an empty resource — fall back to the empty handler.
        this.handler = source.isEmpty() ? EmptyResourceHandler.instance() : new InfiniteResourceHandler<>(source);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.source.isEmpty()) {
            output.store("Source", ItemResource.CODEC, this.source);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        setSource(input.read("Source", ItemResource.CODEC).orElse(ItemResource.EMPTY));
    }
}
