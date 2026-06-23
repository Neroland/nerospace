package za.co.neroland.nerospace.rocket;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import za.co.neroland.nerospace.fluid.RocketFuelTank;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Nerospace rocket (Phase 4): a rideable vehicle entity placed on a {@link RocketLaunchPadBlock}.
 * It carries a {@link RocketTier} and a liquid-fuel tank (millibuckets). Right-clicking with a fuel
 * canister tops up the tank; right-clicking empty-handed mounts the rocket and opens its UI, whose
 * Launch button starts a simulated ascent that ends by transporting the rider to the tier's planet.
 *
 * <p>All gameplay logic is server-authoritative; the client only renders synced state and ascent
 * particles. The entity has no gravity and rests where placed, moving only under launch thrust.</p>
 */
public class RocketEntity extends Entity implements MenuProvider {

    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHING =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    /** Index into the current tier's destination list. */
    private static final EntityDataAccessor<Integer> DATA_DEST =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    /** Player-station selection: {@link #STATION_NONE}, {@link #STATION_FOUND}, or a slot id. */
    private static final EntityDataAccessor<Integer> DATA_STATION =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    /** No station selected — the planet destination ({@code DATA_DEST}) applies. */
    public static final int STATION_NONE = -1;
    /** The FOUND node: launch consumes a Station Charter and founds a new station. */
    public static final int STATION_FOUND = -2;

    /** Ticks of ascent before the rider is transported. */
    public static final int LAUNCH_DURATION = 100;
    /** One fuel canister is worth this many millibuckets. */
    public static final int CANISTER_MB = 1_000;

    private int launchTicks;

    /**
     * The authoritative fuel store: a {@link RocketFuelTank} (26.1 transfer-API handler) of
     * {@code rocket_fuel}, synced to the client via {@link #DATA_FUEL} for the GUI. Physical capacity
     * is the largest tier's; {@link #addFuel} caps top-ups to the current tier's capacity. The handler
     * surface is ready to expose via {@code Capabilities.Fluid.ENTITY} for pipe automation.
     */
    @SuppressWarnings("this-escape") // change-callback wiring, used only after construction
    private final RocketFuelTank fuelTank = new RocketFuelTank(maxTierFuelCapacity(), this::syncFuel);

    /** The largest tier's tank — the physical handler capacity (per-tier caps live in addFuel). */
    private static int maxTierFuelCapacity() {
        int max = 0;
        for (RocketTier tier : RocketTier.values()) {
            max = Math.max(max, tier.fuelCapacity());
        }
        return max;
    }

    /**
     * The single-slot fuel intake's authoritative store, as a transfer-API handler — exposed via
     * {@code Capabilities.Item.ENTITY} and proxied by the launch pad's {@code Capabilities.Item.BLOCK},
     * so pipes and hoppers can feed fuel containers in. Only fuel containers may be inserted;
     * extraction is open so automation can also collect the emptied buckets.
     *
     * <p>NOTE: {@code StacksResourceHandler} COPIES any list passed to its constructor (it never
     * shares a Container's backing list), so the handler owns the slot and {@link #fuelInput} is a
     * {@link net.minecraft.world.Container} view over it for the menu and the entity tick.</p>
     */
    private final IntakeHandler intakeHandler = new IntakeHandler();

    /**
     * Container view of {@link #intakeHandler} for the rocket UI and {@link #consumeFuelInput()}: the
     * player (or hopper/pipe automation) drops a {@link ModItems#ROCKET_FUEL_BUCKET} or
     * {@link ModItems#ROCKET_FUEL_CANISTER} here and the rocket drains it into {@link #fuelTank} on
     * its server tick — returning an empty bucket.
     */
    private final net.minecraft.world.Container fuelInput = new IntakeContainer();

    /** Single-slot intake store: validates fuel containers, exposes the slot to the Container view. */
    private final class IntakeHandler extends ItemStacksResourceHandler {

        private IntakeHandler() {
            super(1);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return isFuelContainer(resource.toStack(1));
        }

