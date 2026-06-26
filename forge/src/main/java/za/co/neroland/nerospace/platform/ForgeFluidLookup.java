package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.forge.ForgeCapabilities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.rocket.RocketPadFluidProxy;

/** Forge query of the mod's fluid capability. */
public final class ForgeFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            return be.getCapability(ForgeCapabilities.FLUID, side).orElse(null);
        }
        if (level.getBlockState(pos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
            return new RocketPadFluidProxy(level, pos);
        }
        return null;
    }
}
