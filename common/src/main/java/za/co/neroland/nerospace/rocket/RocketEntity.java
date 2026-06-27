package za.co.neroland.nerospace.rocket;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.world.gravity.GravityManager;

/**
 * The Nerospace rocket: a rideable vehicle entity placed on a {@link RocketLaunchPadBlock}. It carries
 * a {@link RocketTier} and a liquid-fuel tank (millibuckets). Right-clicking with a fuel bucket/canister
 * tops up the tank; right-clicking empty-handed mounts the rocket and opens its UI, whose Launch button
 * starts a simulated ascent that ends by transporting the rider to the tier's planet.
 *
 * <p>All gameplay logic is server-authoritative; the client only renders synced state and ascent
 * particles. The entity has no gravity and rests where placed, moving only under launch thrust.</p>
 *
 * <p><b>Cross-loader port note.</b> The root binds the fuel store to the NeoForge transfer API and an
 * item-capability intake proxy for pipe/hopper automation, and supports the multi-station founding
 * system (Station Charter → StationRegistry slots, advancement criterion). The multiloader rebuilds the
 * fuel store on the cross-loader {@link FluidTank} and a plain {@link SimpleContainer} intake (manual +
 * UI fuelling still work; automated pipe-into-rocket feeding waits on the entity item-capability seam).
 * The multi-station founding is deferred to its own batch (it needs the data-attachment + criteria
 * seams); the Orbital Station destination here docks the rider at the shared origin platform.</p>
 */
public class RocketEntity extends Entity implements MenuProvider {

    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHING =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    /** Index into {@link Destinations#all()} (Home, Station, planets). */
    private static final EntityDataAccessor<Integer> DATA_DEST =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Selected founded-station slot for the Orbital Station destination ({@code -1} = the origin platform). */
    private static final EntityDataAccessor<Integer> DATA_STATION =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Selected pad index into {@link PadRegistry#inDimension} of the destination dim ({@code -1} = nearest/auto). */
    private static final EntityDataAccessor<Integer> DATA_PAD =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Onboard oxygen (life-support) store, in millibuckets — the server-authoritative value, synced for the UI. */
    private static final EntityDataAccessor<Integer> DATA_OXYGEN =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Onboard power buffer, in FE — loaded by a Launch Controller, synced for the UI. */
    private static final EntityDataAccessor<Integer> DATA_POWER =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    /** Ticks of ascent before the rider is transported. */
    public static final int LAUNCH_DURATION = 100;
    /** One fuel canister is worth this many millibuckets. */
    public static final int CANISTER_MB = 1_000;
    /** Y level of the shared Orbital Station origin platform (the founded-station system is deferred). */
    public static final int PLATFORM_Y = 64;

    private int launchTicks;

    /** Where this rocket lifted off from (transient — used when it completes its launch this session). */
    @Nullable
    private net.minecraft.resources.ResourceKey<Level> launchOriginDim;
    @Nullable
    private BlockPos launchOriginPad;

    /** The pad this rocket should fly back to (set on a return rocket) — its launch origin. Persisted. */
    private String returnDim = "";
    @Nullable
    private BlockPos returnPos;

    /**
     * The authoritative fuel store, synced to the client via {@link #DATA_FUEL} for the GUI. Physical
     * capacity is the largest tier's; {@link #addFuel} caps top-ups to the current tier's capacity.
     */
    @SuppressWarnings("this-escape") // change-callback wiring, used only after construction
    private final FluidTank fuelTank = new FluidTank(maxTierFuelCapacity(), this::syncFuel);

    /** The largest tier's tank — the physical store capacity (per-tier caps live in addFuel). */
    private static int maxTierFuelCapacity() {
        int max = 0;
        for (RocketTier tier : RocketTier.values()) {
            max = Math.max(max, tier.fuelCapacity());
        }
        return max;
    }

    /**
     * Single-slot fuel intake: the player (or the rocket UI) drops a {@link ModItems#ROCKET_FUEL_BUCKET}
     * or {@link ModItems#ROCKET_FUEL_CANISTER} here and the rocket drains it into {@link #fuelTank} on
     * its server tick — returning an empty bucket.
     */
    private final SimpleContainer fuelInput = new SimpleContainer(1);

    /**
     * Synced to the menu: [0]=fuel, [1]=capacity, [2]=tierOrdinal, [3]=launchable, [4]=destinationIndex,
     * [5]=stationSlot (−1 = origin), [6]=destinationMask, [7]=oxygen, [8]=oxygenCapacity, [9]=padValid.
     * All values stay &lt; 32767 so they survive the 16-bit ContainerData sync.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getFuel();
                case 1 -> getTier().fuelCapacity();
                case 2 -> getTier().ordinal();
                case 3 -> isLaunchReady() ? 1 : 0;
                case 4 -> getDestinationIndex();
                case 5 -> getStationSlot();
                case 6 -> destinationMask();
                case 7 -> getOxygen();
                case 8 -> getTier().oxygenCapacity();
                case 9 -> isOnValidPad() ? 1 : 0;
                case 10 -> getPower();
                case 11 -> getTier().powerCapacity();
                case 12 -> getPadIndex();
                case 13 -> Math.min(32000, currentFuelCost());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 14;
        }
    };

    /** Client-side position interpolation so the ascent (and the seated rider) moves smoothly. */
    private final net.minecraft.world.entity.InterpolationHandler interpolation =
            new net.minecraft.world.entity.InterpolationHandler(this);

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    // --- Per-tier presentation ----------------------------------------------

    /** Render scale by tier — bigger boosters genuinely LOOK bigger (T1 keeps the old size). */
    public float visualScale() {
        return switch (getTier()) {
            case TIER_1 -> 1.6F;
            case TIER_2 -> 2.0F;
            case TIER_3 -> 2.4F;
            case TIER_4 -> 2.8F;
        };
    }

    // Note: the root overrides NeoForge's shouldRiderSit() (a loader extension, not vanilla) to make
    // the rider STAND at the console; that hook has no cross-loader equivalent, so it is omitted here.

    /** Stand the rider so their eyes line up with the hull's window band (scaled by tier). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, Math.max(0.4D, 2.1875D * visualScale() - 1.62D), 0.0D);
    }

    /** Convenience constructor for spawning from the rocket item. */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public RocketEntity(Level level, double x, double y, double z, RocketTier tier) {
        this(ModEntities.ROCKET.get(), level);
        this.setPos(x, y, z);
        this.setTier(tier);
    }

    // --- Synced data --------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_TIER, RocketTier.TIER_1.ordinal());
        builder.define(DATA_LAUNCHING, false);
        builder.define(DATA_DEST, 0);
        builder.define(DATA_STATION, -1);
        builder.define(DATA_PAD, -1);
        builder.define(DATA_OXYGEN, 0);
        builder.define(DATA_POWER, 0);
    }

