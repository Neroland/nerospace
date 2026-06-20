package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Creative Battery — an endless energy source and sink for testing power grids. */
public class CreativeBatteryBlockEntity extends BlockEntity {

    private static final NerospaceEnergyStorage INFINITE = new NerospaceEnergyStorage() {
        @Override
        public long getAmount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            return Math.max(0, maxAmount);
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return Math.max(0, maxAmount);
        }
    };

    public CreativeBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_BATTERY.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return INFINITE;
    }
}
