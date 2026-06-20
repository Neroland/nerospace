package za.co.neroland.nerospace.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Universal Pipe — relays energy, gas AND items between adjacent storages. Energy/gas use the
 * cross-loader {@link EnergyLookup}/{@link GasLookup} seams; items use plain vanilla {@link Container}
 * adjacency (so it interoperates with vanilla chests/furnaces and the mod's machines on both loaders
 * with no extra seam). The pipe is itself a {@link WorldlyContainer} (small buffer), so it is exposed
 * as the item capability and chains pipe-to-pipe. Item flow is directed: pull only from non-pipe
 * containers, push to any neighbour — sources feed the line, the line feeds sinks.
 */
public class UniversalPipeBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int CAPACITY = 8_000;
    public static final int MAX_IO = 1_000;
    public static final int GAS_CAPACITY = 8_000;
    public static final int GAS_MAX_IO = 1_000;
    public static final int ITEM_SLOTS = 3;

    private static final int[] ALL_SLOTS = {0, 1, 2};

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IO, MAX_IO, this::setChanged);
    private final GasTank gas = new GasTank(GAS_CAPACITY, this::setChanged);
    private final NonNullList<ItemStack> items = NonNullList.withSize(ITEM_SLOTS, ItemStack.EMPTY);

    public UniversalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSAL_PIPE.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public NerospaceGasStorage getGas() {
        return this.gas;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        relayEnergy(level, pos);
        relayGas(level, pos);
        relayItems(level, pos);
    }

    private void relayEnergy(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            NerospaceEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            long room = this.energy.getCapacity() - this.energy.getAmount();
            if (room > 0) {
                long moved = neighbour.extract(Math.min(room, MAX_IO), false);
                if (moved > 0) {
                    this.energy.insert(moved, false);
                }
            }
        }
        for (Direction dir : Direction.values()) {
            if (this.energy.getAmount() <= 0) {
                break;
            }
            NerospaceEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            long offered = this.energy.extract(Math.min(this.energy.getAmount(), MAX_IO), true);
            long accepted = neighbour.insert(offered, false);
            if (accepted > 0) {
                this.energy.extract(accepted, false);
            }
        }
    }

    private void relayGas(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            long room = this.gas.getCapacity() - this.gas.getAmount();
            if (room <= 0) {
                break;
            }
            NerospaceGasStorage neighbour = GasLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            GasResource ngas = neighbour.getGas();
            if (ngas.isEmpty() || (!this.gas.getGas().isEmpty() && this.gas.getGas() != ngas)) {
                continue;
            }
            long available = neighbour.drain(Math.min(room, GAS_MAX_IO), true);
            long moved = this.gas.fill(ngas, available, false);
            if (moved > 0) {
                neighbour.drain(moved, false);
            }
        }
        for (Direction dir : Direction.values()) {
            if (this.gas.getAmount() <= 0) {
                break;
            }
            NerospaceGasStorage neighbour = GasLookup.INSTANCE.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }
            GasResource g = this.gas.getGas();
            long offered = this.gas.drain(Math.min(this.gas.getAmount(), GAS_MAX_IO), true);
            long accepted = neighbour.fill(g, offered, false);
            if (accepted > 0) {
                this.gas.drain(accepted, false);
            }
        }
    }

    private void relayItems(Level level, BlockPos pos) {
        // Pull one item per tick from each non-pipe neighbour container into the buffer.
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof Container src && !(be instanceof UniversalPipeBlockEntity)) {
                moveOne(src, dir.getOpposite(), this, dir);
            }
        }
        // Push one item per tick from the buffer into each neighbour container (incl. other pipes).
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof Container dst) {
                moveOne(this, dir, dst, dir.getOpposite());
            }
        }
    }

    private static int[] slotsFor(Container c, Direction face) {
        if (c instanceof WorldlyContainer wc) {
            return wc.getSlotsForFace(face);
        }
        int[] all = new int[c.getContainerSize()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return all;
    }

    private static boolean placeable(Container into, int slot, ItemStack stack, Direction face) {
        if (!into.canPlaceItem(slot, stack)) {
            return false;
        }
        return !(into instanceof WorldlyContainer wc) || wc.canPlaceItemThroughFace(slot, stack, face);
    }

    /** Move a single item from {@code from} (extracted through {@code fromFace}) into {@code into}. */
    private static boolean moveOne(Container from, Direction fromFace, Container into, Direction intoFace) {
        for (int fs : slotsFor(from, fromFace)) {
            ItemStack stack = from.getItem(fs);
            if (stack.isEmpty()) {
                continue;
            }
            if (from instanceof WorldlyContainer wc && !wc.canTakeItemThroughFace(fs, stack, fromFace)) {
                continue;
            }
            ItemStack one = stack.copyWithCount(1);
            if (insertOne(into, one, intoFace)) {
                from.removeItem(fs, 1);
                from.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean insertOne(Container into, ItemStack one, Direction face) {
        for (int s : slotsFor(into, face)) {
            ItemStack ex = into.getItem(s);
            int max = Math.min(into.getMaxStackSize(), one.getMaxStackSize());
            if (!ex.isEmpty() && ex.getCount() < max
                    && ItemStack.isSameItemSameComponents(ex, one) && placeable(into, s, one, face)) {
                ex.grow(1);
                into.setChanged();
                return true;
            }
        }
        for (int s : slotsFor(into, face)) {
            if (into.getItem(s).isEmpty() && placeable(into, s, one, face)) {
                into.setItem(s, one);
                into.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Gas", this.gas.getRawGas().getSerializedName());
        output.putInt("GasAmount", this.gas.getRawAmount());
        for (int i = 0; i < ITEM_SLOTS; i++) {
            if (!this.items.get(i).isEmpty()) {
                output.store("Item" + i, ItemStack.OPTIONAL_CODEC, this.items.get(i));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.gas.setRaw(GasResource.byName(input.getStringOr("Gas", "empty")), input.getIntOr("GasAmount", 0));
        for (int i = 0; i < ITEM_SLOTS; i++) {
            this.items.set(i, input.read("Item" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }

    // --- WorldlyContainer (item buffer) -------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return ITEM_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        if (!split.isEmpty()) {
            this.setChanged();
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
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
