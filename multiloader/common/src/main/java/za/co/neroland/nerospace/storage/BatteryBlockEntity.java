package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Battery — a passive energy buffer block entity. Exposes {@link NerospaceEnergyStorage} to the
 * mod's energy capability/lookup on both loaders (see the loader entry points). No ticker, no GUI.
 */
public class BatteryBlockEntity extends BlockEntity {

    public static final int CAPACITY = 1_000_000;
    public static final int MAX_IO = 10_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
    }
}
