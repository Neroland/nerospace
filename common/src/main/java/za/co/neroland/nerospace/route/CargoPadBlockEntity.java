package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

import za.co.neroland.nerospace.api.route.FlightRequestResult;
import za.co.neroland.nerospace.api.route.ScheduleMode;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.machine.MachineSideConfig;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.storage.CoreTankBridge;

/**
 * The Cargo Pad block entity: a 27-slot cargo hold (the server's {@code cargoPadSlots} cap decides how many
 * are usable), a pipe-fed rocket-fuel buffer that is pumped into the docked cargo rocket, Core side
 * configuration on every face (Universal Pipe and its filters work unchanged), the pad's registry id, and
 * the schedule driver. Ownership, access, the selected route and its schedule live in the
 * {@link RouteRegistry}, not here — the store is the owner of record, so erasure cannot be undone by a
 * block reloading (see {@code docs/CARGO-ROCKETS.md} §3).
 *
 * <p>Every gameplay decision (launch, schedule, unloading) happens on the server; the menu reads synced
 * data and sends button ids.</p>
 */
public class CargoPadBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, SideConfigured {

    /** Physical cargo slots (a 3×9 grid); {@link #activeSlots()} caps how many accept items. */
    public static final int SLOTS = 27;
    /** mB per tick pumped from the pad buffer into the docked cargo rocket. */
    private static final int PUMP_RATE = 80;
    private static final int SCHEDULE_INTERVAL = 20;
    private static final int REGISTRY_CHECK_INTERVAL = 200;
    /** How long the last launch verdict stays on the GUI, in ticks. */
    private static final int VERDICT_TICKS = 100;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final FluidTank fuel = new FluidTank(NerospaceConfig.cargoFuelCapacity(), this::setChanged);
    /** The buffer exposed to pipes: rocket fuel only, so a water line cannot poison it. */
    private final NerospaceFluidStorage fuelOnly = new NerospaceFluidStorage() {
        @Override
        public Fluid getFluid() {
            return fuel.getFluid();
        }

        @Override
        public long getAmount() {
            return fuel.getAmount();
        }

        @Override
        public long getCapacity() {
            return fuel.getCapacity();
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return fluid == ModFluids.ROCKET_FUEL.get() ? fuel.fill(fluid, amount, simulate) : 0L;
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return fuel.drain(amount, simulate);
        }
    };

    /** Registry id, {@code -1} until registered. Persisted so a reload finds its record by id before falling back to position. */
    private int padId = -1;
    private int tick;
    /** Overworld game time of the last scheduled launch (transient — a restart simply waits one interval). */
    private long lastScheduledLaunch = -1L;
    /** Last launch verdict for the GUI: 0 = none, 1 = accepted, 2 + denial ordinal = denied. */
    private int verdict;
    private int verdictTicks;

    private final SideConfigComponent sideConfig = new SideConfigComponent(buildSideConfig(), this)
            .withFluid(() -> CoreTankBridge.toCore(this.fuelOnly))
            .withItems(() -> this);

