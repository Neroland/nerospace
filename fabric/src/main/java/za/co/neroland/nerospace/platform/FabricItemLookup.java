package za.co.neroland.nerospace.platform;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.NerospaceItemStore;

/**
 * Fabric implementation of the cross-mod item seam: resolves {@code ItemStorage.SIDED} — the Transfer
 * API lookup every Fabric mod's inventory is published on — and adapts it to {@link NerospaceItemStore}.
 *
 * <p>This is what lets a Universal Pipe reach a machine that never implements vanilla
 * {@link Container}: before this, such a neighbour was simply invisible to the network.</p>
 */
public final class FabricItemLookup implements ItemLookup {

    @Nullable
    @Override
    public NerospaceItemStore find(Level level, BlockPos pos, @Nullable Direction side) {
        // The Transfer API ships a fallback that wraps any Container block entity in a ContainerStorage,
        // so an unfiltered find() would answer for vanilla chests too. ItemLookup's contract says the
        // vanilla-container path stays authoritative (it honours WorldlyContainer face rules directly and
        // needs no transaction), so bow out here and let the caller take that branch.
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container) {
            return null;
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new TransferApiItemStore(storage);
    }

    /**
     * Opens a transaction, nesting when the caller already has one open.
     *
     * <p>The Transfer API allows exactly one outer transaction per thread and throws if a second is
     * opened. Nerospace's pipe tick normally has none, but this store is also reached from inside another
     * mod's transaction (any foreign {@code Storage} that pulls from us mid-transfer), so an unconditional
     * {@code openOuter()} would crash that path.</p>
     */
    private static Transaction openTransaction() {
        return Transaction.isOpen()
                ? Transaction.openNested(Transaction.getCurrentUnsafe())
                : Transaction.openOuter();
    }

    /**
     * {@link NerospaceItemStore} over a foreign {@code Storage<ItemVariant>}.
     *
     * <p>{@code simulate} maps onto the transaction itself: the work is always really performed inside a
     * transaction and then either committed or left to abort on close, which is how the Transfer API
     * expects a dry run to be done — asking a {@code Storage} what it <em>would</em> accept is not part
     * of its interface.</p>
     */
    private static final class TransferApiItemStore implements NerospaceItemStore {

        private final Storage<ItemVariant> storage;

        private TransferApiItemStore(Storage<ItemVariant> storage) {
            this.storage = storage;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            try (Transaction transaction = openTransaction()) {
                // ItemVariant.of(stack) carries the stack's components, so a foreign storage stacks it
                // with matching contents only — the count is passed separately.
                long inserted = this.storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                if (inserted <= 0) {
                    return stack;
                }
                if (!simulate) {
                    transaction.commit();
                }
                int remainder = stack.getCount() - (int) inserted;
                return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
            }
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
            if (maxCount <= 0) {
                return ItemStack.EMPTY;
            }
            try (Transaction transaction = openTransaction()) {
                for (StorageView<ItemVariant> view : this.storage.nonEmptyViews()) {
                    ItemVariant variant = view.getResource();
                    if (variant.isBlank()) {
                        continue;
                    }
                    // The filter is written against ItemStacks, so probe it with a single-item stack —
                    // it carries the same item + components the real extraction will yield.
                    if (!filter.test(variant.toStack(1))) {
                        continue;
                    }
                    long extracted = view.extract(variant, maxCount, transaction);
                    if (extracted <= 0) {
                        continue;
                    }
                    if (!simulate) {
                        transaction.commit();
                    }
                    return variant.toStack((int) extracted);
                }
                return ItemStack.EMPTY;
            }
        }
    }
}