        ItemStack getStack() {
            return this.stacks.get(0);
        }

        void setStack(ItemStack stack) {
            this.stacks.set(0, stack);
        }
    }

    /** Vanilla-Container view over {@link IntakeHandler} (menus and ticks read/write the same slot). */
    private final class IntakeContainer implements net.minecraft.world.Container {

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return RocketEntity.this.intakeHandler.getStack().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return RocketEntity.this.intakeHandler.getStack();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return RocketEntity.this.intakeHandler.getStack().split(amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = RocketEntity.this.intakeHandler.getStack();
            RocketEntity.this.intakeHandler.setStack(ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            RocketEntity.this.intakeHandler.setStack(stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return !RocketEntity.this.isRemoved();
        }

        @Override
        public void clearContent() {
            RocketEntity.this.intakeHandler.setStack(ItemStack.EMPTY);
        }
    }

    /**
     * Synced to the menu: [0]=fuel, [1]=capacity, [2]=tierOrdinal, [3]=launchable,
     * [4]=destinationIndex, [5]=stationSelection (offset by 8 so the FOUND marker −2 survives the
     * unsigned-short container-data sync — see {@code RocketMenu.STATION_SYNC_OFFSET}).
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
                case 5 -> stationSelection() + RocketMenu.STATION_SYNC_OFFSET;
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

    /** Client-side position interpolation: without it the ascent (and the seated rider, who
     *  follows the vehicle) steps once per sync packet instead of moving smoothly. */
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

    // --- Per-tier presentation (cockpit rework) -------------------------------

    /** Render scale by tier — bigger boosters genuinely LOOK bigger (T1 keeps the old size). */
    public float visualScale() {
        return switch (getTier()) {
            case TIER_1 -> 1.6F;
            case TIER_2 -> 2.0F;
            case TIER_3 -> 2.4F;
            case TIER_4 -> 2.8F;
        };
    }

    /** The rider STANDS at the cockpit console instead of sitting on the hull. */
    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /**
     * Stand the rider so their eyes line up with the hull's window band (model y ≈ -11, i.e.
     * {@code (24+11)/16 = 2.1875} model-blocks above the fins, scaled by tier; standing eye
     * height is 1.62).
     */
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
        builder.define(DATA_STATION, STATION_NONE);
    }

    public int getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    /** Pushes the server-side tank amount into the synced data accessor (client GUI + canLaunch). */
    private void syncFuel() {
        if (!level().isClientSide()) {
            this.entityData.set(DATA_FUEL, this.fuelTank.getAmount());
        }
    }

    /** The fuel tank as a transfer-API handler (for a future {@code Capabilities.Fluid.ENTITY}). */
    public ResourceHandler<FluidResource> getFuelTank() {
        return this.fuelTank;
    }

    /** The UI fuel-intake slot (one bucket/canister), for the menu and automation. */
    public net.minecraft.world.Container getFuelInput() {
        return this.fuelInput;
    }

    /** The intake slot as a transfer-API handler ({@code Capabilities.Item.ENTITY} + pad proxy). */
    public ResourceHandler<ItemResource> getIntakeHandler() {
        return this.intakeHandler;
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
        this.entityData.set(DATA_DEST, tier.defaultDestinationIndex());
    }

    // --- Destination selection ----------------------------------------------

    public int getDestinationIndex() {
        return this.entityData.get(DATA_DEST);
    }

    /**
     * The currently selected destination level, or {@code null} if this tier can't fly anywhere.
     * Any station selection (a founded station or the FOUND marker) targets the shared station
     * dimension; the per-station coordinates resolve in {@link #completeLaunch()}.
     */
    @Nullable
    public net.minecraft.resources.ResourceKey<Level> selectedDestination() {
        if (stationSelection() != STATION_NONE) {
            return ModDimensions.STATION_LEVEL;
        }
        return getTier().destination(getDestinationIndex());
    }

