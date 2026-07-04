package za.co.neroland.nerospace.pipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.item.PipeUpgradeItem;
import za.co.neroland.nerospace.menu.PipeConfigMenu;
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Universal Pipe — relays energy, gas AND items between adjacent storages. Energy/gas use the
 * cross-loader {@link EnergyLookup}/{@link GasLookup} seams; items use plain vanilla {@link Container}
 * adjacency (so it interoperates with vanilla chests/furnaces and the mod's machines on both loaders
 * with no extra seam). The pipe is itself a {@link WorldlyContainer} (small buffer), so it is exposed
 * as the item capability and chains pipe-to-pipe. Item flow is directed: pull only from non-pipe
 * containers, push to any neighbour — sources feed the line, the line feeds sinks.
 *
 * <p>Side-config alignment (Neroland Core 1.3.0): the pipe is unchanged — it still queries each
 * neighbour's capability per face through {@link EnergyLookup}/{@link GasLookup}/{@code FluidLookup}.
 * A machine face that Core's side config sets to OUTPUT now returns its (extract-only) storage, so
 * the pipe pulls from exactly those faces; a face set to DISABLED returns {@code null}, so the pipe
 * forms no connection there. The pipe's own per-face {@link PipeIoMode} ({@code AUTO/IN/OUT/OFF})
 * maps onto Core's {@link za.co.neroland.nerolandcore.sideconfig.SideMode} semantics
 * (AUTO≈IO, IN≈INPUT, OUT≈OUTPUT, OFF≈DISABLED) but stays the pipe's own vocabulary — Core's
 * SideConfig model is not duplicated here; the machine side decides what each machine face exposes.
 */
