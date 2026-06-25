package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import za.co.neroland.nerospace.menu.HydrationModuleMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1): the glacite intake of the water stage. It must
 * TOUCH a Terraformer; each work pulse it melts one item from its input slot ({@code
 * nerospace:hydration_input} — glacite by default) into the Terraformer's hydration-unit buffer. No
 * energy buffer of its own — melting is part of the Terraformer's stage-2 column cost.
 *
 * <p>Cross-loader port note: rebuilt on a vanilla {@link WorldlyContainer} + {@link NonNullList} input
 * slot (the root used the NeoForge transfer API + {@code MachineItemHandler}); Tuning values inlined.</p>
 */
public class HydrationModuleBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int SIZE = 1;
    public static final int DATA_COUNT = 3;

    // --- Inlined Tuning base values (config seam deferred) ---
    private static final int HYDRATION_PER_GLACITE = 16;
    private static final int HYDRATION_PER_GLACITE_BLOCK = 9 * HYDRATION_PER_GLACITE;
    private static final int HYDRATION_CAP = 1_024;
    /** Ticks between melt pulses (cheap; one item per pulse). */
    private static final int WORK_INTERVAL_TICKS = 10;

    private static final int @org.jspecify.annotations.NonNull[] SLOTS = {INPUT_SLOT};

    private final @org.jspecify.annotations.NonNull NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** Transient link state for the GUI (recomputed each work pulse). */
    private transient boolean linked;
    private transient int linkedHydration;

    /** Synced to the menu: [0]=linked [1]=linked terraformer's hydration [2]=hydration cap. */
    private final @org.jspecify.annotations.NonNull ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> linked ? 1 : 0;
                case 1 -> linkedHydration;
                case 2 -> HYDRATION_CAP;
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

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Hydration units one input item melts into (tag-driven; unknown tag members melt as glacite). */
    public static int hydrationUnits(ItemStack stack) {
        if (stack.is(ModItems.GLACITE_BLOCK_ITEM.get())) {
            return HYDRATION_PER_GLACITE_BLOCK;
        }
        return HYDRATION_PER_GLACITE;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getGameTime() % NerospaceConfig.scaleInterval(
                        WORK_INTERVAL_TICKS, NerospaceConfig.machineSpeedMultiplier()) != 0) {
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

        ItemStack input = this.items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        int units = hydrationUnits(input);
        // Only melt when the buffer can take the whole item's yield — units must never be lost.
        if (terraformer.getHydration() + units > HYDRATION_CAP) {
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
        output.store("Input", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(INPUT_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.set(INPUT_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Input", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.hydration_module");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new HydrationModuleMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- WorldlyContainer: a single glacite input slot ----------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return stack.is(ModTags.Items.HYDRATION_INPUT);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModTags.Items.HYDRATION_INPUT);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(INPUT_SLOT).isEmpty();
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