    public int getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    /** Pushes the server-side tank amount into the synced data accessor (client GUI + canLaunch). */
    private void syncFuel() {
        if (!level().isClientSide()) {
            this.entityData.set(DATA_FUEL, (int) this.fuelTank.getAmount());
        }
    }

    /** The UI fuel-intake slot (one bucket/canister), for the menu. */
    public net.minecraft.world.Container getFuelInput() {
        return this.fuelInput;
    }

    /** Whether {@code stack} is accepted by the fuel-intake slot. */
    public static boolean isFuelContainer(ItemStack stack) {
        return stack.is(ModItems.ROCKET_FUEL_BUCKET.get()) || stack.is(ModItems.ROCKET_FUEL_CANISTER.get());
    }

    /** Current fuel as a 0–100 percentage of the tier capacity (for the UI readout). */
    public int getFuelPercent() {
        int capacity = getTier().fuelCapacity();
        return capacity == 0 ? 0 : Math.min(100, getFuel() * 100 / capacity);
    }

    public RocketTier getTier() {
        return RocketTier.byOrdinal(this.entityData.get(DATA_TIER));
    }

    public void setTier(RocketTier tier) {
        this.entityData.set(DATA_TIER, tier.ordinal());
        this.entityData.set(DATA_DEST, Destinations.defaultIndex(tier, level().dimension()));
    }

    // --- Destination selection ----------------------------------------------

    public int getDestinationIndex() {
        return this.entityData.get(DATA_DEST);
    }

    /** The currently selected destination level, or {@code null} if this tier can't fly anywhere. */
    @Nullable
    public net.minecraft.resources.ResourceKey<Level> selectedDestination() {
        int index = getDestinationIndex();
        if ((destinationMask() & (1 << index)) == 0) {
            return null;
        }
        return Destinations.byIndex(index);
    }

    /** Bitmask of global destinations currently available to this rocket. */
    public int destinationMask() {
        int mask = Destinations.availableMask(getTier(), level().dimension());
        // Same-dimension hops: allow the current dimension as a destination when it has registered pads.
        if (level() instanceof ServerLevel server) {
            int self = Destinations.indexOf(level().dimension());
            if (self >= 0 && !PadRegistry.get(server.getServer()).inDimension(level().dimension()).isEmpty()) {
                mask |= (1 << self);
            }
        }
        return mask;
    }

