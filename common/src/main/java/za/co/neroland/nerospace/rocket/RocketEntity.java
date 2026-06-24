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
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

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

    /** Ticks of ascent before the rider is transported. */
    public static final int LAUNCH_DURATION = 100;
    /** One fuel canister is worth this many millibuckets. */
    public static final int CANISTER_MB = 1_000;
    /** Y level of the shared Orbital Station origin platform (the founded-station system is deferred). */
    public static final int PLATFORM_Y = 64;

    private int launchTicks;

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
     * [5]=stationSlot (−1 = origin), [6]=destinationMask.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getFuel();
                case 1 -> getTier().fuelCapacity();
                case 2 -> getTier().ordinal();
                case 3 -> canLaunch() ? 1 : 0;
                case 4 -> getDestinationIndex();
                case 5 -> getStationSlot();
                case 6 -> destinationMask();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 6;
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
        if (!Destinations.isAvailable(getTier(), level().dimension(), getDestinationIndex())) {
            return null;
        }
        return Destinations.byIndex(getDestinationIndex());
    }

    /** Bitmask of global destinations currently available to this rocket. */
    public int destinationMask() {
        return Destinations.availableMask(getTier(), level().dimension());
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
                return;
            }
        }
    }

    /** Selects a global destination directly by index (server-side; from the trajectory buttons). */
    public void setDestinationIndex(int index) {
        if (level().isClientSide() || isLaunching()) {
            return;
        }
        if (Destinations.isAvailable(getTier(), level().dimension(), index)) {
            this.entityData.set(DATA_DEST, index);
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
        if (all.isEmpty()) {
            next = -1;
        } else if (current < 0) {
            next = all.get(0).slot();
        } else {
            int idx = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).slot() == current) {
                    idx = i;
                    break;
                }
            }
            next = (idx < 0 || idx + 1 >= all.size()) ? -1 : all.get(idx + 1).slot();
        }
        this.entityData.set(DATA_STATION, next);
    }

    public boolean isLaunching() {
        return this.entityData.get(DATA_LAUNCHING);
    }

    private void setLaunching(boolean launching) {
        this.entityData.set(DATA_LAUNCHING, launching);
    }

    // --- Fuel / launch logic ------------------------------------------------

    /** @return millibuckets of fuel that could not be accepted (overflow). Caps at the tier capacity. */
    public int addFuel(int amount) {
        int room = Math.max(0, getTier().fuelCapacity() - (int) this.fuelTank.getAmount());
        int toFill = Math.min(amount, room);
        int filled = (int) this.fuelTank.fill((Fluid) ModFluids.ROCKET_FUEL.get(), toFill, false);
        return amount - filled;
    }

    /** Whether a launch could be started right now (fuelled, has a rider, and a destination selected). */
    public boolean canLaunch() {
        return !isLaunching()
                && selectedDestination() != null
                && getFuel() >= requiredFuelForLaunch()
                && this.getFirstPassenger() instanceof Player
                && isOnValidPad();
    }

    private int requiredFuelForLaunch() {
        net.minecraft.resources.ResourceKey<Level> target = selectedDestination();
        if (target == null) {
            return Integer.MAX_VALUE;
        }
        int outbound = getTier().fuelPerLaunch();
        return Destinations.needsReturnReserve(target) ? outbound + returnReserveFuel() : outbound;
    }

    private int returnReserveFuel() {
        return getTier().fuelPerLaunch();
    }

    /**
     * Launch-pad gating (re-checked at launch): the rocket must stand ON a complete 3x3 pad that
     * contains the rocket's position. A Tier 3 rocket additionally needs that pad ringed with Station
     * Wall OR a Heavy Launch Complex; a Tier 4 rocket needs the Heavy Launch Complex specifically.
     */
    public boolean isOnValidPad() {
        if (isOnReturnSite()) {
            return true;
        }
        BlockPos origin = padScanOrigin();
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level(), origin);
        if (LaunchPadMultiblock.fullSquareCornerContaining(pads, 3, origin) == null) {
            return false;
        }
        if (getTier() == RocketTier.TIER_4) {
            return LaunchPadMultiblock.isHeavyComplexContaining(level(), pads, origin);
        }
        return getTier() != RocketTier.TIER_3
                || LaunchPadMultiblock.hasStationWallRingAround(level(), pads, origin)
                || LaunchPadMultiblock.isHeavyComplexContaining(level(), pads, origin);
    }

    /** Where to look for the pad under the rocket. The rocket stands ON the pad's 3px plate. */
    private BlockPos padScanOrigin() {
        BlockPos feet = this.blockPosition();
        return level().getBlockState(feet).getBlock() instanceof RocketLaunchPadBlock ? feet : feet.below();
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
                Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level(), padScanOrigin());
                String message;
                if (!LaunchPadMultiblock.isFullThreeByThree(pads)) {
                    message = "item.nerospace.rocket.pad_incomplete";
                } else if (getTier() == RocketTier.TIER_4) {
                    message = "item.nerospace.rocket.pad_heavy_required";
                } else {
                    message = "item.nerospace.rocket.pad_ring_required";
                }
                rider.sendSystemMessage(Component.translatable(message));
            }
            return;
        }
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
        net.minecraft.resources.ResourceKey<Level> targetKey = selectedDestination();
        if (targetKey == null) {
            setLaunching(false);
            return;
        }

        int outboundFuel = getTier().fuelPerLaunch();
        int carriedFuel = Math.max(0, getFuel() - outboundFuel);
        this.fuelTank.drain(outboundFuel, false);

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof ServerPlayer player && level() instanceof ServerLevel current) {
            MinecraftServer server = current.getServer();
            ServerLevel destination = server.getLevel(targetKey);
            if (destination != null) {
                player.stopRiding();

                BlockPos preferred;
                Component arrivalMessage;
                if (targetKey.equals(ModDimensions.STATION_LEVEL)) {
                    // Dock at the selected founded station, or the shared origin platform when none is chosen.
                    int slot = getStationSlot();
                    StationRegistry.StationEntry entry =
                            slot >= 0 ? StationRegistry.get(server).get(slot) : null;
                    preferred = entry != null ? entry.center() : new BlockPos(0, PLATFORM_Y, 0);
                    arrivalMessage = entry != null
                            ? Component.translatable("entity.nerospace.rocket.station_arrived", entry.name())
                            : Component.translatable("entity.nerospace.rocket.docked");
                } else {
                    int blockX = Mth.floor(player.getX());
                    int blockZ = Mth.floor(player.getZ());
                    preferred = new BlockPos(blockX, 0, blockZ);
                    arrivalMessage = targetKey.equals(Level.OVERWORLD)
                            ? Component.translatable("entity.nerospace.rocket.home_arrived")
                            : Component.translatable("entity.nerospace.rocket.arrived");
                }

                ReturnSitePlacement.Arrival arrival =
                        ReturnSitePlacement.place(destination, targetKey, preferred, getTier(), carriedFuel);
                player.teleportTo(destination, arrival.x(), arrival.y(), arrival.z(),
                        Set.of(), player.getYRot(), player.getXRot(), true);
                player.sendSystemMessage(arrivalMessage);
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

        if (!level().isClientSide()) {
            if (this.getFirstPassenger() == null) {
                player.startRiding(this);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                // Push the founded-station names so the in-rocket "Dock:" cycler can label them.
                za.co.neroland.nerospace.network.ModNetwork.sendToPlayer(serverPlayer,
                        za.co.neroland.nerospace.network.StationSyncPayload.of(
                                StationRegistry.get(serverPlayer.level().getServer())));
                serverPlayer.openMenu(this);
            }
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
