package za.co.neroland.nerospace.machine;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.RelativeFace;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.menu.FuelTankMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.storage.CoreTankBridge;

/**
 * Block entity for the {@link FuelTankBlock}. It stores a large buffer of {@code rocket_fuel} and, each
 * server tick, automatically feeds a rocket standing on an adjacent launch pad — the multiblock-pad
 * machinery. A complete 3x3 pad pumps faster (4x), a Heavy Launch Complex faster still (12x).
 *
 * <p>Cross-loader port note: the root binds the tank and the canister intake to the NeoForge transfer
 * API. The multiloader rebuilds the tank on the shared {@link FluidTank} and the single canister slot
 * as a vanilla {@link WorldlyContainer} (exposed via {@code Capabilities.Item.BLOCK} / Fabric
 * {@code ContainerStorage}); fuel values are inlined (identity-multiplier). The pump FX uses a vanilla
 * sound (the root's {@code ModSounds.FUEL_TANK_PUMP} alias is not ported).</p>
 */
public class FuelTankBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, SideConfigured {

    /** One bucket / canister of fuel, in millibuckets. */
    public static final int CONTAINER_MB = 1_000;
    /** Tank capacity, mB (base value; the root scales by the energy-rate multiplier). */
    public static final int CAPACITY = 32_000;

    /** Pump rates by pad footprint (base / full 3x3 / Heavy complex). */
    private static final int PUMP_RATE = 40;
    private static final int PUMP_RATE_FULL_PAD = 160;
    private static final int PUMP_RATE_HEAVY_PAD = 480;

    private static final int FX_PARTICLE_INTERVAL = 8;
    private static final int FX_SOUND_INTERVAL = 24;

    public static final int CANISTER_SLOT = 0;
    public static final int SIZE = 1;

