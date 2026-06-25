package za.co.neroland.nerospace.machine;

import java.util.stream.IntStream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.CombustionGeneratorMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Combustion Generator — burns a fuel item into energy. GUI-less for now: fuel is inserted by
 * hoppers/pipes into the single fuel slot (item capability), energy is pulled by pipes (energy
 * capability). First ticking machine: proves the item + energy seams together with a
 * {@code BlockEntityTicker}. The menu/screen comes with the menu seam.
 */
public class CombustionGeneratorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int FUEL_SLOT = 0;
    public static final int SIZE = 1;
    public static final int CAPACITY = 100_000;
    public static final int FE_PER_TICK = 20;
    private static final int[] FUEL_SLOTS = IntStream.range(0, SIZE).toArray();

    private final @org.jspecify.annotations.NonNull NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, FE_PER_TICK * 64, this::setChanged);
    private int burnTime;
    private int maxBurnTime;

    /** Synced to the menu: [0]=energy [1]=capacity [2]=burnTime [3]=maxBurnTime. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy.getRaw() * 1000L / CAPACITY); // permille (ContainerData syncs as short)
                case 1 -> 1000;
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy.setRaw(value);
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

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Comparator output (0..15) scaled to the stored energy fraction. */
    public int comparatorSignal() {
        int cap = (int) this.energy.getCapacity();
        int stored = (int) this.energy.getAmount();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    /** Burn value (ticks) for a fuel item, or 0 if not accepted. */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1_600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16_000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 2_400;
        }
        if (stack.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            return 4_000;
        }
        return 0;
    }

    /** Every accepted fuel item (mirrors {@link #fuelValue}), for display/integration (JEI). */
    public static java.util.List<ItemStack> knownFuels() {
        return java.util.List.of(
                new ItemStack(Items.COAL),
                new ItemStack(Items.CHARCOAL),
                new ItemStack(Items.COAL_BLOCK),
                new ItemStack(Items.BLAZE_ROD),
                new ItemStack(ModItems.ROCKET_FUEL_CANISTER.get()));
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        if (this.burnTime > 0) {
            if (this.energy.getAmount() < this.energy.getCapacity()) {
                this.burnTime--;
                this.energy.generate(NerospaceConfig.scale(FE_PER_TICK, NerospaceConfig.energyRateMultiplier()));
            }
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int value = fuelValue(fuel);
            if (value > 0 && this.energy.getAmount() < this.energy.getCapacity()) {
                this.burnTime = value;
                this.maxBurnTime = value;
                fuel.shrink(1);
                this.setChanged();
            } else if (this.maxBurnTime != 0) {
                this.maxBurnTime = 0;
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putInt("BurnTime", this.burnTime);
        output.putInt("MaxBurnTime", this.maxBurnTime);
        output.store("Fuel", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(FUEL_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        this.items.set(FUEL_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Fuel", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
    }

    // --- MenuProvider ---------------------------------------------------------
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.combustion_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, this.data);
    }

    // --- WorldlyContainer: fuel in only ---------------------------------------
    @Override
    public int[] getSlotsForFace(Direction side) {
        return FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return fuelValue(stack) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return fuelValue(stack) == 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fuelValue(stack) > 0;
    }

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
