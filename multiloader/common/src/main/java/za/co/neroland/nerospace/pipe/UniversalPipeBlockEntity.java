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
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Universal Pipe — relays energy between adjacent storages. Each tick it pulls from neighbours that
 * allow extraction (generators, other pipes) into its small buffer, then pushes into neighbours that
 * allow insertion (machines, batteries). Direction is enforced naturally by each storage's own
 * insert/extract limits. Uses {@link EnergyLookup} — the query side of the cross-loader energy seam.
 */
public class UniversalPipeBlockEntity extends BlockEntity {

    public static final int CAPACITY = 8_000;
    public static final int MAX_IO = 1_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
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
