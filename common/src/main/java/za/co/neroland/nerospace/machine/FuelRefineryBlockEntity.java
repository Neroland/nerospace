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

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.RelativeFace;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.menu.FuelRefineryMenu;
import za.co.neroland.nerospace.platform.FluidLookup;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.storage.CoreTankBridge;

/**
 * Fuel Refinery: the logistics-grade rocket-fuel source. It takes <b>grid power</b> (energy, insert-only),
 * a <b>carbon</b> feed (coal/charcoal) and a <b>catalyst</b> (blaze powder), and over a work cycle refines
 * them into liquid {@code rocket_fuel} in an internal tank exposed via the Fluid capability — so pipes
 * carry the fuel to a Fuel Tank or straight to a padded rocket.
 *
 * <p>Cross-loader port note: rebuilt on the shared {@link EnergyBuffer} + {@link FluidTank} and a vanilla
 * {@link WorldlyContainer} for the two input slots (the root used the NeoForge transfer API). Tuning
 * values are inlined (identity multiplier).</p>
 *
 * <p>2.0 additions (all appended after the 1.x slots, all saved under optional keys so a 1.x refinery loads
 * unchanged): a Perchlorate Oxidiser slot (Mars, +15 % fuel per batch while covered, see {@link RefineryRules})
 * and a Raw Ilmenite line (Luna): raw ilmenite in, a Titanium Ingot out, and oxygen pushed into any adjacent gas
 * store (it vents when there is nowhere to put it, so titanium never stalls on oxygen).</p>
 */
