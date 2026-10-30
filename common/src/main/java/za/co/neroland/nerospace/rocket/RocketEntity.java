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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.menu.MenuOpener;
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
    /** Descent state machine phase ({@link DescentMath.Phase} ordinal) — server-driven, client renders. */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Ticks spent in the current phase. */
    private static final EntityDataAccessor<Integer> DATA_PHASE_TICK =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    /** Ticks of ascent before the rider is transported. */
    public static final int LAUNCH_DURATION = 100;
    /** One fuel canister is worth this many millibuckets. */
    public static final int CANISTER_MB = 1_000;
    /** Y level of the shared Orbital Station origin platform (the founded-station system is deferred). */
    public static final int PLATFORM_Y = 64;

    private int launchTicks;

    // --- Client-side presentation state (never saved or synced) ---------------

    /** Ticks of the ignition ramp the renderer animates (plume bloom, hull shudder, hatch seal). */
    public static final int IGNITION_TICKS = 12;
    /**
     * The rider's eye line per tier, in model pixels above the pad: the window band of each hull is
     * centred on it. MUST mirror the {@code eye} values in {@code tools/rocket_models.py} (the
     * generator verifies this).
     */
    private static final float[] RIDER_EYE_PX = {43F, 49F, 53F, 57F};
    /** A standing player's eye (1.62) minus the vehicle attachment riders are seated by (0.6). */
    private static final double RIDER_EYE_ABOVE_SEAT = 1.02D;
    private static final double HULL_WALL_PX = 7.0D;

    private float launchAnim;
    private float launchAnimPrev;
    private float hatchAnim;
    private float hatchAnimPrev;

    /**
     * Where this rocket lifted off from. Saved as optional fields so a rocket reloaded mid-ascent keeps its return
     * target; a 1.6.0 in-flight rocket (no fields) falls back to where it is when the ascent completes.
     */
    @Nullable
    private net.minecraft.resources.ResourceKey<Level> launchOriginDim;
    @Nullable
    private BlockPos launchOriginPad;

    /** The pad this rocket should fly back to (set on a return rocket) — its launch origin. Persisted. */
    private String returnDim = "";
    @Nullable
    private BlockPos returnPos;

    /**
     * Whether this rocket follows the 2.0 charter rule. A new rocket does; a rocket loaded from a 1.6.0
     * save has no {@code CharterRule} flag, so its in-flight "New Station" is honoured once for free.
     */
    private boolean charterRuleAware = true;

    /**
     * Whether this rocket arrived at a station whose rider could not {@code canUse} it. Saved as
     * the optional {@code Visitor} boolean (default false, so 1.6.0 rockets are never sent home); never an identity.
     */
    private boolean visitor;
    /** Transient: the Star Guide arrival drives already ran for this descent's rider. */
    private boolean arrivalDrivesDone;
    /** Transient per-viewer cache for the Rocket screen's gate mask / Observatory reveal (refreshed every 20 t). */
    public static final int VIEWER_DATA_REFRESH_TICKS = 20;

    // --- Descent — the target pose is saved; the rest is transient -----------------------

    @Nullable
    private Vec3 descentTarget;
    private float descentYaw;
    private PadKind descentKind = PadKind.LAUNCH_PAD;
    @Nullable
    private BlockPos descentBlock;
    private int descentNode = -1;
    /** Oxygen the rider arrived with (for the link event at touchdown). */
    private int arrivalOxygen;
    /** Game time this rocket came to rest on its pad (−1 = unknown); read by the station console. */
    private long parkedSince = -1L;
    /** Transient: the flight the descent was resolved for (re-resolve if the pad breaks). */
    @Nullable
    private LandingResolverLevelAdapter.Flight descentFlight;
    /** Transient: the rider to keep seated (never saved — no player identity on the entity). */
    @Nullable
    private java.util.UUID descentRider;
    @Nullable
    private Component arrivalMessage;
    private boolean arrivalPadMissing;
    private boolean arrivalPadOccupied;
    @Nullable
    private net.minecraft.resources.ResourceKey<Level> arrivalOrigin;

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
                case 1 -> fuelCapacity();
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
                default -> 0; // [14] destinationGateMask is per viewer: see createMenu
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return RocketMenu.DATA_COUNT;
        }
    };

    /** Client-side position interpolation so the ascent (and the seated rider) moves smoothly. */
    //? if <26.3 {
    private final net.minecraft.world.entity.InterpolationHandler interpolation =
            new net.minecraft.world.entity.InterpolationHandler(this);
    //?}

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    //? if >=26.3 {
    /*@Override
    protected net.minecraft.world.entity.InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this);
    }
    *///?} else {
    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return this.interpolation;
    }
    //?}

    // --- Per-tier presentation ----------------------------------------------

    /** Render scale by tier — bigger boosters genuinely LOOK bigger. */
    public float visualScale() {
        return switch (getTier()) {
            case TIER_1 -> 1.5F;
            case TIER_2 -> 1.8F;
            case TIER_3 -> 2.1F;
            case TIER_4 -> 2.4F;
        };
    }

    // Note: the root overrides NeoForge's shouldRiderSit() (a loader extension, not vanilla) to make
    // the rider STAND at the console; that hook has no cross-loader equivalent, so it is omitted here.

    /** Seat the rider so their eyes sit exactly on the hull's window band (scaled by tier). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        double eye = RIDER_EYE_PX[getTier().ordinal()] * visualScale() / 16.0D;
        return new Vec3(0.0D, Math.max(0.4D, eye - RIDER_EYE_ABOVE_SEAT), 0.0D);
    }

    /**
     * The visual envelope the renderer culls on: the hitbox is a 1×3 column but the hull is several
     * times taller, and the exhaust plume / separated boosters reach well below it.
     */
    public AABB visualBounds() {
        double s = visualScale();
        AABB box = getBoundingBox();
        return new AABB(box.minX - 2.0D * s, box.minY - 5.0D * s, box.minZ - 2.0D * s,
                box.maxX + 2.0D * s, box.minY + 6.0D * s, box.maxZ + 2.0D * s);
    }

    /** Ascent progress 0..1 as this client has seen it (the server's counter is not synced). */
    public float launchProgress(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, this.launchAnimPrev, this.launchAnim) / LAUNCH_DURATION, 0.0F, 1.0F);
    }

    /** Ignition ramp 0..1 over the first {@link #IGNITION_TICKS} of the ascent. */
    public float ignition(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, this.launchAnimPrev, this.launchAnim) / IGNITION_TICKS, 0.0F, 1.0F);
    }

    /** Hatch opening 0 (sealed) .. 1 (open). */
    public float hatchOpen(float partialTick) {
        return Mth.lerp(partialTick, this.hatchAnimPrev, this.hatchAnim);
    }

    /** Whether this hull's hatch swings open on the pad (an uncrewed cargo hull stays sealed). */
    protected boolean hatchOpens() {
        return true;
    }

    /** Client tick: advance the ignition counter and ease the hatch; vent a little fuel vapour at rest. */
    private void tickPresentation() {
        this.launchAnimPrev = this.launchAnim;
        if (isDescending()) {
            // The ascent animation played in reverse over the 48-block drop.
            double height = Math.max(0.0D, DescentMath.DESCENT_HEIGHT - getPhaseTick() * DescentMath.DESCENT_SPEED);
            this.launchAnim = DescentMath.reversedLaunchAnim(LAUNCH_DURATION, height);
        } else {
            this.launchAnim = isLaunching() ? Math.min(LAUNCH_DURATION, this.launchAnim + 1.0F) : 0.0F;
        }
        this.hatchAnimPrev = this.hatchAnim;
        float target = !isLaunching() && hatchOpens() && getPassengers().isEmpty() ? 1.0F : 0.0F;
        this.hatchAnim += (target - this.hatchAnim) * 0.16F;
        if (!isLaunching() && getFuel() > 0 && this.random.nextInt(7) == 0) {
            double s = visualScale();
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double r = (HULL_WALL_PX / 16.0D) * s;
            level().addParticle(ParticleTypes.SNOWFLAKE, getX() + Math.cos(angle) * r,
                    getY() + (0.8D + 0.6D * this.random.nextDouble()) * s, getZ() + Math.sin(angle) * r,
                    0.0D, -0.015D, 0.0D);
        }
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
        builder.define(DATA_PHASE, DescentMath.Phase.IDLE.ordinal());
        builder.define(DATA_PHASE_TICK, 0);
    }

    // --- Descent phase --------------------------------------------------------------------------

    public DescentMath.Phase getPhase() {
        int i = this.entityData.get(DATA_PHASE);
        DescentMath.Phase[] all = DescentMath.Phase.values();
        return i >= 0 && i < all.length ? all[i] : DescentMath.Phase.IDLE;
    }

    private void setPhase(DescentMath.Phase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
        this.entityData.set(DATA_PHASE_TICK, 0);
    }

    private int getPhaseTick() {
        return this.entityData.get(DATA_PHASE_TICK);
    }

    /** Whether the rocket is coming down onto its landing site; it cannot be used meanwhile. */
    public boolean isDescending() {
        DescentMath.Phase p = getPhase();
        return p == DescentMath.Phase.DESCENDING || p == DescentMath.Phase.ABORTED;
    }

    /** Whether this rocket is a visitor at the station it is parked on (the console's send-home acts on these only). */
    public boolean isVisitor() {
        return this.visitor;
    }

    /** Game ticks this rocket has stood parked (riderless, at rest), or −1 when unknown. For the station console. */
    public long parkedTicks() {
        if (this.parkedSince < 0L || isLaunching() || isDescending() || !getPassengers().isEmpty()) {
            return -1L;
        }
        return Math.max(0L, level().getGameTime() - this.parkedSince);
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
        int capacity = fuelCapacity();
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
        // Planets whose dimension is absent on this server are hidden (server-side; the client reads the synced mask).
        int mask = Destinations.availableMask(getTier(), level().dimension(),
                level() instanceof ServerLevel serverLevel ? serverLevel.getServer() : null);
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
        stepDestination(1, null);
    }

    /**
     * Steps the selection to the next ({@code dir} &gt; 0) or previous reachable destination, skipping worlds
     * whose Core gate is closed for {@code player} (server-side; the console cyclers and the Launch Controller).
     */
    public DestinationSelection.Outcome stepDestination(int dir, @Nullable Player player) {
        if (level().isClientSide()) {
            return DestinationSelection.Outcome.NO_ROCKET;
        }
        int next = DestinationSelection.step(destinationMask(), player != null ? gateMask(player) : 0,
                getDestinationIndex(), dir, Destinations.all().size());
        return selectDestination(next, player);
    }

    /** Selects a global destination directly by index (server-side; ignores Core gates — API callers). */
    public void setDestinationIndex(int index) {
        selectDestination(index, null);
    }

    /**
     * Selects a global destination for {@code player} (server-side; the menu's authority over the screens'
     * optimistic highlight). Refuses while launching, outside this rocket's range, or behind a Core gate still
     * closed for {@code player}; a refusal leaves the selection unchanged so the screen reconciles to it.
     */
    public DestinationSelection.Outcome selectDestination(int index, @Nullable Player player) {
        if (level().isClientSide()) {
            return DestinationSelection.Outcome.NO_ROCKET;
        }
        DestinationSelection.Outcome outcome = DestinationSelection.evaluate(index, destinationMask(),
                player != null ? gateMask(player) : 0, getDestinationIndex(), isLaunching());
        if (outcome == DestinationSelection.Outcome.OK) {
            this.entityData.set(DATA_DEST, index);
            this.entityData.set(DATA_PAD, -1); // pads differ per dimension — reset the sub-selection
        }
        return outcome;
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
        cycleStation(null);
    }

    /**
     * {@link #cycleStation()} for a viewer: "New Station" (−2) is skipped when {@code viewer} carries no Station
     * Charter (client hint, re-checked at arrival), and a station whose docking policy is MEMBERS is
     * skipped for a viewer who cannot use it.
     */
    public void cycleStation(@Nullable Player viewer) {
        if (!(level() instanceof ServerLevel server) || isLaunching()) {
            return;
        }
        java.util.List<StationRegistry.StationEntry> all = new java.util.ArrayList<>();
        for (StationRegistry.StationEntry e : StationRegistry.get(server.getServer()).all()) {
            if (viewer == null || canDockAt(e, viewer)) {
                all.add(e);
            }
        }
        boolean allowNew = viewer == null || hasCharter(viewer);
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
        if (next == -2 && !allowNew) {
            next = -1;
        }
        this.entityData.set(DATA_STATION, next);
    }

    /** Whether {@code viewer} may pick {@code entry} as a destination (MEMBERS docking policy). */
    private static boolean canDockAt(StationRegistry.StationEntry entry, Player viewer) {
        boolean canUse = viewer instanceof ServerPlayer sp
                ? StationRegistry.canUse(entry, sp)
                : za.co.neroland.nerospace.world.station.StationAccess.canUse(entry.owner(), entry.members(),
                        viewer.getUUID().toString(), false);
        return StationDockingRule.mayChoose(entry.docking(), canUse);
    }

    /** Whether {@code player} carries at least one Station Charter (main inventory or off-hand). */
    public static boolean hasCharter(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(ModItems.STATION_CHARTER.get())) {
                return true;
            }
        }
        return player.getOffhandItem().is(ModItems.STATION_CHARTER.get());
    }

    /** Consumes exactly one Station Charter, preferring the held stacks. @return whether one was taken. */
    private static boolean consumeCharter(Player player) {
        for (ItemStack stack : new ItemStack[] { player.getMainHandItem(), player.getOffhandItem() }) {
            if (stack.is(ModItems.STATION_CHARTER.get())) {
                stack.shrink(1);
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(ModItems.STATION_CHARTER.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
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

    protected void setLaunching(boolean launching) {
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

    /** @return millibuckets of fuel that could not be accepted (overflow). Caps at {@link #fuelCapacity()}. */
    public int addFuel(int amount) {
        int room = Math.max(0, fuelCapacity() - (int) this.fuelTank.getAmount());
        int toFill = Math.min(amount, room);
        int filled = (int) this.fuelTank.fill(ModFluids.ROCKET_FUEL.get(), toFill, false);
        return amount - filled;
    }

    /** The usable tank size — the tier's capacity for crewed rockets; the cargo hull overrides it. */
    public int fuelCapacity() {
        return getTier().fuelCapacity();
    }

    /** Removes up to {@code mb} of fuel from the tank (server). @return millibuckets actually removed. */
    public int drainFuelRaw(int mb) {
        if (level().isClientSide() || mb <= 0) {
            return 0;
        }
        return (int) this.fuelTank.drain(mb, false);
    }

    /**
     * Whether the rocket is ready to fly — fuelled, a destination selected, and on a valid pad —
     * independent of whether anyone is aboard yet. Drives the UI Launch button (the player boards by
     * pressing it, so the button must light up before they enter).
     */
    public boolean isLaunchReady() {
        return !isLaunching() && !isDescending()
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
        net.minecraft.world.entity.Entity seated = this.getFirstPassenger();
        if (!LaunchSeat.mayLaunch(seated == null, seated == player)) {
            player.sendSystemMessage(Component.translatable("entity.nerospace.rocket.seat_taken"));
            return false;
        }
        if (seated == null) {
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
                || ReturnSiteBlock.isReturnSite(level().getBlockState(feet.below()))
                // A Docking Ring is a return site too: any tier may lift off from a dock.
                || level().getBlockState(feet).getBlock() instanceof DockingRingBlock
                || level().getBlockState(feet.below()).getBlock() instanceof DockingRingBlock;
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
        // A MEMBERS-only station is re-checked at launch (the cycler is only a client-facing filter).
        if (this.getFirstPassenger() instanceof ServerPlayer docking && level() instanceof ServerLevel dockLevel
                && LandingResolverLevelAdapter.isStation(selectedDestination()) && getStationSlot() >= 0) {
            StationRegistry.StationEntry target = StationRegistry.get(dockLevel.getServer()).get(getStationSlot());
            if (target != null && !canDockAt(target, docking)) {
                docking.sendSystemMessage(Component.translatable("entity.nerospace.rocket.station_members_only"));
                return;
            }
        }
        // Core progression gate read at LAUNCH (never at load): Nyxara needs deep_space open for the pilot.
        if (this.getFirstPassenger() instanceof ServerPlayer gated) {
            var gatedPlanet = za.co.neroland.nerospace.world.planet.Planets.get(selectedDestination());
            if (gatedPlanet != null && gatedPlanet.coreGate().isPresent()
                    && !za.co.neroland.nerolandcore.progression.ProgressionGates.isOpen(gated, gatedPlanet.coreGate().get())) {
                gated.sendSystemMessage(Component.translatable("entity.nerospace.rocket.gate_locked",
                        Destinations.displayName(gatedPlanet.dimension())));
                return;
            }
        }
        // One launch per rider per 100 ticks (stops macro-spam; in-memory only), checked BEFORE the optional
        // NeroEconomy launch/docking fee so a launch refused for the cooldown is never charged. The fee is charged
        // only now that every crewed check has passed; no request key (a crewed launch has no flight id).
        if (this.getFirstPassenger() instanceof ServerPlayer pilot && level() instanceof ServerLevel serverLevel) {
            boolean toStation = ModDimensions.STATION_LEVEL.equals(selectedDestination());
            LaunchCooldowns.Gate gate = LaunchCooldowns.gate(pilot.getUUID(), level().getGameTime(),
                    DescentMath.LAUNCH_COOLDOWN,
                    () -> za.co.neroland.nerospace.compat.neroeconomy.NerospaceEconomy.chargeFlight(
                            serverLevel.getServer(), pilot.getUUID(), toStation, null, null));
            if (gate.verdict() == LaunchCooldowns.Verdict.COOLDOWN) {
                pilot.sendSystemMessage(Component.translatable("entity.nerospace.rocket.launch_cooldown"));
                return;
            }
            String refused = gate.refusal();
            if (gate.verdict() == LaunchCooldowns.Verdict.REFUSED && refused != null) {
                pilot.sendSystemMessage(Component.translatable("message.nerospace.economy.launch_refused"));
                pilot.sendSystemMessage(Component.translatable(refused));
                return;
            }
        }
        // Remember the pad we're lifting off from so the return rocket can fly straight back to it.
        this.launchOriginDim = level().dimension();
        BlockPos originPad = validPadScanOrigin();
        this.launchOriginPad = originPad != null ? originPad.immutable() : blockPosition();
        beginAscent();
    }

    /**
     * Starts the ascent animation with no gating of its own — {@link #startLaunch} calls this after the
     * crewed checks pass, and the uncrewed cargo rocket calls it after the Cargo Pad has validated and
     * recorded the flight. Server-side.
     */
    protected void beginAscent() {
        setLaunching(true);
        this.launchTicks = 0;
        level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 3.0F, 0.6F);
    }

    /**
     * Called once the ascent counter reaches {@link #LAUNCH_DURATION}. The crewed rocket transports its
     * rider and lands a fresh rocket at the destination; the cargo rocket overrides this to simply vanish,
     * because its flight is a server-side record that materialises a rocket on arrival.
     */
    protected void finishAscent() {
        completeLaunch();
    }

    @Override
    public void tick() {
        super.tick();

        // Advance the client-side interpolation toward the latest server position (vanilla vehicle pattern).
        //? if <26.3 {
        if (level().isClientSide() && this.interpolation.hasActiveInterpolation()) {
            this.interpolation.interpolate();
        }
        //?}

        if (level().isClientSide()) {
            tickPresentation();
        }

        if (getPhase() != DescentMath.Phase.IDLE) {
            if (level() instanceof ServerLevel serverLevel) {
                tickDescent(serverLevel);
            } else if (isDescending()) {
                spawnLaunchParticles();
            }
            return;
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
                    finishAscent();
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
    protected void dropFuelInput() {
        if (level() instanceof ServerLevel server) {
            ItemStack stack = this.fuelInput.removeItemNoUpdate(0);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(server, stack);
            }
        }
    }

    /** Sets the pad this rocket should fly back to (its launch origin), so a return trip lands there. */
    public void setReturnTarget(@Nullable net.minecraft.resources.ResourceKey<Level> dim, @Nullable BlockPos pos) {
        this.returnDim = dim != null ? dim.identifier().toString() : "";
        this.returnPos = pos != null ? pos.immutable() : null;
    }

    /**
     * Client-side exhaust: a pad cloud rolling outward during ignition, then flame at the bells and a
     * smoke column trailing below the climbing hull. The flame itself is the renderer's plume model.
     */
    protected void spawnLaunchParticles() {
        double s = visualScale();
        double bx = this.getX();
        double by = this.getY();
        double bz = this.getZ();
        if (this.launchAnim < IGNITION_TICKS + 10) {
            for (int i = 0; i < 6; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double r = 0.3D + this.random.nextDouble() * 0.5D * s;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                level().addParticle(ParticleTypes.CLOUD, bx + dx * r, by + 0.15D, bz + dz * r,
                        dx * 0.12D * s, 0.01D, dz * 0.12D * s);
            }
        }
        double ign = ignition(1.0F);
        for (int i = 0; i < 3; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.3D * s;
            double oz = (this.random.nextDouble() - 0.5D) * 0.3D * s;
            level().addParticle(ParticleTypes.FLAME, bx + ox, by + 0.1D * s, bz + oz,
                    0.0D, -0.25D - 0.2D * this.random.nextDouble(), 0.0D);
            level().addParticle(ParticleTypes.LARGE_SMOKE, bx + ox * 2.0D, by - 0.6D * s * ign, bz + oz * 2.0D,
                    0.0D, -0.08D, 0.0D);
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
                // A rocket reloaded mid-ascent keeps its saved origin; a 1.6.0 in-flight one (no saved origin)
                // falls back to where it is now, so the arriving rocket always has a way back.
                net.minecraft.resources.ResourceKey<Level> savedDim = this.launchOriginDim;
                BlockPos savedPad = this.launchOriginPad;
                net.minecraft.resources.ResourceKey<Level> originDim = savedDim != null ? savedDim : current.dimension();
                BlockPos originPad = savedPad != null ? savedPad : blockPosition();
                LandingResolverLevelAdapter.Origin origin = originPad != null && current.dimension().equals(originDim)
                        ? LandingResolverLevelAdapter.origin(current, originPad, originPad)
                        : LandingResolverLevelAdapter.Origin.NONE;

                // Station branch: resolve the slot (charter on arrival) before the transfer.
                boolean station = LandingResolverLevelAdapter.isStation(targetKey);
                StationRegistry.StationEntry entry = null;
                BlockPos stationCentre = null;
                int slot = -1;
                if (station) {
                    entry = stationForArrival(server, player);
                    slot = entry != null ? entry.slot() : -1;
                    stationCentre = entry != null ? entry.center() : new BlockPos(0, PLATFORM_Y, 0);
                }
                LandingResolverLevelAdapter.Flight flight = new LandingResolverLevelAdapter.Flight(targetKey,
                        station ? -1 : getPadIndex(), this.returnDim, this.returnPos, origin.name(), origin.registered(),
                        player, slot, stationCentre);
                LandingDecision decision = LandingResolverLevelAdapter.resolve(destination, flight, Set.of());
                LandingResolverLevelAdapter.prepare(destination, decision);
                NerospaceTelemetry.breadcrumb("rocket", "landing.step=" + landingStep(decision));

                Vec3 spawn = decision.spawn();
                RocketEntity arriving = new RocketEntity(destination, spawn.x,
                        spawn.y + DescentMath.DESCENT_HEIGHT, spawn.z, getTier());
                if (carriedFuel > 0) {
                    arriving.addFuel(carriedFuel);
                }
                arriving.setReturnTarget(originDim, originPad); // remember where the rider came from
                arriving.visitor = StationVisitorSender.isVisitor(station, entry != null, true,
                        entry != null && StationRegistry.canUse(entry, player));
                arriving.beginDescent(decision, flight, player, arrivalMessage(decision, targetKey, entry), getOxygen());
                destination.addFreshEntity(arriving);

                player.stopRiding();
                player.teleportTo(destination, spawn.x, spawn.y + DescentMath.DESCENT_HEIGHT + 1.0D, spawn.z,
                        Set.of(), decision.yaw(), player.getXRot(), true);
                player.startRiding(arriving);
                player.sendOverlayMessage(Component.translatable("entity.nerospace.rocket.descending"));
                // Life support: the rider is in vacuum during the descent, so the reserve is granted now.
                za.co.neroland.nerospace.world.OxygenManager.grantArrivalReserve(player, getOxygen());
            }
        }

        // The in-flight rocket is expended; the arriving rocket carries the fuel left over.
        dropFuelInput();
        this.discard();
    }

    /** The integer landing step for telemetry (1–4 pad rules, 5 pod, 6 station dock). No names. */
    private static int landingStep(LandingDecision decision) {
        if (decision instanceof LandingDecision.PadLanding pad) {
            return pad.step();
        }
        return decision instanceof LandingDecision.PodLanding ? 5 : 6;
    }

    /**
     * The station this arrival docks at. "New Station" (−2) founds one on arrival and takes a charter; a 1.6.0
     * in-flight rocket is honoured once for free; no charter or a full registry → the origin
     * platform with a message. Runs before the dimension transfer so an aborted descent still ends on it.
     */
    @Nullable
    private StationRegistry.StationEntry stationForArrival(MinecraftServer server, ServerPlayer player) {
        int slot = getStationSlot();
        StationRegistry registry = StationRegistry.get(server);
        if (slot >= 0) {
            return registry.get(slot);
        }
        if (slot != -2) {
            return null;
        }
        boolean legacy = !this.charterRuleAware;
        StationFounding.Outcome outcome = StationFounding.resolve(legacy, hasCharter(player), registry.isFull());
        switch (outcome) {
            case REDIRECT -> {
                player.sendSystemMessage(Component.translatable("entity.nerospace.rocket.no_charter"));
                return null;
            }
            case REDIRECT_FULL -> {
                player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
                return null;
            }
            default -> {
            }
        }
        StationRegistry.StationEntry entry = registry.found(null, player.getUUID().toString());
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
            return null;
        }
        if (outcome == StationFounding.Outcome.FOUND_CONSUME) {
            consumeCharter(player);
        } else {
            NerospaceCommon.LOGGER.info("[nerospace/station] legacy found-new honoured");
        }
        NerospaceTelemetry.breadcrumb("station", "founded slot=" + entry.slot());
        za.co.neroland.nerospace.link.NerospaceLinkEvents.stationStateChanged(server, entry.slot(), "FOUNDED");
        za.co.neroland.nerospace.progression.StarGuideGrants.driveFirstColony(player);
        return entry;
    }

    /** The arrival line for a decision (pad name, station name, or the plain arrival text). */
    private static Component arrivalMessage(LandingDecision decision, net.minecraft.resources.ResourceKey<Level> dest,
            @Nullable StationRegistry.StationEntry entry) {
        if (decision instanceof LandingDecision.StationDock || LandingResolverLevelAdapter.isStation(dest)) {
            return entry != null
                    ? Component.translatable("entity.nerospace.rocket.station_arrived", entry.name())
                    : Component.translatable("entity.nerospace.rocket.docked");
        }
        if (decision instanceof LandingDecision.PadLanding pad && pad.pad().id() >= 0) {
            return Component.translatable("entity.nerospace.rocket.pad_arrived", pad.pad().name());
        }
        return dest.equals(Level.OVERWORLD)
                ? Component.translatable("entity.nerospace.rocket.home_arrived")
                : Component.translatable("entity.nerospace.rocket.arrived");
    }

    // --- Descent state machine ---------------------------------------------------------

    /** Arms this freshly spawned arrival rocket: it will descend 48 blocks onto {@code decision}'s pose. */
    private void beginDescent(LandingDecision decision, LandingResolverLevelAdapter.Flight flight,
            @Nullable ServerPlayer rider, Component message, int oxygen) {
        setTarget(decision);
        this.descentFlight = flight;
        this.descentRider = rider != null ? rider.getUUID() : null;
        this.arrivalMessage = message;
        this.arrivalPadMissing = decision.flags().padMissing();
        this.arrivalPadOccupied = decision.flags().padOccupied();
        this.arrivalOxygen = oxygen;
        this.arrivalOrigin = flight.destination();
        this.arrivalDrivesDone = false;
        this.noPhysics = true;
        this.setYRot(decision.yaw());
        this.yRotO = decision.yaw();
        setPhase(DescentMath.Phase.DESCENDING);
    }

    private void setTarget(LandingDecision decision) {
        this.descentTarget = decision.spawn();
        this.descentYaw = decision.yaw();
        this.descentKind = LandingResolverLevelAdapter.targetKind(decision);
        this.descentBlock = LandingResolverLevelAdapter.standBlock(decision);
        this.descentNode = decision.nodeId();
    }

    private boolean descentRetargeted;
    /** Set once a touchdown has moved this rocket to a pod because its target was taken (never loops). */
    private boolean descentPodFallback;

    /** One server tick of the state machine. */
    private void tickDescent(ServerLevel level) {
        DescentMath.Phase phase = getPhase();
        int t = getPhaseTick() + 1;
        this.entityData.set(DATA_PHASE_TICK, t);
        switch (phase) {
            case DESCENDING -> {
                Vec3 target = this.descentTarget;
                if (target == null) {
                    setPhase(DescentMath.Phase.IDLE);
                    return;
                }
                reseatRider(level);
                BlockPos block = this.descentBlock;
                if (DescentMath.checkPadThisTick(t) && block != null) {
                    // Re-checked on every pad check: a parked rocket in a cold chunk only appears once the chunk's
                    // entities have loaded, which happens some ticks after the blocks the resolver read.
                    boolean present = LandingResolverLevelAdapter.targetPresent(level, this.descentKind, block);
                    boolean taken = present
                            && LandingResolverLevelAdapter.targetOccupied(level, this.descentKind, block, this);
                    if (LandingResolverLevelAdapter.mustLeaveTarget(present, taken)) {
                        retarget(level, taken);
                    }
                }
                target = this.descentTarget;
                if (target == null) {
                    return;
                }
                double nx = approach(getX(), target.x, DescentMath.SLIDE_SPEED);
                double nz = approach(getZ(), target.z, DescentMath.SLIDE_SPEED);
                boolean over = nx == target.x && nz == target.z;
                double ny = over ? Math.max(target.y, getY() - DescentMath.DESCENT_SPEED) : getY();
                this.setPos(nx, ny, nz);
                this.setDeltaMovement(Vec3.ZERO);
                if ((t & 1) == 0) {
                    level.sendParticles(ParticleTypes.FLAME, nx, ny - 0.2D, nz, 1, 0.15D, 0.05D, 0.15D, 0.02D);
                }
                DescentMath.Phase next = over ? DescentMath.step(t, ny - target.y)
                        : (t >= DescentMath.DESCENT_TIMEOUT ? DescentMath.Phase.ABORTED : DescentMath.Phase.DESCENDING);
                BlockPos at = this.descentBlock;
                if (next == DescentMath.Phase.TOUCHDOWN && at != null
                        && !LandingResolverLevelAdapter.entitiesReady(level, at)) {
                    next = DescentMath.Phase.DESCENDING; // hover until the target's entities are known
                }
                if (next == DescentMath.Phase.TOUCHDOWN) {
                    touchdown(level, false);
                } else if (next == DescentMath.Phase.ABORTED) {
                    touchdown(level, true);
                }
            }
            case ABORTED -> touchdown(level, true); // a mid-descent rocket loaded from disk
            case TOUCHDOWN -> {
                if (t >= DescentMath.SETTLE_TICKS) {
                    setPhase(DescentMath.Phase.LANDED);
                }
            }
            case LANDED -> setPhase(DescentMath.Phase.IDLE);
            default -> {
            }
        }
    }

    private static double approach(double from, double to, double step) {
        double d = to - from;
        return Math.abs(d) <= step ? to : from + Math.signum(d) * step;
    }

    /** Keeps the rider seated for the whole descent (sneak-dismount is undone next tick). */
    private void reseatRider(ServerLevel level) {
        java.util.UUID id = this.descentRider;
        if (id == null) {
            return;
        }
        ServerPlayer rider = level.getServer().getPlayerList().getPlayer(id);
        if (rider != null && rider.isAlive() && rider.level() == level && rider.getVehicle() != this) {
            rider.startRiding(this);
        }
        if (rider != null && rider.level() == level && !this.arrivalDrivesDone) {
            // The rider has reached the destination: the Star Guide drives run while they are certainly present, so
            // a disconnect mid-descent cannot lose the progression (the arrival message may be lost; that is fine).
            arrivalDrives(level, rider);
        }
    }

    /** Neroland Core progression (one-directional): reaching orbit; far planets open deep space. Once per descent. */
    private void arrivalDrives(ServerLevel level, ServerPlayer player) {
        if (this.arrivalDrivesDone) {
            return;
        }
        this.arrivalDrivesDone = true;
        za.co.neroland.nerospace.progression.StarGuideGrants.driveReachedOrbit(player);
        var arrivedPlanet = za.co.neroland.nerospace.world.planet.Planets.get(level.dimension());
        if (arrivedPlanet != null && arrivedPlanet.unlockTier() >= RocketTier.TIER_3.level()) {
            za.co.neroland.nerospace.progression.StarGuideGrants.driveDeepSpace(player);
        }
    }

    /** The landing target broke mid-descent: re-resolve once without it and slide over. */
    private void retarget(ServerLevel level, boolean occupied) {
        LandingResolverLevelAdapter.Flight flight = this.descentFlight;
        if (this.descentRetargeted || flight == null) {
            return;
        }
        this.descentRetargeted = true;
        LandingResolverLevelAdapter.Flight auto = new LandingResolverLevelAdapter.Flight(flight.destination(), -1,
                flight.returnDim(), flight.returnPos(), flight.originPadName(), flight.originRegistered(),
                flight.rider(), flight.stationSlot(), flight.stationCentre());
        Set<Integer> excluded = this.descentNode >= 0 ? Set.of(this.descentNode) : Set.of();
        LandingDecision decision = LandingResolverLevelAdapter.resolve(level, auto, excluded);
        LandingResolverLevelAdapter.prepare(level, decision);
        setTarget(decision);
        if (occupied) {
            this.arrivalPadOccupied = true;
        } else {
            this.arrivalPadMissing = true;
        }
        this.setYRot(decision.yaw());
    }

    /** {@code TOUCHDOWN} (or the {@code ABORTED} failsafe): settle, effects, release the rider, arrival side-effects. */
    private void touchdown(ServerLevel level, boolean aborted) {
        Vec3 saved = this.descentTarget;
        BlockPos at = this.descentBlock;
        if (!this.descentPodFallback && saved != null && at != null
                && LandingResolverLevelAdapter.targetOccupied(level, this.descentKind, at, this)) {
            // Another rocket holds the target: never stack, fall back to a Landing Pod and keep descending.
            this.descentPodFallback = true;
            this.descentRetargeted = true;
            LandingDecision pod = LandingResolverLevelAdapter.podFallback(level, this.descentFlight, at);
            LandingResolverLevelAdapter.prepare(level, pod);
            setTarget(pod);
            this.arrivalPadOccupied = true;
            this.setYRot(pod.yaw());
            this.setPos(getX(), Math.max(getY(), pod.spawn().y), getZ());
            this.noPhysics = true;
            setPhase(DescentMath.Phase.DESCENDING);
            return;
        }
        Vec3 target = saved != null ? saved : position();
        this.noPhysics = false;
        this.setPos(target.x, target.y, target.z);
        if (aborted) {
            for (int up = 0; up < DescentMath.ABORT_MAX_PUSH && !level.noCollision(this, getBoundingBox()); up++) {
                this.setPos(target.x, getY() + 1.0D, target.z);
            }
        }
        this.setYRot(this.descentYaw);
        setPhase(DescentMath.Phase.TOUCHDOWN);
        this.parkedSince = level.getGameTime();
        touchdownEffects(level);

        ServerPlayer rider = null;
        java.util.UUID id = this.descentRider;
        if (id != null) {
            rider = level.getServer().getPlayerList().getPlayer(id);
        }
        if (rider == null && getFirstPassenger() instanceof ServerPlayer seated) {
            rider = seated;
        }
        this.descentRider = null;
        this.descentFlight = null;
        if (rider != null && rider.level() == level) {
            rider.stopRiding();
            rider.teleportTo(level, getX(), getY() + 1.0D, getZ(), Set.of(), this.descentYaw, rider.getXRot(), true);
            arrivalSideEffects(level, rider);
        }
    }

    private void touchdownEffects(ServerLevel level) {
        BlockPos savedBlock = this.descentBlock;
        BlockPos block = savedBlock != null ? savedBlock : blockPosition();
        BlockState under = level.getBlockState(block);
        if (this.descentKind == PadKind.DOCK) {
            BlockPos centre = under.getBlock() instanceof DockingRingBlock
                    ? under.getValue(DockingRingBlock.PART).centreFrom(block) : block;
            DockingRingBlock.flash(level, centre);
        } else if (this.descentKind == PadKind.POD && under.is(za.co.neroland.nerospace.registry.ModBlocks.LANDING_POD.get())
                && !under.getValue(ReturnSiteBlock.INFLATED)) {
            level.setBlockAndUpdate(block, under.setValue(ReturnSiteBlock.INFLATED, true));
        }
        BlockState dust = level.getBlockState(block.below());
        if (dust.isAir()) {
            dust = under.isAir() ? net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState() : under;
        }
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, dust),
                getX(), getY() + 0.1D, getZ(), 24, 1.2D, 0.1D, 1.2D, 0.15D);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5F, 0.5F);
    }

    /** Arrival message, link event and Star Guide drives — at touchdown, not before the descent. */
    private void arrivalSideEffects(ServerLevel level, ServerPlayer player) {
        if (this.arrivalPadMissing) {
            player.sendSystemMessage(Component.translatable("entity.nerospace.rocket.pad_missing"));
        }
        if (this.arrivalPadOccupied) {
            player.sendSystemMessage(Component.translatable("entity.nerospace.rocket.pad_occupied"));
        }
        Component message = this.arrivalMessage;
        player.sendSystemMessage(message != null ? message : Component.translatable("entity.nerospace.rocket.arrived"));
        net.minecraft.resources.ResourceKey<Level> dim = level.dimension();
        // NeroLink: an owner-scoped rocket_landed push. Best-effort — the publisher swallows its own failures.
        za.co.neroland.nerospace.link.NerospaceLinkEvents.rocketLanded(player, dim, getTier(),
                getFuel(), this.arrivalOxygen, blockPosition());
        arrivalDrives(level, player); // no-op when the descent already ran them
        this.arrivalMessage = null;
        this.arrivalPadMissing = false;
        this.arrivalPadOccupied = false;
    }

    /**
     * Station console "Send visitor rocket home": a riderless <em>visitor</em> rocket at rest flies
     * uncrewed back to its own return pad (or a Landing Pod by the normal resolver in its origin dimension). A rocket
     * with a rider, or one that is not flagged {@code Visitor} (the owner's or a member's own), is never moved.
     * @return whether it left.
     */
    public boolean sendHomeUncrewed() {
        if (!(level() instanceof ServerLevel current) || !this.visitor || isLaunching() || isDescending()
                || !getPassengers().isEmpty() || this.returnDim.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceKey<Level> home = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(this.returnDim));
        ServerLevel destination = current.getServer().getLevel(home);
        if (destination == null) {
            return false;
        }
        LandingResolverLevelAdapter.Flight flight = new LandingResolverLevelAdapter.Flight(home, -1, this.returnDim,
                this.returnPos, java.util.Optional.empty(), false, null, -1,
                LandingResolverLevelAdapter.isStation(home) ? new BlockPos(0, PLATFORM_Y, 0) : null);
        LandingDecision decision = LandingResolverLevelAdapter.resolve(destination, flight, Set.of());
        LandingResolverLevelAdapter.prepare(destination, decision);
        // Rockets carry no owner, so nobody is messaged; the log names the action and station slot only.
        int slot = -1;
        for (StationRegistry.StationEntry e : StationRegistry.get(current.getServer()).all()) {
            if (LandingResolver.chebyshev(e.center(), blockPosition()) <= StationTarget.CLAIM_RADIUS) {
                slot = e.slot();
            }
        }
        NerospaceCommon.LOGGER.info("[nerospace/station] action=send_visitor_home slot={}", slot);
        Vec3 spawn = decision.spawn();
        RocketEntity arriving = new RocketEntity(destination, spawn.x, spawn.y + DescentMath.DESCENT_HEIGHT, spawn.z,
                getTier());
        arriving.fuelTank.setRaw(this.fuelTank.getRawFluid(), this.fuelTank.getRawAmount());
        arriving.syncFuel();
        arriving.beginDescent(decision, flight, null, Component.empty(), 0);
        destination.addFreshEntity(arriving);
        dropFuelInput();
        this.discard();
        return true;
    }

    // --- Interaction --------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (isLaunching() || isDescending()) {
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
                            StationRegistry.get(serverPlayer.level().getServer()), serverPlayer));
            za.co.neroland.nerospace.network.ModNetwork.sendToPlayer(serverPlayer,
                    za.co.neroland.nerospace.network.PadSyncPayload.of(
                            PadRegistry.get(serverPlayer.level().getServer())));
            return MenuOpener.openOrConsume(serverPlayer, this);
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
        if (this.isRemoved() || isLaunching() || isDescending()) {
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
        // 2.0 fields — all optional, so a 1.6.0 rocket loads as before ([COMPAT] 4).
        // A parked 1.6.0 rocket follows the 2.0 charter rule; only one already in flight is honoured once.
        // The field is absent exactly when the two defaults disagree.
        boolean ruleIfAbsentFalse = input.getBooleanOr("CharterRule", false);
        boolean absent = !ruleIfAbsentFalse && input.getBooleanOr("CharterRule", true);
        this.charterRuleAware = StationFounding.charterRuleAware(absent ? null : Boolean.valueOf(ruleIfAbsentFalse),
                input.getBooleanOr("Launching", false));
        this.visitor = input.getBooleanOr("Visitor", false);
        String originDim = input.getStringOr("LaunchOriginDim", "");
        if (!originDim.isEmpty() && input.getBooleanOr("HasLaunchOrigin", false)) {
            this.launchOriginDim = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(originDim));
            this.launchOriginPad = new BlockPos(input.getIntOr("LaunchOriginX", 0), input.getIntOr("LaunchOriginY", 0),
                    input.getIntOr("LaunchOriginZ", 0));
        }
        this.parkedSince = input.getLongOr("ParkedSince", -1L);
        this.arrivalOxygen = input.getIntOr("ArrivalOxygen", 0);
        DescentMath.Phase saved = DescentMath.Phase.byName(input.getStringOr("Phase", ""));
        if (input.getBooleanOr("HasDescent", false)) {
            this.descentTarget = new Vec3(input.getDoubleOr("DescentX", getX()), input.getDoubleOr("DescentY", getY()),
                    input.getDoubleOr("DescentZ", getZ()));
            this.descentYaw = input.getFloatOr("DescentYaw", 0.0F);
            this.descentKind = PadKind.byName(input.getStringOr("DescentKind", ""));
            BlockPos at = BlockPos.containing(this.descentTarget);
            this.descentBlock = this.descentKind == PadKind.POD ? at.below() : at;
            this.descentNode = input.getIntOr("DescentNode", -1);
        }
        // A rocket saved mid-descent finishes at once: it is never found hovering.
        DescentMath.Phase resumed = this.descentTarget == null && saved != DescentMath.Phase.IDLE
                ? DescentMath.Phase.IDLE : DescentMath.onLoad(saved);
        this.entityData.set(DATA_PHASE, resumed.ordinal());
        this.entityData.set(DATA_PHASE_TICK, 0);
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
        output.putBoolean("CharterRule", true);
        output.putBoolean("Visitor", this.visitor);
        net.minecraft.resources.ResourceKey<Level> originDim = this.launchOriginDim;
        BlockPos originPad = this.launchOriginPad;
        output.putBoolean("HasLaunchOrigin", originDim != null && originPad != null);
        if (originDim != null && originPad != null) {
            output.putString("LaunchOriginDim", originDim.identifier().toString());
            output.putInt("LaunchOriginX", originPad.getX());
            output.putInt("LaunchOriginY", originPad.getY());
            output.putInt("LaunchOriginZ", originPad.getZ());
        }
        output.putLong("ParkedSince", this.parkedSince);
        output.putInt("ArrivalOxygen", this.arrivalOxygen);
        output.putString("Phase", getPhase().name());
        Vec3 target = this.descentTarget;
        output.putBoolean("HasDescent", target != null);
        if (target != null) {
            output.putDouble("DescentX", target.x);
            output.putDouble("DescentY", target.y);
            output.putDouble("DescentZ", target.z);
            output.putFloat("DescentYaw", this.descentYaw);
            output.putString("DescentKind", this.descentKind.name());
            output.putInt("DescentNode", this.descentNode);
        }
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.rocket");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RocketMenu(containerId, playerInventory, this, viewerData(player));
    }

    /**
     * The menu's synced data for one viewer: the rocket's shared values plus [14] {@code destinationGateMask}
     * (a bit per {@link Destinations} index whose Core gate is still closed for this player).
     */
    private ContainerData viewerData(Player player) {
        ContainerData shared = this.dataAccess;
        return new ContainerData() {
            /** Game time of the last refresh; the two per-viewer values are recomputed at most every 20 ticks. */
            private long refreshedAt = Long.MIN_VALUE;
            private int gate;
            private int reveal;

            private void refresh() {
                long now = player.level().getGameTime();
                if (now - this.refreshedAt < VIEWER_DATA_REFRESH_TICKS && now >= this.refreshedAt) {
                    return;
                }
                this.refreshedAt = now;
                this.gate = gateMask(player);
                this.reveal = player instanceof ServerPlayer sp
                        ? za.co.neroland.nerospace.world.station.StationObservatory.revealFor(sp) : 0;
            }

            @Override
            public int get(int index) {
                if (index == RocketMenu.DATA_GATE_MASK) {
                    refresh();
                    return this.gate;
                }
                if (index == RocketMenu.DATA_REVEAL) {
                    refresh();
                    return this.reveal;
                }
                return shared.get(index);
            }

            @Override
            public void set(int index, int value) {
                // Read-only from the client.
            }

            @Override
            public int getCount() {
                return RocketMenu.DATA_COUNT;
            }
        };
    }

    /** Bit per destination index whose planet's Core gate is closed for {@code player} (server-side). */
    public static int gateMask(Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            return 0;
        }
        int mask = 0;
        java.util.List<net.minecraft.resources.ResourceKey<Level>> all = Destinations.all();
        for (int i = 0; i < all.size(); i++) {
            var planet = za.co.neroland.nerospace.world.planet.Planets.get(all.get(i));
            if (planet != null && planet.coreGate().isPresent()
                    && !za.co.neroland.nerolandcore.progression.ProgressionGates.isOpen(sp, planet.coreGate().get())) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /** Exposed for the menu's server constructor. */
    public ContainerData getDataAccess() {
        return this.dataAccess;
    }
}
