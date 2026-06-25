package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;

/**
 * Query side of the energy seam: find the energy storage exposed by the block at {@code pos} on
 * {@code side}. Each loader implements it over its own lookup mechanism (NeoForge
 * {@code Level.getCapability}, Fabric {@code BlockApiLookup.find}). Resolved via {@link Services}.
 */
public interface EnergyLookup {

    EnergyLookup INSTANCE = Services.load(EnergyLookup.class);

    @Nullable
    NerospaceEnergyStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side);
}
