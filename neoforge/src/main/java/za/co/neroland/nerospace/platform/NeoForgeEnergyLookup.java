package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/** NeoForge query of the mod's energy capability. */
public final class NeoForgeEnergyLookup implements EnergyLookup {

    @Nullable
    @Override
    public NerospaceEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        NerospaceEnergyStorage storage = level.getCapability(NeoForgeCapabilities.ENERGY, pos, side);
        return storage;
    }
}
