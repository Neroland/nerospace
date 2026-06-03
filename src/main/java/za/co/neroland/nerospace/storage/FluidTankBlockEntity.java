package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Fluid Tank: a passive single-fluid store. Accepts any one fluid (buckets or pipes), provides it back
 * out; right-click with a bucket fills/empties it.
 */
public class FluidTankBlockEntity extends BlockEntity {

    private final Tank tank = new Tank();

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    public FluidResource storedFluid() {
        return this.tank.getResource(0);
    }

    public int storedAmount() {
        return this.tank.getAmountAsInt(0);
    }

    public int capacity() {
        return Config.FLUID_TANK_CAPACITY.get();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("Tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("Tank"));
    }

    private final class Tank extends FluidStacksResourceHandler {
        private Tank() {
            super(1, Config.FLUID_TANK_CAPACITY.get());
        }

        @Override
        protected void onContentsChanged(int index, FluidStack oldStack) {
            FluidTankBlockEntity.this.setChanged();
        }
    }
}
