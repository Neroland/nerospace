package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.forge.ForgeCapabilities;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/** Forge query of the mod's gas capability. */
@SuppressWarnings("null")
public final class ForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        return be.getCapability(ForgeCapabilities.GAS, side).orElse(null);
    }
}
