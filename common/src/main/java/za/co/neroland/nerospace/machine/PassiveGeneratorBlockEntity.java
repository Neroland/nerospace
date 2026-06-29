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

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.PassiveGeneratorMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Passive Generator — consumes a nerosium "core" (raw/ingot/dust) which grants a long run-time of
 * a small steady energy trickle. Item + energy seams + ticker + GUI; hands-off but weak.
 */
public class PassiveGeneratorBlockEntity extends BlockEntity
        implements WorldlyContainer, MenuProvider, SideConfigured {

    public static final int CORE_SLOT = 0;
    public static final int SIZE = 1;
    public static final int CAPACITY = 100_000;
    public static final int FE_PER_TICK = 8;
    public static final int CORE_TICKS = 24_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, FE_PER_TICK * 64, this::setChanged);
    private int coreTicks;

    /**
     * Universal side configuration (Neroland Core): ITEM (fuel core in, input-only) + ENERGY (grid
     * power out). GENERATOR preset — energy leaves on every face, the core item enters; energy
     * INPUT/IO forbidden (the generator only emits power). Composed, not inherited.
     */
    private final SideConfigComponent sideConfig =
            new SideConfigComponent(buildSideConfig(), this)
                    .withEnergy(this::getEnergy)
                    .withItems(() -> this);

    private static SideConfig buildSideConfig() {
        return SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", CORE_SLOT), null)
                .channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.INPUT, false)
                .allow(Channel.ENERGY, SideMode.IO, false)
                .defaultPreset(SidePreset.GENERATOR)
                .build();
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy.getRaw() * 1000L / CAPACITY); // permille (ContainerData syncs as short)
                case 1 -> 1000;
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

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Comparator output (0..15) scaled to the stored energy fraction. */
    public int comparatorSignal() {
        int cap = (int) this.energy.getCapacity();
        int stored = (int) this.energy.getAmount();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    public static boolean isCore(ItemStack stack) {
        return stack.is(ModItems.RAW_NEROSIUM.get()) || stack.is(ModItems.NEROSIUM_INGOT.get())
                || stack.is(ModItems.NEROSIUM_DUST.get());
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);
        if (this.coreTicks <= 0) {
            ItemStack core = this.items.get(CORE_SLOT);
            if (isCore(core)) {
                core.shrink(1);
                this.coreTicks = CORE_TICKS;
                this.setChanged();
            }
        }
        if (this.coreTicks > 0 && this.energy.getAmount() < this.energy.getCapacity()) {
            this.coreTicks--;
            this.energy.generate(NerospaceConfig.scale(FE_PER_TICK, NerospaceConfig.energyRateMultiplier()));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putInt("CoreTicks", this.coreTicks);
        output.store("Core", ItemStack.OPTIONAL_CODEC, this.items.get(CORE_SLOT));
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.coreTicks = input.getIntOr("CoreTicks", 0);
        this.items.set(CORE_SLOT, input.read("Core", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.sideConfig.load(input);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.passive_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PassiveGeneratorMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.sideConfig.itemSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && this.sideConfig.canInsertItem(slot, side) && isCore(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return this.sideConfig.canExtractItem(slot, side);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isCore(stack);
    }

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
