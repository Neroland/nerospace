package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Creative Fluid Tank — an endless source and sink of {@code rocket_fuel} for testing fluid logistics. */
public class CreativeFluidTankBlockEntity extends BlockEntity {

    private static final NerospaceFluidStorage INFINITE = new NerospaceFluidStorage() {
        @Override
        public Fluid getFluid() {
            return (Fluid) ModFluids.ROCKET_FUEL.get();
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
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return Math.max(0, amount); // accepts (voids) anything
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return Math.max(0, amount); // endless rocket fuel
        }
    };

    public CreativeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FLUID_TANK.get(), pos, state);
    }

    public NerospaceFluidStorage getTank() {
        return INFINITE;
    }
}