    /** Cycles to the next destination available to this tier (server-side; from the menu button). */
    public void cycleDestination() {
        if (level().isClientSide() || isLaunching()) {
            return;
        }
        int count = getTier().destinations().size();
        if (count > 1) {
            this.entityData.set(DATA_DEST, Math.floorMod(getDestinationIndex() + 1, count));
            this.entityData.set(DATA_STATION, STATION_NONE);
        }
    }

    /** Selects a destination directly by index (server-side; from the trajectory buttons). */
    public void setDestinationIndex(int index) {
        if (level().isClientSide() || isLaunching()) {
            return;
        }
        int count = getTier().destinations().size();
        if (count > 0) {
            this.entityData.set(DATA_DEST, Math.floorMod(index, count));
            this.entityData.set(DATA_STATION, STATION_NONE);
        }
    }

    // --- Player stations (MULTI_STATION_DESIGN.md) ---------------------------

    /** {@link #STATION_NONE}, {@link #STATION_FOUND}, or the selected station's slot id. */
    public int stationSelection() {
        return this.entityData.get(DATA_STATION);
    }

    /** Selects a founded station by slot id (server-side; validated against the registry). */
    public void selectStation(int slot) {
        if (level().isClientSide() || isLaunching() || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (StationRegistry.get(server.getServer()).get(slot) != null) {
            this.entityData.set(DATA_STATION, slot);
        }
    }

    /** Cycles through founded stations in founding order (server-authoritative registry order). */
    public void cycleStation() {
        if (level().isClientSide() || isLaunching() || !(level() instanceof ServerLevel server)) {
            return;
        }
        int next = StationRegistry.get(server.getServer())
                .nextSlotAfter(Math.max(stationSelection(), STATION_NONE));
        if (next >= 0) {
            this.entityData.set(DATA_STATION, next);
        }
    }

    /** Selects the FOUND marker (server-side; the rider must carry a Station Charter). */
    public void selectFound() {
        if (level().isClientSide() || isLaunching() || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (this.getFirstPassenger() instanceof Player rider && hasCharter(rider)
                && !StationRegistry.get(server.getServer()).isFull()) {
            this.entityData.set(DATA_STATION, STATION_FOUND);
        }
    }

    /** Whether {@code player} carries at least one Station Charter. */
    public static boolean hasCharter(Player player) {
        return findCharter(player) >= 0;
    }

    /** Inventory slot of the first Station Charter, or −1. */
    private static int findCharter(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.STATION_CHARTER.get())) {
                return i;
            }
        }
        return -1;
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
        int room = Math.max(0, getTier().fuelCapacity() - this.fuelTank.getAmount());
        int toFill = Math.min(amount, room);
        int filled = this.fuelTank.fill(toFill);
        return amount - filled;
    }

    /** Whether a launch could be started right now (fuelled, has a rider, and a destination selected). */
    public boolean canLaunch() {
        return !isLaunching()
                && selectedDestination() != null
                && getFuel() >= getTier().fuelPerLaunch()
                && this.getFirstPassenger() instanceof Player
                && isOnValidPad();
    }

