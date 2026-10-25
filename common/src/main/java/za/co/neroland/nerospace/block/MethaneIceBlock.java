package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.MethaneRules;
import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * Methane Ice (002 §1.4, §11.5): when it is placed, or a flame block appears next to it, it scans its 5×5×5 for
 * fire/lava/campfire/torch; while one is there it re-checks on a 40-tick schedule. A flame within 2 blocks (and chain
 * budget left: 4 per chunk per 100 ticks, config may lower) explodes it at power 2. Blocks are broken only when
 * {@code blocks.methaneIceBreaksBlocks} (default false); entities are hurt. With the default no fire is placed either
 * (vanilla only places explosion fire when the explosion interacts with blocks) — it "ignites nothing".
 */
public class MethaneIceBlock extends Block {

    public static final MapCodec<MethaneIceBlock> CODEC = BlockCodecs.simple(MethaneIceBlock::new);

    /** level → (chunk → packed (windowStart << 8 | count)). Counts only. */
    private static final Map<Level, Long2LongOpenHashMap> CHAIN = Collections.synchronizedMap(new WeakHashMap<>());

    public MethaneIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends MethaneIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        // Only a flame-type neighbour can arm it: flowing water or redstone next to it never triggers the scan.
        if (level instanceof ServerLevel server && isFlame(neighbor.defaultBlockState())) {
            check(server, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            check(server, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        check(level, pos);
    }

    private static boolean isFlame(BlockState s) {
        return s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.LAVA) || s.is(Blocks.TORCH)
                || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH);
    }

    /** Chebyshev distance to the nearest flame in the 5×5×5 (≤ 125 reads), or −1. */
    private static int nearestFlame(ServerLevel level, BlockPos pos) {
        int best = -1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = MethaneRules.SCAN_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.isLoaded(p) && isFlame(level.getBlockState(p))) {
                        int d = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                        if (best < 0 || d < best) {
                            best = d;
                        }
                    }
                }
            }
        }
        return best;
    }

    private void check(ServerLevel level, BlockPos pos) {
        int flame = nearestFlame(level, pos);
        if (flame < 0) {
            return; // disarmed: no schedule while no flame is near
        }
        long chunk = ChunkPos.pack(pos);
        long now = level.getGameTime();
        Long2LongOpenHashMap byChunk = CHAIN.computeIfAbsent(level, l -> new Long2LongOpenHashMap());
        long packed = byChunk.getOrDefault(chunk, 0L);
        long windowStart = packed >>> 8;
        int count = (int) (packed & 0xFF);
        if (now - windowStart >= MethaneRules.CHAIN_WINDOW_TICKS || now < windowStart) {
            byChunk.remove(chunk); // window closed: prune, re-added only if this one explodes
            windowStart = now;
            count = 0;
        }
        boolean budget = MethaneRules.chainBudgetLeft(count, NerospaceConfig.methaneChainCap());
        if (MethaneRules.ignites(flame, budget)) {
            byChunk.put(chunk, (windowStart << 8) | Math.min(255, count + 1));
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, MethaneRules.POWER, true,
                    NerospaceConfig.methaneIceBreaksBlocks() ? Level.ExplosionInteraction.BLOCK
                            : Level.ExplosionInteraction.NONE);
        } else {
            level.scheduleTick(pos, this, MethaneRules.SCAN_INTERVAL_TICKS);
        }
    }
}
