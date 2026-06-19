package za.co.neroland.nerospace.fluid;

import net.minecraft.world.level.material.Fluid;

/**
 * Loader-neutral single-fluid tank interface (amount in millibuckets). Exposed per loader via a
 * mod-owned capability/lookup, like {@link za.co.neroland.nerospace.energy.NerospaceEnergyStorage}.
 * Platform-standard fluid handlers (NeoForge {@code Capabilities.Fluid} / Fabric
 * {@code FluidStorage}) + vanilla bucket interop are a deferred enhancement.
 */
public interface NerospaceFluidStorage {

    /** The stored fluid, or {@code Fluids.EMPTY} if empty. */
    Fluid getFluid();

    long getAmount();

    long getCapacity();

    /** Fill with {@code fluid} (must match the stored fluid unless empty). @return mB filled. */
    long fill(Fluid fluid, long amount, boolean simulate);

    /** Drain the stored fluid. @return mB drained. */
    long drain(long amount, boolean simulate);
}
