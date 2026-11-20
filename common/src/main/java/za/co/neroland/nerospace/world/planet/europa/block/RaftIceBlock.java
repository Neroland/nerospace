package za.co.neroland.nerospace.world.planet.europa.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.RaftFallRules;

/**
 * Chaos Ice: broken rafts of ice. A neighbour change schedules a check two ticks later; the check flood-fills the
 * face-connected chaos ice (6 blocks at most) and, when none of them has a solid block other than chaos ice directly
 * below, drops the whole raft as falling blocks. A group larger than 6 never falls, so the open chaos terrain is
 * stable; only small undercut pieces come down.
 *
 * <p>Cost: a check reads at most 7 nodes x 6 neighbours plus one support read each, and at most 2 rafts fall per chunk
 * per game tick (a third waits two ticks). Generation never runs it: worldgen sends no neighbour update.</p>
 */
public class RaftIceBlock extends Block {

    public static final MapCodec<RaftIceBlock> CODEC = BlockCodecs.simple(RaftIceBlock::new);

    /** Fall plans run per chunk in the current game tick, per level. */
    private static final Map<ServerLevel, Budget> BUDGETS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Budget {
        private long gameTime = Long.MIN_VALUE;
        private final Map<Long, Integer> plans = new HashMap<>();
    }

    public RaftIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends RaftIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        if (level instanceof ServerLevel) {
            level.scheduleTick(pos, this, RaftFallRules.CHECK_DELAY);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> group = new ArrayList<>();
        boolean truncated = collect(level, pos, group);
        long[] keys = new long[group.size()];
        int[] ys = new int[group.size()];
        boolean[] supported = new boolean[group.size()];
        for (int i = 0; i < keys.length; i++) {
            BlockPos p = group.get(i);
            keys[i] = p.asLong();
            ys[i] = p.getY();
            BlockState below = level.getBlockState(p.below());
            supported[i] = p.getY() <= level.getMinY() || (!below.is(this) && !FallingBlock.isFree(below));
        }
        long[] plan = RaftFallRules.plan(keys, ys, supported, truncated);
        if (plan.length == 0) {
            return;
        }
        if (!takeBudget(level, pos)) {
            level.scheduleTick(pos, this, RaftFallRules.CHECK_DELAY);
            return;
        }
        for (long key : plan) {
            BlockPos p = BlockPos.of(key);
            BlockState s = level.getBlockState(p);
            if (s.is(this)) {
                FallingBlockEntity.fall(level, p, s);
            }
        }
    }

    /** Flood fill of face-connected chaos ice from {@code start}; true when it stopped at the cap with more left. */
    private boolean collect(ServerLevel level, BlockPos start, List<BlockPos> out) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        out.add(start.immutable());
        queue.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (out.contains(n) || !level.getBlockState(n).is(this)) {
                    continue;
                }
                if (out.size() >= RaftFallRules.CAP) {
                    return true;
                }
                out.add(n);
                queue.add(n);
            }
        }
        return false;
    }

    private static boolean takeBudget(ServerLevel level, BlockPos pos) {
        Budget budget = BUDGETS.computeIfAbsent(level, l -> new Budget());
        synchronized (budget) {
            long now = level.getGameTime();
            if (budget.gameTime != now) {
                budget.gameTime = now;
                budget.plans.clear();
            }
            long chunk = ((pos.getX() >> 4) & 0xFFFFFFFFL) | (((pos.getZ() >> 4) & 0xFFFFFFFFL) << 32);
            int used = budget.plans.getOrDefault(chunk, 0);
            if (!RaftFallRules.allowed(used)) {
                return false;
            }
            budget.plans.put(chunk, used + 1);
            return true;
        }
    }
}
