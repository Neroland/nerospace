package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * NeoForge energy query — delegates to Neroland Core's shared {@code nerolandcore:energy} lookup so a
 * neighbour query finds machines from any Nero mod. Nerospace's own machines are also registered on
 * Core's capability (see {@code NeoForgeCapabilities}), so they remain findable through this path.
 */
public final class NeoForgeEnergyLookup implements EnergyLookup {

    @Nullable
    @Override
    public NeroEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        return za.co.neroland.nerolandcore.platform.EnergyLookup.INSTANCE.find(level, pos, side);
    }
}
