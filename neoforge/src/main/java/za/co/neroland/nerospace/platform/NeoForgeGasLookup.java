package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/** NeoForge query of the mod's gas capability. */
public final class NeoForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side) {
        NerospaceGasStorage storage = level.getCapability(NeoForgeCapabilities.GAS, pos, side);
        return storage;
    }
}