public class FuelRefineryBlockEntity extends BlockEntity
        implements WorldlyContainer, MenuProvider, SideConfigured {

    public static final int CARBON_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    /** Mars: Perchlorate Oxidiser (optional boost). */
    public static final int OXIDISER_SLOT = 2;
    /** Luna: Raw Ilmenite input. */
    public static final int ILMENITE_SLOT = 3;
    /** Luna: Titanium Ingot output. */
    public static final int TITANIUM_SLOT = 4;
    public static final int SIZE = 5;
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
    /** Internal oxygen buffer from refined ilmenite (pushed out each tick; the surplus vents). */
    public static final int OXYGEN_CAPACITY = 1_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_BUFFER, ENERGY_MAX_INSERT, 0, this::setChanged);
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, this::setChanged);
    private int progress;
    /** Base mB of output still covered by the oxidiser in use. */
    private int oxidisedMb;
    private int ilmeniteProgress;
    private final GasTank oxygen = new GasTank(OXYGEN_CAPACITY, this::setChanged);

    /**
     * Universal side configuration (Neroland Core): ITEM (two inputs in one input group, no output) +
     * FLUID (refined rocket fuel, output only) + ENERGY (grid power in). The fluid channel forbids
     * INPUT/IO (fuel only leaves) and energy forbids OUTPUT/IO/PUSH (it only draws power); every face
     * is seeded to emit fuel on OUTPUT so the default is a sensible "fuel out". Composed, not inherited.
     *
     * <p>The active {@link #pushFluid} auto-eject stays in place; the side-config auto-eject defaults OFF
     * per face, so the two never double-pump until a player explicitly enables a face.</p>
     */
    private final SideConfigComponent sideConfig =
            new SideConfigComponent(buildSideConfig(), this)
                    .withEnergy(this::getEnergy)
                    .withFluid(() -> CoreTankBridge.toCore(this.getTank()))
                    .withItems(() -> this);

    private static SideConfig buildSideConfig() {
        SideConfig config = SideConfig.builder()
                // Both inputs (carbon + catalyst) live in one input group; there is no item output.
                .channel(Channel.ITEM, SlotGroup.of("input", CARBON_SLOT, CATALYST_SLOT, OXIDISER_SLOT, ILMENITE_SLOT),
                        SlotGroup.of("output", TITANIUM_SLOT))
                .channel(Channel.FLUID)
                .channel(Channel.ENERGY)
                // Refined fuel leaves only — never accepted.
                .allow(Channel.FLUID, SideMode.INPUT, false)
                .allow(Channel.FLUID, SideMode.IO, false)
                // Power in only.
                .allow(Channel.ENERGY, SideMode.OUTPUT, false)
                .allow(Channel.ENERGY, SideMode.IO, false)
                .allow(Channel.ENERGY, SideMode.PUSH, false)
                .defaultPreset(SidePreset.PROCESSOR)
                .build();
        // PROCESSOR seeds non-energy channels INPUT on most faces, which FLUID forbids (it clamps). Seed
        // every face to emit fuel on OUTPUT instead, so the default is a sensible "fuel out" everywhere.
        for (RelativeFace face : RelativeFace.VALUES) {
            config.setMode(Channel.FLUID, face, SideMode.OUTPUT);
        }
        return config;
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

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
        return ModFluids.ROCKET_FUEL.get();
    }

    private static boolean slotAccepts(int index, ItemStack stack) {
        return switch (index) {
            case CARBON_SLOT -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            case CATALYST_SLOT -> stack.is(Items.BLAZE_POWDER);
            case OXIDISER_SLOT -> stack.is(za.co.neroland.nerospace.world.planet.mars.MarsContent.PERCHLORATE_OXIDISER.get());
            case ILMENITE_SLOT -> stack.is(za.co.neroland.nerospace.world.planet.luna.LunaContent.RAW_ILMENITE.get());
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
                && this.tank.getCapacity() - this.tank.getAmount() >= nextBatchMb();
    }

    /** The next batch's output: boosted while the oxidiser covers it, so the tank never overfills. */
    private int nextBatchMb() {
        return RefineryRules.batchMb(MB_PER_BATCH,
                RefineryRules.boosted(this.oxidisedMb, !this.items.get(OXIDISER_SLOT).isEmpty()));
    }

    /** Whether the ilmenite line can work this tick (input, room for the ingot, power). */
    public boolean canRefineIlmenite() {
        ItemStack out = this.items.get(TITANIUM_SLOT);
        return !this.items.get(ILMENITE_SLOT).isEmpty()
                && this.energy.getAmount() >= energyPerTick()
                && RefineryRules.titaniumFits(out.isEmpty(),
                        out.is(za.co.neroland.nerospace.world.planet.luna.LunaContent.TITANIUM_INGOT.get()),
                        out.getCount(), out.getMaxStackSize());
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Optional side-config auto-eject (fuel) / auto-input — default off per face, so a safe no-op until
        // a player enables it. Runs in ADDITION to the active pushFluid below (which always drains the tank).
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);

        // Active auto-eject: push the refined fuel into any adjacent fluid store (tank/pipe) even with no
        // pipe pulling — so a full output tank keeps draining and the refinery never idles on "tank full".
        pushFluid(level, pos);
        pushOxygen(level, pos);
        tickIlmenite();

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
            if (RefineryRules.consumesOxidiser(this.oxidisedMb, !this.items.get(OXIDISER_SLOT).isEmpty())) {
                this.items.get(OXIDISER_SLOT).shrink(1);
                this.oxidisedMb = RefineryRules.OXIDISER_COVERAGE_MB;
            }
            boolean boosted = this.oxidisedMb > 0;
            this.tank.fill(rocketFuel(), RefineryRules.batchMb(MB_PER_BATCH, boosted), false);
            if (boosted) {
                this.oxidisedMb = RefineryRules.coverageAfterBatch(this.oxidisedMb, MB_PER_BATCH);
            }
        }
        setChanged();
    }

    /** Luna line: raw ilmenite to a titanium ingot plus oxygen, on its own progress and the shared power. */
    private void tickIlmenite() {
        if (!canRefineIlmenite()) {
            if (this.ilmeniteProgress != 0) {
                this.ilmeniteProgress = 0;
                setChanged();
            }
            return;
        }
        this.energy.consume(energyPerTick());
        this.ilmeniteProgress++;
        if (this.ilmeniteProgress >= NerospaceConfig.scaleInterval(WORK_TICKS, NerospaceConfig.machineSpeedMultiplier())) {
            this.ilmeniteProgress = 0;
            this.items.get(ILMENITE_SLOT).shrink(1);
            ItemStack out = this.items.get(TITANIUM_SLOT);
            if (out.isEmpty()) {
                this.items.set(TITANIUM_SLOT, new ItemStack(
                        za.co.neroland.nerospace.world.planet.luna.LunaContent.TITANIUM_INGOT.get(),
                        RefineryRules.TITANIUM_PER_ILMENITE));
            } else {
                out.grow(RefineryRules.TITANIUM_PER_ILMENITE);
            }
            // Surplus oxygen vents: the buffer is a convenience, never a reason to stall titanium.
            this.oxygen.fill(GasResource.OXYGEN, RefineryRules.ILMENITE_OXYGEN_MB, false);
        }
        setChanged();
    }

    /** Pushes buffered oxygen into any adjacent {@link NerospaceGasStorage} (Gas Tank, gas pipe, ...). */
    private void pushOxygen(Level level, BlockPos pos) {
        if (this.oxygen.getAmount() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (this.oxygen.getAmount() <= 0) {
                break;
            }
            NerospaceGasStorage dest = GasLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (dest == null) {
                continue;
            }
            long accepted = dest.fill(GasResource.OXYGEN, Math.min(EJECT_RATE, this.oxygen.getAmount()), false);
            if (accepted > 0) {
                this.oxygen.drain(accepted, false);
            }
        }
    }

    /** Pushes up to {@link #EJECT_RATE} mB of the stored fuel into each adjacent {@link NerospaceFluidStorage}. */
    private void pushFluid(Level level, BlockPos pos) {
        Fluid fluid = this.tank.getFluid();
        if (this.tank.getAmount() <= 0 || fluid == Fluids.EMPTY) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (this.tank.getAmount() <= 0) {
                break;
            }
            NerospaceFluidStorage dest = FluidLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
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
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.tank.getRawFluid()).toString());
        output.putInt("Amount", this.tank.getRawAmount());
        output.putInt("Progress", this.progress);
        output.store("Carbon", ItemStack.OPTIONAL_CODEC, this.items.get(CARBON_SLOT));
        output.store("Catalyst", ItemStack.OPTIONAL_CODEC, this.items.get(CATALYST_SLOT));
        // 2.0 keys: all optional on load (a 1.x refinery has none of them).
        output.store("Oxidiser", ItemStack.OPTIONAL_CODEC, this.items.get(OXIDISER_SLOT));
        output.putInt("OxidisedMb", this.oxidisedMb);
        output.store("Ilmenite", ItemStack.OPTIONAL_CODEC, this.items.get(ILMENITE_SLOT));
        output.store("Titanium", ItemStack.OPTIONAL_CODEC, this.items.get(TITANIUM_SLOT));
        output.putInt("IlmeniteProgress", this.ilmeniteProgress);
        output.putInt("Oxygen", this.oxygen.getRawAmount());
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.tank.setRaw(fluid, input.getIntOr("Amount", 0));
        this.progress = input.getIntOr("Progress", 0);
        this.items.set(CARBON_SLOT, input.read("Carbon", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.items.set(CATALYST_SLOT, input.read("Catalyst", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.items.set(OXIDISER_SLOT, input.read("Oxidiser", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.oxidisedMb = Math.max(0, input.getIntOr("OxidisedMb", 0));
        this.items.set(ILMENITE_SLOT, input.read("Ilmenite", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.items.set(TITANIUM_SLOT, input.read("Titanium", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.ilmeniteProgress = Math.max(0, input.getIntOr("IlmeniteProgress", 0));
        int o2 = Math.max(0, Math.min(OXYGEN_CAPACITY, input.getIntOr("Oxygen", 0)));
        this.oxygen.setRaw(o2 > 0 ? GasResource.OXYGEN : GasResource.EMPTY, o2);
        this.sideConfig.load(input);
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

    // --- WorldlyContainer: per-face routing via the side config -------------
    // Exposed slots and insert/extract permissions follow the side config; the slot/recipe guard is kept
    // as an extra gate so a face can never push the wrong item into a slot. The only item output is the
    // titanium slot; inputs are never extracted through a face.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.sideConfig.itemSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && this.sideConfig.canInsertItem(slot, side) && slotAccepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == TITANIUM_SLOT && this.sideConfig.canExtractItem(slot, side);
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
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
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
