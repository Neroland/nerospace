package za.co.neroland.nerospace.gas;

/**
 * Loader-neutral single-gas store (amount in millibuckets), exposed per loader via a mod-owned
 * capability/lookup — the gas analogue of {@link za.co.neroland.nerospace.fluid.NerospaceFluidStorage}
 * and {@link za.co.neroland.nerospace.energy.NerospaceEnergyStorage}.
 */
public interface NerospaceGasStorage {

    /** The stored gas, or {@link GasResource#EMPTY} if empty. */
    GasResource getGas();

    long getAmount();

    long getCapacity();

    /** Fill with {@code gas} (must match the stored gas unless empty). @return mB filled. */
    long fill(GasResource gas, long amount, boolean simulate);

    /** Drain the stored gas. @return mB drained. */
    long drain(long amount, boolean simulate);
}
