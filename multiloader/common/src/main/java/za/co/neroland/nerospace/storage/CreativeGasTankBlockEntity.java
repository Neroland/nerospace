package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Creative Gas Tank — an endless source and sink of oxygen for testing gas logistics. */
public class CreativeGasTankBlockEntity extends BlockEntity {

    private static final NerospaceGasStorage INFINITE = new NerospaceGasStorage() {
        @Override
        public GasResource getGas() {
            return GasResource.OXYGEN;
        }

        @Override
        public long getAmount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long fill(GasResource gas, long amount, boolean simulate) {
            return Math.max(0, amount); // accepts (voids) anything
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return Math.max(0, amount); // endless oxygen
        }
    };

    public CreativeGasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_GAS_TANK.get(), pos, state);
    }

    public NerospaceGasStorage getTank() {
        return INFINITE;
    }
}
