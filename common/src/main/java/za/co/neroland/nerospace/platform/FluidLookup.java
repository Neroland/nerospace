package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * Query side of the fluid seam: find the fluid storage exposed by the block at {@code pos} on
 * {@code side}. Mirrors {@link EnergyLookup}/{@link GasLookup} — NeoForge implements it over
 * {@code Level.getCapability}, Fabric over {@code BlockApiLookup.find}. Resolved via {@link Services}.
 */
public interface FluidLookup {

    FluidLookup INSTANCE = Services.load(FluidLookup.class);

    @Nullable
    NerospaceFluidStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side);
}
