package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1): the glacite intake of the water stage. It must
 * TOUCH a Terraformer (sign-off: strict adjacency); each work pulse it melts one item from its input
 * slot ({@code nerospace:hydration_input} — glacite by default) into the Terraformer's hydration-unit
 * buffer. No energy buffer of its own — melting is part of the Terraformer's stage-2 column cost.
 */
public class HydrationModuleBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int SIZE = 1;
    public static final int DATA_COUNT = 3;

    /** Ticks between melt pulses (cheap; one item per pulse). */
    private static final int WORK_INTERVAL_TICKS = 10;

    /**
     * The authoritative input slot ({@link MachineItemHandler}): the capability surface AND the
     * backing store of the Container/GUI/tick — never a parallel copy (see that class's javadoc).
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler inputHandler = new MachineItemHandler(SIZE, this::setChanged,
            (index, resource) -> resource.toStack(1).is(ModTags.Items.HYDRATION_INPUT));

    /** Transient link state for the GUI (recomputed each work pulse). */
    private transient boolean linked;
    private transient int linkedHydration;

    /** Synced to the menu: [0]=linked [1]=linked terraformer's hydration [2]=hydration cap. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> linked ? 1 : 0;
                case 1 -> linkedHydration;
                case 2 -> Tuning.TERRAFORM_HYDRATION_CAP;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> linked = value != 0;
                case 1 -> linkedHydration = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HydrationModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRATION_MODULE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getInputHandler() {
        return this.inputHandler;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Hydration units one input item melts into (tag-driven; unknown tag members melt as glacite). */
    public static int hydrationUnits(ItemStack stack) {
        if (stack.is(ModItems.GLACITE_BLOCK_ITEM.get())) {
            return Tuning.HYDRATION_PER_GLACITE_BLOCK;
        }
        return Tuning.HYDRATION_PER_GLACITE;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getGameTime() % WORK_INTERVAL_TICKS != 0) {
            return;
        }
        meltPulse(serverLevel, pos);
    }

    /** One melt pulse (public so the gametests can drive it without waiting on the interval). */
    public void meltPulse(ServerLevel serverLevel, BlockPos pos) {
        TerraformerBlockEntity terraformer = findAdjacentTerraformer(serverLevel, pos);
        this.linked = terraformer != null;
        this.linkedHydration = terraformer == null ? 0 : terraformer.getHydration();
        if (terraformer == null) {
            return;
        }

        ItemStack input = this.inputHandler.getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        int units = hydrationUnits(input);
        // Only melt when the buffer can take the whole item's yield — units must never be lost.
        if (terraformer.getHydration() + units > Tuning.TERRAFORM_HYDRATION_CAP) {
            return;
        }
        input.shrink(1);
        terraformer.acceptHydration(units);
        this.linkedHydration = terraformer.getHydration();
        setChanged();
    }

    /** The Terraformer this module feeds: the first one TOUCHING any of the six faces. */
    @Nullable
    private static TerraformerBlockEntity findAdjacentTerraformer(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof TerraformerBlockEntity be) {
                return be;
            }
        }
        return null;
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, this.inputHandler.getStack(INPUT_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inputHandler.setStack(INPUT_SLOT,
                input.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.hydration_module");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new HydrationModuleMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.inputHandler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inputHandler.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.inputHandler.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.inputHandler.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.inputHandler.setStack(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModTags.Items.HYDRATION_INPUT);
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
        this.inputHandler.clearStore();
    }
}
