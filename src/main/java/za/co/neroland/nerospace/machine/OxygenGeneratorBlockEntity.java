package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Oxygen Generator (Phase 8c/8d/9): projects a breathable bubble while powered. It runs on an
 * internal energy buffer (exposed via {@code Capabilities.Energy.BLOCK}) replenished by burning a
 * fuel item — coal, charcoal, a blaze rod, or a rocket fuel canister. The single fuel slot is shown
 * in the machine's GUI, fillable by hand, and exposed via {@code Capabilities.Item.BLOCK} so hoppers
 * and pipes can feed it. Idle once fuel + buffer are spent; {@code GreenxertzAtmosphere} treats the
 * area within {@code oxygenBubbleRadius} of an active generator as oxygenated.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int FUEL_SLOT = 0;
    public static final int SIZE = 1;

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INSERT = 500;
    public static final int GENERATE_PER_TICK = 20;
    public static final int RUN_PER_TICK = 10;
    /** Minimum stored energy for the generator to count as active. */
    public static final int ACTIVE_THRESHOLD = RUN_PER_TICK;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final GeneratorEnergy energy = new GeneratorEnergy();
    /** A transfer-API view of the fuel slot for {@code Capabilities.Item.BLOCK} (hopper feeding). */
    private final ItemStacksResourceHandler fuelHandler = new ItemStacksResourceHandler(this.items) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return fuelValue(resource.toStack(1)) > 0;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldStack) {
            OxygenGeneratorBlockEntity.this.setChanged();
        }
    };

    /** Remaining burn ticks of the current fuel item, and the value it started with (for the gauge). */
    private int burnTime;
    private int maxBurnTime;

    /** Synced to the open menu: [0]=energy, [1]=capacity, [2]=burnTime, [3]=maxBurnTime. */
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

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Energy.BLOCK}. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Item.BLOCK}. */
    public ResourceHandler<ItemResource> getFuelHandler() {
        return this.fuelHandler;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Whether the generator is currently powering a breathable bubble. */
    public boolean isActive() {
        return this.energy.getAmountAsInt() >= ACTIVE_THRESHOLD;
    }

    /** Burn value (ticks of charging) for an accepted fuel item, or 0 if not a fuel. */
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
        if (stack.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            return 4_000;
        }
        return 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        if (this.burnTime > 0) {
            this.burnTime--;
            this.energy.generate(GENERATE_PER_TICK);
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
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

        // Running cost: a little energy each tick keeps the bubble up.
        if (this.energy.getAmountAsInt() >= RUN_PER_TICK) {
            this.energy.consume(RUN_PER_TICK);
        }
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("BurnTime", this.burnTime);
        output.putInt("MaxBurnTime", this.maxBurnTime);
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, this.items.get(FUEL_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        this.items.set(FUEL_SLOT, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.oxygen_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OxygenGeneratorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(FUEL_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fuelValue(stack) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    /** Internal energy buffer: receives power but does not allow extraction by others. */
    private final class GeneratorEnergy extends SimpleEnergyHandler {
        private GeneratorEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            OxygenGeneratorBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int current = getAmountAsInt();
            int next = Math.min(getCapacityAsInt(), current + amount);
            if (next != current) {
                set(next);
            }
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
