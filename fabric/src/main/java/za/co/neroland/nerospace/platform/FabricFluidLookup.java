package za.co.neroland.nerospace.platform;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fabric.NerospaceFabric;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * Fabric query of the mod's fluid block-api lookup, with a cross-mod fallback.
 *
 * <p>The mod-private {@code nerospace:fluid} lookup is asked FIRST and its answer is returned verbatim,
 * so every Nerospace block keeps exactly the behaviour it had. Only when that misses do we ask
 * {@code FluidStorage.SIDED} — the Transfer API lookup foreign Fabric mods publish their tanks on — and
 * wrap the result. That is the IMPORT half of the fluid bridge; the export half is
 * {@link FabricFluidStorageAdapter}, registered on {@code FluidStorage.SIDED} in {@code NerospaceFabric}.</p>
 */
public final class FabricFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        NerospaceFluidStorage own = NerospaceFabric.FLUID.find(level, pos, side);
        if (own != null) {
            return own;
        }
        Storage<FluidVariant> foreign = FluidStorage.SIDED.find(level, pos, side);
        return foreign == null ? null : new TransferApiFluidStorage(foreign);
    }

    /**
     * Opens a transaction, nesting when the caller already has one open.
     *
     * <p>The Transfer API permits exactly one outer transaction per thread and throws on a second. A pipe
     * tick usually holds none, but this lookup is also reached from inside a foreign mod's transaction
     * (for instance while that mod is moving fluid through us), so an unconditional {@code openOuter()}
     * would crash that path. {@code getCurrentUnsafe()} is "unsafe" only in that the caller must not
     * retain the context — nesting immediately, as here, is its intended use.</p>
     */
    private static Transaction openTransaction() {
        return Transaction.isOpen()
                ? Transaction.openNested(Transaction.getCurrentUnsafe())
                : Transaction.openOuter();
    }

    /**
     * {@link NerospaceFluidStorage} over a foreign {@code Storage<FluidVariant>}.
     *
     * <p><b>Units.</b> The Transfer API counts droplets, Nerospace counts millibuckets; every crossing
     * goes through {@link FabricFluidStorageAdapter#DROPLETS_PER_MILLIBUCKET} and droplets→mB rounds
     * DOWN, so a foreign tank holding a fraction of a millibucket reports it as nothing rather than as
     * fluid the pipe cannot actually move.</p>
     *
     * <p><b>Exactness.</b> {@link #fill}/{@link #drain} are two-phase: a nested, always-aborted
     * transaction learns what the foreign storage would move, that figure is floored to whole mB, and
     * only that whole-mB amount is then really moved. The real move is committed ONLY when it lands on
     * exactly that figure; anything else is aborted and reported as nothing moved. Both halves matter —
     * committing the raw first attempt would transfer up to 80 droplets the floored return value does
     * not account for, and since the pipe settles its own tank against our return value, that mismatch
     * mints fluid on the fill path and voids it on the drain path.</p>
     *
     * <p>A Nerospace tank is single-fluid, so this view reports the foreign storage's contents as one
     * fluid: whatever {@link StorageUtil#findStoredResource} names first.</p>
     */
    private static final class TransferApiFluidStorage implements NerospaceFluidStorage {

        private final Storage<FluidVariant> storage;

        private TransferApiFluidStorage(Storage<FluidVariant> storage) {
            this.storage = storage;
        }

        @Override
        public Fluid getFluid() {
            FluidVariant stored = StorageUtil.findStoredResource(this.storage);
            return (stored == null || stored.isBlank()) ? Fluids.EMPTY : stored.getFluid();
        }

        @Override
        public long getAmount() {
            FluidVariant stored = StorageUtil.findStoredResource(this.storage);
            if (stored == null || stored.isBlank()) {
                return 0;
            }
            long droplets = 0;
            for (StorageView<FluidVariant> view : this.storage) {
                if (stored.equals(view.getResource())) {
                    droplets += view.getAmount();
                }
            }
            return droplets / FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
        }

        @Override
        public long getCapacity() {
            FluidVariant stored = StorageUtil.findStoredResource(this.storage);
            long droplets = 0;
            for (StorageView<FluidVariant> view : this.storage) {
                // Count the views that hold the reported fluid plus the empty ones, which is the room this
                // fluid could occupy. A multi-tank foreign storage holding something else in another slot
                // is deliberately not counted — that room is not available to us.
                if (view.isResourceBlank() || (stored != null && stored.equals(view.getResource()))) {
                    droplets += view.getCapacity();
                }
            }
            // A foreign view may report capacity 0 while holding fluid (an unbounded or purely
            // computed storage), which would leave us claiming capacity < amount and break every caller
            // that treats the difference as free room. Floor at what we already report as stored — the
            // same guard the NeoForge twin applies.
            return Math.max(droplets / FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET, getAmount());
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
                return 0;
            }
            FluidVariant variant = FluidVariant.of(fluid);
            long requestedDroplets = amount * FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
            try (Transaction transaction = openTransaction()) {
                // simulateInsert runs in its own nested, aborted transaction — nothing here is visible yet.
                long acceptedMb = StorageUtil.simulateInsert(this.storage, variant, requestedDroplets, transaction)
                        / FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
                if (acceptedMb <= 0 || simulate) {
                    return Math.max(acceptedMb, 0);   // closing without commit aborts
                }
                long intended = acceptedMb * FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
                long moved = this.storage.insert(variant, intended, transaction);
                // Commit ONLY an exact whole-millibucket move. Committing a move that is not a multiple
                // of 81 droplets while returning the floored mB figure tells the caller we shipped less
                // than we did — and the pipe then debits our own tank by that smaller figure, minting up
                // to a millibucket per push, per face, per tick. Anything inexact is aborted (closing
                // without commit) and reported as nothing moved.
                if (moved != intended) {
                    return 0;
                }
                transaction.commit();
                return acceptedMb;
            }
        }

        @Override
        public long drain(long amount, boolean simulate) {
            if (amount <= 0) {
                return 0;
            }
            FluidVariant stored = StorageUtil.findStoredResource(this.storage);
            if (stored == null || stored.isBlank()) {
                return 0;
            }
            long requestedDroplets = amount * FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
            try (Transaction transaction = openTransaction()) {
                long availableMb;
                // There is no simulateExtract in the API, so do the extraction in a nested transaction and
                // let it abort on close — the same trick StorageUtil.simulateInsert uses internally.
                try (Transaction probe = Transaction.openNested(transaction)) {
                    availableMb = this.storage.extract(stored, requestedDroplets, probe)
                            / FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
                }
                if (availableMb <= 0 || simulate) {
                    return Math.max(availableMb, 0);
                }
                long intended = availableMb * FabricFluidStorageAdapter.DROPLETS_PER_MILLIBUCKET;
                long moved = this.storage.extract(stored, intended, transaction);
                // Same exactness rule as fill(): a committed move whose droplet count is not a whole
                // number of millibuckets would be reported ~1 mB short, and that remainder is voided —
                // it leaves the foreign storage and never lands in ours. Abort instead.
                if (moved != intended) {
                    return 0;
                }
                transaction.commit();
                return availableMb;
            }
        }
    }
}
