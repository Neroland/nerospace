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

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.registry.ModBlockEntities;

import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Nerosium Grinder. Holds a two-slot inventory (input + output), an internal
 * energy buffer exposed via the NeoForge energy capability, and grinding progress. Ticks
 * server-side: charges its internal buffer, and converts grindable inputs into dust over time.
 */
public class NerosiumGrinderBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SIZE = 2;

    public static final int ENERGY_MAX_INSERT = 500;

    private final GrinderEnergy energy = new GrinderEnergy();
    private int progress;

    /**
     * The authoritative inventory ({@link MachineItemHandler}): the capability surface (sided via
     * {@code RangedResourceHandler} in {@code ModCapabilities}) AND the backing store of the
     * Container/GUI/tick — never a parallel copy (see that class's javadoc). Only grindable items may
     * be inserted into the input slot; the output slot rejects external insertion (extraction is
     * always allowed).
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler itemHandler = new MachineItemHandler(SIZE, this::setChanged,
            (index, resource) -> index != OUTPUT_SLOT
                    && !GrinderRecipes.getResult(resource.toStack(1)).isEmpty());

    /** Synced to the open menu: [0]=progress, [1]=maxProgress, [2]=energy, [3]=capacity. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> Tuning.grinderMaxProgress();
                case 2 -> energy.getAmountAsInt();
                case 3 -> energy.getCapacityAsInt();
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

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Energy.BLOCK}. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Item.BLOCK}. */
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    // --- Ticking ------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean changed = false;

        // Grid-powered: the Grinder no longer self-charges — it runs on energy fed by the power grid
        // (a generator via the Universal Pipe network, or any external cable) into its internal buffer.

        ItemStack input = this.itemHandler.getStack(INPUT_SLOT);
        ItemStack result = GrinderRecipes.getResult(input);
        int energyPerTick = Tuning.grinderEnergyPerTick();
        boolean canWork = !result.isEmpty()
                && canInsertOutput(result)
                && this.energy.getAmountAsInt() >= energyPerTick;

        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= Tuning.grinderMaxProgress()) {
                craft(result);
                this.progress = 0;
            }
            changed = true;
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private void craft(ItemStack result) {
        this.itemHandler.getStack(INPUT_SLOT).shrink(1);
        ItemStack output = this.itemHandler.getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.itemHandler.setStack(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    private boolean canInsertOutput(ItemStack result) {
        ItemStack output = this.itemHandler.getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, this.itemHandler.getStack(INPUT_SLOT));
        output.store("Output", ItemStack.OPTIONAL_CODEC, this.itemHandler.getStack(OUTPUT_SLOT));
        output.putInt("Progress", this.progress);
        this.energy.serialize(output.child("Energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.itemHandler.setStack(INPUT_SLOT, input.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.itemHandler.setStack(OUTPUT_SLOT, input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.progress = input.getIntOr("Progress", 0);
        this.energy.deserialize(input.childOrEmpty("Energy"));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.nerosium_grinder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NerosiumGrinderMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.itemHandler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.itemHandler.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.itemHandler.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.itemHandler.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.itemHandler.setStack(slot, stack);
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
        this.itemHandler.clearStore();
    }

    /**
     * Internal energy buffer. Receives power from the pipe network but does not allow extraction.
     * {@link #consume} is a transaction-free helper for the machine's own logic;
     * {@code onEnergyChanged} keeps the chunk saved.
     */
    private final class GrinderEnergy extends SimpleEnergyHandler {
        private GrinderEnergy() {
            super(Tuning.grinderBuffer(), ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            NerosiumGrinderBlockEntity.this.setChanged();
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
