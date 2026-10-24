package za.co.neroland.nerospace.block;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import za.co.neroland.nerospace.block.rules.ChunkCounterRules;

/**
 * Per-chunk block counts for capped spreading (002 §3), cached for {@link ChunkCounterRules#TTL_TICKS}. A cold
 * entry costs one palette check per section and a scan only of sections that may hold the block. Keyed weakly by
 * level; holds counts only.
 */
public final class ChunkSpreadCounter {

    private static final Map<ServerLevel, Map<Block, Long2LongOpenHashMap>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ChunkSpreadCounter() {
    }

    /** Count of {@code block} in the chunk holding {@code pos}. */
    public static int count(ServerLevel level, BlockPos pos, Block block) {
        long chunkKey = ChunkPos.pack(pos);
        Long2LongOpenHashMap byChunk = CACHE.computeIfAbsent(level, l -> new WeakHashMap<>())
                .computeIfAbsent(block, b -> new Long2LongOpenHashMap());
        long now = level.getGameTime();
        long packed = byChunk.getOrDefault(chunkKey, -1L);
        if (packed >= 0) {
            long at = packed >>> 20;
            if (!ChunkCounterRules.stale(at, now)) {
                return (int) (packed & 0xFFFFF);
            }
        }
        int count = scan(level, pos, block);
        byChunk.put(chunkKey, (now << 20) | Math.min(count, 0xFFFFF));
        return count;
    }

    /** Bump a cached count after this mod placed one (keeps the cap honest between rescans). */
    public static void increment(ServerLevel level, BlockPos pos, Block block) {
        Map<Block, Long2LongOpenHashMap> perLevel = CACHE.get(level);
        Long2LongOpenHashMap byChunk = perLevel == null ? null : perLevel.get(block);
        if (byChunk == null) {
            return;
        }
        long chunkKey = ChunkPos.pack(pos);
        long packed = byChunk.getOrDefault(chunkKey, -1L);
        if (packed >= 0) {
            byChunk.put(chunkKey, (packed & ~0xFFFFFL) | Math.min((packed & 0xFFFFF) + 1, 0xFFFFF));
        }
    }

    private static int scan(ServerLevel level, BlockPos pos, Block block) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return 0;
        }
        int count = 0;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> s.is(block))) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).is(block)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}
