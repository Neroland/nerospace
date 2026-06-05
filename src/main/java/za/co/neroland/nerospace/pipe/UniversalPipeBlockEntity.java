package za.co.neroland.nerospace.pipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasStacksResourceHandler;
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

    /** Per-face (6), per-resource-type (4) I/O mode. Default AUTO everywhere. */
    private final PipeIoMode[][] faceModes = new PipeIoMode[6][PipeResourceType.VALUES.length];
    /** Soft cap on simultaneous travelling stacks per segment (extraction pauses above it). */
    public static final int MAX_TRAVELLING_ITEMS = 16;
    /** Max installed upgrades of each kind per segment. */
    public static final int MAX_UPGRADES = 3;

    private final PipeEnergy energy = new PipeEnergy();
    private final PipeFluid fluid = new PipeFluid();
    private final PipeGas gas = new PipeGas();
    private final List<TravellingItem> items = new ArrayList<>();

    /** Per-face item-layer filter (EMPTY = unfiltered), indexed by {@code Direction.get3DDataValue()}. */
    private final ItemResource[] faceFilters = new ItemResource[6];
    private int speedUpgrades;
    private int capacityUpgrades;

    /** Transient: the network this pipe belongs to, lazily (re)built and shared with all members. */
    @Nullable
    private PipeNetwork network;

    /** CLIENT-only: animation clock for smooth travelling-item motion (set by the renderer). */
    public float clientItemTime;

    /** Server: last synced content fingerprint (drives the renderer's stream layers). */
    private int lastContentSync = -1;

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < PipeResourceType.VALUES.length; t++) {
                this.faceModes[f][t] = PipeIoMode.AUTO;
            }
            this.faceFilters[f] = ItemResource.EMPTY;
        }
    }

    // --- Filters + upgrades ----------------------------------------------------

    public ItemResource filter(Direction dir) {
        return this.faceFilters[dir.get3DDataValue()];
    }

    public void setFilter(Direction dir, ItemResource filter) {
        this.faceFilters[dir.get3DDataValue()] = filter;
        setChanged();
    }

    public boolean installUpgrade(za.co.neroland.nerospace.item.PipeUpgradeItem.Kind kind) {
        if (upgradeCount(kind) >= MAX_UPGRADES) {
            return false;
        }
        if (kind == za.co.neroland.nerospace.item.PipeUpgradeItem.Kind.SPEED) {
            this.speedUpgrades++;
        } else {
            this.capacityUpgrades++;
        }
        setChanged();
        return true;
    }

    public int upgradeCount(za.co.neroland.nerospace.item.PipeUpgradeItem.Kind kind) {
        return kind == za.co.neroland.nerospace.item.PipeUpgradeItem.Kind.SPEED
                ? this.speedUpgrades : this.capacityUpgrades;
    }

    /** Pops all installed upgrades back out (sneak-right-click with an empty hand). @return count. */
    public int uninstallUpgrades() {
        int total = this.speedUpgrades + this.capacityUpgrades;
        if (total > 0 && this.level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            if (this.speedUpgrades > 0) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(
                                za.co.neroland.nerospace.registry.ModItems.SPEED_UPGRADE.get(), this.speedUpgrades));
            }
            if (this.capacityUpgrades > 0) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(
                                za.co.neroland.nerospace.registry.ModItems.CAPACITY_UPGRADE.get(), this.capacityUpgrades));
            }
            this.speedUpgrades = 0;
            this.capacityUpgrades = 0;
            setChanged();
        }
        return total;
    }

    /** Throughput multiplier for the energy/fluid/gas layers (and item speed). */
    public int speedMultiplier() {
        return 1 + this.speedUpgrades;
    }

    /** Buffer multiplier for the fluid/gas tanks and the in-transit item cap. */
    public int capacityMultiplier() {
        return 1 + this.capacityUpgrades;
    }

    /** Ticks an item needs to cross this segment (speed upgrades shorten it). */
    public int itemTicksPerBlock() {
        return Math.max(1, Tuning.itemPipeTicksPerBlock() / speedMultiplier());
    }

    /** Exposed to {@code Capabilities.Energy.BLOCK} so generators, machines and other cables interop. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    PipeEnergy energy() {
        return this.energy;
    }

    /** Exposed to {@code Capabilities.Fluid.BLOCK} so tanks, machines and other mods' pipes interop. */
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluid;
    }

    PipeFluid fluid() {
        return this.fluid;
    }

    /** Exposed via the mod's dedicated {@code GasCapability.BLOCK}. */
    public ResourceHandler<GasResource> getGasHandler() {
        return this.gas;
    }

    PipeGas gas() {
        return this.gas;
    }

    /** The item stacks currently travelling through this segment (mutated by the network). */
    public List<TravellingItem> items() {
        return this.items;
    }

    /** Mark the travelling items dirty + push a sync packet so clients can render them. */
    void syncItems() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public PipeIoMode mode(Direction dir, PipeResourceType type) {
        return this.faceModes[dir.get3DDataValue()][type.ordinal()];
    }

    /** Cycle a face's mode for one resource type (Configurator). @return the new mode. */
    public PipeIoMode cycleMode(Direction dir, PipeResourceType type) {
        PipeIoMode next = mode(dir, type).next();
        this.faceModes[dir.get3DDataValue()][type.ordinal()] = next;
        setChanged();
        return next;
    }

    /** Directly set a face's mode for one resource type (Configurator GUI / commands). */
    public void setMode(Direction dir, PipeResourceType type, PipeIoMode mode) {
        this.faceModes[dir.get3DDataValue()][type.ordinal()] = mode;
        setChanged();
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
        PipeNetwork net = this.network; // local so the null/valid check holds for the analyzer
        if (net == null || !net.isValid()) {
            net = PipeNetwork.getOrBuild(serverLevel, pos);
            this.network = net;
        }
        net.tick(serverLevel);

        // Sync content PRESENCE changes to clients (the renderer's streams key off them). Amounts
        // change every tick from balancing, so only the cheap fingerprint triggers a packet.
        if ((serverLevel.getGameTime() & 7L) == 0L) {
            int fingerprint = (this.energy.getAmountAsInt() > 0 ? 1 : 0)
                    | (this.fluid.amount() > 0 ? 2 : 0)
                    | (this.gas.amount() > 0 ? 4 : 0);
            if (fingerprint != this.lastContentSync) {
                this.lastContentSync = fingerprint;
                serverLevel.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        long packed = 0L;
        int types = PipeResourceType.VALUES.length;
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < types; t++) {
                packed |= ((long) (this.faceModes[f][t].ordinal() & 0x3)) << ((f * types + t) * 2);
            }
        }
        output.putLong("Faces", packed);
        this.energy.serialize(output.child("Energy"));
        this.fluid.serialize(output.child("Fluid"));
        this.gas.serialize(output.child("Gas"));
        ValueOutput.TypedOutputList<TravellingItem> itemList = output.list("Items", TravellingItem.CODEC);
        for (TravellingItem item : this.items) {
            itemList.add(item);
        }
        ValueOutput.TypedOutputList<ItemResource> filterList = output.list("Filters", ItemResource.OPTIONAL_CODEC);
        for (ItemResource filter : this.faceFilters) {
            filterList.add(filter);
        }
        output.putInt("SpeedUpgrades", this.speedUpgrades);
        output.putInt("CapacityUpgrades", this.capacityUpgrades);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        long packed = input.getLongOr("Faces", 0L);
        int types = PipeResourceType.VALUES.length;
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < types; t++) {
                this.faceModes[f][t] = PipeIoMode.VALUES[(int) ((packed >> ((f * types + t) * 2)) & 0x3L)];
            }
        }
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.fluid.deserialize(input.childOrEmpty("Fluid"));
        this.gas.deserialize(input.childOrEmpty("Gas"));
        this.items.clear();
        for (TravellingItem item : input.listOrEmpty("Items", TravellingItem.CODEC)) {
            this.items.add(item);
        }
        int f = 0;
        for (ItemResource filter : input.listOrEmpty("Filters", ItemResource.OPTIONAL_CODEC)) {
            if (f < 6) {
                this.faceFilters[f++] = filter;
            }
        }
        this.speedUpgrades = input.getIntOr("SpeedUpgrades", 0);
        this.capacityUpgrades = input.getIntOr("CapacityUpgrades", 0);
    }

    // --- Client sync (travelling items are rendered) --------------------------

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /**
     * Breaking a pipe vents its gas with a visible puff (the gas is lost — it IS gas) and drops any
     * items that were travelling through the segment.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel) {
            if (this.gas.amount() > 0) {
                int puffs = Math.min(12, 2 + this.gas.amount() / 500);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        puffs, 0.2, 0.2, 0.2, 0.02);
            }
            for (TravellingItem item : this.items) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        item.resource().toStack(item.amount()));
            }
            this.items.clear();
        }
    }

    /** The pipe's FE buffer; capacity/throughput from config. {@link #setStored} is used for balancing. */
    final class PipeEnergy extends SimpleEnergyHandler {
        PipeEnergy() {
            super(Tuning.energyPipeCapacity(),
                    Tuning.energyPipeThroughput(), Tuning.energyPipeThroughput());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            UniversalPipeBlockEntity.this.setChanged();
        }

        void setStored(int value) {
            set(Math.max(0, Math.min(getCapacityAsInt(), value)));
        }
    }

    /**
     * The pipe's fluid buffer (one stack slot, capacity from config). The network enforces "one fluid
     * per network" and balances the contents across segments; {@link #setContents} is the balancing hook.
     */
    final class PipeFluid extends FluidStacksResourceHandler {
        PipeFluid() {
            super(1, Tuning.fluidPipeCapacity());
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return this.capacity * capacityMultiplier();
        }

        @Override
        protected void onContentsChanged(int index, FluidStack oldStack) {
            UniversalPipeBlockEntity.this.setChanged();
        }

        FluidResource resource() {
            return getResource(0);
        }

        int amount() {
            return getAmountAsInt(0);
        }

        /** Directly set the buffer (network balancing); zero amount clears to empty. */
        void setContents(FluidResource resource, int amount) {
            int clamped = Math.max(0, Math.min(getCapacity(0, resource), amount));
            set(0, clamped == 0 ? FluidResource.EMPTY : resource, clamped);
        }
    }

    /**
     * The pipe's gas buffer (one slot, capacity from config). One gas per network; balanced like the
     * fluid layer. Lost (vented) when the pipe is broken.
     */
    final class PipeGas extends GasStacksResourceHandler {
        PipeGas() {
            super(1, Tuning.gasPipeCapacity());
        }

        @Override
        protected int getCapacity(int index, GasResource resource) {
            return super.getCapacity(index, resource) * capacityMultiplier();
        }

        @Override
        protected void onContentsChanged(int index, net.neoforged.neoforge.transfer.resource.ResourceStack<GasResource> oldStack) {
            UniversalPipeBlockEntity.this.setChanged();
        }

        GasResource resource() {
            return getResource(0);
        }

        int amount() {
            return getAmountAsInt(0);
        }

        /** Directly set the buffer (network balancing); zero amount clears to empty. */
        void setContents(GasResource resource, int amount) {
            int clamped = Math.max(0, Math.min(getCapacity(0, resource), amount));
            set(0, clamped == 0 ? GasResource.EMPTY : resource, clamped);
        }
    }
}
