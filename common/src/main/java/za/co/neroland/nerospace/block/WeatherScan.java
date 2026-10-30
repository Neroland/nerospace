package za.co.neroland.nerospace.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import za.co.neroland.nerospace.block.rules.WeatherScanRules;

/**
 * Weather transition scan: on an event start/end the loaded chunks near players are queued (in a shuffled
 * order) and {@link #tick} drains at most {@link WeatherScanRules#CHUNKS_PER_TICK} of them per server tick, so a
 * transition over N chunks takes {@link WeatherScanRules#ticksFor ticksFor(N)} ticks. Inside a chunk at most
 * {@code perChunkCap} matches are visited, starting from a random section and a random cell so a capped chunk does not
 * always grow/flip the same blocks. Sections whose palette cannot hold a match cost one palette check. Used by Frost
 * Crystal (cap 64) and Dawnband Stone (cap 256). Holds chunk coordinates only.
 */
public final class WeatherScan {

    private WeatherScan() {
    }

    @FunctionalInterface
    public interface Visitor {
        /** Return true when the block was changed (counts against the cap). */
        boolean visit(BlockPos pos, BlockState state);
    }

    private record Job(ServerLevel level, Predicate<BlockState> match, int perChunkCap, Visitor visitor,
            List<Long> chunks, int[] next) {
    }

    private static final Deque<Job> JOBS = new ArrayDeque<>();

    /** Queues a transition; returns the number of chunks queued. Chunks are re-checked for loading when drained. */
    public static int forEachLoadedBlock(ServerLevel level, Predicate<BlockState> match, int perChunkCap, Visitor visitor) {
        List<Long> chunks = new ArrayList<>();
        LoadedChunks.forEachNearPlayers(level, (cx, cz) -> chunks.add(ChunkPos.pack(cx, cz)));
        if (chunks.isEmpty() || perChunkCap <= 0) {
            return 0;
        }
        Collections.shuffle(chunks, new java.util.Random(level.getRandom().nextLong()));
        synchronized (JOBS) {
            JOBS.addLast(new Job(level, match, perChunkCap, visitor, chunks, new int[1]));
        }
        return chunks.size();
    }

    /** Per server tick (from {@link BlockTicks}): process up to 64 queued chunks across all pending transitions. */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        int budget = WeatherScanRules.CHUNKS_PER_TICK;
        synchronized (JOBS) {
            while (budget > 0 && !JOBS.isEmpty()) {
                Job job = JOBS.peekFirst();
                if (job.level().getServer() != server) {
                    JOBS.pollFirst(); // level from a stopped server
                    continue;
                }
                while (budget > 0 && job.next()[0] < job.chunks().size()) {
                    long key = job.chunks().get(job.next()[0]++);
                    scanChunk(job, ChunkPos.getX(key), ChunkPos.getZ(key));
                    budget--;
                }
                if (job.next()[0] >= job.chunks().size()) {
                    JOBS.pollFirst();
                }
            }
        }
    }

    static void clear() {
        synchronized (JOBS) {
            JOBS.clear();
        }
    }

    private static void scanChunk(Job job, int cx, int cz) {
        ServerLevel level = job.level();
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            return;
        }
        RandomSource random = level.getRandom();
        LevelChunkSection[] sections = chunk.getSections();
        int count = sections.length;
        int startSection = count == 0 ? 0 : random.nextInt(count);
        int touched = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int s = 0; s < count && WeatherScanRules.underCap(touched, job.perChunkCap()); s++) {
            int i = (startSection + s) % count;
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(job.match())) {
                continue;
            }
            int baseY = (chunk.getMinSectionY() + i) << 4;
            int startCell = random.nextInt(4096);
            for (int k = 0; k < 4096 && WeatherScanRules.underCap(touched, job.perChunkCap()); k++) {
                int c = (startCell + k) & 4095;
                int x = c & 15;
                int z = (c >> 4) & 15;
                int y = c >> 8;
                BlockState state = section.getBlockState(x, y, z);
                if (job.match().test(state) && job.visitor().visit(pos.set((cx << 4) + x, baseY + y, (cz << 4) + z), state)) {
                    touched++;
                }
            }
        }
    }
}
