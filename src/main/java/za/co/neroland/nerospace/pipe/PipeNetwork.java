package za.co.neroland.nerospace.pipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.gas.GasCapability;
import za.co.neroland.nerospace.gas.GasResource;

/**
 * A connected group of {@link UniversalPipeBlockEntity} segments that move resources as one. Built by
 * flood-fill over adjacent pipes and shared by reference with every member; it is rebuilt only when the
 * pipe topology changes (a pipe placed/broken invalidates the affected members). Each tick it moves the
 * <b>energy</b> layer: pulls FE from neighbouring providers, pushes to receivers, and balances the FE
 * buffers across all segments so the network behaves as a single shared pool.
 *
 * <p>Performance: work is bounded by the member count (capped) and runs once per network per game tick;
 * non-pipe neighbour changes need no rebuild (capabilities are re-queried each tick).</p>
 */
public final class PipeNetwork {

    private static final int MAX_MEMBERS = 4096;

    private final List<BlockPos> members;
    private final LongOpenHashSet memberSet;
    private boolean valid = true;
    private long lastTick = -1L;
    /** Rotates over routing candidates so deliveries spread round-robin across destinations. */
    private int itemRoundRobin;

    private PipeNetwork(List<BlockPos> members, LongOpenHashSet memberSet) {
        this.members = members;
        this.memberSet = memberSet;
    }

    public boolean isValid() {
        return this.valid && !this.members.isEmpty();
    }

    /** Flood-fill the connected pipes from {@code seed}, build the network, and adopt every member. */
    public static PipeNetwork getOrBuild(ServerLevel level, BlockPos seed) {
        List<BlockPos> members = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        seen.add(seed.asLong());

        while (!queue.isEmpty() && members.size() < MAX_MEMBERS) {
            BlockPos pos = queue.poll();
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

        LongOpenHashSet memberSet = new LongOpenHashSet(members.size());
        for (BlockPos pos : members) {
            memberSet.add(pos.asLong());
        }
        PipeNetwork network = new PipeNetwork(members, memberSet);
        for (BlockPos pos : members) {
            if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
                pipe.adopt(network);
            }
        }
        return network;
    }

