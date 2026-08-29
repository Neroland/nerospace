package za.co.neroland.nerospace.platform;

import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * EXPORT half of the Fabric fluid bridge: presents one Nerospace {@link NerospaceFluidStorage} to the
 * rest of the ecosystem as a Fabric Transfer API {@link SingleSlotStorage} of {@link FluidVariant}.
 *
 * <p>Why this exists: Nerospace tanks are only visible on the mod's own {@code nerospace:fluid} lookup,
 * so a foreign pipe or pump — which only ever asks {@code FluidStorage.SIDED} — sees nothing at all
 * where a Nerospace tank sits. Registering this adapter on {@code FluidStorage.SIDED} alongside the
 * mod-private lookup makes those tanks fillable and drainable by any other Fabric mod, with no change to
 * the mod-private path.</p>
 *
 * <p><b>Units.</b> The Transfer API counts <em>droplets</em> ({@code FluidConstants.BUCKET} = 81000 per
 * bucket); {@link NerospaceFluidStorage} counts <em>millibuckets</em>. Every value crossing this class
 * is converted through {@link #DROPLETS_PER_MILLIBUCKET}, and droplets→mB always rounds DOWN so we
 * never promise fluid the tank cannot actually deliver. A caller asking for fewer than 81 droplets
 * therefore moves nothing — correct, because a Nerospace tank has no sub-millibucket state to spend.</p>
 *
 * <p><b>Transactions.</b> A Transfer API caller may abort the transaction our mutation happened in, so
 * this extends {@link SnapshotParticipant} over a {@code (variant, mB)} pair: the snapshot is taken
 * (via {@link #updateSnapshots(TransactionContext)}) BEFORE the tank is touched, and a rollback restores
 * the tank by emptying it and re-filling the snapshotted contents. The tank itself knows nothing about
 * transactions, which is exactly why the snapshot has to be a full value copy rather than a delta.</p>
 *
 * <p><b>Precondition on what may be wrapped.</b> That drain-then-refill restore is faithful only for a
 * storage whose {@code fill} and {@code drain} are genuine inverses — a real tank. A one-way proxy must
 * NOT be registered through this adapter: rollback cannot take back what its {@code fill} did, and the
 * re-fill leg then applies the effect a SECOND time. That is why
 * {@code za.co.neroland.nerospace.rocket.RocketPadFluidProxy} (a pure sink whose {@code drain} always
 * returns 0) stays on the mod-private lookup only — see the comment at its registration in
 * {@code NerospaceFabric}. Aborted transactions are routine, not exceptional:
 * {@code StorageUtil.simulateInsert} is an insert followed by an abort.</p>
 */
public final class FabricFluidStorageAdapter extends SnapshotParticipant<ResourceAmount<FluidVariant>>
        implements SingleSlotStorage<FluidVariant> {

    /**
     * Droplets in one millibucket (81). The single conversion constant for the whole bridge —
     * {@code FluidConstants.BUCKET} is 81000 droplets and a bucket is 1000 mB.
     */
    public static final long DROPLETS_PER_MILLIBUCKET = FluidConstants.BUCKET / 1000L;

    private final NerospaceFluidStorage tank;

    private FabricFluidStorageAdapter(NerospaceFluidStorage tank) {
        this.tank = tank;
    }

    /**
     * Wrap {@code tank}, propagating {@code null}. The mod-private providers hand back {@code null} for a
     * face the machine's side config has disabled, and a {@code BlockApiLookup} provider is likewise
     * allowed to return {@code null} — so the null-in/null-out shape lets every registration in
     * {@code NerospaceFabric} stay a one-line lambda that keeps the gating it already had.
     */
    @Nullable
    public static FabricFluidStorageAdapter of(@Nullable NerospaceFluidStorage tank) {
        return tank == null ? null : new FabricFluidStorageAdapter(tank);
    }

    // ---------------------------------------------------------------- Storage / StorageView (droplets)

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long requestedMb = maxAmount / DROPLETS_PER_MILLIBUCKET;   // round down: never over-promise
        if (requestedMb <= 0) {
            return 0;
        }
        // Simulate first so the snapshot is only taken when the tank will really change.
        long acceptedMb = this.tank.fill(resource.getFluid(), requestedMb, true);
        if (acceptedMb <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        long filledMb = this.tank.fill(resource.getFluid(), acceptedMb, false);
        return filledMb * DROPLETS_PER_MILLIBUCKET;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        // A Nerospace tank is single-fluid and drain() takes no fluid argument, so the variant match has
        // to be enforced here or we would hand a caller the wrong fluid under its own name.
        if (resource.getFluid() != this.tank.getFluid()) {
            return 0;
        }
        long requestedMb = maxAmount / DROPLETS_PER_MILLIBUCKET;
        if (requestedMb <= 0) {
            return 0;
        }
        long availableMb = this.tank.drain(requestedMb, true);
        if (availableMb <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        long drainedMb = this.tank.drain(availableMb, false);
        return drainedMb * DROPLETS_PER_MILLIBUCKET;
    }

    @Override
    public boolean isResourceBlank() {
        return getResource().isBlank();
    }

    @Override
    public FluidVariant getResource() {
        return variantOf(this.tank.getFluid());
    }

    @Override
    public long getAmount() {
        return this.tank.getAmount() * DROPLETS_PER_MILLIBUCKET;
    }

    @Override
    public long getCapacity() {
        return this.tank.getCapacity() * DROPLETS_PER_MILLIBUCKET;
    }

    // ---------------------------------------------------------------- SlottedStorage / Iterable
    // SingleSlotStorage supplies defaults for these in the current Transfer API, but they are spelled out
    // so the adapter does not depend on which of them are default methods in a given API revision.

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return List.<StorageView<FluidVariant>>of(this).iterator();
    }

    @Override
    public int getSlotCount() {
        return 1;
    }

    @Override
    public SingleSlotStorage<FluidVariant> getSlot(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException("Slot " + slot + " out of bounds for a single-slot tank");
        }
        return this;
    }

    @Override
    public List<SingleSlotStorage<FluidVariant>> getSlots() {
        return List.of(this);
    }

    // ---------------------------------------------------------------- SnapshotParticipant (millibuckets)

    /** Snapshot amounts are kept in mB — the tank's own unit — so a rollback replays exactly what it held. */
    @Override
    protected ResourceAmount<FluidVariant> createSnapshot() {
        return new ResourceAmount<>(variantOf(this.tank.getFluid()), this.tank.getAmount());
    }

    @Override
    protected void readSnapshot(ResourceAmount<FluidVariant> snapshot) {
        // NerospaceFluidStorage has no "set contents" operation, so restore = empty it, then re-fill. The
        // drain is unconditional because the aborted transaction may have changed the FLUID, not just the
        // amount, and a tank refuses a fill of a fluid different from the one it currently holds.
        // This is exact only for an invertible tank — see the precondition in the class javadoc.
        long held = this.tank.getAmount();
        if (held > 0) {
            this.tank.drain(held, false);
        }
        if (!snapshot.resource().isBlank() && snapshot.amount() > 0) {
            this.tank.fill(snapshot.resource().getFluid(), snapshot.amount(), false);
        }
    }

    private static FluidVariant variantOf(@Nullable Fluid fluid) {
        return (fluid == null || fluid == Fluids.EMPTY) ? FluidVariant.blank() : FluidVariant.of(fluid);
    }
}
