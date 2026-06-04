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

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Combustion Generator: burns a fuel item into energy (FE) and exposes an extractable energy buffer so
 * the Universal Pipe network can pull it to power machines. Has a single fuel slot (shown in its GUI,
 * fillable by hand or hopper) — coal, charcoal, a coal block, a blaze rod, or a Rocket Fuel Canister.
 */
public class CombustionGeneratorBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int FUEL_SLOT = 0;
    public static final int SIZE = 1;
    public static final int ENERGY_CAPACITY = 50_000;

    private final GenEnergy energy = new GenEnergy();
    /**
     * The authoritative fuel slot ({@link MachineItemHandler}): the capability surface AND the
     * backing store of the Container/GUI/tick — never a parallel copy (see that class's javadoc).
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler fuelHandler = new MachineItemHandler(SIZE, this::setChanged,
            (index, resource) -> fuelValue(resource.toStack(1)) > 0);

    private int burnTime;
    private int maxBurnTime;

    /** Synced to the menu: [0]=energy [1]=capacity [2]=burnTime [3]=maxBurnTime. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ResourceHandler<ItemResource> getFuelHandler() {
        return this.fuelHandler;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public int comparatorSignal() {
        int cap = this.energy.getCapacityAsInt();
        int stored = this.energy.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    /** Burn value (ticks of generation) for an accepted fuel item, or 0 if not a fuel. */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.COAL) || stack.is(net.minecraft.world.item.Items.CHARCOAL)) {
            return 1_600;
        }
        if (stack.is(net.minecraft.world.item.Items.COAL_BLOCK)) {
            return 16_000;
        }
        if (stack.is(net.minecraft.world.item.Items.BLAZE_ROD)) {
            return 2_400;
        }
        if (stack.is(za.co.neroland.nerospace.registry.ModItems.ROCKET_FUEL_CANISTER.get())) {
            return 4_000;
        }
        return 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        if (this.burnTime > 0) {
            if (this.energy.getAmountAsInt() < ENERGY_CAPACITY) {
                this.burnTime--;
                this.energy.generate(Config.COMBUSTION_GENERATOR_FE_PER_TICK.get());
            }
        } else {
            ItemStack fuel = this.fuelHandler.getStack(FUEL_SLOT);
            int value = fuelValue(fuel);
            if (value > 0 && this.energy.getAmountAsInt() < ENERGY_CAPACITY) {
                this.burnTime = value;
                this.maxBurnTime = value;
                fuel.shrink(1);
                setChanged();
            } else if (this.maxBurnTime != 0) {
                this.maxBurnTime = 0;
            }
        }
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("BurnTime", this.burnTime);
        output.putInt("MaxBurnTime", this.maxBurnTime);
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, this.fuelHandler.getStack(FUEL_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        this.fuelHandler.setStack(FUEL_SLOT, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.combustion_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.fuelHandler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.fuelHandler.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.fuelHandler.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.fuelHandler.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.fuelHandler.setStack(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return CombustionGeneratorBlockEntity.fuelValue(stack) > 0;
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
        this.fuelHandler.clearStore();
    }

    private final class GenEnergy extends SimpleEnergyHandler {
        private GenEnergy() {
            super(ENERGY_CAPACITY, 0, Config.ENERGY_PIPE_THROUGHPUT.get());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            CombustionGeneratorBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int next = Math.min(getCapacityAsInt(), getAmountAsInt() + amount);
            if (next != getAmountAsInt()) {
                set(next);
            }
        }
    }
}
