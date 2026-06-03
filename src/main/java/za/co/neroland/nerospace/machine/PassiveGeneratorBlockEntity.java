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

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Passive Generator: trickles a small, steady amount of energy (FE) while consuming a nerosium core
 * from its slot. Each core (raw nerosium, a nerosium ingot, or nerosium dust) lasts a long time.
 * Weaker but hands-off compared to the Combustion Generator. Exposes an extractable energy buffer.
 */
public class PassiveGeneratorBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int CORE_SLOT = 0;
    public static final int SIZE = 1;
    public static final int ENERGY_CAPACITY = 20_000;
    /** Run-time (ticks) granted per nerosium core item. */
    public static final int CORE_TICKS = 24_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final GenEnergy energy = new GenEnergy();
    private final ItemStacksResourceHandler coreHandler = new ItemStacksResourceHandler(this.items) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return isCore(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldStack) {
            PassiveGeneratorBlockEntity.this.setChanged();
        }
    };

    private int coreTicks;

    /** Synced to the menu: [0]=energy [1]=capacity [2]=coreTicks [3]=maxCoreTicks. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> coreTicks;
                case 3 -> CORE_TICKS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 2) {
                coreTicks = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PassiveGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PASSIVE_GENERATOR.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ResourceHandler<ItemResource> getCoreHandler() {
        return this.coreHandler;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public static boolean isCore(ItemStack stack) {
        return stack.is(ModItems.RAW_NEROSIUM.get()) || stack.is(ModItems.NEROSIUM_INGOT.get())
                || stack.is(ModItems.NEROSIUM_DUST.get());
    }

    public int comparatorSignal() {
        int cap = this.energy.getCapacityAsInt();
        int stored = this.energy.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        if (this.coreTicks <= 0) {
            ItemStack core = this.items.get(CORE_SLOT);
            if (isCore(core)) {
                core.shrink(1);
                this.coreTicks = CORE_TICKS;
                setChanged();
            }
        }
        if (this.coreTicks > 0 && this.energy.getAmountAsInt() < ENERGY_CAPACITY) {
            this.coreTicks--;
            this.energy.generate(Config.PASSIVE_GENERATOR_FE_PER_TICK.get());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("CoreTicks", this.coreTicks);
        output.store("Core", ItemStack.OPTIONAL_CODEC, this.items.get(CORE_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.coreTicks = input.getIntOr("CoreTicks", 0);
        this.items.set(CORE_SLOT, input.read("Core", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.passive_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PassiveGeneratorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(CORE_SLOT).isEmpty();
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
        return isCore(stack);
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
        setChanged();
    }

    private final class GenEnergy extends SimpleEnergyHandler {
        private GenEnergy() {
            super(ENERGY_CAPACITY, 0, Config.ENERGY_PIPE_THROUGHPUT.get());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            PassiveGeneratorBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int next = Math.min(getCapacityAsInt(), getAmountAsInt() + amount);
            if (next != getAmountAsInt()) {
                set(next);
            }
        }
    }
}