    private int fxTick;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);

    /**
     * Universal side configuration (Neroland Core): FLUID (the fuel buffer, fill + drain) + ITEM (the
     * canister intake). STORAGE preset — every face is IO — so the fuel tank fills or drains on any face,
     * which is the desired behaviour for a storage tank. The ITEM canister channel is then forced back to
     * INPUT on every face so canisters can only be inserted (never auto-extracted), preserving the
     * original canister-in-only intake. Composed, not inherited.
     */
    private final SideConfigComponent sideConfig =
            new SideConfigComponent(buildSideConfig(), this)
                    .withFluid(() -> CoreTankBridge.toCore(this.getTank()))
                    .withItems(() -> this);

    private static SideConfig buildSideConfig() {
        SideConfig config = SideConfig.builder()
                .channel(Channel.FLUID)
                .channel(Channel.ITEM, SlotGroup.of("input", CANISTER_SLOT), null)
                .defaultPreset(SidePreset.STORAGE)
                .build();
        // Canister intake is input only — never let a face auto-extract canisters.
        for (RelativeFace face : RelativeFace.VALUES) {
            config.setMode(Channel.ITEM, face, SideMode.INPUT);
        }
        return config;
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    /** Synced to the open menu: [0]=fuel, [1]=capacity. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? (int) tank.getAmount() : (int) tank.getCapacity();
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    private static Fluid rocketFuel() {
        return ModFluids.ROCKET_FUEL.get();
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.fuel_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuelTankMenu(containerId, playerInventory, this.dataAccess);
    }

    // --- Fuel access (used by the block's item interaction + the fluid capability) -----

    /** The tank, exposed via the mod's fluid capability/lookup (pipe filling). */
    public NerospaceFluidStorage getTank() {
        return this.tank;
    }

    public int getFluidAmount() {
        return (int) this.tank.getAmount();
    }

    public int getCapacity() {
        return (int) this.tank.getCapacity();
    }

    /** Tries to add one container (bucket/canister) of fuel; {@code true} if the whole lot fit. */
    public boolean tryFillContainer() {
        return this.tank.fill(rocketFuel(), CONTAINER_MB, false) == CONTAINER_MB;
    }

    /** Tries to draw one bucket of fuel out of the tank (for refilling an empty bucket). */
    public boolean tryDrainBucket() {
        return this.tank.drain(CONTAINER_MB, false) == CONTAINER_MB;
    }

    /** Comparator output: 0 (empty) .. 15 (full), scaled by fill fraction. */
    public int comparatorSignal() {
        long amount = this.tank.getAmount();
        if (amount <= 0) {
            return 0;
        }
        return 1 + (int) (amount / (double) this.tank.getCapacity() * 14.0D);
    }

    // --- Ticking ------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        // Optional auto-eject / auto-input — default off per face, so a safe no-op until enabled.
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);

        drawFromCanister();

        if (this.tank.getAmount() <= 0) {
            return;
        }

        BlockPos padPos = LaunchPadMultiblock.adjacentPad(level, pos);
        if (padPos == null) {
            return;
        }

        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, padPos);
        RocketEntity rocket = LaunchPadMultiblock.rocketAbove(level, pads);
        if (rocket == null) {
            return;
        }

        int toPump = (int) Math.min(pumpRate(level, pads), this.tank.getAmount());
        if (toPump <= 0) {
            return;
        }

        int drained = (int) this.tank.drain(toPump, false);
        int overflow = rocket.addFuel(drained);
        if (overflow > 0) {
            this.tank.fill(rocketFuel(), overflow, false);
        }
        if (drained - overflow > 0 && level instanceof ServerLevel serverLevel) {
            pumpingFx(serverLevel, pos, rocket);
        }
    }

    /** Consume one buffered canister into {@link #CONTAINER_MB} of fuel if the whole lot fits. */
    private void drawFromCanister() {
        ItemStack canister = this.items.get(CANISTER_SLOT);
        if (canister.isEmpty()) {
            return;
        }
        if (this.tank.getCapacity() - this.tank.getAmount() < CONTAINER_MB) {
            return;
        }
        this.tank.fill(rocketFuel(), CONTAINER_MB, false);
        canister.shrink(1);
        this.items.set(CANISTER_SLOT, canister.isEmpty() ? ItemStack.EMPTY : canister);
    }

    /** Pump rate by pad footprint: base on a partial cluster, 4x on the 3x3, 12x on a Heavy complex. */
    public static int pumpRate(Level level, Set<BlockPos> pads) {
        if (LaunchPadMultiblock.isHeavyComplex(level, pads)) {
            return PUMP_RATE_HEAVY_PAD;
        }
        return LaunchPadMultiblock.isFullThreeByThree(pads) ? PUMP_RATE_FULL_PAD : PUMP_RATE;
    }

    private void pumpingFx(ServerLevel level, BlockPos pos, RocketEntity rocket) {
        this.fxTick++;
        double sx = pos.getX() + 0.5D;
        double sy = pos.getY() + 1.0D;
        double sz = pos.getZ() + 0.5D;
        double ex = rocket.getX();
        double ey = rocket.getY() + 0.5D;
        double ez = rocket.getZ();

        if (this.fxTick % FX_PARTICLE_INTERVAL == 0) {
            for (double t = 0.15D; t < 1.0D; t += 0.3D) {
                level.sendParticles(ParticleTypes.CLOUD,
                        Mth.lerp(t, sx, ex), Mth.lerp(t, sy, ey), Mth.lerp(t, sz, ez),
                        1, 0.05D, 0.05D, 0.05D, 0.0D);
            }
            level.sendParticles(ParticleTypes.SMALL_FLAME, ex, ey, ez, 1, 0.15D, 0.1D, 0.15D, 0.0D);
        }
        if (this.fxTick % FX_SOUND_INTERVAL == 0) {
            level.playSound(null, (sx + ex) / 2.0D, (sy + ey) / 2.0D, (sz + ez) / 2.0D,
                    SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS,
                    0.35F, 0.85F + level.getRandom().nextFloat() * 0.25F);
        }
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.tank.getRawFluid()).toString());
        output.putInt("Amount", this.tank.getRawAmount());
        output.store("Canister", ItemStack.OPTIONAL_CODEC, this.items.get(CANISTER_SLOT));
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.tank.setRaw(fluid, input.getIntOr("Amount", 0));
        this.items.set(CANISTER_SLOT, input.read("Canister", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.sideConfig.load(input);
    }

    // --- WorldlyContainer: canister in only ---------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.sideConfig.itemSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && this.sideConfig.canInsertItem(slot, side) && isCanister(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return this.sideConfig.canExtractItem(slot, side);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isCanister(stack);
    }

    private static boolean isCanister(ItemStack stack) {
        return stack.is(ModItems.ROCKET_FUEL_CANISTER.get());
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(CANISTER_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(this.items, slot, amount);
        if (!r.isEmpty()) {
            this.setChanged();
        }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
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
