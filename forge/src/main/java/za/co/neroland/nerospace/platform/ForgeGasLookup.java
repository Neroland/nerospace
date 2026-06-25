package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.forge.ForgeCapabilities;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/** Forge query of the mod's gas capability. */
public final class ForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        LazyOptional<? extends NerospaceGasStorage> storage = be.getCapability(ForgeCapabilities.GAS, side);
        if (storage.isPresent()) {
            return storage.orElseThrow(IllegalStateException::new);
        }
        return null;
    }
}