    /**
     * Launch-pad gating (re-checked at launch, so breaking pad blocks under a deployed rocket
     * grounds it): the rocket must stand ON a complete 3x3 pad — the square must CONTAIN the
     * rocket's position, so an intact square elsewhere in the connected cluster cannot keep a
     * degraded pad launchable. A Tier 3 rocket additionally needs that pad ringed with Station
     * Wall OR to stand within a Heavy Launch Complex (5x5 + gantry); a Tier 4 rocket needs the
     * Heavy Launch Complex specifically (NEW_DESTINATION_DESIGN.md §5 — no ring shortcut).
     */
    public boolean isOnValidPad() {
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

    /**
     * Where to look for the pad under the rocket. The rocket stands ON the pad's 3px plate, so its
     * feet share the pad's block position; standing on a full block puts the pad one below.
     */
    private BlockPos padScanOrigin() {
        BlockPos feet = this.blockPosition();
        return level().getBlockState(feet).getBlock() instanceof RocketLaunchPadBlock ? feet : feet.below();
    }

    /** Begins the ascent. Server-side; called from the rocket menu's Launch button. */
    public void startLaunch() {
        // Station selections re-validate against the live registry/inventory before ignition.
        if (!level().isClientSide() && level() instanceof ServerLevel server
                && this.getFirstPassenger() instanceof ServerPlayer rider) {
            int station = stationSelection();
            if (station == STATION_FOUND) {
                if (!hasCharter(rider)) {
                    rider.sendSystemMessage(Component.translatable("entity.nerospace.rocket.no_charter"));
                    this.entityData.set(DATA_STATION, STATION_NONE);
                    return;
                }
                if (StationRegistry.get(server.getServer()).isFull()) {
                    rider.sendSystemMessage(Component.translatable("entity.nerospace.rocket.stations_full"));
                    this.entityData.set(DATA_STATION, STATION_NONE);
                    return;
                }
            } else if (station >= 0
                    && StationRegistry.get(server.getServer()).get(station) == null) {
                rider.sendSystemMessage(Component.translatable("entity.nerospace.rocket.station_missing"));
                this.entityData.set(DATA_STATION, STATION_NONE);
                return;
            }
        }
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

        // Advance the client-side interpolation toward the latest server position (vanilla
        // vehicle pattern) — this is what actually smooths the ascent for the rider.
        if (level().isClientSide() && this.interpolation.hasActiveInterpolation()) {
            this.interpolation.interpolate();
        }

        if (isLaunching()) {
            if (level().isClientSide()) {
                // The client only draws particles; it interpolates the rocket's position from the
                // server's movement updates, which keeps the ascent smooth (no double-stepping).
                spawnLaunchParticles();
            } else {
                // Smooth eased acceleration, applied server-side only.
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
            // Idle on the pad: drain any fuel container the player/automation dropped in the intake.
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

        this.fuelTank.drain(getTier().fuelPerLaunch());

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof ServerPlayer player && level() instanceof ServerLevel current) {
            MinecraftServer server = current.getServer();
            ServerLevel destination = server.getLevel(targetKey);
            if (destination != null) {
                player.stopRiding();

                double arrivalX;
                double arrivalY;
                double arrivalZ;
                Component arrivalMessage;
                if (targetKey.equals(ModDimensions.STATION_LEVEL)) {
                    // Origin = the shared public platform; player stations resolve their slot's
                    // offset (MULTI_STATION_DESIGN.md §2); FOUND allocates a fresh slot and
                    // anchors it with a Station Core. Platforms build once — never restacked.
                    int station = stationSelection();
                    BlockPos centre = new BlockPos(0, StationRegistry.PLATFORM_Y, 0);
                    arrivalMessage = Component.translatable("entity.nerospace.rocket.docked");
                    if (station == STATION_FOUND) {
                        StationRegistry.StationEntry founded = foundStation(server, destination, player);
                        if (founded != null) {
                            centre = founded.center();
                            arrivalMessage = Component.translatable(
                                    "entity.nerospace.rocket.founded", founded.name());
                        }
                    } else if (station >= 0) {
                        StationRegistry.StationEntry entry =
                                StationRegistry.get(server).get(station);
                        if (entry != null) {
                            centre = entry.center();
                            arrivalMessage = Component.translatable(
                                    "entity.nerospace.rocket.station_arrived", entry.name());
                        } else {
                            arrivalMessage = Component.translatable(
                                    "entity.nerospace.rocket.station_missing");
                        }
                    }
                    destination.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
                    if (!destination.getBlockState(centre).is(ModBlocks.STATION_FLOOR.get())
                            && !destination.getBlockState(centre).is(ModBlocks.STATION_CORE.get())) {
                        buildStationPlatform(destination, centre.getX(), centre.getY(), centre.getZ());
                    }
                    arrivalX = centre.getX() + 0.5D;
                    arrivalY = centre.getY() + 1.0D;
                    arrivalZ = centre.getZ() + 0.5D;
                } else {
                    int blockX = Mth.floor(player.getX());
                    int blockZ = Mth.floor(player.getZ());
                    destination.getChunk(blockX >> 4, blockZ >> 4);
                    arrivalX = player.getX();
                    arrivalZ = player.getZ();
                    arrivalY = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ) + 1.0D;
                    arrivalMessage = Component.translatable("entity.nerospace.rocket.arrived");
                }

                player.teleportTo(destination, arrivalX, arrivalY, arrivalZ, Set.of(), player.getYRot(), player.getXRot(), true);
                player.sendSystemMessage(arrivalMessage);
            }
        }

        // The rocket is expended on launch; a return trip needs a pad + rocket on the destination.
        dropFuelInput();
        this.discard();
    }

    /**
     * Founds a new station (MULTI_STATION_DESIGN.md §1): consumes one Station Charter from the
     * rider (its anvil name becomes the station name), allocates the next slot, builds the
     * platform with a bound Station Core at its centre, and fires the {@code founded_station}
     * criterion. @return the new entry, or {@code null} if the charter/slot vanished mid-flight
     * (the rider then docks at the public origin platform instead).
     */
    @Nullable
    private StationRegistry.StationEntry foundStation(MinecraftServer server, ServerLevel station,
            ServerPlayer rider) {
        int charterSlot = findCharter(rider);
        if (charterSlot < 0) {
            return null;
        }
        ItemStack charter = rider.getInventory().getItem(charterSlot);
        Component customName = charter.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        StationRegistry.StationEntry entry = StationRegistry.get(server)
                .found(customName == null ? null : customName.getString());
        if (entry == null) {
            return null;
        }
        charter.shrink(1);

        BlockPos centre = entry.center();
        station.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        buildStationPlatform(station, centre.getX(), centre.getY(), centre.getZ());
        station.setBlockAndUpdate(centre, ModBlocks.STATION_CORE.get().defaultBlockState());
        if (station.getBlockEntity(centre) instanceof StationCoreBlockEntity core) {
            core.bindStation(entry.slot(), entry.name());
        }
        za.co.neroland.nerospace.registry.ModCriteria.FOUNDED_STATION.get().trigger(rider);
        return entry;
    }

    /** Lay a 7x7 station-floor landing pad so a rider arriving in the void station has solid ground. */
    private static void buildStationPlatform(ServerLevel level, int centerX, int y, int centerZ) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(centerX + dx, y, centerZ + dz), floor);
            }
        }
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
                // Menu-open snapshot (MULTI_STATION_DESIGN.md §4): entity id + the station list
                // (slot + name pairs) + whether the rider carries a charter. Stations change
                // rarely, so a per-open snapshot is correct enough; reopening refreshes.
                java.util.List<StationRegistry.StationEntry> stations =
                        level() instanceof ServerLevel server
                                ? StationRegistry.get(server.getServer()).all()
                                : java.util.List.of();
                boolean charter = hasCharter(serverPlayer);
                serverPlayer.openMenu(this, buffer -> {
                    buffer.writeVarInt(this.getId());
                    buffer.writeVarInt(stations.size());
                    for (StationRegistry.StationEntry entry : stations) {
                        buffer.writeVarInt(entry.slot());
                        buffer.writeUtf(entry.name());
                    }
                    buffer.writeBoolean(charter);
                });
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
        this.entityData.set(DATA_DEST, input.getIntOr("Destination", getTier().defaultDestinationIndex()));
        this.entityData.set(DATA_STATION, input.getIntOr("Station", STATION_NONE));
        this.fuelTank.deserialize(input.childOrEmpty("FuelTank"));
        this.fuelInput.setItem(0, input.read("FuelInput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        syncFuel();
        setLaunching(input.getBooleanOr("Launching", false));
        this.launchTicks = input.getIntOr("LaunchTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Tier", getTier().ordinal());
        output.putInt("Destination", getDestinationIndex());
        output.putInt("Station", stationSelection());
        this.fuelTank.serialize(output.child("FuelTank"));
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
