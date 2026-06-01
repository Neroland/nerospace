package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Oxygen Generator (Phase 8c): a machine that projects a breathable bubble while it has power. It
 * holds an internal energy buffer exposed via {@code Capabilities.Energy.BLOCK} (so a real generator
 * can drive it later); for this slice it self-charges (RTG placeholder), like the Nerosium Grinder,
 * so a placed generator is simply active. {@code GreenxertzAtmosphere} treats the area within
 * {@code oxygenBubbleRadius} of an active generator as a safe, oxygenated zone.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity {

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INSERT = 500;
    public static final int GENERATE_PER_TICK = 20;
    public static final int RUN_PER_TICK = 10;
    /** Minimum stored energy for the generator to count as active. */
    public static final int ACTIVE_THRESHOLD = RUN_PER_TICK;

    private final GeneratorEnergy energy = new GeneratorEnergy();

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Energy.BLOCK}. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Whether the generator is currently powering a breathable bubble. */
    public boolean isActive() {
        return this.energy.getAmountAsInt() >= ACTIVE_THRESHOLD;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        // Self-charge (placeholder power source), then burn a trickle while running.
        this.energy.generate(GENERATE_PER_TICK);
        if (this.energy.getAmountAsInt() >= RUN_PER_TICK) {
            this.energy.consume(RUN_PER_TICK);
        }
    }

    // --- Persistence (Value I/O) -------------------------------------------

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

    /** Internal energy buffer: receives power but does not allow extraction by others. */
    private final class GeneratorEnergy extends SimpleEnergyHandler {
        private GeneratorEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            OxygenGeneratorBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int current = getAmountAsInt();
            int next = Math.min(getCapacityAsInt(), current + amount);
            if (next != current) {
                set(next);
            }
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
