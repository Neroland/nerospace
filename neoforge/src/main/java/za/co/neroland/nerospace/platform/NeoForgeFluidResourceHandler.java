package za.co.neroland.nerospace.platform;

import java.util.Objects;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * EXPORT half of the NeoForge fluid bridge: presents one Nerospace {@link NerospaceFluidStorage} to the
 * rest of the ecosystem as a platform-standard {@code ResourceHandler<FluidResource>} with a single
 * logical tank ({@code size() == 1}).
 *
 * <p>Why this exists: a Nerospace tank is only visible on the mod's own {@code nerospace:fluid}
 * capability, so a foreign pipe or pump — which only ever asks {@code Capabilities.Fluid.BLOCK} — sees
 * nothing where a Nerospace tank sits. Registering this adapter on that capability for every block
 * entity that already provides the mod-private one makes those tanks fillable and drainable by any
 * other NeoForge mod, with no change to the mod-private path. This is the NeoForge twin of
 * {@code FabricFluidStorageAdapter}, and it deliberately has the same shape — see below.</p>
 *
 * <p><b>Units.</b> NeoForge's transfer API counts fluid in millibuckets, and so does
 * {@link NerospaceFluidStorage} — nothing is converted here (unlike the Fabric side, which has to cross
 * a droplet boundary).</p>
 *
 * <p><b>Transactions — mutate eagerly, undo on rollback.</b> A transfer-API caller may abort the
 * transaction our mutation happened in, and {@link NerospaceFluidStorage} knows nothing about
 * transactions. This class therefore does exactly what the Fabric sibling does: simulate first, take a
 * snapshot of the tank's {@code (fluid, amount)} through {@link #updateSnapshots(TransactionContext)}
 * BEFORE touching it, then perform the real {@code fill}/{@code drain} immediately;
 * {@link #revertToSnapshot} puts the tank back if the transaction aborts.</p>
 *
 * <p>Deferring the mutation to a commit hook ({@link SnapshotJournal#onRootCommit}) looks tidier and is
 * WRONG here, for a reason worth recording so it is not re-attempted: a fresh handler is built per
 * {@code getCapability} call, so one tank can have several live handles inside a single transaction (a
 * foreign pipe touching a Nerospace tank on two faces, for instance). With deferral each handle stages
 * against the untouched tank and so answers the same fluid twice — two {@code extract(1000)} calls each
 * return 1000 mB from a 1000 mB tank, and the commit that runs second finds nothing left to move. Eager
 * mutation makes that structurally impossible: the second handle reads a tank that has already moved.</p>
 *
 * <p><b>Precondition on what may be registered.</b> Because a rollback is replayed as "drain everything,
 * re-fill the snapshot", only an INVERTIBLE storage may be exposed through this handler — one where
 * {@code fill} and {@code drain} really are each other's inverse. A write-only sink is not invertible and
 * must NOT be registered: {@code RocketPadFluidProxy} is the live example (its {@code drain} always
 * returns 0 while its {@code fill} permanently fuels a docked rocket), so a routine
 * insert-then-abort probe by a foreign mod would hand the rocket free fuel every tick. It is deliberately
 * absent from {@code NeoForgeCapabilities.registerStandardFluid} for that reason.</p>
 *
 * <p>An instance is bound to one tank and is created fresh per capability query; it holds no position.</p>
 */
public final class NeoForgeFluidResourceHandler extends SnapshotJournal<NeoForgeFluidResourceHandler.Contents>
        implements ResourceHandler<FluidResource> {

    private final NerospaceFluidStorage tank;

    private NeoForgeFluidResourceHandler(NerospaceFluidStorage tank) {
        this.tank = tank;
    }

    /**
     * Wrap {@code tank}, propagating {@code null}. The mod-private providers hand back {@code null} for a
     * face the machine's side config has disabled, so the null-in/null-out shape lets every registration
     * in {@code NeoForgeCapabilities} mirror its mod-private twin as a one-line lambda and keep exactly
     * the gating it already had — a DISABLED face stays invisible on the standard capability too.
     */
    @Nullable
    public static NeoForgeFluidResourceHandler of(@Nullable NerospaceFluidStorage tank) {
        return tank == null ? null : new NeoForgeFluidResourceHandler(tank);
    }

    // ---------------------------------------------------------------- ResourceHandler (millibuckets)

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, 1);
        Fluid held = normalise(this.tank.getFluid());
        return held == Fluids.EMPTY ? FluidResource.EMPTY : FluidResource.of(held);
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, 1);
        return this.tank.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, 1);
        return this.tank.getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, 1);
        if (resource.isEmpty()) {
            return true;
        }
        // A Nerospace tank stores a bare Fluid: it has nowhere to put a component patch, so accepting a
        // component-carrying resource would silently strip it. Refuse instead.
        if (!resource.isComponentsPatchEmpty()) {
            return false;
        }
        Fluid held = normalise(this.tank.getFluid());
        return held == Fluids.EMPTY || held == resource.getFluid();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, 1);
        if (resource.isEmpty() || amount <= 0 || !isValid(index, resource)) {
            return 0;
        }
        // Simulate first so the snapshot is only taken when the tank will really change. The tank's own
        // rules — finite capacity, single-fluid, a creative source that accepts nothing — decide the answer.
        long accepted = this.tank.fill(resource.getFluid(), amount, true);
        if (accepted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        long filled = this.tank.fill(resource.getFluid(), Math.min(accepted, amount), false);
        return (int) Math.max(0L, Math.min(filled, amount));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, 1);
        if (resource.isEmpty() || amount <= 0 || !resource.isComponentsPatchEmpty()) {
            return 0;
        }
        // A Nerospace tank is single-fluid and drain() takes no fluid argument, so the resource match has
        // to be enforced here or we would hand a caller the wrong fluid under its own name.
        Fluid held = normalise(this.tank.getFluid());
        if (held == Fluids.EMPTY || held != resource.getFluid()) {
            return 0;
        }
        long available = this.tank.drain(amount, true);
        if (available <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        long drained = this.tank.drain(Math.min(available, amount), false);
        return (int) Math.max(0L, Math.min(drained, amount));
    }

    private static Fluid normalise(@Nullable Fluid fluid) {
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    // ---------------------------------------------------------------- SnapshotJournal

    /**
     * The tank's contents as a value, in millibuckets — the tank's own unit, so a rollback replays exactly
     * what it held. Never {@code null}: the journal uses {@code null} as its own sentinel.
     */
    record Contents(Fluid fluid, long amount) {
    }

    @Override
    protected Contents createSnapshot() {
        return new Contents(normalise(this.tank.getFluid()), this.tank.getAmount());
    }

    @Override
    protected void revertToSnapshot(Contents snapshot) {
        // NerospaceFluidStorage has no "set contents" operation, so restore = empty it, then re-fill. The
        // drain is unconditional because the aborted transaction may have changed the FLUID, not just the
        // amount, and a tank refuses a fill of a fluid different from the one it currently holds.
        long held = this.tank.getAmount();
        if (held > 0) {
            this.tank.drain(held, false);
        }
        if (snapshot.fluid() != Fluids.EMPTY && snapshot.amount() > 0) {
            this.tank.fill(snapshot.fluid(), snapshot.amount(), false);
        }
    }
}
