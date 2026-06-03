package za.co.neroland.nerospace.pipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import za.co.neroland.nerospace.Config;

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

    /** Move + balance the energy layer. Guarded so it runs at most once per game tick. */
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

        int throughput = Config.ENERGY_PIPE_THROUGHPUT.get();

        // Pull from providers / push to receivers on every non-pipe face, within one transaction.
        try (Transaction tx = Transaction.openRoot()) {
            for (BlockPos pos : this.members) {
                if (!(level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe)) {
                    continue;
                }
                for (Direction dir : Direction.values()) {
                    BlockPos np = pos.relative(dir);
                    if (this.memberSet.contains(np.asLong())) {
                        continue; // internal pipe-to-pipe link is handled by balancing
                    }
                    PipeConnectionMode mode = pipe.faceMode(dir);
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
                            flowParticle(level, pos, dir, false); // into the pipe (from a provider)
                        }
                    }
                    if (mode.canPush()) {
                        int pushed = EnergyHandlerUtil.move(pipe.energy(), neighbor, throughput, tx);
                        if (pushed > 0) {
                            flowParticle(level, pos, dir, true);  // out of the pipe (to a receiver)
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

    /**
     * Spawn a sparse, colour-coded flow particle drifting along an active connection so players can see
     * energy moving (cyan = energy; item/fluid/gas layers will use their own colours). Throttled.
     */
    private static void flowParticle(ServerLevel level, BlockPos pipe, Direction dir, boolean outward) {
        if (level.getRandom().nextFloat() >= 0.18F) {
            return;
        }
        Direction flow = outward ? dir : dir.getOpposite();
        // Start near the pipe's face and drift toward the destination.
        double x = pipe.getX() + 0.5 + dir.getStepX() * 0.45;
        double y = pipe.getY() + 0.5 + dir.getStepY() * 0.45;
        double z = pipe.getZ() + 0.5 + dir.getStepZ() * 0.45;
        level.sendParticles(ParticleTypes.GLOW, x, y, z, 0,
                flow.getStepX() * 0.08, flow.getStepY() * 0.08, flow.getStepZ() * 0.08, 1.0);
    }
}

