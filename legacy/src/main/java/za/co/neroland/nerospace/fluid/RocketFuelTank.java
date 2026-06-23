package za.co.neroland.nerospace.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * A single-tank fluid store for {@code rocket_fuel}, built on 26.1's transfer API
 * ({@link FluidStacksResourceHandler} / {@link FluidResource}) rather than the deprecated classic
 * {@code FluidTank}/{@code IFluidHandler}. One stack slot of fixed capacity that only accepts rocket
 * fuel; a change callback drives GUI sync / chunk saving.
 *
 * <p>Internal machine logic uses the non-transactional {@link #fill(int)} / {@link #drain(int)}
 * helpers; the inherited {@link net.neoforged.neoforge.transfer.ResourceHandler} surface (transaction
 * based {@code insert}/{@code extract}) is what pipes/automation will use once a capability is
 * registered. Persistence is the inherited {@link #serialize}/{@link #deserialize} (ValueIO).</p>
 */
public class RocketFuelTank extends FluidStacksResourceHandler {

    private final Runnable onChange;

    public RocketFuelTank(int capacity, Runnable onChange) {
        super(1, capacity);
        this.onChange = onChange;
    }

    /** The rocket-fuel resource (resolved lazily; the fluid is registered before any tick runs). */
    private static FluidResource fuel() {
        return FluidResource.of(ModFluids.ROCKET_FUEL.get());
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || resource.getFluid() == ModFluids.ROCKET_FUEL.get();
    }

    @Override
    protected void onContentsChanged(int index, FluidStack oldStack) {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    /** Current stored fuel, in millibuckets. */
    public int getAmount() {
        return getAmountAsInt(0);
    }

    /** Maximum capacity, in millibuckets. */
    public int getCapacity() {
        return this.capacity;
    }

    public boolean isEmpty() {
        return getAmount() <= 0;
    }

    /**
     * Adds up to {@code amount} mB (capped at capacity).
     *
     * @return the millibuckets actually added.
     */
    public int fill(int amount) {
        int current = getAmount();
        int next = Math.min(this.capacity, current + Math.max(0, amount));
        if (next != current) {
            set(0, fuel(), next);
        }
        return next - current;
    }

    /**
     * Removes up to {@code amount} mB.
     *
     * @return the millibuckets actually removed.
     */
    public int drain(int amount) {
        int current = getAmount();
        int removed = Math.min(Math.max(0, amount), current);
        if (removed > 0) {
            int next = current - removed;
            set(0, next == 0 ? FluidResource.EMPTY : fuel(), next);
        }
        return removed;
    }
}
