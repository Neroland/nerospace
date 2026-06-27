package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.forge.ForgeCapabilities;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.rocket.RocketPadGasProxy;

/** Forge query of the mod's gas capability. */
public final class ForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            NerospaceGasStorage cap = be.getCapability(ForgeCapabilities.GAS, side).orElse(null);
            if (cap != null) {
                return cap;
            }
        }
        // The launch pad has no block entity: expose the rocket gas sink directly (Oxygen Generator /
        // gas pipe → pad → docked rocket's life-support tank).
        if (level.getBlockState(pos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
            return new RocketPadGasProxy(level, pos);
        }
        return null;
    }
}
