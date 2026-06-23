package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/** NeoForge query of the mod's gas capability. */
public final class NeoForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        return level.getCapability(NeoForgeCapabilities.GAS, pos, side);
    }
}
