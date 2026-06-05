package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.ResourceStack;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasStacksResourceHandler;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Gas Tank: a passive single-gas store, filled and drained through the pipe gas layer. */
public class GasTankBlockEntity extends BlockEntity {

    private final Tank tank = new Tank();

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TANK.get(), pos, state);
    }

    public ResourceHandler<GasResource> getGasHandler() {
        return this.tank;
    }

    public GasResource storedGas() {
        return this.tank.getResource(0);
    }

    public int storedAmount() {
        return this.tank.getAmountAsInt(0);
    }

    public int capacity() {
        return Tuning.gasTankCapacity();
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

    private final class Tank extends GasStacksResourceHandler {
        private Tank() {
            super(1, Tuning.gasTankCapacity());
        }

        @Override
        protected void onContentsChanged(int index, ResourceStack<GasResource> oldStack) {
            GasTankBlockEntity.this.setChanged();
        }
    }
}
