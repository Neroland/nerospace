package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fabric.NerospaceFabric;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/** Fabric query of the mod's gas block-api lookup. */
public final class FabricGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        return NerospaceFabric.GAS.find(level, pos, side);
    }
}
