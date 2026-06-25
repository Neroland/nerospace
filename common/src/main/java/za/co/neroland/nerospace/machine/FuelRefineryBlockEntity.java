package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.menu.FuelRefineryMenu;
import za.co.neroland.nerospace.platform.FluidLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Fuel Refinery: the logistics-grade rocket-fuel source. It takes <b>grid power</b> (energy, insert-only),
 * a <b>carbon</b> feed (coal/charcoal) and a <b>catalyst</b> (blaze powder), and over a work cycle refines
 * them into liquid {@code rocket_fuel} in an internal tank exposed via the Fluid capability — so pipes
 * carry the fuel to a Fuel Tank or straight to a padded rocket.
 *
 * <p>Cross-loader port note: rebuilt on the shared {@link EnergyBuffer} + {@link FluidTank} and a vanilla
 * {@link WorldlyContainer} for the two input slots (the root used the NeoForge transfer API). Tuning
 * values are inlined (identity multiplier).</p>
 */
public class FuelRefineryBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int CARBON_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int SIZE = 2;
    public static final int DATA_COUNT = 6;

    /** Inlined Tuning base values. */
    public static final int ENERGY_BUFFER = 40_000;
    public static final int ENERGY_MAX_INSERT = 1_000;
    public static final int TANK_CAPACITY = 8_000;
    /** Max mB pushed to each adjacent fluid store per tick by the active auto-eject. */
    private static final long EJECT_RATE = 200L;
    public static final int FE_PER_TICK = 40;
    public static final int MB_PER_BATCH = 2_000;
    public static final int WORK_TICKS = 100;

    private static final int [] SLOTS = {CARBON_SLOT, CATALYST_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_BUFFER, ENERGY_MAX_INSERT, 0, this::setChanged);
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, this::setChanged);
    private int progress;

    /** Synced to the menu: [0]=energy [1]=energyCap [2]=fuel [3]=fuelCap [4]=progress [5]=maxProgress. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy.getRaw() * 1000L / ENERGY_BUFFER); // permille (ContainerData syncs as short)
                case 1 -> 1000;
                case 2 -> (int) tank.getAmount();
                case 3 -> (int) tank.getCapacity();
                case 4 -> progress;
                case 5 -> NerospaceConfig.scaleInterval(WORK_TICKS, NerospaceConfig.machineSpeedMultiplier());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 4) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FuelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_REFINERY.get(), pos, state);
    }

    private static Fluid rocketFuel() {
        return java.util.Objects.requireNonNull(ModFluids.ROCKET_FUEL.get());
    }

    private static boolean slotAccepts(int index, ItemStack stack) {
        return switch (index) {
            case CARBON_SLOT -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            case CATALYST_SLOT -> stack.is(Items.BLAZE_POWDER);
            default -> false;
        };
    }

    /** Exposed via the mod's energy capability/lookup (insert-only — grid powered). */
    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Exposed via the mod's fluid capability/lookup — pipes pull the refined fuel away. */
    public NerospaceFluidStorage getTank() {
        return this.tank;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public int comparatorSignal() {
        long stored = this.tank.getAmount();
        return stored <= 0 ? 0 : 1 + (int) (stored / (double) this.tank.getCapacity() * 14.0D);
    }

    /** FE consumed per working tick, scaled by the fuel-cost multiplier (a consumable running cost). */
    private static int energyPerTick() {
        return NerospaceConfig.scale(FE_PER_TICK, NerospaceConfig.fuelCostMultiplier());
    }

    public boolean canRun() {
        return !this.items.get(CARBON_SLOT).isEmpty()
                && !this.items.get(CATALYST_SLOT).isEmpty()
                && this.energy.getAmount() >= energyPerTick()
                && this.tank.getCapacity() - this.tank.getAmount() >= MB_PER_BATCH;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Active auto-eject: push the refined fuel into any adjacent fluid store (tank/pipe) even with no
        // pipe pulling — so a full output tank keeps draining and the refinery never idles on "tank full".
        pushFluid(level, pos);

        if (!canRun()) {
            if (this.progress != 0) {
                this.progress = 0;
                setChanged();
            }
            return;
        }

        this.energy.consume(energyPerTick());
        this.progress++;
        if (this.progress >= NerospaceConfig.scaleInterval(WORK_TICKS, NerospaceConfig.machineSpeedMultiplier())) {
            this.progress = 0;
            this.items.get(CARBON_SLOT).shrink(1);
            this.items.get(CATALYST_SLOT).shrink(1);
            this.tank.fill(rocketFuel(), MB_PER_BATCH, false);
        }
        setChanged();
    }

    /** Pushes up to {@link #EJECT_RATE} mB of the stored fuel into each adjacent {@link NerospaceFluidStorage}. */
    private void pushFluid(Level level, BlockPos pos) {
        Level checkedLevel = NerospaceCommon.requireNonNull(level);
        BlockPos checkedPos = NerospaceCommon.requireNonNull(pos);
        Fluid fluid = this.tank.getFluid();
        if (this.tank.getAmount() <= 0 || fluid == Fluids.EMPTY) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (this.tank.getAmount() <= 0) {
                break;
            }
            Direction checkedDir = java.util.Objects.requireNonNull(dir);
            NerospaceFluidStorage dest = FluidLookup.INSTANCE.find(checkedLevel,
                    checkedPos.relative(checkedDir), checkedDir.getOpposite());
            if (dest == null) {
                continue;
            }
            long offered = Math.min(EJECT_RATE, this.tank.getAmount());
            long accepted = dest.fill(fluid, offered, false);
            if (accepted > 0) {
                this.tank.drain(accepted, false);
                setChanged();
            }
        }
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(
                NerospaceCommon.requireNonNull(this.tank.getRawFluid())).toString());
        output.putInt("Amount", this.tank.getRawAmount());
        output.putInt("Progress", this.progress);
        output.store("Carbon", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(CARBON_SLOT));
        output.store("Catalyst", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(CATALYST_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.tank.setRaw(fluid, input.getIntOr("Amount", 0));
        this.progress = input.getIntOr("Progress", 0);
        this.items.set(CARBON_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Carbon", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
        this.items.set(CATALYST_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Catalyst", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.fuel_refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuelRefineryMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- WorldlyContainer: coal + blaze powder in only ----------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slotAccepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slotAccepts(slot, stack);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(CARBON_SLOT).isEmpty() && this.items.get(CATALYST_SLOT).isEmpty();
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
        Level currentLevel = this.level;
        if (currentLevel == null || currentLevel.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