    private static SideConfig buildSideConfig() {
        int[] all = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            all[i] = i;
        }
        return SideConfig.builder()
                .channel(Channel.FLUID)
                .channel(Channel.ITEM, SlotGroup.of(SlotGroup.INPUT, all), SlotGroup.of(SlotGroup.OUTPUT, all))
                .defaultPreset(SidePreset.STORAGE)
                .build();
    }

    public CargoPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_PAD.get(), pos, state);
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    // --- Registry identity -------------------------------------------------------------------

    public int padId() {
        return this.padId;
    }

    /** The pad's registry record, or {@code null} if it is not (yet) registered. */
    @Nullable
    public PadRecord record() {
        MinecraftServer server = this.level == null ? null : this.level.getServer();
        if (server == null) {
            return null;
        }
        RouteRegistry registry = RouteRegistry.get(server);
        PadRecord byId = this.padId >= 0 ? registry.pad(this.padId) : null;
        if (byId != null && byId.pos().equals(this.worldPosition) && byId.dim().equals(RouteRegistry.dimId(this.level.dimension()))) {
            return byId;
        }
        return registry.padAt(this.level.dimension(), this.worldPosition);
    }

    /** Placement: register with the placer as owner. */
    void registerOnPlace(ServerLevel level, @Nullable UUID owner, @Nullable String name) {
        PadRecord record = RouteRegistry.get(level.getServer()).registerPad(level.dimension(), this.worldPosition, owner, name);
        this.padId = record == null ? -1 : record.id();
        setChanged();
    }

    /** Load / recovery: make sure the store knows this pad; never overrides an owner (the store decides). */
    private void ensureRegistered(ServerLevel level) {
        PadRecord record = record();
        if (record == null) {
            record = RouteRegistry.get(level.getServer()).registerPad(level.dimension(), this.worldPosition, null, null);
        }
        int id = record == null ? -1 : record.id();
        if (id != this.padId) {
            this.padId = id;
            setChanged();
        }
    }

    boolean rename(ServerLevel level, Player player, String name) {
        PadRecord record = record();
        if (record == null || !record.managedBy(player.getUUID())) {
            return false;
        }
        return RouteRegistry.get(level.getServer()).renamePad(record.id(), name);
    }

    boolean claim(ServerLevel level, ServerPlayer player) {
        PadRecord record = record();
        return record != null && RouteRegistry.get(level.getServer()).claimPad(record.id(), player.getUUID());
    }

    /** Whether {@code player} may operate this pad's GUI controls (owner, or anyone while unowned). */
    public boolean managedBy(@Nullable UUID player) {
        PadRecord record = record();
        return record != null && record.managedBy(player);
    }

    // --- Cargo -------------------------------------------------------------------------------

    /** Usable slots under the server cap. */
    public int activeSlots() {
        return Math.max(1, Math.min(SLOTS, NerospaceConfig.cargoPadSlots()));
    }

    public int usedSlots() {
        int n = 0;
        for (int i = 0; i < activeSlots(); i++) {
            if (!this.items.get(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean isCargoEmpty() {
        return usedSlots() == 0;
    }

    public boolean isCargoFull() {
        return usedSlots() >= activeSlots();
    }

    /** Copies of every non-empty cargo stack (what a launch would carry). */
    public List<ItemStack> manifestCopy() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < activeSlots(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                out.add(stack.copy());
            }
        }
        return out;
    }

    /** Removes and returns every cargo stack — called once a flight has been recorded. */
    List<ItemStack> takeManifest() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < activeSlots(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                out.add(stack);
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        setChanged();
        return out;
    }

    /**
     * Inserts an arriving stack into the hold (merging first, then empty slots within the cap).
     * @return what did not fit ({@link ItemStack#EMPTY} when everything was accepted)
     */
    public ItemStack acceptCargo(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack rest = incoming.copy();
        int limit = activeSlots();
        for (int i = 0; i < limit && !rest.isEmpty(); i++) {
            ItemStack slot = this.items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                int room = Math.min(slot.getMaxStackSize(), getMaxStackSize()) - slot.getCount();
                if (room > 0) {
                    int move = Math.min(room, rest.getCount());
                    slot.grow(move);
                    rest.shrink(move);
                }
            }
        }
        for (int i = 0; i < limit && !rest.isEmpty(); i++) {
            if (this.items.get(i).isEmpty()) {
                int move = Math.min(rest.getMaxStackSize(), rest.getCount());
                this.items.set(i, rest.copyWithCount(move));
                rest.shrink(move);
            }
        }
        setChanged();
        return rest.isEmpty() ? ItemStack.EMPTY : rest;
    }

    public int comparatorSignal() {
        int used = usedSlots();
        return used == 0 ? 0 : 1 + used * 14 / activeSlots();
    }

    // --- Fuel + rocket -----------------------------------------------------------------------

    /** The pipe-facing fuel buffer (rocket fuel only). */
    public NerospaceFluidStorage getTank() {
        return this.fuelOnly;
    }

    public int fuelAmount() {
        return (int) this.fuel.getAmount();
    }

    public int fuelCapacity() {
        return (int) this.fuel.getCapacity();
    }

    /** The cargo rocket standing on this pad's cluster, or {@code null} (a crewed rocket does not count). */
    @Nullable
    public CargoRocketEntity dockedRocket() {
        if (this.level == null) {
            return null;
        }
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(this.level, this.worldPosition);
        RocketEntity rocket = LaunchPadMultiblock.rocketAbove(this.level, pads);
        return rocket instanceof CargoRocketEntity cargo ? cargo : null;
    }

    /** Whether any non-launching rocket (cargo or crewed) occupies the cluster — arrivals must wait. */
    public boolean isOccupied() {
        if (this.level == null) {
            return false;
        }
        return LaunchPadMultiblock.rocketAbove(this.level, LaunchPadMultiblock.connectedPads(this.level, this.worldPosition)) != null;
    }

    /** The pad tier the formation containing this block provides (2 = a 3×3; the cargo minimum). */
    public int padTier() {
        if (this.level == null) {
            return 0;
        }
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(this.level, this.worldPosition);
        return LaunchPadMultiblock.padTierContaining(this.level, pads, this.worldPosition);
    }

    // --- Launch verdicts (GUI feedback) -----------------------------------------------------

    void recordVerdict(FlightRequestResult result) {
        this.verdict = result.accepted() ? 1 : 2 + (result.denial() == null ? 0 : result.denial().ordinal());
        this.verdictTicks = VERDICT_TICKS;
    }

    /** 0 none, 1 accepted, 2 + {@link FlightRequestResult.Denial} ordinal when denied. */
    public int verdict() {
        return this.verdictTicks > 0 ? this.verdict : 0;
    }

    /** A player pressed Launch. */
    public void launchNow(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        recordVerdict(CargoLaunch.launchFromPad(serverLevel, this, player.getUUID()));
    }

    // --- Ticking -----------------------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        this.tick++;
        if (this.verdictTicks > 0) {
            this.verdictTicks--;
        }
        if (this.fuel.getCapacity() != NerospaceConfig.cargoFuelCapacity()) {
            this.fuel.resize(NerospaceConfig.cargoFuelCapacity()); // live config reload
        }
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);

        if (this.padId < 0 || this.tick % REGISTRY_CHECK_INTERVAL == 0) {
            ensureRegistered(serverLevel);
        }

        CargoRocketEntity rocket = null;
        if (this.fuel.getAmount() > 0) {
            rocket = dockedRocket();
            if (rocket != null) {
                int drained = (int) this.fuel.drain(PUMP_RATE, false);
                int overflow = rocket.addFuel(drained);
                if (overflow > 0) {
                    this.fuel.fill(ModFluids.ROCKET_FUEL.get(), overflow, false);
                }
            }
        }

        if (this.tick % SCHEDULE_INTERVAL == 0) {
            runSchedule(serverLevel);
        }
    }

    /** The standalone scheduler: launch-when-full and launch-every-N, on the pad's own route. */
    private void runSchedule(ServerLevel level) {
        if (this.padId < 0 || isCargoEmpty()) {
            return;
        }
        RouteRecord route = RouteRegistry.get(level.getServer()).routeForOrigin(this.padId);
        if (route == null || route.mode() == ScheduleMode.MANUAL) {
            return;
        }
        long now = level.getServer().overworld().getGameTime();
        boolean due = switch (route.mode()) {
            case WHEN_FULL -> isCargoFull();
            case EVERY_INTERVAL -> this.lastScheduledLaunch < 0 || now - this.lastScheduledLaunch >= route.intervalTicks();
            default -> false;
        };
        if (!due) {
            return;
        }
        // Scheduled launches act as the pad owner; an unowned pad has no one to launch for.
        UUID owner = route.owner().isEmpty() ? null : parseUuid(route.owner());
        if (owner == null) {
            return;
        }
        FlightRequestResult result = CargoLaunch.launchFromPad(level, this, owner);
        if (result.accepted()) {
            this.lastScheduledLaunch = now;
            recordVerdict(result);
        } else if (route.mode() == ScheduleMode.EVERY_INTERVAL) {
            this.lastScheduledLaunch = now; // wait a full interval before nagging again
        }
    }

    @Nullable
    static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // --- Removal -----------------------------------------------------------------------------

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        PadRecord record = record();
        if (record != null) {
            RouteRegistry.get(serverLevel.getServer()).unregisterPad(record.id());
        }
        this.padId = -1;
    }

    // --- Persistence -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("PadId", this.padId);
        output.putString("FuelFluid", BuiltInRegistries.FLUID.getKey(this.fuel.getRawFluid()).toString());
        output.putInt("FuelAmount", this.fuel.getRawAmount());
        ContainerHelper.saveAllItems(output, this.items);
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.padId = input.getIntOr("PadId", -1);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("FuelFluid", "minecraft:empty")));
        this.fuel.setRaw(fluid, input.getIntOr("FuelAmount", 0));
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        this.sideConfig.load(input);
    }

    // --- MenuProvider ------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.cargo_pad");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CargoPadMenu(containerId, playerInventory, this, player);
    }

    // --- WorldlyContainer --------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.sideConfig.itemSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && this.sideConfig.canInsertItem(slot, side) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return this.sideConfig.canExtractItem(slot, side);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < activeSlots();
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
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
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
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
