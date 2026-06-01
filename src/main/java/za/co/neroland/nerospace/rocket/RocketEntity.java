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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import za.co.neroland.nerospace.fluid.ModFluids;
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

    /** Ticks of ascent before the rider is transported. */
    public static final int LAUNCH_DURATION = 100;
    /** One fuel canister is worth this many millibuckets. */
    public static final int CANISTER_MB = 1_000;

    private int launchTicks;

    /**
     * The authoritative fuel store: a real {@link FluidTank} of {@code rocket_fuel}, exposed to the
     * NeoForge fluid capability (so pipes/tanks can fill it) and synced to the client via
     * {@link #DATA_FUEL} for the GUI. Physical capacity is the largest tier's; {@link #addFuel} caps
     * top-ups to the current tier's capacity.
     */
    private final FluidTank fuelTank = new FluidTank(RocketTier.TIER_3.fuelCapacity(),
            stack -> stack.getFluid() == ModFluids.ROCKET_FUEL.get()) {
        @Override
        protected void onContentsChanged() {
            syncFuel();
        }
    };

    /**
     * A single-slot fuel intake exposed in the rocket UI. The player (or, later, a hopper/automation)
     * drops a {@link ModItems#ROCKET_FUEL_BUCKET} or {@link ModItems#ROCKET_FUEL_CANISTER} here and
     * the rocket drains it into {@link #fuelTank} on its server tick — returning an empty bucket.
     */
    private final net.minecraft.world.SimpleContainer fuelInput = new net.minecraft.world.SimpleContainer(1);

    /** Synced to the menu: [0]=fuel, [1]=capacity, [2]=tierOrdinal, [3]=launchable, [4]=destinationIndex. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getFuel();
                case 1 -> getTier().fuelCapacity();
                case 2 -> getTier().ordinal();
                case 3 -> canLaunch() ? 1 : 0;
                case 4 -> getDestinationIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    /** Convenience constructor for spawning from the rocket item. */
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
    }

    public int getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    /** Pushes the server-side tank amount into the synced data accessor (client GUI + canLaunch). */
    private void syncFuel() {
        if (!level().isClientSide()) {
            this.entityData.set(DATA_FUEL, this.fuelTank.getFluidAmount());
        }
    }

    /** The fuel tank, exposed to {@code Capabilities.FluidHandler.ENTITY}. */
    public IFluidHandler getFuelTank() {
        return this.fuelTank;
    }

    /** The UI fuel-intake slot (one bucket/canister), for the menu and automation. */
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
        this.entityData.set(DATA_DEST, tier.defaultDestinationIndex());
    }

    // --- Destination selection ----------------------------------------------

    public int getDestinationIndex() {
        return this.entityData.get(DATA_DEST);
    }

    /** The currently selected destination level, or {@code null} if this tier can't fly anywhere. */
    @Nullable
    public net.minecraft.resources.ResourceKey<Level> selectedDestination() {
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
        }
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
        int room = Math.max(0, getTier().fuelCapacity() - this.fuelTank.getFluidAmount());
        int toFill = Math.min(amount, room);
        int filled = this.fuelTank.fill(
                new FluidStack(ModFluids.ROCKET_FUEL.get(), toFill), IFluidHandler.FluidAction.EXECUTE);
        return amount - filled;
    }

    /** Whether a launch could be started right now (fuelled, has a rider, and a destination selected). */
    public boolean canLaunch() {
        return !isLaunching()
                && selectedDestination() != null
                && getFuel() >= getTier().fuelPerLaunch()
                && this.getFirstPassenger() instanceof Player;
    }

    /** Begins the ascent. Server-side; called from the rocket menu's Launch button. */
    public void startLaunch() {
        if (level().isClientSide() || !canLaunch()) {
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

        this.fuelTank.drain(getTier().fuelPerLaunch(), IFluidHandler.FluidAction.EXECUTE);

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof ServerPlayer player && level() instanceof ServerLevel current) {
            MinecraftServer server = current.getServer();
            ServerLevel destination = server.getLevel(targetKey);
            if (destination != null) {
                player.stopRiding();

                double x = player.getX();
                double z = player.getZ();
                int blockX = Mth.floor(x);
                int blockZ = Mth.floor(z);
                destination.getChunk(blockX >> 4, blockZ >> 4);

                double arrivalY;
                if (targetKey.equals(ModDimensions.STATION_LEVEL)) {
                    // The station is an empty void; lay down a landing platform to stand on.
                    int platformY = 64;
                    buildStationPlatform(destination, blockX, platformY, blockZ);
                    arrivalY = platformY + 1.0D;
                } else {
                    arrivalY = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ) + 1.0D;
                }

                player.teleportTo(destination, x, arrivalY, z, Set.of(), player.getYRot(), player.getXRot(), true);
                player.sendSystemMessage(Component.translatable(targetKey.equals(ModDimensions.STATION_LEVEL)
                        ? "entity.nerospace.rocket.docked"
                        : "entity.nerospace.rocket.arrived"));
            }
        }

        // The rocket is expended on launch; a return trip needs a pad + rocket on the destination.
        dropFuelInput();
        this.discard();
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
                serverPlayer.openMenu(this, buffer -> buffer.writeVarInt(this.getId()));
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
