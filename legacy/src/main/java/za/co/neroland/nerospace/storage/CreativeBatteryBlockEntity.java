package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.InfiniteEnergyHandler;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Creative Battery: an endless energy source (and sink) for testing grids. */
public class CreativeBatteryBlockEntity extends BlockEntity {

    public CreativeBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_BATTERY.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return InfiniteEnergyHandler.INSTANCE;
    }
}
