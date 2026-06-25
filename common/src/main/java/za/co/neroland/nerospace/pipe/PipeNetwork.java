package za.co.neroland.nerospace.pipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.platform.FluidLookup;
import za.co.neroland.nerospace.platform.GasLookup;

/**
 * A connected group of {@link UniversalPipeBlockEntity} segments that move resources as ONE pool — the
 * cross-loader port of the root mod's network (which used NeoForge transactions). Built by flood-fill
 * over adjacent pipes and shared by reference with every member; rebuilt only when the topology changes
 * (a vanished member invalidates the network so the next tick rebuilds). Each tick it pulls from / pushes
 * to non-pipe neighbours on every face, then <b>balances</b> the energy/fluid/gas buffers evenly across
 * all members so the whole line behaves as a single shared pool — giving unlimited range instead of the
 * old per-pipe relay that only reached one block. Items route source → any pipe's buffer → any sink in
 * the network (backpressure, never dropped).
 */
public final class PipeNetwork {

    private static final int MAX_MEMBERS = 4096;
    /** Item extraction pulse: every N ticks each pulling face draws up to {@link #ITEM_EXTRACT_AMOUNT} (×speed). */
    private static final int ITEM_EXTRACT_PERIOD = 4;
    private static final int ITEM_EXTRACT_AMOUNT = 16;

    private final List<BlockPos> members;
    private final Set<Long> memberSet;
    private boolean valid = true;
    private long lastTick = -1L;
    /** Rotates over sinks so deliveries spread round-robin across destinations. */
    private int itemRoundRobin;

    private PipeNetwork(@NonNull List<BlockPos> members, @NonNull Set<Long> memberSet) {
        this.members = members;
        this.memberSet = memberSet;
    }

    public boolean isValid() {
        return this.valid && !this.members.isEmpty();
    }

    /** Flood-fill the connected pipes from {@code seed}, build the network, and adopt every member. */
    public static @NonNull PipeNetwork getOrBuild(@NonNull ServerLevel level, @NonNull BlockPos seed) {
        List<BlockPos> members = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        seen.add(seed.asLong());

        while (!queue.isEmpty() && members.size() < MAX_MEMBERS) {
            BlockPos pos = NerospaceCommon.requireNonNull(queue.poll());
            if (!(level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity)) {
                continue;
            }
            members.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (seen.add(np.asLong()) && level.getBlockEntity(np) instanceof UniversalPipeBlockEntity) {
                    queue.add(np);
                }
            }
        }