    /** Cycles to the next destination available to this tier (server-side; from the menu button). */
    public void cycleDestination() {
        if (level().isClientSide() || isLaunching()) {
            return;
        }
        int mask = destinationMask();
        int current = getDestinationIndex();
        for (int step = 1; step <= Destinations.all().size(); step++) {
            int next = Math.floorMod(current + step, Destinations.all().size());
            if ((mask & (1 << next)) != 0) {
                this.entityData.set(DATA_DEST, next);
                this.entityData.set(DATA_PAD, -1); // pads differ per dimension — reset the sub-selection
                return;
            }
        }
    }

    /** Selects a global destination directly by index (server-side; from the trajectory buttons). */
    public void setDestinationIndex(int index) {
        if (level().isClientSide() || isLaunching()) {
            return;
        }
        if ((destinationMask() & (1 << index)) != 0) {
            this.entityData.set(DATA_DEST, index);
            this.entityData.set(DATA_PAD, -1);
        }
    }

    // --- Orbital-station selection (which founded station the Station destination docks at) ---

    /** Selected founded-station slot, or {@code -1} for the shared origin platform. */
    public int getStationSlot() {
        return this.entityData.get(DATA_STATION);
    }

    /**
     * Cycles the docking target for the Orbital Station destination: origin → each founded station (in
     * founding order) → back to origin. Server-side; routed from the menu button.
     */
    public void cycleStation() {
        if (!(level() instanceof ServerLevel server) || isLaunching()) {
            return;
        }
        java.util.List<StationRegistry.StationEntry> all = StationRegistry.get(server.getServer()).all();
        int current = getStationSlot();
        int next;
        // Cycle: Origin (−1) → each founded station → New Station (−2) → Origin.
        if (current == -2) {
            next = -1;
        } else if (current == -1) {
            next = all.isEmpty() ? -2 : all.get(0).slot();
        } else {
            int idx = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).slot() == current) {
                    idx = i;
                    break;
                }
            }
            next = (idx < 0 || idx + 1 >= all.size()) ? -2 : all.get(idx + 1).slot();
        }
        this.entityData.set(DATA_STATION, next);
    }

    // --- Landing-pad selection + distance-based fuel -------------------------

    /** Selected pad index into the destination dimension's registered pads ({@code -1} = nearest/auto). */
    public int getPadIndex() {
        return this.entityData.get(DATA_PAD);
    }

    /** Cycles the landing pad within the selected destination dimension: nearest → each pad → nearest. */
    public void cyclePad() {
        if (!(level() instanceof ServerLevel server) || isLaunching()) {
            return;
        }
        net.minecraft.resources.ResourceKey<Level> target = selectedDestination();
        if (target == null) {
            return;
        }
        java.util.List<PadRegistry.PadNode> pads = PadRegistry.get(server.getServer()).inDimension(target);
        if (pads.isEmpty()) {
            this.entityData.set(DATA_PAD, -1);
            return;
        }
        int next = getPadIndex() + 1;
        this.entityData.set(DATA_PAD, next >= pads.size() ? -1 : next);
    }

    /** The block position this rocket will land at for its current destination + sub-selection. */
    @Nullable
    public BlockPos selectedTargetPos(net.minecraft.server.MinecraftServer server) {
        net.minecraft.resources.ResourceKey<Level> target = selectedDestination();
        if (target == null) {
            return null;
        }
        if (target.equals(ModDimensions.STATION_LEVEL)) {
            int slot = getStationSlot();
            StationRegistry.StationEntry entry = slot >= 0 ? StationRegistry.get(server).get(slot) : null;
            return entry != null ? entry.center() : new BlockPos(0, PLATFORM_Y, 0);
        }
        java.util.List<PadRegistry.PadNode> pads = PadRegistry.get(server).inDimension(target);
        int idx = getPadIndex();
        if (idx >= 0 && idx < pads.size()) {
            return pads.get(idx).pos();
        }
        BlockPos near = new BlockPos(Mth.floor(getX()), 0, Mth.floor(getZ()));
        PadRegistry.PadNode nearest = PadRegistry.get(server).nearest(target, near);
        return nearest != null ? nearest.pos() : near;
    }

    /**
     * The fuel this launch will burn: a base cost plus distance (same-dimension hops) or a flat
     * cross-dimension surcharge. Server-authoritative; the client reads the synced value for the UI.
     */
    public int currentFuelCost() {
        net.minecraft.resources.ResourceKey<Level> target = selectedDestination();
        if (target == null) {
            return Integer.MAX_VALUE;
        }
        boolean crossDim = !target.equals(level().dimension());
        net.minecraft.server.MinecraftServer server = level().getServer();
        int dist = 0;
        if (!crossDim && server != null) {
            BlockPos tp = selectedTargetPos(server);
            dist = tp == null ? 0 : RocketTravel.distance(blockPosition(), tp);
        }
        return RocketTravel.cost(crossDim, dist);
    }

    public boolean isLaunching() {
        return this.entityData.get(DATA_LAUNCHING);
    }

    private void setLaunching(boolean launching) {
        this.entityData.set(DATA_LAUNCHING, launching);
    }

    // --- Fuel / launch logic ------------------------------------------------

    // --- Onboard oxygen (life support) --------------------------------------

    /** Current onboard oxygen, in millibuckets. */
    public int getOxygen() {
        return this.entityData.get(DATA_OXYGEN);
    }

    /** Current oxygen as a 0–100 percentage of the tier capacity (for the UI readout). */
    public int getOxygenPercent() {
        int capacity = getTier().oxygenCapacity();
        return capacity == 0 ? 0 : Math.min(100, getOxygen() * 100 / capacity);
    }

    /**
     * Fills the onboard oxygen tank (server-side), capped at the tier capacity.
     * @return millibuckets that could not be accepted (overflow).
     */
    public int addOxygen(int amount) {
        if (level().isClientSide() || amount <= 0) {
            return amount;
        }
        int room = Math.max(0, getTier().oxygenCapacity() - getOxygen());
        int fill = Math.min(amount, room);
        if (fill > 0) {
            this.entityData.set(DATA_OXYGEN, getOxygen() + fill);
        }
        return amount - fill;
    }

    /** Draws oxygen from the onboard tank (server-side). @return millibuckets actually removed. */
    public int drainOxygen(int amount) {
        if (level().isClientSide() || amount <= 0) {
            return 0;
        }
        int removed = Math.min(amount, getOxygen());
        if (removed > 0) {
            this.entityData.set(DATA_OXYGEN, getOxygen() - removed);
        }
        return removed;
    }

    /** Current onboard power, in FE. */
    public int getPower() {
        return this.entityData.get(DATA_POWER);
    }

    /** Current power as a 0–100 percentage of the tier capacity (for the UI readout). */
    public int getPowerPercent() {
        int capacity = getTier().powerCapacity();
        return capacity == 0 ? 0 : Math.min(100, getPower() * 100 / capacity);
    }

    /** Fills the onboard power buffer (server-side), capped at the tier capacity. @return FE not accepted. */
    public int addPower(int amount) {
        if (level().isClientSide() || amount <= 0) {
            return amount;
        }
        int room = Math.max(0, getTier().powerCapacity() - getPower());
        int fill = Math.min(amount, room);
        if (fill > 0) {
            this.entityData.set(DATA_POWER, getPower() + fill);
        }
        return amount - fill;
    }

    /** @return millibuckets of fuel that could not be accepted (overflow). Caps at the tier capacity. */
    public int addFuel(int amount) {
        int room = Math.max(0, getTier().fuelCapacity() - (int) this.fuelTank.getAmount());
        int toFill = Math.min(amount, room);
        int filled = (int) this.fuelTank.fill(ModFluids.ROCKET_FUEL.get(), toFill, false);
        return amount - filled;
    }

    /**
     * Whether the rocket is ready to fly — fuelled, a destination selected, and on a valid pad —
     * independent of whether anyone is aboard yet. Drives the UI Launch button (the player boards by
     * pressing it, so the button must light up before they enter).
     */
    public boolean isLaunchReady() {
        return !isLaunching()
                && selectedDestination() != null
                && getFuel() >= requiredFuelForLaunch()
                && isOnValidPad();
    }

    /** Whether a launch could be started right now (launch-ready AND a player is aboard). */
    public boolean canLaunch() {
        return isLaunchReady() && this.getFirstPassenger() instanceof Player;
    }

    /**
     * Boards {@code player} (if the rocket is empty) and begins the launch — the UI Launch button calls
     * this so pressing Launch is what puts the player in the rocket. Server-side.
     * @return {@code true} if the ascent actually started.
     */
    public boolean boardAndLaunch(ServerPlayer player) {
        if (level().isClientSide() || isLaunching()) {
            return false;
        }
        if (this.getFirstPassenger() == null) {
            player.startRiding(this);
        }
        startLaunch();
        return isLaunching();
    }

    private int requiredFuelForLaunch() {
        return currentFuelCost();
    }

    /**
     * Launch-pad gating (re-checked at launch): the pad formation the rocket stands on must provide a
     * tier at least equal to the rocket's tier. Tier 1 = a single pad, Tier 2 = a 3x3, Tier 3 = a 3x3
     * ringed with Station Wall, Tier 4 = a 5x5 Heavy Launch Complex. See {@link LaunchPadMultiblock#padTier}.
     */
    public boolean isOnValidPad() {
        if (isOnReturnSite()) {
            return true;
        }
        BlockPos origin = validPadScanOrigin();
        if (origin == null) {
            return false;
        }
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level(), origin);
        return isValidPadCluster(pads, origin);
    }

    @Nullable
    private BlockPos validPadScanOrigin() {
        for (BlockPos origin : padScanOrigins()) {
            Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level(), origin);
            if (isValidPadCluster(pads, origin)) {
                return origin;
            }
        }
        return null;
    }

    private boolean isValidPadCluster(Set<BlockPos> pads, BlockPos origin) {
        return LaunchPadMultiblock.padTierContaining(level(), pads, origin) >= getTier().level();
    }

    /** Where to look for the pad under the rocket. The rocket stands ON the pad's 3px plate. */
    private BlockPos padScanOrigin() {
        BlockPos feet = this.blockPosition();
        return level().getBlockState(feet).getBlock() instanceof RocketLaunchPadBlock ? feet : feet.below();
    }

    private java.util.List<BlockPos> padScanOrigins() {
        BlockPos feet = padScanOrigin();
        int y = feet.getY();
        int centreX = Mth.floor(this.getX());
        int centreZ = Mth.floor(this.getZ());
        java.util.List<BlockPos> origins = new java.util.ArrayList<>(5);
        addUniqueOrigin(origins, new BlockPos(centreX, y, centreZ));
        addUniqueOrigin(origins, feet);
        addUniqueOrigin(origins, new BlockPos(centreX + 1, y, centreZ));
        addUniqueOrigin(origins, new BlockPos(centreX - 1, y, centreZ));
        addUniqueOrigin(origins, new BlockPos(centreX, y, centreZ + 1));
        addUniqueOrigin(origins, new BlockPos(centreX, y, centreZ - 1));
        return origins;
    }

    private static void addUniqueOrigin(java.util.List<BlockPos> origins, BlockPos pos) {
        if (!origins.contains(pos)) {
            origins.add(pos);
        }
    }

    private boolean isOnReturnSite() {
        BlockPos feet = this.blockPosition();
        return ReturnSiteBlock.isReturnSite(level().getBlockState(feet))
                || ReturnSiteBlock.isReturnSite(level().getBlockState(feet.below()));
    }

    /** Begins the ascent. Server-side; called from the rocket menu's Launch button. */
    public void startLaunch() {
        if (level().isClientSide() || !canLaunch()) {
            if (!level().isClientSide() && !isLaunching() && !isOnValidPad()
                    && this.getFirstPassenger() instanceof ServerPlayer rider) {
                // Report the tier the pad under the rocket actually reads as (the same scan isOnValidPad
                // uses), so a player whose pad "looks right" can see exactly what the game detects.
                int detected = 0;
                for (BlockPos origin : padScanOrigins()) {
                    detected = Math.max(detected, LaunchPadMultiblock.padTierContaining(
                            level(), LaunchPadMultiblock.connectedPads(level(), origin), origin));
                }
                rider.sendSystemMessage(Component.translatable(
                        "item.nerospace.rocket.launch_pad_tier", detected, getTier().level()));
                rider.sendSystemMessage(Component.translatable(getTier().padRequirementKey()));
            }
            return;
        }
        // Remember the pad we're lifting off from so the return rocket can fly straight back to it.
        this.launchOriginDim = level().dimension();
        BlockPos originPad = validPadScanOrigin();
        this.launchOriginPad = originPad != null ? originPad.immutable() : blockPosition();
        setLaunching(true);
        this.launchTicks = 0;
        level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 3.0F, 0.6F);
    }

    @Override
    public void tick() {
        super.tick();

        // Advance the client-side interpolation toward the latest server position (vanilla vehicle pattern).
        if (level().isClientSide() && this.interpolation.hasActiveInterpolation()) {
            this.interpolation.interpolate();
        }

        if (isLaunching()) {
            if (level().isClientSide()) {
                spawnLaunchParticles();
            } else {
                double t = (double) this.launchTicks / LAUNCH_DURATION;
                double speed = 0.08D + 0.5D * (t * t);
                // Cosmetic gravity feel (GRAVITY_DESIGN.md §5b): lower local gravity → a slightly faster
                // climb. Bounded; the launch still completes on the fixed LAUNCH_DURATION counter (the
                // arrival is a teleport), so this only changes how high the rocket visually rises.
                double factor = Math.min(1.5D, GravityManager.factorAt((ServerLevel) level(), blockPosition()));
                speed *= 1.5D - 0.5D * factor;
                this.setDeltaMovement(0.0D, speed, 0.0D);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.launchTicks++;
                if (this.launchTicks >= LAUNCH_DURATION) {
                    completeLaunch();
                }
            }
        } else if (!level().isClientSide()) {
            // Idle on the pad: drain any fuel container the player dropped in the intake.
            consumeFuelInput();
        }
    }

    /**
     * If the intake slot holds a fuel container and the tank has room, drains one unit (1000 mB) into
     * the tank. A bucket is returned empty; a canister is consumed. Runs once per tick, server-side.
     */
    private void consumeFuelInput() {
        ItemStack stack = this.fuelInput.getItem(0);
        if (stack.isEmpty() || !isFuelContainer(stack)) {
            return;
        }
        if (addFuel(CANISTER_MB) >= CANISTER_MB) {
            return; // tank full — leave the container in place.
        }
        if (stack.is(ModItems.ROCKET_FUEL_BUCKET.get())) {
            if (stack.getCount() == 1) {
                this.fuelInput.setItem(0, new ItemStack(Items.BUCKET));
            } else {
                stack.shrink(1);
                if (level() instanceof ServerLevel server) {
                    this.spawnAtLocation(server, new ItemStack(Items.BUCKET));
                }
            }
        } else {
            stack.shrink(1); // canister consumed
        }
        level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.6F, 1.0F);
    }

    /** Drops the intake slot's contents into the world (called before the rocket is discarded). */
    private void dropFuelInput() {
        if (level() instanceof ServerLevel server) {
            ItemStack stack = this.fuelInput.removeItemNoUpdate(0);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(server, stack);
            }
        }
    }

    /** Lands a fresh rocket of {@code tier} on the destination pad carrying {@code fuel} — pad-to-pad travel. */
    /** Sets the pad this rocket should fly back to (its launch origin), so a return trip lands there. */
    public void setReturnTarget(@Nullable net.minecraft.resources.ResourceKey<Level> dim, @Nullable BlockPos pos) {
        this.returnDim = dim != null ? dim.identifier().toString() : "";
        this.returnPos = pos != null ? pos.immutable() : null;
    }

    private static void landRocketOn(ServerLevel level, BlockPos pad, RocketTier tier, int fuel,
            @Nullable net.minecraft.resources.ResourceKey<Level> originDim, @Nullable BlockPos originPad) {
        RocketEntity landed = new RocketEntity(level, pad.getX() + 0.5D,
                pad.getY() + RocketLaunchPadBlock.SURFACE_HEIGHT, pad.getZ() + 0.5D, tier);
        if (fuel > 0) {
            landed.addFuel(fuel);
        }
        landed.setReturnTarget(originDim, originPad); // remember where the rider came from
        level.addFreshEntity(landed);
    }

    private void spawnLaunchParticles() {
        double bx = this.getX();
        double by = this.getY();
        double bz = this.getZ();
        for (int i = 0; i < 4; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.4D;
            double oz = (this.random.nextDouble() - 0.5D) * 0.4D;
            level().addParticle(ParticleTypes.FLAME, bx + ox, by - 0.2D, bz + oz, 0.0D, -0.1D, 0.0D);
            level().addParticle(ParticleTypes.LARGE_SMOKE, bx + ox, by - 0.4D, bz + oz, 0.0D, -0.05D, 0.0D);
        }
    }

    private void completeLaunch() {
        NerospaceTelemetry.trace("rocket.launch", "completeLaunch", this::completeLaunch0);
    }

    private void completeLaunch0() {
        net.minecraft.resources.ResourceKey<Level> targetKey = selectedDestination();
        if (targetKey == null) {
            setLaunching(false);
            return;
        }

        int outboundFuel = currentFuelCost();
        int carriedFuel = Math.max(0, getFuel() - outboundFuel);
        this.fuelTank.drain(outboundFuel, false);
        NerospaceTelemetry.breadcrumb("rocket",
                "launch tier=" + getTier() + " dim=" + Destinations.name(targetKey));

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof ServerPlayer player && level() instanceof ServerLevel current) {
            MinecraftServer server = current.getServer();
            ServerLevel destination = server.getLevel(targetKey);
            if (destination != null) {
                player.stopRiding();

                net.minecraft.resources.ResourceKey<Level> originDim = this.launchOriginDim;
                BlockPos originPad = this.launchOriginPad;

                BlockPos preferred;
                BlockPos directPad = null; // a specific pad to land on directly (bypasses the registry search)
                Component arrivalMessage;
                if (targetKey.equals(ModDimensions.STATION_LEVEL)) {
                    // Ensure the selected station (room + airlock + Tier-2 pad) exists, then land on its
                    // pad — the airlock connects the pad back to the Station Core room.
                    int slot = getStationSlot();
                    StationRegistry.StationEntry entry = slot == -2
                            // found a brand-new station from the rocket, owned by the rider
                            ? StationRegistry.get(server).found(null, player.getUUID().toString())
                            : (slot >= 0 ? StationRegistry.get(server).get(slot) : null);
                    BlockPos core = entry != null ? entry.center() : new BlockPos(0, PLATFORM_Y, 0);
                    StationStructure.build(destination, core); // ensure room + airlock + registered pad exist
                    preferred = core; // land on the nearest registered pad to the core (within ~32 blocks)
                    arrivalMessage = entry != null
                            ? Component.translatable("entity.nerospace.rocket.station_arrived", entry.name())
                            : Component.translatable("entity.nerospace.rocket.docked");
                } else {
                    java.util.List<PadRegistry.PadNode> dimPads = PadRegistry.get(server).inDimension(targetKey);
                    int padIdx = getPadIndex();
                    preferred = new BlockPos(Mth.floor(player.getX()), 0, Mth.floor(player.getZ()));
                    BlockPos returnTo = this.returnPos;
                    if (padIdx >= 0 && padIdx < dimPads.size()) {
                        directPad = dimPads.get(padIdx).pos(); // explicitly chosen pad
                    } else if (returnTo != null
                            && targetKey.identifier().toString().equals(this.returnDim)) {
                        // Auto: fly straight back to the pad you launched from (if it's still there).
                        destination.getChunk(returnTo.getX() >> 4, returnTo.getZ() >> 4);
                        if (destination.getBlockState(returnTo)
                                .is(za.co.neroland.nerospace.registry.ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                            directPad = returnTo;
                        } else {
                            preferred = returnTo;
                        }
                    }
                    arrivalMessage = targetKey.equals(Level.OVERWORLD)
                            ? Component.translatable("entity.nerospace.rocket.home_arrived")
                            : Component.translatable("entity.nerospace.rocket.arrived");
                }

                // Resolve the landing pad: a chosen/return pad directly, else the nearest registered pad in
                // the destination, else (no launch pad there) an auto-built Landing Pod.
                BlockPos landPos = directPad;
                if (landPos == null) {
                    PadRegistry.PadNode pad = PadRegistry.get(server).nearest(targetKey, preferred);
                    if (pad != null) {
                        landPos = pad.pos();
                        arrivalMessage = Component.translatable("entity.nerospace.rocket.pad_arrived", pad.name());
                    }
                }

                double ax;
                double ay;
                double az;
                if (landPos != null) {
                    destination.getChunk(landPos.getX() >> 4, landPos.getZ() >> 4);
                    landRocketOn(destination, landPos, getTier(), carriedFuel, originDim, originPad);
                    ax = landPos.getX() + 0.5D;
                    ay = landPos.getY() + 1.0D;
                    az = landPos.getZ() + 0.5D;
                } else {
                    // No launch pad in the destination — auto-build a Landing Pod so a first trip never
                    // strands the player. (Only used when the destination genuinely has no pad.)
                    ReturnSitePlacement.Arrival arrival =
                            ReturnSitePlacement.place(destination, targetKey, preferred, getTier(), carriedFuel);
                    ax = arrival.x();
                    ay = arrival.y();
                    az = arrival.z();
                }
                player.teleportTo(destination, ax, ay, az,
                        Set.of(), player.getYRot(), player.getXRot(), true);
                player.sendSystemMessage(arrivalMessage);
                // Life support: dump the rocket's remaining oxygen into the rider as a slow-draining
                // surface reserve (this also tops their suit/personal O2 to full on arrival).
                za.co.neroland.nerospace.world.OxygenManager.grantArrivalReserve(player, getOxygen());
                // Neroland Core progression (one-directional): a completed launch reaches orbit; arriving
                // at a far planet is a deep-space milestone. tryOpen respects the gate `requires` chain.
                za.co.neroland.nerospace.progression.StarGuideGrants.driveReachedOrbit(player);
                if (targetKey.equals(ModDimensions.CINDARA_LEVEL) || targetKey.equals(ModDimensions.GLACIRA_LEVEL)) {
                    za.co.neroland.nerospace.progression.StarGuideGrants.driveDeepSpace(player);
                }
            }
        }

        // The in-flight rocket is expended; the arrival site contains the same-tier return kit.
        dropFuelInput();
        this.discard();
    }

    // --- Interaction --------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (isLaunching()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ROCKET_FUEL_BUCKET.get())) {
            if (!level().isClientSide()) {
                int overflow = addFuel(1_000);
                if (overflow < 1_000) {
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    }
                    level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.8F, 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            if (!level().isClientSide()) {
                int overflow = addFuel(CANISTER_MB);
                if (overflow < CANISTER_MB) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.7F, 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Open the flight console WITHOUT boarding — the player only enters the rocket when they press
            // Launch (RocketMenu.boardAndLaunch). Push the founded-station names for the dock cycler first.
            za.co.neroland.nerospace.network.ModNetwork.sendToPlayer(serverPlayer,
                    za.co.neroland.nerospace.network.StationSyncPayload.of(
                            StationRegistry.get(serverPlayer.level().getServer())));
            serverPlayer.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    // --- Passenger handling -------------------------------------------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(itemForTier());
    }

    private net.minecraft.world.item.Item itemForTier() {
        return switch (getTier()) {
            case TIER_1 -> ModItems.ROCKET_TIER_1.get();
            case TIER_2 -> ModItems.ROCKET_TIER_2.get();
            case TIER_3 -> ModItems.ROCKET_TIER_3.get();
            case TIER_4 -> ModItems.ROCKET_TIER_4.get();
        };
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        if (this.isRemoved() || isLaunching()) {
            return false;
        }
        if (damageSource.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, new ItemStack(itemForTier()));
            }
            dropFuelInput();
            this.discard();
            return true;
        }
        return false;
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_TIER, input.getIntOr("Tier", RocketTier.TIER_1.ordinal()));
        boolean globalDestination = input.getBooleanOr("GlobalDestination", false);
        int storedDestination = input.getIntOr("Destination", globalDestination
                ? Destinations.defaultIndex(getTier(), level().dimension())
                : getTier().defaultDestinationIndex());
        int destination = globalDestination
                ? storedDestination
                : Destinations.legacyIndex(getTier(), storedDestination);
        this.entityData.set(DATA_DEST,
                Destinations.sanitizeIndex(getTier(), level().dimension(), destination));
        this.entityData.set(DATA_STATION, input.getIntOr("StationSlot", -1));
        this.entityData.set(DATA_PAD, input.getIntOr("Pad", -1));
        this.returnDim = input.getStringOr("ReturnDim", "");
        if (input.getBooleanOr("HasReturn", false)) {
            this.returnPos = new BlockPos(input.getIntOr("ReturnX", 0),
                    input.getIntOr("ReturnY", 0), input.getIntOr("ReturnZ", 0));
        }
        this.entityData.set(DATA_OXYGEN, input.getIntOr("Oxygen", 0));
        this.entityData.set(DATA_POWER, input.getIntOr("Power", 0));
        Fluid fluid = BuiltInRegistries.FLUID.getValue(
                Identifier.parse(input.getStringOr("FuelFluid", "minecraft:empty")));
        this.fuelTank.setRaw(fluid, input.getIntOr("FuelAmount", 0));
        this.fuelInput.setItem(0, input.read("FuelInput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        syncFuel();
        setLaunching(input.getBooleanOr("Launching", false));
        this.launchTicks = input.getIntOr("LaunchTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Tier", getTier().ordinal());
        output.putInt("Destination", getDestinationIndex());
        output.putBoolean("GlobalDestination", true);
        output.putInt("StationSlot", getStationSlot());
        output.putInt("Pad", getPadIndex());
        BlockPos returnTo = this.returnPos;
        output.putString("ReturnDim", this.returnDim);
        output.putBoolean("HasReturn", returnTo != null);
        if (returnTo != null) {
            output.putInt("ReturnX", returnTo.getX());
            output.putInt("ReturnY", returnTo.getY());
            output.putInt("ReturnZ", returnTo.getZ());
        }
        output.putInt("Oxygen", getOxygen());
        output.putInt("Power", getPower());
        output.putString("FuelFluid", BuiltInRegistries.FLUID.getKey(this.fuelTank.getRawFluid()).toString());
        output.putInt("FuelAmount", this.fuelTank.getRawAmount());
        output.store("FuelInput", ItemStack.OPTIONAL_CODEC, this.fuelInput.getItem(0));
        output.putBoolean("Launching", isLaunching());
        output.putInt("LaunchTicks", this.launchTicks);
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.rocket");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RocketMenu(containerId, playerInventory, this, this.dataAccess);
    }

    /** Exposed for the menu's server constructor. */
    public ContainerData getDataAccess() {
        return this.dataAccess;
    }
}
