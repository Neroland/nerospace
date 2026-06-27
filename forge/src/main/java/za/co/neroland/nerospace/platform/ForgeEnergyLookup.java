package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * Forge energy query — delegates to Neroland Core's shared {@code nerolandcore:energy} lookup so a
 * neighbour query finds machines from any Nero mod. Nerospace's own machines also attach Core's
 * capability (see {@code ForgeCapabilities}), so they remain findable through this path.
 */
public final class ForgeEnergyLookup implements EnergyLookup {

    @Nullable
    @Override
    public NeroEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        return za.co.neroland.nerolandcore.platform.EnergyLookup.INSTANCE.find(level, pos, side);
    }
}