        Set<Long> memberSet = new HashSet<>(Math.max(16, members.size() * 2));
        for (BlockPos pos : members) {
            memberSet.add(pos.asLong());
        }
        PipeNetwork network = new PipeNetwork(members, memberSet);
        for (BlockPos pos : members) {
            if (level.getBlockEntity(NerospaceCommon.requireNonNull(pos)) instanceof UniversalPipeBlockEntity pipe) {
                pipe.adopt(network);
            }
        }
        return network;
    }

    /** Move + balance every resource layer. Guarded so it runs at most once per game tick. */
    public void tick(@NonNull ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime == this.lastTick) {
            return;
        }
        this.lastTick = gameTime;

        List<UniversalPipeBlockEntity> pipes = new ArrayList<>(this.members.size());
        for (BlockPos pos : this.members) {
            if (level.getBlockEntity(NerospaceCommon.requireNonNull(pos)) instanceof UniversalPipeBlockEntity pipe) {
                pipes.add(pipe);
            } else {
                this.valid = false; // a member vanished — rebuild next tick
                return;
            }
        }
        if (pipes.isEmpty()) {
            this.valid = false;
            return;
        }

        tickEnergy(level, pipes);
        tickFluid(level, pipes);
        tickGas(level, pipes);
        tickItems(level, pipes);
    }

    // --- Energy ---------------------------------------------------------------

    private void tickEnergy(@NonNull ServerLevel level, @NonNull List<UniversalPipeBlockEntity> pipes) {
        for (UniversalPipeBlockEntity pipe : pipes) {
            long io = (long) UniversalPipeBlockEntity.MAX_IO * pipe.speedMultiplier();
            BlockPos pos = pipe.getBlockPos();
            EnergyBuffer buf = pipe.energy();
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (this.memberSet.contains(np.asLong())) {
                    continue; // internal pipe link is handled by balancing
                }
                PipeIoMode mode = pipe.mode(dir, PipeResourceType.ENERGY);
                if (!mode.isConnected()) {
                    continue;
                }
                NerospaceEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, np, dir.getOpposite());
                if (neighbour == null) {
                    continue;
                }
                if (mode.canPull()) {
                    moveEnergy(neighbour, buf, io);
                }
                if (mode.canPush()) {
                    moveEnergy(buf, neighbour, io);
                }
            }
        }
        // Balance the FE evenly so the network behaves as one shared pool (unlimited range).
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            total += pipe.energy().getAmount();
        }
        int n = pipes.size();
        long base = total / n;
        long rem = total % n;
        for (int i = 0; i < n; i++) {
            NerospaceCommon.requireNonNull(pipes.get(i)).energy().setRaw((int) (base + (i < rem ? 1 : 0)));
        }
    }

    private static void moveEnergy(NerospaceEnergyStorage from, NerospaceEnergyStorage to, long max) {
        long avail = from.extract(max, true);
        if (avail <= 0) {
            return;
        }
        long room = to.insert(avail, true);
        long move = Math.min(avail, room);
        if (move > 0) {
            from.extract(move, false);
            to.insert(move, false);
        }
    }

    // --- Fluid ----------------------------------------------------------------

    private void tickFluid(@NonNull ServerLevel level, @NonNull List<UniversalPipeBlockEntity> pipes) {
        Fluid networkFluid = Fluids.EMPTY;
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (pipe.fluid().getFluid() != Fluids.EMPTY) {
                networkFluid = pipe.fluid().getFluid();
                break;
            }
        }

        for (UniversalPipeBlockEntity pipe : pipes) {
            long io = (long) UniversalPipeBlockEntity.FLUID_MAX_IO * pipe.speedMultiplier();
            BlockPos pos = pipe.getBlockPos();
            FluidTank tank = pipe.fluid();
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (this.memberSet.contains(np.asLong())) {
                    continue;
                }
                PipeIoMode mode = pipe.mode(dir, PipeResourceType.FLUID);
                if (!mode.isConnected()) {
                    continue;
                }
                NerospaceFluidStorage neighbour = FluidLookup.INSTANCE.find(level, np, dir.getOpposite());
                if (neighbour == null) {
                    continue;
                }
                if (mode.canPull()) {
                    Fluid nf = neighbour.getFluid();
                    boolean typeOk = nf != Fluids.EMPTY
                            && (networkFluid == Fluids.EMPTY || nf == networkFluid)
                            && (tank.getFluid() == Fluids.EMPTY || tank.getFluid() == nf);
                    if (typeOk) {
                        long room = tank.getCapacity() - tank.getAmount();
                        long avail = neighbour.drain(Math.min(room, io), true);
                        long moved = tank.fill(nf, avail, false);
                        if (moved > 0) {
                            neighbour.drain(moved, false);
                            if (networkFluid == Fluids.EMPTY) {
                                networkFluid = nf;
                            }
                        }
                    }
                }
                if (mode.canPush() && tank.getAmount() > 0) {
                    Fluid f = tank.getFluid();
                    long offered = tank.drain(Math.min(tank.getAmount(), io), true);
                    long accepted = neighbour.fill(f, offered, false);
                    if (accepted > 0) {
                        tank.drain(accepted, false);
                    }
                }
            }
        }

        if (networkFluid == Fluids.EMPTY) {
            return;
        }
        List<UniversalPipeBlockEntity> matching = new ArrayList<>(pipes.size());
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            Fluid held = pipe.fluid().getFluid();
            if (held == Fluids.EMPTY || held == networkFluid) {
                matching.add(pipe);
                total += pipe.fluid().getAmount();
            }
        }
        if (matching.isEmpty()) {
            return;
        }
        int n = matching.size();
        long base = total / n;
        long rem = total % n;
        for (int i = 0; i < n; i++) {
            matching.get(i).fluid().setRaw(networkFluid, (int) (base + (i < rem ? 1 : 0)));
        }
    }

    // --- Gas ------------------------------------------------------------------

    private void tickGas(@NonNull ServerLevel level, @NonNull List<UniversalPipeBlockEntity> pipes) {
        GasResource networkGas = GasResource.EMPTY;
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (!pipe.gas().getGas().isEmpty()) {
                networkGas = pipe.gas().getGas();
                break;
            }
        }

        for (UniversalPipeBlockEntity pipe : pipes) {
            long io = (long) UniversalPipeBlockEntity.GAS_MAX_IO * pipe.speedMultiplier();
            BlockPos pos = pipe.getBlockPos();
            GasTank tank = pipe.gas();
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (this.memberSet.contains(np.asLong())) {
                    continue;
                }
                PipeIoMode mode = pipe.mode(dir, PipeResourceType.GAS);
                if (!mode.isConnected()) {
                    continue;
                }
                NerospaceGasStorage neighbour = GasLookup.INSTANCE.find(level, np, dir.getOpposite());
                if (neighbour == null) {
                    continue;
                }
                if (mode.canPull()) {
                    GasResource ng = neighbour.getGas();
                    boolean typeOk = !ng.isEmpty()
                            && (networkGas.isEmpty() || ng == networkGas)
                            && (tank.getGas().isEmpty() || tank.getGas() == ng);
                    if (typeOk) {
                        long room = tank.getCapacity() - tank.getAmount();
                        long avail = neighbour.drain(Math.min(room, io), true);
                        long moved = tank.fill(ng, avail, false);
                        if (moved > 0) {
                            neighbour.drain(moved, false);
                            if (networkGas.isEmpty()) {
                                networkGas = ng;
                            }
                        }
                    }
                }
                if (mode.canPush() && tank.getAmount() > 0) {
                    GasResource g = tank.getGas();
                    long offered = tank.drain(Math.min(tank.getAmount(), io), true);
                    long accepted = neighbour.fill(g, offered, false);
                    if (accepted > 0) {
                        tank.drain(accepted, false);
                    }
                }
            }
        }

        if (networkGas.isEmpty()) {
            return;
        }
        List<UniversalPipeBlockEntity> matching = new ArrayList<>(pipes.size());
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            GasResource held = pipe.gas().getGas();
            if (held.isEmpty() || held == networkGas) {
                matching.add(pipe);
                total += pipe.gas().getAmount();
            }
        }
        if (matching.isEmpty()) {
            return;
        }
        int n = matching.size();
        long base = total / n;
        long rem = total % n;
        for (int i = 0; i < n; i++) {
            matching.get(i).gas().setRaw(networkGas, (int) (base + (i < rem ? 1 : 0)));
        }
    }

    // --- Items ----------------------------------------------------------------

    private record Sink(@NonNull BlockPos pipePos, @NonNull Direction outFace, @NonNull Container container,
            @NonNull Direction side, @NonNull ItemStack filter) {
    }

    private void tickItems(@NonNull ServerLevel level, @NonNull List<UniversalPipeBlockEntity> pipes) {
        // Collect every sink in the network: a non-pipe Container behind a face whose item mode pushes.
        List<Sink> sinks = new ArrayList<>();
        for (UniversalPipeBlockEntity pipe : pipes) {
            BlockPos pos = pipe.getBlockPos();
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (this.memberSet.contains(np.asLong())
                        || !pipe.mode(dir, PipeResourceType.ITEM).canPush()) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(np);
                if (be instanceof Container dst && !(be instanceof UniversalPipeBlockEntity)) {
                    sinks.add(new Sink(pos, dir, dst, dir.getOpposite(), pipe.filter(dir)));
                }
            }
        }

        // 1. Extraction pulse: pull from sources into pipe buffers (backpressure — only if the pipe has room).
        if ((level.getGameTime() % ITEM_EXTRACT_PERIOD) == 0L) {
            for (UniversalPipeBlockEntity pipe : pipes) {
                int extractMax = ITEM_EXTRACT_AMOUNT * pipe.speedMultiplier();
                BlockPos pos = pipe.getBlockPos();
                for (Direction dir : Direction.values()) {
                    BlockPos np = pos.relative(dir);
                    if (this.memberSet.contains(np.asLong())
                            || !pipe.mode(dir, PipeResourceType.ITEM).canPull()) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(np);
                    if (!(be instanceof Container src) || be instanceof UniversalPipeBlockEntity) {
                        continue;
                    }
                    ItemStack pulled = extract(src, dir.getOpposite(), pipe.filter(dir), extractMax);
                    if (pulled.isEmpty()) {
                        continue;
                    }
                    ItemStack toBuffer = pulled.copy();
                    ItemStack leftover = insertIntoPipe(pipe, toBuffer);
                    int accepted = pulled.getCount() - leftover.getCount();
                    if (accepted > 0 && sinks.isEmpty()) {
                        pipe.showTravelling(pulled.copyWithCount(accepted), dir, null);
                    }
                    if (!leftover.isEmpty()) {
                        insert(src, dir.getOpposite(), leftover); // pipe full — put the remainder back, never drop
                    }
                }
            }
        }

        // 2. Drain every pipe's buffer toward the network's sinks (any source can reach any sink).
        if (!sinks.isEmpty()) {
            for (UniversalPipeBlockEntity pipe : pipes) {
                for (int slot = 0; slot < pipe.getContainerSize(); slot++) {
                    ItemStack stack = pipe.getItem(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    int n = sinks.size();
                    int offset = Math.floorMod(this.itemRoundRobin++, n);
                    for (int i = 0; i < n && !stack.isEmpty(); i++) {
                        Sink sink = NerospaceCommon.requireNonNull(sinks.get((offset + i) % n));
                        if (!sink.filter().isEmpty() && !ItemStack.isSameItemSameComponents(sink.filter(), stack)) {
                            continue;
                        }
                        ItemStack before = stack.copy();
                        int beforeCount = stack.getCount();
                        stack = insert(sink.container(), sink.side(), stack);
                        int moved = beforeCount - stack.getCount();
                        if (moved > 0) {
                            showItemPath(level, pipe.getBlockPos(), sink.pipePos(), sink.outFace(),
                                    before.copyWithCount(moved));
                        }
                    }
                    pipe.setItem(slot, stack);
                }
            }
        }
    }

    private void showItemPath(@NonNull ServerLevel level, @NonNull BlockPos source, @NonNull BlockPos target,
            @NonNull Direction outFace, @NonNull ItemStack moved) {
        if (moved.isEmpty()) {
            return;
        }
        List<BlockPos> path = route(source, target);
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = NerospaceCommon.requireNonNull(path.get(i));
            if (!(level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe)) {
                continue;
            }
            Direction nextFace = i == path.size() - 1 ? outFace
                    : directionBetween(pos, NerospaceCommon.requireNonNull(path.get(i + 1)));
            Direction inFace = i == 0 ? nextFace.getOpposite()
                    : directionBetween(pos, NerospaceCommon.requireNonNull(path.get(i - 1)));
            pipe.showTravelling(moved, inFace, nextFace);
        }
    }

    private @NonNull List<BlockPos> route(@NonNull BlockPos source, @NonNull BlockPos target) {
        if (source.equals(target)) {
            return NerospaceCommon.requireNonNull(List.of(source));
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<Long, BlockPos> previous = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        queue.add(source);
        seen.add(source.asLong());

        while (!queue.isEmpty()) {
            BlockPos pos = NerospaceCommon.requireNonNull(queue.poll());
            if (pos.equals(target)) {
                break;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                long key = next.asLong();
                if (!this.memberSet.contains(key) || !seen.add(key)) {
                    continue;
                }
                previous.put(key, pos);
                queue.add(next);
            }
        }
        if (!seen.contains(target.asLong())) {
            return NerospaceCommon.requireNonNull(List.of(source));
        }

        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = target;
        while (cursor != null) {
            BlockPos current = NerospaceCommon.requireNonNull(cursor);
            path.add(current);
            if (current.equals(source)) {
                break;
            }
            cursor = previous.get(current.asLong());
        }
        Collections.reverse(path);
        return path;
    }

    private static @NonNull Direction directionBetween(@NonNull BlockPos from, @NonNull BlockPos to) {
        for (Direction dir : Direction.values()) {
            if (from.relative(dir).equals(to)) {
                return dir;
            }
        }
        return Direction.NORTH;
    }

    /** Standard sided insertion into a container; returns the un-inserted remainder. */
    private static @NonNull ItemStack insert(@NonNull Container dst, @NonNull Direction side,
            @NonNull ItemStack stack) {
        int @NonNull[] slots = dst instanceof WorldlyContainer w ? w.getSlotsForFace(side) : allSlots(dst);
        // Pass 1: merge into matching stacks.
        for (int slot : slots) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (!canPlace(dst, slot, stack, side)) {
                continue;
            }
            ItemStack inSlot = dst.getItem(slot);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int max = Math.min(dst.getMaxStackSize(), inSlot.getMaxStackSize());
                int move = Math.min(max - inSlot.getCount(), stack.getCount());
                if (move > 0) {
                    inSlot.grow(move);
                    stack.shrink(move);
                    dst.setChanged();
                }
            }
        }
        // Pass 2: fill empty slots.
        for (int slot : slots) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (!canPlace(dst, slot, stack, side) || !dst.getItem(slot).isEmpty()) {
                continue;
            }
            int max = Math.min(dst.getMaxStackSize(), stack.getMaxStackSize());
            ItemStack put = stack.copyWithCount(Math.min(max, stack.getCount()));
            dst.setItem(slot, put);
            stack.shrink(put.getCount());
            dst.setChanged();
        }
        return stack;
    }

    /** Extract up to {@code maxCount} items matching {@code filter} from one slot of a sided container. */
    private static @NonNull ItemStack extract(@NonNull Container src, @NonNull Direction side,
            @NonNull ItemStack filter, int maxCount) {
        int @NonNull[] slots = src instanceof WorldlyContainer w ? w.getSlotsForFace(side) : allSlots(src);
        for (int slot : slots) {
            ItemStack inSlot = src.getItem(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            if (!filter.isEmpty() && !ItemStack.isSameItemSameComponents(filter, inSlot)) {
                continue;
            }
            if (src instanceof WorldlyContainer w && !w.canTakeItemThroughFace(slot, inSlot, side)) {
                continue;
            }
            return src.removeItem(slot, Math.min(maxCount, inSlot.getCount()));
        }
        return ItemStack.EMPTY;
    }

    /** Insert into the pipe's own internal buffer (no sided restriction — it is a pass-through). */
    private static @NonNull ItemStack insertIntoPipe(@NonNull UniversalPipeBlockEntity pipe,
            @NonNull ItemStack stack) {
        for (int slot = 0; slot < pipe.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack inSlot = pipe.getItem(slot);
            if (inSlot.isEmpty()) {
                int max = Math.min(pipe.getMaxStackSize(), stack.getMaxStackSize());
                ItemStack put = stack.copyWithCount(Math.min(max, stack.getCount()));
                pipe.setItem(slot, put);
                stack.shrink(put.getCount());
            } else if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int max = Math.min(pipe.getMaxStackSize(), inSlot.getMaxStackSize());
                int move = Math.min(max - inSlot.getCount(), stack.getCount());
                if (move > 0) {
                    inSlot.grow(move);
                    stack.shrink(move);
                    pipe.setChanged();
                }
            }
        }
        return stack;
    }

    private static boolean canPlace(@NonNull Container dst, int slot, @NonNull ItemStack stack,
            @NonNull Direction side) {
        if (dst instanceof WorldlyContainer w) {
            return w.canPlaceItem(slot, stack) && w.canPlaceItemThroughFace(slot, stack, side);
        }
        return dst.canPlaceItem(slot, stack);
    }

    private static int @NonNull[] allSlots(@NonNull Container c) {
        int @NonNull[] slots = new int[c.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
