package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Solar Panel — a daylight generator: while it is day and the panel can see the sky it trickles energy
 * into its buffer (reduced in rain). Exposes the energy capability (extract-only, drained by the pipe
 * network). Single-tier and GUI-less here; the root project's tiered sun-tracking array + renderer are
 * a deferred enhancement.
 */
public class SolarPanelBlockEntity extends BlockEntity {

    public static final int CAPACITY = 40_000;
    public static final int MAX_EXTRACT = 256;
    public static final int FE_PER_TICK = 16;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, MAX_EXTRACT, this::setChanged);

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        long dayTime = level.getDayTime() % 24000L;
        boolean day = dayTime < 12300L || dayTime > 23850L;
        if (!day || !level.canSeeSky(pos.above())) {
            return;
        }
        int rate = FE_PER_TICK;
        if (level.isRaining() || level.isThundering()) {
            rate /= 2;
        }
        this.energy.generate(rate);
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
