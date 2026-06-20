package za.co.neroland.nerospace.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Universal Pipe — relays energy AND gas between adjacent storages. Each tick it pulls from
 * neighbours that allow extraction (generators, other pipes) into its small buffers, then pushes into
 * neighbours that allow insertion (machines, tanks, batteries). Direction is enforced naturally by
 * each storage's own insert/extract limits. Uses {@link EnergyLookup} / {@link GasLookup} — the query
 * sides of the cross-loader seams. The gas buffer holds one gas type at a time (only oxygen exists yet).
 */
public class UniversalPipeBlockEntity extends BlockEntity {

    public static final int CAPACITY = 8_000;
    public static final int MAX_IO = 1_000;
    public static final int GAS_CAPACITY = 8_000;
    public static final int GAS_MAX_IO = 1_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);
    private final GasTank gas = new GasTank(GAS_CAPACITY, this::setChanged);

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public NerospaceGasStorage getGas() {
        return this.gas;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        relayEnergy(level, pos);
        relayGas(level, pos);
    }

    private void relayEnergy(Level level, BlockPos pos) {
        // Pull from extractable neighbours into the buffer.
        for (Direction dir : Direction.values()) {
            NerospaceEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            long room = this.energy.getCapacity() - this.energy.getAmount();
            if (room > 0) {
                long moved = neighbour.extract(Math.min(room, MAX_IO), false);
                if (moved > 0) {
                    this.energy.insert(moved, false);
                }
            }
        }
        // Push from the buffer into insertable neighbours.
        for (Direction dir : Direction.values()) {
            if (this.energy.getAmount() <= 0) {
                break;
            }
            NerospaceEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            long offered = this.energy.extract(Math.min(this.energy.getAmount(), MAX_IO), true);
            long accepted = neighbour.insert(offered, false);
            if (accepted > 0) {
                this.energy.extract(accepted, false);
            }
        }
    }

    private void relayGas(Level level, BlockPos pos) {
        // Pull a (single) gas type from extractable neighbours into the buffer.
        for (Direction dir : Direction.values()) {
            long room = this.gas.getCapacity() - this.gas.getAmount();
            if (room <= 0) {
                break;
            }
            NerospaceGasStorage neighbour = GasLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            GasResource ngas = neighbour.getGas();
            if (ngas.isEmpty() || (!this.gas.getGas().isEmpty() && this.gas.getGas() != ngas)) {
                continue;
            }
            long available = neighbour.drain(Math.min(room, GAS_MAX_IO), true);
            long moved = this.gas.fill(ngas, available, false);
            if (moved > 0) {
                neighbour.drain(moved, false);
            }
        }
        // Push the buffered gas into insertable neighbours.
        for (Direction dir : Direction.values()) {
            if (this.gas.getAmount() <= 0) {
                break;
            }
            NerospaceGasStorage neighbour = GasLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            GasResource g = this.gas.getGas();
            long offered = this.gas.drain(Math.min(this.gas.getAmount(), GAS_MAX_IO), true);
            long accepted = neighbour.fill(g, offered, false);
            if (accepted > 0) {
                this.gas.drain(accepted, false);
            }
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
