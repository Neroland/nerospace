package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/** NeoForge query of the mod's fluid capability. */
public final class NeoForgeFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side) {
        NerospaceFluidStorage storage = level.getCapability(NeoForgeCapabilities.FLUID, pos, side);
        return storage;
    }
}
