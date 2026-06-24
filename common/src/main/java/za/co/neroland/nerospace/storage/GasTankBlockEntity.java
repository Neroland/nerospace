package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Gas Tank — a single-gas buffer block entity, exposed via the mod's gas capability/lookup. */
public class GasTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 16_000; // mB

    private final GasTank tank = new GasTank(CAPACITY, this::setChanged);

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TANK.get(), pos, state);
    }

    public NerospaceGasStorage getTank() {
        return this.tank;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Gas", this.tank.getRawGas().getSerializedName());
        output.putInt("Amount", this.tank.getRawAmount());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        GasResource gas = GasResource.byName(input.getStringOr("Gas", "empty"));
        this.tank.setRaw(gas, input.getIntOr("Amount", 0));
    }
}
