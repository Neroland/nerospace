package za.co.neroland.nerospace.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Block entity for the {@link UniversalPipeBlock}. One physical pipe that participates in a
 * {@link PipeNetwork}: it auto-connects to adjacent pipes (forming one network), and on each non-pipe
 * face it can pull from / push to a neighbouring block's capability according to a per-face
 * {@link PipeConnectionMode} (set with the Configurator).
 *
 * <p>Currently carries the <b>energy</b> layer (native FE): each pipe holds a small FE buffer; the
 * network balances those buffers across all segments (a shared pool) and moves energy from providers
 * to receivers. Item / fluid / gas layers will ride the same connection graph in later slices.</p>
 */
public class UniversalPipeBlockEntity extends BlockEntity {

    private final PipeConnectionMode[] faceModes = new PipeConnectionMode[6];
    private final PipeEnergy energy = new PipeEnergy();

    /** Transient: the network this pipe belongs to, lazily (re)built and shared with all members. */
    @Nullable
    private PipeNetwork network;

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
        for (int i = 0; i < 6; i++) {
            this.faceModes[i] = PipeConnectionMode.AUTO;
        }
    }

    /** Exposed to {@code Capabilities.Energy.BLOCK} so generators, machines and other cables interop. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    PipeEnergy energy() {
        return this.energy;
    }

    public PipeConnectionMode faceMode(Direction dir) {
        return this.faceModes[dir.get3DDataValue()];
    }

    /** Cycle a face's mode (Configurator). @return the new mode. */
    public PipeConnectionMode cycleFaceMode(Direction dir) {
        PipeConnectionMode next = faceMode(dir).next();
        this.faceModes[dir.get3DDataValue()] = next;
        setChanged();
        return next;
    }

    public void invalidateNetwork() {
        this.network = null;
    }

    /** Called by {@link PipeNetwork#getOrBuild} so every member shares the one network instance. */
    void adopt(PipeNetwork net) {
        this.network = net;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.network == null || !this.network.isValid()) {
            this.network = PipeNetwork.getOrBuild(serverLevel, pos);
        }
        this.network.tick(serverLevel);
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        int packed = 0;
        for (int i = 0; i < 6; i++) {
            packed |= (this.faceModes[i].ordinal() & 0x3) << (i * 2);
        }
        output.putInt("Faces", packed);
        this.energy.serialize(output.child("Energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int packed = input.getIntOr("Faces", 0);
        PipeConnectionMode[] modes = PipeConnectionMode.values();
        for (int i = 0; i < 6; i++) {
            this.faceModes[i] = modes[(packed >> (i * 2)) & 0x3];
        }
        this.energy.deserialize(input.childOrEmpty("Energy"));
    }

    /** The pipe's FE buffer; capacity/throughput from config. {@link #setStored} is used for balancing. */
    final class PipeEnergy extends SimpleEnergyHandler {
        PipeEnergy() {
            super(Config.ENERGY_PIPE_CAPACITY.get(),
                    Config.ENERGY_PIPE_THROUGHPUT.get(), Config.ENERGY_PIPE_THROUGHPUT.get());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            UniversalPipeBlockEntity.this.setChanged();
        }

        void setStored(int value) {
            set(Math.max(0, Math.min(getCapacityAsInt(), value)));
        }
    }
}