    /** Move + balance every resource layer. Guarded so it runs at most once per game tick. */
    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime == this.lastTick) {
            return;
        }
        this.lastTick = gameTime;

        // Resolve member pipes; if any vanished, mark invalid so members rebuild next tick.
        List<UniversalPipeBlockEntity> pipes = new ArrayList<>(this.members.size());
        for (BlockPos pos : this.members) {
            if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
                pipes.add(pipe);
            } else {
                this.valid = false;
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

    // --- Energy layer ---------------------------------------------------------

    private void tickEnergy(ServerLevel level, List<UniversalPipeBlockEntity> pipes) {
        int baseThroughput = Tuning.energyPipeThroughput();

        // Pull from providers / push to receivers on every non-pipe face, within one transaction.
        try (Transaction tx = Transaction.openRoot()) {
            for (UniversalPipeBlockEntity pipe : pipes) {
                int throughput = baseThroughput * pipe.speedMultiplier();
                BlockPos pos = pipe.getBlockPos();
                for (Direction dir : Direction.values()) {
                    BlockPos np = pos.relative(dir);
                    if (this.memberSet.contains(np.asLong())) {
                        continue; // internal pipe-to-pipe link is handled by balancing
                    }
                    PipeIoMode mode = pipe.mode(dir, PipeResourceType.ENERGY);
                    if (!mode.isConnected()) {
                        continue;
                    }
                    EnergyHandler neighbor =
                            Capabilities.Energy.BLOCK.getCapability(level, np, null, null, dir.getOpposite());
                    if (neighbor == null) {
                        continue;
                    }
                    if (mode.canPull()) {
                        int pulled = EnergyHandlerUtil.move(neighbor, pipe.energy(), throughput, tx);
                        if (pulled > 0) {
                            flowParticle(level, ParticleTypes.GLOW, pos, dir, false);
                        }
                    }
                    if (mode.canPush()) {
                        int pushed = EnergyHandlerUtil.move(pipe.energy(), neighbor, throughput, tx);
                        if (pushed > 0) {
                            flowParticle(level, ParticleTypes.GLOW, pos, dir, true);
                        }
                    }
                }
            }
            tx.commit();
        }

        // Balance the FE buffers so the network behaves as one shared pool.
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            total += pipe.energy().getAmountAsInt();
        }
        int n = pipes.size();
        int base = (int) (total / n);
        int remainder = (int) (total % n);
        for (int i = 0; i < n; i++) {
            pipes.get(i).energy().setStored(base + (i < remainder ? 1 : 0));
        }
    }

    // --- Fluid layer ------------------------------------------------------------

    /**
     * One fluid type per network: the first fluid in claims the fluid layer; other fluids are refused
     * at the inlets until the network drains. Pipes push whatever they hold (so a stray fluid from a
     * network merge still drains out), and the shared pool is balanced across matching segments.
     */
    private void tickFluid(ServerLevel level, List<UniversalPipeBlockEntity> pipes) {
        int baseThroughput = Tuning.fluidPipeThroughput();

        // The network's claimed fluid = the first non-empty buffer found.
        FluidResource networkFluid = FluidResource.EMPTY;
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (!pipe.fluid().resource().isEmpty()) {
                networkFluid = pipe.fluid().resource();
                break;
            }
        }

        try (Transaction tx = Transaction.openRoot()) {
            for (UniversalPipeBlockEntity pipe : pipes) {
                int throughput = baseThroughput * pipe.speedMultiplier();
                BlockPos pos = pipe.getBlockPos();
                for (Direction dir : Direction.values()) {
                    BlockPos np = pos.relative(dir);
                    if (this.memberSet.contains(np.asLong())) {
                        continue;
                    }
                    PipeIoMode mode = pipe.mode(dir, PipeResourceType.FLUID);
                    if (!mode.isConnected()) {
                        continue;
                    }
                    ResourceHandler<FluidResource> neighbor =
                            Capabilities.Fluid.BLOCK.getCapability(level, np, null, null, dir.getOpposite());
                    if (neighbor == null) {
                        continue;
                    }
                    if (mode.canPull()) {
                        FluidResource claimed = networkFluid;
                        int pulled = ResourceHandlerUtil.move(neighbor, pipe.fluid(),
                                r -> claimed.isEmpty() || r.equals(claimed), throughput, tx);
                        if (pulled > 0) {
                            if (networkFluid.isEmpty()) {
                                networkFluid = pipe.fluid().resource(); // first fluid claims the network
                            }
                            flowParticle(level, ParticleTypes.SPLASH, pos, dir, false);
                        }
                    }
                    if (mode.canPush()) {
                        int pushed = ResourceHandlerUtil.move(pipe.fluid(), neighbor,
                                r -> true, throughput, tx);
                        if (pushed > 0) {
                            flowParticle(level, ParticleTypes.SPLASH, pos, dir, true);
                        }
                    }
                }
            }
            tx.commit();
        }

        // Balance the claimed fluid across matching (or empty) segments — one shared pool.
        if (networkFluid.isEmpty()) {
            return;
        }
        List<UniversalPipeBlockEntity> matching = new ArrayList<>(pipes.size());
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            FluidResource held = pipe.fluid().resource();
            if (held.isEmpty() || held.equals(networkFluid)) {
                matching.add(pipe);
                total += pipe.fluid().amount();
            }
        }
        if (matching.isEmpty()) {
            return;
        }
        int n = matching.size();
        int base = (int) (total / n);
        int remainder = (int) (total % n);
        for (int i = 0; i < n; i++) {
            matching.get(i).fluid().setContents(networkFluid, base + (i < remainder ? 1 : 0));
        }
    }

    // --- Gas layer --------------------------------------------------------------

    /**
     * Same rules as the fluid layer (one gas claims the network, push-anything, balanced pool), over
     * the mod's dedicated gas capability. Venting on pipe break is handled by the block entity.
     */
    private void tickGas(ServerLevel level, List<UniversalPipeBlockEntity> pipes) {
        int baseThroughput = Tuning.gasPipeThroughput();

        GasResource networkGas = GasResource.EMPTY;
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (!pipe.gas().resource().isEmpty()) {
                networkGas = pipe.gas().resource();
                break;
            }
        }

        try (Transaction tx = Transaction.openRoot()) {
            for (UniversalPipeBlockEntity pipe : pipes) {
                int throughput = baseThroughput * pipe.speedMultiplier();
                BlockPos pos = pipe.getBlockPos();
                for (Direction dir : Direction.values()) {
                    BlockPos np = pos.relative(dir);
                    if (this.memberSet.contains(np.asLong())) {
                        continue;
                    }
                    PipeIoMode mode = pipe.mode(dir, PipeResourceType.GAS);
                    if (!mode.isConnected()) {
                        continue;
                    }
                    ResourceHandler<GasResource> neighbor =
                            GasCapability.BLOCK.getCapability(level, np, null, null, dir.getOpposite());
                    if (neighbor == null) {
                        continue;
                    }
                    if (mode.canPull()) {
                        GasResource claimed = networkGas;
                        int pulled = ResourceHandlerUtil.move(neighbor, pipe.gas(),
                                r -> claimed.isEmpty() || r == claimed, throughput, tx);
                        if (pulled > 0) {
                            if (networkGas.isEmpty()) {
                                networkGas = pipe.gas().resource();
                            }
                            flowParticle(level, ParticleTypes.HAPPY_VILLAGER, pos, dir, false);
                        }
                    }
                    if (mode.canPush()) {
                        int pushed = ResourceHandlerUtil.move(pipe.gas(), neighbor,
                                r -> true, throughput, tx);
                        if (pushed > 0) {
                            flowParticle(level, ParticleTypes.HAPPY_VILLAGER, pos, dir, true);
                        }
                    }
                }
            }
            tx.commit();
        }

        if (networkGas == GasResource.EMPTY) {
            return;
        }
        List<UniversalPipeBlockEntity> matching = new ArrayList<>(pipes.size());
        long total = 0L;
        for (UniversalPipeBlockEntity pipe : pipes) {
            GasResource held = pipe.gas().resource();
            if (held.isEmpty() || held == networkGas) {
                matching.add(pipe);
                total += pipe.gas().amount();
            }
        }
        if (matching.isEmpty()) {
            return;
        }
        int n = matching.size();
        int base = (int) (total / n);
        int remainder = (int) (total % n);
        for (int i = 0; i < n; i++) {
            matching.get(i).gas().setContents(networkGas, base + (i < remainder ? 1 : 0));
        }
    }

    // --- Item layer -------------------------------------------------------------

    /**
     * Items travel as visible packets: pulling faces extract from inventories on a pulse, packets run
     * through segments at a configured speed, junctions hand them to the next segment toward their
     * destination, and arrival inserts into the target inventory. Destinations are chosen round-robin
     * over every reachable inventory that accepts the item; a full/blocked destination causes a
     * re-route, and with no route the packet parks in the pipe until one opens up. Nothing is dropped.
     */
    private void tickItems(ServerLevel level, List<UniversalPipeBlockEntity> pipes) {
        // 1. Advance, hand off and deliver travelling items.
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (pipe.items().isEmpty()) {
                continue;
            }
            float step = 1.0F / pipe.itemTicksPerBlock();
            BlockPos pos = pipe.getBlockPos();
            boolean changed = false;
            Iterator<TravellingItem> it = pipe.items().iterator();
            while (it.hasNext()) {
                TravellingItem item = it.next();

                if (item.isParked()) {
                    // Periodically look for a route that has opened up.
                    if ((level.getGameTime() & 7L) == 0L) {
                        Direction exit = chooseExit(level, pipe, item.resource(), null);
                        if (exit != null) {
                            item.redirect(item.from(), exit, 0.0F);
                            changed = true;
                        }
                    }
                    continue;
                }

                item.advance(step);
                if (item.progress() < 1.0F) {
                    continue;
                }

                Direction to = item.to();
                if (to == null) {
                    continue; // defensive: isParked() covered this, but make it provable
                }
                BlockPos np = pos.relative(to);

                // Hand off to the next pipe segment.
                if (this.memberSet.contains(np.asLong())
                        && level.getBlockEntity(np) instanceof UniversalPipeBlockEntity next) {
                    it.remove();
                    changed = true;
                    Direction enter = to.getOpposite();
                    Direction exit = chooseExit(level, next, item.resource(), enter);
                    next.items().add(new TravellingItem(item.resource(), item.amount(), enter, exit, 0.0F));
                    next.syncItems();
                    continue;
                }

                // Arrived at a non-pipe face: try to insert (the face filter must allow it).
                int inserted = 0;
                ItemResource deliveryFilter = pipe.filter(to);
                if (pipe.mode(to, PipeResourceType.ITEM).canPush()
                        && (deliveryFilter.isEmpty() || deliveryFilter.equals(item.resource()))) {
                    ResourceHandler<ItemResource> target =
                            Capabilities.Item.BLOCK.getCapability(level, np, null, null, to.getOpposite());
                    if (target != null) {
                        try (Transaction tx = Transaction.openRoot()) {
                            inserted = ResourceHandlerUtil.insertStacking(target, item.resource(), item.amount(), tx);
                            if (inserted > 0) {
                                tx.commit();
                            }
                        }
                    }
                }
                if (inserted >= item.amount()) {
                    it.remove();
                    changed = true;
                    continue;
                }
                if (inserted > 0) {
                    item.shrink(inserted);
                    changed = true;
                }
                // Full/blocked: re-route the remainder; with no route, park and wait.
                Direction exit = chooseExit(level, pipe, item.resource(), to);
                if (exit != null) {
                    item.redirect(to, exit, 0.0F);
                } else {
                    item.redirect(to, null, 0.0F);
                }
                changed = true;
            }
            if (changed) {
                pipe.syncItems();
            }
        }

        // 2. Extraction pulse on pulling faces.
        if (level.getGameTime() % Tuning.itemPipeExtractPeriod() != 0L) {
            return;
        }
        int extractMax = Tuning.ITEM_PIPE_EXTRACT_AMOUNT;
        for (UniversalPipeBlockEntity pipe : pipes) {
            if (pipe.items().size() >= UniversalPipeBlockEntity.MAX_TRAVELLING_ITEMS * pipe.capacityMultiplier()) {
                continue;
            }
            BlockPos pos = pipe.getBlockPos();
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (this.memberSet.contains(np.asLong())
                        || !pipe.mode(dir, PipeResourceType.ITEM).canPull()) {
                    continue;
                }
                ResourceHandler<ItemResource> source =
                        Capabilities.Item.BLOCK.getCapability(level, np, null, null, dir.getOpposite());
                if (source == null) {
                    continue;
                }
                // Peek what would come out (transaction aborted), route it, then extract for real.
                // NOTE: extractFirst returns null (not an empty stack) when nothing is extractable.
                ItemResource faceFilter = pipe.filter(dir);
                ItemResource peeked;
                try (Transaction tx = Transaction.openRoot()) {
                    ResourceStack<ItemResource> got = ResourceHandlerUtil.extractFirst(source,
                            r -> faceFilter.isEmpty() || r.equals(faceFilter), extractMax, tx);
                    peeked = got == null ? ItemResource.EMPTY : got.resource();
                }
                if (peeked.isEmpty()) {
                    continue;
                }
                Direction exit = chooseExit(level, pipe, peeked, dir);
                if (exit == null) {
                    continue; // nowhere to send it — leave the items where they are
                }
                int amount = 0;
                try (Transaction tx = Transaction.openRoot()) {
                    ResourceStack<ItemResource> got =
                            ResourceHandlerUtil.extractFirst(source, peeked::equals, extractMax, tx);
                    amount = got == null ? 0 : got.amount();
                    if (amount > 0) {
                        tx.commit();
                    }
                }
                if (amount > 0) {
                    pipe.items().add(new TravellingItem(peeked, amount, dir, exit, 0.0F));
                    pipe.syncItems();
                }
            }
        }
    }

    /**
     * Pick the exit face an item should take from {@code start}: BFS the pipe graph for every
     * destination inventory that can accept the resource (a non-pipe neighbour behind a face whose
     * item mode allows pushing), rotate round-robin over the candidates, and return the first BFS step
     * toward the chosen one ({@code null} = no route right now). {@code excludeFace} stops the packet
     * from being sent straight back into the inventory/face it came from.
     */
    @Nullable
    private Direction chooseExit(ServerLevel level, UniversalPipeBlockEntity start, ItemResource resource,
            @Nullable Direction excludeFace) {
        record Candidate(ResourceHandler<ItemResource> target, Direction firstStep) {
        }
        List<Candidate> candidates = new ArrayList<>();

        // BFS over member pipes, remembering the first step out of the start pipe.
        Long2LongOpenHashMap firstSteps = new Long2LongOpenHashMap(); // pos -> Direction ordinal
        firstSteps.defaultReturnValue(-1L);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        BlockPos startPos = start.getBlockPos();
        queue.add(startPos);
        seen.add(startPos.asLong());

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe)) {
                continue;
            }
            boolean isStart = pos.equals(startPos);
            long inheritedStep = firstSteps.get(pos.asLong());

            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                boolean isPipe = this.memberSet.contains(np.asLong());

                if (isPipe) {
                    if (seen.add(np.asLong())) {
                        firstSteps.put(np.asLong(), isStart ? dir.get3DDataValue() : inheritedStep);
                        queue.add(np);
                    }
                    continue;
                }
                if (isStart && dir == excludeFace) {
                    continue;
                }
                if (!pipe.mode(dir, PipeResourceType.ITEM).canPush()) {
                    continue;
                }
                ItemResource faceFilter = pipe.filter(dir);
                if (!faceFilter.isEmpty() && !faceFilter.equals(resource)) {
                    continue;
                }
                ResourceHandler<ItemResource> target =
                        Capabilities.Item.BLOCK.getCapability(level, np, null, null, dir.getOpposite());
                if (target == null) {
                    continue;
                }
                Direction firstStep = isStart ? dir : Direction.from3DDataValue((int) inheritedStep);
                candidates.add(new Candidate(target, firstStep));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        // Round-robin over candidates; take the first that actually accepts the item right now.
        int offset = Math.floorMod(this.itemRoundRobin++, candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get((offset + i) % candidates.size());
            try (Transaction tx = Transaction.openRoot()) {
                if (ResourceHandlerUtil.insertStacking(candidate.target(), resource, 1, tx) > 0) {
                    return candidate.firstStep(); // aborted — it was only a probe
                }
            }
        }
        return null;
    }

    /**
     * Spawn a sparse, per-layer flow particle drifting along an active connection so players can see
     * what is moving (glow = energy, splash = fluid; gas/item layers add their own). Throttled.
     */
    private static void flowParticle(ServerLevel level, ParticleOptions particle, BlockPos pipe,
            Direction dir, boolean outward) {
        if (level.getRandom().nextFloat() >= 0.18F) {
            return;
        }
        Direction flow = outward ? dir : dir.getOpposite();
        // Start near the pipe's face and drift toward the destination.
        double x = pipe.getX() + 0.5 + dir.getStepX() * 0.45;
        double y = pipe.getY() + 0.5 + dir.getStepY() * 0.45;
        double z = pipe.getZ() + 0.5 + dir.getStepZ() * 0.45;
        level.sendParticles(particle, x, y, z, 0,
                flow.getStepX() * 0.08, flow.getStepY() * 0.08, flow.getStepZ() * 0.08, 1.0);
    }
}

