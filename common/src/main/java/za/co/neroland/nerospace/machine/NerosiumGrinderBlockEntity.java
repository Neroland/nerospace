package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Nerosium Grinder — grid-powered processing machine. Input slot + output slot + an energy buffer
 * fed by pipes (insert-only); grinds inputs into dust over time. Exercises the item (in/out) and
 * energy seams, a ticker, and the menu/screen seam together.
 */
public class NerosiumGrinderBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SIZE = 2;
    public static final int CAPACITY = 20_000;
    public static final int MAX_INSERT = 500;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;
    private static final int @org.jspecify.annotations.NonNull[] SLOTS = {INPUT_SLOT, OUTPUT_SLOT};

    private final @org.jspecify.annotations.NonNull NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_INSERT, 0, this::setChanged);
    private int progress;

    private final @org.jspecify.annotations.NonNull ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier());
                case 2 -> energy.getRaw();
                case 3 -> CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public NerosiumGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEROSIUM_GRINDER.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        boolean changed = false;
        ItemStack input = this.items.get(INPUT_SLOT);
        ItemStack result = GrinderRecipes.getResult(input);
        int energyPerTick = NerospaceConfig.scale(ENERGY_PER_TICK, NerospaceConfig.fuelCostMultiplier());
        boolean canWork = !result.isEmpty() && canInsertOutput(result) && this.energy.getAmount() >= energyPerTick;
        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier())) {
                craft(result);
                this.progress = 0;
            }
            changed = true;
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }
        if (changed) {
            this.setChanged();
        }
    }

    private void craft(ItemStack result) {
        this.items.get(INPUT_SLOT).shrink(1);
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    private boolean canInsertOutput(ItemStack result) {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(za.co.neroland.nerospace.NerospaceCommon.requireNonNull(output),
                za.co.neroland.nerospace.NerospaceCommon.requireNonNull(result))
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(INPUT_SLOT));
        output.store("Output", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(OUTPUT_SLOT));
        output.putInt("Progress", this.progress);
        output.putInt("Energy", this.energy.getRaw());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.set(INPUT_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Input", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
        this.items.set(OUTPUT_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Output", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
        this.progress = input.getIntOr("Progress", 0);
        this.energy.setRaw(input.getIntOr("Energy", 0));
    }

    // --- MenuProvider ---------------------------------------------------------
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.nerosium_grinder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new NerosiumGrinderMenu(containerId, playerInventory, this, this.data);
    }

    // --- WorldlyContainer: input in (grindable), output out -------------------
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && !GrinderRecipes.getResult(stack).isEmpty();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && !GrinderRecipes.getResult(stack).isEmpty();
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
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