public class UniversalPipeBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int CAPACITY = 8_000;
    public static final int MAX_IO = 1_000;
    public static final int GAS_CAPACITY = 8_000;
    public static final int GAS_MAX_IO = 1_000;
    public static final int FLUID_CAPACITY = 8_000;
    public static final int FLUID_MAX_IO = 1_000;
    public static final int ITEM_SLOTS = 3;

    private static final int[] ALL_SLOTS = {0, 1, 2};

    /** Max installed upgrades of each kind per segment. */
    public static final int MAX_UPGRADES = 3;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);
    private final GasTank gas = new GasTank(GAS_CAPACITY, this::setChanged);
    private final FluidTank fluid = new FluidTank(FLUID_CAPACITY, this::setChanged);
    private final NonNullList<ItemStack> items = NonNullList.withSize(ITEM_SLOTS, ItemStack.EMPTY);

    /** The shared {@link PipeNetwork} this segment belongs to (lazily built; rebuilt on topology change). */
    @Nullable
    private PipeNetwork network;

    /** Per-face (6) × per-resource-type (4) I/O mode, set with the Configurator. Default AUTO. */
    private final PipeIoMode[][] faceModes = new PipeIoMode[6][PipeResourceType.VALUES.length];
    /**
     * Per-face item-layer filters as PHYSICAL contained items, indexed by
     * {@code Direction.get3DDataValue()}: each slot holds one Pipe Filter or Advanced Pipe Filter
     * item (issue #25 follow-up). The face's effective {@link FaceFilter} is derived from the
     * contained item in {@link #filter}, so the Configurator GUI shows exactly what each face is
     * doing and popping the item out clears the face. Installed like upgrades (right-click
     * consumes; removed via the GUI slots or block break).
     */
    private final SimpleContainer filterItems = new FilterContainer();

    /** 6-slot filter containment whose mutations mark the block entity dirty. */
    private final class FilterContainer extends SimpleContainer {
        FilterContainer() {
            super(6);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            UniversalPipeBlockEntity.this.setChanged();
        }
    }
    private int speedUpgrades;
    private int capacityUpgrades;

    // --- Travelling-item visuals (cosmetic echo of the item relay) -----------
    /** Max simultaneously-animated in-transit stacks per segment (visual cap, not a throughput limit). */
    public static final int MAX_TRAVELLING = 6;
    /** Base ticks a visual packet takes to cross one pipe (scaled down by Speed upgrades). */
    private static final int ITEM_TICKS_PER_BLOCK = 8;
    /** How often (server ticks) a pipe pushes a travelling-item snapshot to nearby clients (snappy motion). */
    private static final int SYNC_INTERVAL = 3;
    /** How often (server ticks) a pipe syncs its buffered energy/fluid/gas presence (for the slower stream pulses). */
    private static final int CONTENT_SYNC_INTERVAL = 10;
    /** How often (server ticks) a pipe re-derives its 6 connection blockstate properties from neighbours.
     *  Kept low (2 ticks ≈ 100 ms) so a freshly-placed neighbour links up near-instantly — the throttle only
     *  exists to avoid re-deriving every tick; refreshConnections() no-ops unless a connection actually changed. */
    private static final int CONNECTION_REFRESH_INTERVAL = 2;
    /** In-transit visual packets (a cosmetic echo of the relay; advanced + expired each tick, synced + persisted). */
    private final List<TravellingItem> travelling = new ArrayList<>();
    /** Whether the last client sync carried any visual (items or buffered content), to send one final clear. */
    private boolean lastSyncHadVisual;
    /** Client-only: game time of the last render extraction, for smooth local advance between syncs. */
    public float clientItemTime;

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < PipeResourceType.VALUES.length; t++) {
                this.faceModes[f][t] = PipeIoMode.AUTO;
            }
        }
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public NerospaceGasStorage getGas() {
        return this.gas;
    }

    public NerospaceFluidStorage getFluidTank() {
        return this.fluid;
    }

    // --- Network accessors (concrete buffers, used by PipeNetwork balancing) --
    EnergyBuffer energy() {
        return this.energy;
    }

    FluidTank fluid() {
        return this.fluid;
    }

    GasTank gas() {
        return this.gas;
    }

    /** Adopt the shared network this segment now belongs to. */
    void adopt(PipeNetwork net) {
        this.network = net;
    }

    // --- Per-face I/O modes (Configurator) -----------------------------------

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

    // --- Config GUI (Configurator sneak-use) ---------------------------------

    /** Transient UI state: which resource layer the open config menu edits (not saved). */
    private int configType = 0;

    /** Synced to the config menu: [0]=configType, [1..6]=each face's mode ordinal for that layer. */
    private final ContainerData configData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return configType;
            }
            Direction dir = Direction.from3DDataValue(index - 1);
            return faceModes[dir.get3DDataValue()][configType].ordinal();
        }

        @Override
        public void set(int index, int value) {
            // read-only from the client
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public int configType() {
        return this.configType;
    }

    /** Cycle which resource layer the config menu edits (menu button). */
    public void cycleConfigType() {
        this.configType = Math.floorMod(this.configType + 1, PipeResourceType.VALUES.length);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerospace.universal_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PipeConfigMenu(containerId, inventory, this, this.configData);
    }

    /** Drop the contained filter items when the pipe is broken (they are physical items). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel) {
            for (int f = 0; f < 6; f++) {
                ItemStack filter = this.filterItems.getItem(f);
                if (!filter.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5, filter);
                    this.filterItems.setItem(f, ItemStack.EMPTY);
                }
            }
        }
    }

    // --- Per-face item filter (contained Pipe Filter / Advanced Pipe Filter items) ---

    /** The 6-slot filter containment (one face per slot by {@code Direction.get3DDataValue()});
     *  backs the Configurator GUI's per-face filter slots. */
    public SimpleContainer filterItems() {
        return this.filterItems;
    }

    /** The filter item installed on a face (EMPTY = unfiltered). */
    public ItemStack filterItem(Direction dir) {
        return this.filterItems.getItem(dir.get3DDataValue());
    }

    /** The face's effective filter, derived from the contained filter item. */
    public FaceFilter filter(Direction dir) {
        return FaceFilter.fromFilterItem(filterItem(dir));
    }

    /**
     * Install a filter item on a face (like upgrades, the physical item lives in the pipe).
     * @return the previously installed item (EMPTY if none) — hand it back to the player.
     */
    public ItemStack installFilter(Direction dir, ItemStack filterItem) {
        ItemStack previous = filterItem(dir);
        this.filterItems.setItem(dir.get3DDataValue(), filterItem.copyWithCount(1));
        return previous;
    }

    // --- Upgrades (Speed / Capacity) -----------------------------------------

    public boolean installUpgrade(PipeUpgradeItem.Kind kind) {
        if (upgradeCount(kind) >= MAX_UPGRADES) {
            return false;
        }
        if (kind == PipeUpgradeItem.Kind.SPEED) {
            this.speedUpgrades++;
        } else {
            this.capacityUpgrades++;
            syncTankCapacities();
        }
        setChanged();
        return true;
    }

    /** Re-size the fluid + gas tanks to the base capacity scaled by the installed Capacity upgrades. */
    private void syncTankCapacities() {
        int mult = capacityMultiplier();
        this.fluid.resize((long) FLUID_CAPACITY * mult);
        this.gas.resize((long) GAS_CAPACITY * mult);
    }

    public int upgradeCount(PipeUpgradeItem.Kind kind) {
        return kind == PipeUpgradeItem.Kind.SPEED ? this.speedUpgrades : this.capacityUpgrades;
    }

    /** Pops all installed upgrades back out (sneak-right-click with an empty hand). @return count. */
    public int uninstallUpgrades() {
        int total = this.speedUpgrades + this.capacityUpgrades;
        if (total > 0 && this.level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            if (this.speedUpgrades > 0) {
                Containers.dropItemStack(serverLevel, x, y, z,
                        new ItemStack(ModItems.SPEED_UPGRADE.get(), this.speedUpgrades));
            }
            if (this.capacityUpgrades > 0) {
                Containers.dropItemStack(serverLevel, x, y, z,
                        new ItemStack(ModItems.CAPACITY_UPGRADE.get(), this.capacityUpgrades));
            }
            this.speedUpgrades = 0;
            this.capacityUpgrades = 0;
            syncTankCapacities();
            setChanged();
        }
        return total;
    }

    /** Throughput multiplier for the energy/gas layers (and item move rate). */
    public int speedMultiplier() {
        return 1 + this.speedUpgrades;
    }

    /** Buffer multiplier — reserved for the fluid/gas tanks + in-transit cap in the graph slice. */
    public int capacityMultiplier() {
        return 1 + this.capacityUpgrades;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        // Move + balance resources across the whole connected network (one shared pool → unlimited range),
        // instead of the old per-pipe relay that only reached a single neighbour and dropped items.
        if (level instanceof ServerLevel serverLevel) {
            PipeNetwork net = this.network;
            if (net == null || !net.isValid()) {
                net = PipeNetwork.getOrBuild(serverLevel, pos);
                this.network = net;
            }
            net.tick(serverLevel);
        }
        tickTravelling();
        maybeSyncClient(level, pos, state);
        refreshConnections(level, pos, state);
    }

    /** Re-derive the 6 connection blockstate properties from neighbours (throttled) so the tube model +
     *  voxel shape track the world without the version-fragile neighbour-event overrides. */
    private void refreshConnections(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % CONNECTION_REFRESH_INTERVAL != 0) {
            return;
        }
        BlockState connected = UniversalPipeBlock.withConnections(state, level, pos);
        if (connected != state) {
            level.setBlock(pos, connected, Block.UPDATE_ALL);
        }
    }

    /** Advance + expire the cosmetic in-transit packets (the visuals; the relay already moved the items). */
    private void tickTravelling() {
        if (this.travelling.isEmpty()) {
            return;
        }
        float step = 1.0F / itemTicksPerBlock();
        this.travelling.removeIf(item -> {
            item.advance(step);
            return item.isFinished();
        });
    }

    /**
     * Push a throttled block-entity update so the renderer can draw the travelling items and the
     * energy/fluid/gas stream pulses: a snappy cadence while items are in flight, a slower cadence while
     * the pipe merely holds content, plus one final snapshot so the client clears a now-idle segment.
     */
    private void maybeSyncClient(Level level, BlockPos pos, BlockState state) {
        boolean items = !this.travelling.isEmpty();
        boolean content = this.energy.getAmount() > 0 || this.gas.getAmount() > 0 || this.fluid.getAmount() > 0
                || hasBufferedItems();
        long now = level.getGameTime();
        boolean send = (items && now % SYNC_INTERVAL == 0)
                || (content && now % CONTENT_SYNC_INTERVAL == 0)
                || (this.lastSyncHadVisual && !items && !content && now % SYNC_INTERVAL == 0);
        if (send) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            this.lastSyncHadVisual = items || content;
        }
    }

    /** Visual ticks for one packet to cross a segment — faster with Speed upgrades (min 3). */
    public int itemTicksPerBlock() {
        return Math.max(3, ITEM_TICKS_PER_BLOCK / speedMultiplier());
    }

    /** Read-only view of the in-transit packets, for the renderer. */
    public List<TravellingItem> travelling() {
        return this.travelling;
    }

    public boolean hasBufferedItems() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Network-level visual packet for an item routed through this segment. */
    void showTravelling(ItemStack moved, Direction inFace, @Nullable Direction outFace) {
        if (moved.isEmpty() || this.travelling.size() >= MAX_TRAVELLING) {
            return;
        }
        this.travelling.add(new TravellingItem(moved.copy(), inFace, outFace, 0.0F));
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Gas", this.gas.getRawGas().getSerializedName());
        output.putInt("GasAmount", this.gas.getRawAmount());
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid.getRawFluid()).toString());
        output.putInt("FluidAmount", this.fluid.getRawAmount());
        for (int i = 0; i < ITEM_SLOTS; i++) {
            if (!this.items.get(i).isEmpty()) {
                output.store("Item" + i, ItemStack.OPTIONAL_CODEC, this.items.get(i));
            }
        }
        // Per-face × per-type I/O modes packed two bits each (6 faces × 4 types = 48 bits).
        long packed = 0L;
        int types = PipeResourceType.VALUES.length;
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < types; t++) {
                packed |= ((long) (this.faceModes[f][t].ordinal() & 0x3)) << ((f * types + t) * 2);
            }
        }
        output.putLong("Faces", packed);
        for (int f = 0; f < 6; f++) {
            if (!this.filterItems.getItem(f).isEmpty()) {
                output.store("FilterItem" + f, ItemStack.OPTIONAL_CODEC, this.filterItems.getItem(f));
            }
        }
        output.putInt("SpeedUpgrades", this.speedUpgrades);
        output.putInt("CapacityUpgrades", this.capacityUpgrades);
        if (!this.travelling.isEmpty()) {
            output.store("Travelling", TravellingItem.CODEC.listOf(), this.travelling);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Restore the upgrade counts + tank capacities first, so loading the stored fluid/gas amounts
        // below isn't clamped to the base capacity on an upgraded pipe.
        this.speedUpgrades = input.getIntOr("SpeedUpgrades", 0);
        this.capacityUpgrades = input.getIntOr("CapacityUpgrades", 0);
        syncTankCapacities();
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.gas.setRaw(GasResource.byName(input.getStringOr("Gas", "empty")), input.getIntOr("GasAmount", 0));
        Fluid storedFluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.fluid.setRaw(storedFluid, input.getIntOr("FluidAmount", 0));
        for (int i = 0; i < ITEM_SLOTS; i++) {
            this.items.set(i, input.read("Item" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        long packed = input.getLongOr("Faces", 0L);
        int types = PipeResourceType.VALUES.length;
        for (int f = 0; f < 6; f++) {
            for (int t = 0; t < types; t++) {
                this.faceModes[f][t] = PipeIoMode.VALUES[(int) ((packed >> ((f * types + t) * 2)) & 0x3L)];
            }
        }
        for (int f = 0; f < 6; f++) {
            // Containment format first ("FilterItem" = the physical filter item). Migration chain:
            // "FaceFilter" (the brief config-only #25 format) and legacy "Filter" (pre-#25 single
            // ItemStack) both synthesize an equivalent contained filter item — same behaviour, and
            // the player can now pop the item back out of the GUI.
            ItemStack stored = input.read("FilterItem" + f, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            if (stored.isEmpty()) {
                FaceFilter migrated = input.read("FaceFilter" + f, FaceFilter.CODEC).orElse(null);
                if (migrated == null) {
                    migrated = FaceFilter.ofItem(
                            input.read("Filter" + f, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
                }
                stored = migrated.toFilterItem();
            }
            this.filterItems.setItem(f, stored);
        }
        this.speedUpgrades = input.getIntOr("SpeedUpgrades", 0);
        this.capacityUpgrades = input.getIntOr("CapacityUpgrades", 0);
        this.travelling.clear();
        this.travelling.addAll(input.read("Travelling", TravellingItem.CODEC.listOf()).orElse(List.of()));
    }

    // --- Client sync (travelling-item visuals ride the block-entity update packet) ------------

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // --- WorldlyContainer (item buffer) -------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return ITEM_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        if (!split.isEmpty()) {
            this.setChanged();
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
