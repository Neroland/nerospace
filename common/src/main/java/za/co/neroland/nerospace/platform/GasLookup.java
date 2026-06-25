package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/**
 * Query side of the gas seam: find the gas storage exposed by the block at {@code pos} on
 * {@code side}. Mirrors {@link EnergyLookup} — NeoForge implements it over {@code Level.getCapability},
 * Fabric over {@code BlockApiLookup.find}. Resolved via {@link Services}.
 */
public interface GasLookup {

    GasLookup INSTANCE = Services.load(GasLookup.class);

    @Nullable
    NerospaceGasStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side);
}
