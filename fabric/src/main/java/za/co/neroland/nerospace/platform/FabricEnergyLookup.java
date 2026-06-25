package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fabric.NerospaceFabric;

/** Fabric query of the mod's energy block-api lookup. */
public final class FabricEnergyLookup implements EnergyLookup {

    @Nullable
    @Override
    public NerospaceEnergyStorage find(@NonNull Level level, @NonNull BlockPos pos, @Nullable Direction side) {
        return NerospaceFabric.ENERGY.find(level, pos, side);
    }
}
