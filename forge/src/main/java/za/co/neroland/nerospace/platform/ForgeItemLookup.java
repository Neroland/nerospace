package za.co.neroland.nerospace.platform;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.NerospaceItemStore;

/**
 * Forge query of the platform-standard {@code ITEM_HANDLER} capability — the cross-mod half of the item
 * seam. Nerospace's own inventories are vanilla {@link net.minecraft.world.Container}s and are resolved
 * before this lookup is ever consulted, so everything reached here belongs to a foreign mod that speaks
 * only {@link IItemHandler}.
 */
public final class ForgeItemLookup implements ItemLookup {

    @Nullable
    @Override
    public NerospaceItemStore find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        IItemHandler handler =
                be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side)
                        .orElse(null);
        return handler == null ? null : new HandlerStore(handler);
    }

    /**
     * {@link NerospaceItemStore} over a Forge {@link IItemHandler}. The handler is already side-bound (it
     * was resolved for one face), so no face filtering happens here — the handler's own
     * {@code insertItem}/{@code extractItem} enforce whatever the owning mod allows.
     */
    private record HandlerStore(IItemHandler handler) implements NerospaceItemStore {

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack remainder = stack;
            // insertItem never mutates its argument; it returns what did not fit, so threading the
            // remainder from slot to slot spreads one stack across as many slots as will take it.
            for (int slot = 0; slot < this.handler.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = this.handler.insertItem(slot, remainder, simulate);
            }
            return remainder;
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
            if (maxCount <= 0) {
                return ItemStack.EMPTY;
            }
            for (int slot = 0; slot < this.handler.getSlots(); slot++) {
                ItemStack inSlot = this.handler.getStackInSlot(slot);
                if (inSlot.isEmpty() || !filter.test(inSlot)) {
                    continue;
                }
                // A matching slot can still refuse extraction (output-only locks, cooldowns); keep
                // scanning rather than reporting the whole inventory as empty.
                ItemStack taken = this.handler.extractItem(slot, maxCount, simulate);
                if (!taken.isEmpty()) {
                    return taken;
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
