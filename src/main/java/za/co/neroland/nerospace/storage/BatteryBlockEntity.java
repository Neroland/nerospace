package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Battery: a passive energy (FE) store. Accepts and provides energy on every side at pipe throughput,
 * so it buffers a grid — generators fill it, machines drain it through the network.
 */
public class BatteryBlockEntity extends BlockEntity {

    private final Cell energy = new Cell();

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
    }

    private final class Cell extends SimpleEnergyHandler {
        private Cell() {
            super(Tuning.batteryCapacity(),
                    Tuning.energyPipeThroughput(), Tuning.energyPipeThroughput());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            BatteryBlockEntity.this.setChanged();
        }
    }
}
