package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Creative Item Store: an endless source of one configured item, exposed as a single-slot
 * {@link Container} (so pipes/hoppers pull from it through the mod's item capability). Right-click with
 * an item to choose it; sneak-right-click to clear. Extraction never depletes the source; insertion is
 * rejected.
 *
 * <p>Cross-loader port note: the root used the NeoForge-transfer {@code InfiniteResourceHandler}; the
 * multiloader exposes the same endless source through a vanilla Container view.</p>
 */
public class CreativeItemStoreBlockEntity extends BlockEntity implements Container {

    private @org.jspecify.annotations.NonNull ItemStack source = ItemStack.EMPTY;

    public CreativeItemStoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_ITEM_STORE.get(), pos, state);
    }

    public @org.jspecify.annotations.NonNull ItemStack source() {
        return this.source;
    }

    public void setSource(@org.jspecify.annotations.NonNull ItemStack stack) {
        this.source = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Source", NerospaceCommon.ITEM_STACK_CODEC, NerospaceCommon.requireNonNull(this.source));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.source = NerospaceCommon.orElse(input.read("Source", NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY);
    }

    // --- Container: a single endless-source slot --------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.source.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.source.isEmpty() ? ItemStack.EMPTY : this.source.copyWithCount(this.source.getMaxStackSize());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (this.source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return this.source.copyWithCount(Math.min(amount, this.source.getMaxStackSize())); // endless — never depletes
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Source is configured by right-click, not by insertion; ignore.
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false; // a source, not a sink
    }

    @Override
    public boolean stillValid(Player player) {
        var currentLevel = this.level;
        return currentLevel != null && currentLevel.getBlockEntity(this.worldPosition) == this;
    }

    @Override
    public void clearContent() {
        this.source = ItemStack.EMPTY;
    }
}
