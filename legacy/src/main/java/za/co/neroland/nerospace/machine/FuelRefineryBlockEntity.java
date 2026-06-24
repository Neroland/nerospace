package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.fluid.RocketFuelTank;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Fuel Refinery (BALANCE_COMPAT_AUDIT.md §3): the logistics-grade rocket-fuel source. It takes
 * <b>grid power</b> (energy capability, insert-only), a <b>carbon</b> feed (coal/charcoal) and a
 * <b>catalyst</b> (blaze powder), and over a work cycle refines them into liquid {@code rocket_fuel}
 * in an internal tank exposed via the Fluid capability — so pipes carry the fuel to a Fuel Tank or
 * straight to a padded rocket. This replaces hand-crafting canisters as the way to automate fuelling.
 */
public class FuelRefineryBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** Carbon feed slot: coal or charcoal. */
    public static final int CARBON_SLOT = 0;
    /** Catalyst slot: blaze powder. */
    public static final int CATALYST_SLOT = 1;
    public static final int SIZE = 2;
    public static final int DATA_COUNT = 6;

    /** Grid power only: insert-capped, never extractable by others. */
    public static final int ENERGY_MAX_INSERT = 1_000;

    private final RefineryEnergy energy = new RefineryEnergy();

    /**
     * The authoritative input slots ({@link MachineItemHandler}): the capability surface AND the
     * backing store of the Container/GUI/tick. Slot validators route a piped coal to the carbon slot
     * and blaze powder to the catalyst slot.
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler input = new MachineItemHandler(SIZE, this::setChanged,
            FuelRefineryBlockEntity::slotAccepts);

    @SuppressWarnings("this-escape")
    private final RocketFuelTank tank = new RocketFuelTank(Tuning.fuelRefineryTankCapacity(), this::setChanged);

    private int progress;

    /** Synced to the menu: [0]=energy [1]=energyCap [2]=fuel [3]=fuelCap [4]=progress [5]=maxProgress. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> tank.getAmount();
                case 3 -> tank.getCapacity();
                case 4 -> progress;
                case 5 -> Tuning.fuelRefineryWorkTicks();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> progress = value;
                default -> { }
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

    /** Per-slot insert filter for the capability surface. */
    private static boolean slotAccepts(int index, ItemResource resource) {
        return slotAcceptsStack(index, resource.toStack(1));
    }

    /** Per-slot validity rule shared by the capability filter and the GUI/Container. */
    private static boolean slotAcceptsStack(int index, ItemStack stack) {
        return switch (index) {
            case CARBON_SLOT -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            case CATALYST_SLOT -> stack.is(Items.BLAZE_POWDER);
            default -> false;
        };
    }

    /** Exposed via {@code Capabilities.Energy.BLOCK} (insert-only — grid powered). */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Exposed via {@code Capabilities.Item.BLOCK} — hoppers/pipes feed coal + blaze powder. */
    public ResourceHandler<ItemResource> getInputHandler() {
        return this.input;
    }

    /** Exposed via {@code Capabilities.Fluid.BLOCK} — pipes pull the refined fuel away. */
    public RocketFuelTank getTank() {
        return this.tank;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Comparator output: 0 (empty tank) .. 15 (full). */
    public int comparatorSignal() {
        int stored = this.tank.getAmount();
        return stored <= 0 ? 0 : 1 + (int) (stored / (double) this.tank.getCapacity() * 14.0D);
    }

    /** Whether the refinery has everything it needs to advance a batch this tick. */
    public boolean canRun() {
        return !this.input.getStack(CARBON_SLOT).isEmpty()
                && !this.input.getStack(CATALYST_SLOT).isEmpty()
                && this.energy.getAmountAsInt() >= Tuning.fuelRefineryFePerTick()
                && this.tank.getCapacity() - this.tank.getAmount() >= Tuning.fuelRefineryMbPerBatch();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        if (!canRun()) {
            if (this.progress != 0) {
                this.progress = 0;
                setChanged();
            }
            return;
        }

        this.energy.consume(Tuning.fuelRefineryFePerTick());
        this.progress++;
        if (this.progress >= Tuning.fuelRefineryWorkTicks()) {
            this.progress = 0;
            this.input.getStack(CARBON_SLOT).shrink(1);
            this.input.getStack(CATALYST_SLOT).shrink(1);
            this.tank.fill(Tuning.fuelRefineryMbPerBatch());
        }
        setChanged();
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        this.tank.serialize(output.child("FuelTank"));
        output.putInt("Progress", this.progress);
        output.store("Carbon", ItemStack.OPTIONAL_CODEC, this.input.getStack(CARBON_SLOT));
        output.store("Catalyst", ItemStack.OPTIONAL_CODEC, this.input.getStack(CATALYST_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.tank.deserialize(input.childOrEmpty("FuelTank"));
        this.progress = input.getIntOr("Progress", 0);
        this.input.setStack(CARBON_SLOT, input.read("Carbon", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.input.setStack(CATALYST_SLOT, input.read("Catalyst", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
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

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.input.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.input.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.input.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.input.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.input.setStack(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slotAcceptsStack(slot, stack);
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
        this.input.clearStore();
    }

    /** Internal energy buffer: receives power but does not allow extraction by others. */
    private final class RefineryEnergy extends SimpleEnergyHandler {
        private RefineryEnergy() {
            super(Tuning.fuelRefineryBuffer(), ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            FuelRefineryBlockEntity.this.setChanged();
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
