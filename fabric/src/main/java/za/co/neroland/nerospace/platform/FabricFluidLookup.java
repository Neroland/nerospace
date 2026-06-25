package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.fabric.NerospaceFabric;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/** Fabric query of the mod's fluid block-api lookup. */
public final class FabricFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side) {
        return NerospaceFabric.FLUID.find(level, pos, side);
    }
}
