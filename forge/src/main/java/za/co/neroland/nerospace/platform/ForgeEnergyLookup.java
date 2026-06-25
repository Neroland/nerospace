package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.forge.ForgeCapabilities;

/** Forge query of the mod's energy capability. */
public final class ForgeEnergyLookup implements EnergyLookup {

    @Nullable
    @Override
    public NerospaceEnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        LazyOptional<? extends NerospaceEnergyStorage> storage = be.getCapability(ForgeCapabilities.ENERGY, side);
        if (storage.isPresent()) {
            return storage.orElseThrow(IllegalStateException::new);
        }
        return null;
    }
}
