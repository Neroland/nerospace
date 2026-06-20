package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Oxygen Generator — a grid-powered electrolyser: each tick it spends energy from its internal buffer
 * to synthesise {@link GasResource#OXYGEN} into its gas tank. Exposes the energy capability (insert
 * only, fed by the pipe network) and the gas capability (extract only, tapped by the pipe network /
 * adjacent gas tanks). GUI-less for now; the world oxygen-field effect is a deferred atmosphere system.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity {

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int GAS_CAPACITY = 8_000;
    public static final int MAX_INSERT = 1_000;
    public static final int MB_PER_TICK = 4;
    public static final int FE_PER_MB = 20;

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, MAX_INSERT, 0, this::setChanged);
    private final GasTank gas = new GasTank(GAS_CAPACITY, this::setChanged);

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Extract-only view of the gas tank — the pipe/network may pull oxygen out but not push it back. */
    public NerospaceGasStorage getGas() {
        return new NerospaceGasStorage() {
            @Override
            public GasResource getGas() {
                return gas.getGas();
            }

            @Override
            public long getAmount() {
                return gas.getAmount();
            }

            @Override
            public long getCapacity() {
                return gas.getCapacity();
            }

            @Override
            public long fill(GasResource g, long amount, boolean simulate) {
                return 0;
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return gas.drain(amount, simulate);
            }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        long room = this.gas.getCapacity() - this.gas.getAmount();
        int produce = (int) Math.min(MB_PER_TICK, room);
        if (produce <= 0) {
            return;
        }
        int cost = produce * FE_PER_MB;
        if (this.energy.getAmount() >= cost) {
            this.energy.consume(cost);
            this.gas.fill(GasResource.OXYGEN, produce, false);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Gas", this.gas.getRawGas().getSerializedName());
        output.putInt("GasAmount", this.gas.getRawAmount());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.gas.setRaw(GasResource.byName(input.getStringOr("Gas", "empty")), input.getIntOr("GasAmount", 0));
    }
}
