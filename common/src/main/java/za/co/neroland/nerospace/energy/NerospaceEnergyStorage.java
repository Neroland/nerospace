package za.co.neroland.nerospace.energy;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * Loader-neutral energy storage interface. Now a thin specialisation of Neroland Core's
 * {@link NeroEnergyStorage} — the canonical Nero energy surface — so every Nerospace generator,
 * battery and machine is an {@code NeroEnergyStorage} and interoperates with machines from any other
 * Nero mod on the shared {@code nerolandcore:energy} capability. The method shape is identical
 * (Core adds default {@code canReceive()}/{@code canExtract()}), so this remains source-compatible.
 * Cross-mod bridging to the platforms' native FE libraries stays deferred until they port to 26.x.
 */
public interface NerospaceEnergyStorage extends NeroEnergyStorage {

    @Override
    long getAmount();

    @Override
    long getCapacity();

    /** @return energy actually inserted (0 if none). */
    @Override
    long insert(long maxAmount, boolean simulate);

    /** @return energy actually extracted (0 if none). */
    @Override
    long extract(long maxAmount, boolean simulate);
}
