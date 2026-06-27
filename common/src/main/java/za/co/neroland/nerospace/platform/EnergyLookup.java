package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * Query side of the energy seam: find the energy storage exposed by the block at {@code pos} on
 * {@code side}. Each loader implementation now delegates to Neroland Core's shared energy lookup
 * ({@code nerolandcore:energy}), so a query returns the {@link NeroEnergyStorage} of ANY Nero mod's
 * machine — not just Nerospace's — letting energy cross mod boundaries on one network. Resolved via
 * {@link Services}.
 */
public interface EnergyLookup {

    EnergyLookup INSTANCE = Services.load(EnergyLookup.class);

    @Nullable
    NeroEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side);
}
