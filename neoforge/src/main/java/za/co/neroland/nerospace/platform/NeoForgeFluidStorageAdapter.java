package za.co.neroland.nerospace.platform;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * IMPORT half of the NeoForge fluid bridge: presents a FOREIGN mod's platform-standard
 * {@code ResourceHandler<FluidResource>} to Nerospace as a {@link NerospaceFluidStorage}, so the
 * Universal Pipe, the Fuel Refinery and the Quarry can move fluid into and out of any other mod's tank.
 * This is what fixes "the Fluid Tank is useless because I cannot pipe fluid to a bigger tank".
 *
 * <p>Wrapped only as a FALLBACK by {@link NeoForgeFluidLookup}, after the mod-private
 * {@code nerospace:fluid} capability has come back {@code null} — Nerospace's own blocks keep their exact
 * current behaviour and never travel through this class.</p>
 *
 * <p><b>Shape mismatch.</b> A {@code ResourceHandler} is a multi-index, resource-typed store;
 * {@link NerospaceFluidStorage} is one single-fluid tank. The reconciliation is: the handler's FIRST
 * non-empty index defines the reported resource, and amounts/capacity are summed across the indices that
 * hold that resource (plus, for capacity, the empty ones it could still spread into). A handler holding
 * three different fluids therefore looks like a tank of the first one — the pipe only ever moves one
 * fluid at a time anyway, and under-reporting is the safe direction.</p>
 *
 * <p><b>Capacity.</b> {@code getCapacityAsLong(index, resource)} is the handler's own declared capacity
 * for that resource, so no probing is needed; sums saturate rather than overflow because an infinite
 * handler may report {@link Long#MAX_VALUE}. The limitation: a handler that declares no capacity (returns
 * 0) while still accepting fluid will under-report here, and a completely empty handler is asked for its
 * capacity with {@link FluidResource#EMPTY}, which some handlers answer with 0. Nothing in Nerospace
 * gates a push on a neighbour's capacity — {@code PipeNetwork} pushes by calling {@link #fill} and
 * believing the return value — so an under-report costs display accuracy, never throughput.</p>
 *
 * <p><b>Transactions.</b> {@link NerospaceFluidStorage} has a {@code simulate} flag where the transfer API
 * has transactions, so every call opens its own root transaction and commits it only when
 * {@code simulate} is false; closing without committing IS the rollback, which is exactly a simulation.
 * Opening a ROOT transaction is safe because this adapter is only ever reached from Nerospace's
 * transaction-unaware common code (pipe ticks, machine ticks), never from inside someone else's
 * transaction.</p>
 *
 * <p>An instance is bound to one position and one face for the tick it was resolved in; never cache one.</p>
 */
public final class NeoForgeFluidStorageAdapter implements NerospaceFluidStorage {

    private final ResourceHandler<FluidResource> handler;

    private NeoForgeFluidStorageAdapter(ResourceHandler<FluidResource> handler) {
        this.handler = handler;
    }

    /** Wrap {@code handler}, propagating {@code null} so the lookup stays a one-line fallback. */
    @Nullable
    public static NeoForgeFluidStorageAdapter of(@Nullable ResourceHandler<FluidResource> handler) {
        return handler == null ? null : new NeoForgeFluidStorageAdapter(handler);
    }

    @Override
    public Fluid getFluid() {
        return resource().getFluid();
    }

    @Override
    public long getAmount() {
        FluidResource reported = resource();
        if (reported.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (int i = 0; i < this.handler.size(); i++) {
            if (reported.equals(this.handler.getResource(i))) {
                total = saturatedAdd(total, this.handler.getAmountAsLong(i));
            }
        }
        return total;
    }

    @Override
    public long getCapacity() {
        FluidResource reported = resource();
        long total = 0L;
        for (int i = 0; i < this.handler.size(); i++) {
            FluidResource held = this.handler.getResource(i);
            // An index holding some OTHER fluid is not capacity this tank can ever use.
            if (held.isEmpty() || reported.isEmpty() || reported.equals(held)) {
                total = saturatedAdd(total, this.handler.getCapacityAsLong(i, reported));
            }
        }
        return Math.max(total, getAmount());
    }

    @Override
    public long fill(Fluid fluid, long amount, boolean simulate) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return 0L;
        }
        // Reuse the held resource when it is the same fluid so a component-carrying resource stacks
        // instead of being offered a bare copy the handler would refuse.
        FluidResource held = resource();
        FluidResource offered = !held.isEmpty() && held.getFluid() == fluid ? held : FluidResource.of(fluid);
        int request = clampToInt(amount);
        try (Transaction transaction = Transaction.openRoot()) {
            int filled = ResourceHandlerUtil.insertStacking(this.handler, offered, request, transaction);
            if (filled > 0 && !simulate) {
                transaction.commit();
            }
            return filled;
        }
    }

    @Override
    public long drain(long amount, boolean simulate) {
        FluidResource held = resource();
        if (held.isEmpty() || amount <= 0) {
            return 0L;
        }
        int request = clampToInt(amount);
        try (Transaction transaction = Transaction.openRoot()) {
            int drained = this.handler.extract(held, request, transaction);
            if (drained > 0 && !simulate) {
                transaction.commit();
            }
            return drained;
        }
    }

    /** The handler's first non-empty resource — the single fluid this adapter claims to be. */
    private FluidResource resource() {
        for (int i = 0; i < this.handler.size(); i++) {
            FluidResource held = this.handler.getResource(i);
            if (!held.isEmpty()) {
                return held;
            }
        }
        return FluidResource.EMPTY;
    }

    /** The transfer API counts in {@code int}; a Nerospace tank in {@code long}. Never ask for more. */
    private static int clampToInt(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }
}
