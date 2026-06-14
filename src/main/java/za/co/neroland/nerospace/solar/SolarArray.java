package za.co.neroland.nerospace.solar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * A connected run of same-tier solar panels treated as ONE machine: total storage is the sum of every
 * member's buffer and total generation is the sum of every member's (sky-/weather-/dimension-scaled)
 * output. The pooled energy is kept balanced evenly across the members' buffers, so a pipe pulling
 * from ANY panel's output face effectively drains the whole array (every face is an output port).
 *
 * <p>Built by flood-fill from a seed panel, exactly like {@link za.co.neroland.nerospace.pipe.PipeNetwork}:
 * membership is rebuilt lazily so placing or breaking a panel (merging/splitting arrays) needs no
 * explicit hooks. Only neighbours of the SAME {@link SolarTier} are adopted — different tiers stay
 * separate arrays.</p>
 */
public final class SolarArray {

    private static final int MAX_MEMBERS = 4096;

    private final SolarTier tier;
    private final List<BlockPos> members;
    private final LongOpenHashSet memberSet;
    private boolean valid = true;
    private long lastTick = -1L;

    private SolarArray(SolarTier tier, List<BlockPos> members, LongOpenHashSet memberSet) {
        this.tier = tier;
        this.members = members;
        this.memberSet = memberSet;
    }

    public boolean isValid() {
        return this.valid;
    }

    public SolarTier tier() {
        return this.tier;
    }

    public int size() {
        return this.members.size();
    }

    /** Flood-fill the connected same-tier panels from {@code seed}, build the array, adopt every member. */
    public static SolarArray getOrBuild(ServerLevel level, BlockPos seed, SolarTier tier) {
        List<BlockPos> members = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        seen.add(seed.asLong());

        while (!queue.isEmpty() && members.size() < MAX_MEMBERS) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel) || panel.tier() != tier) {
                continue;
            }
            members.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (seen.add(np.asLong())
                        && level.getBlockEntity(np) instanceof SolarPanelBlockEntity neighbour
                        && neighbour.tier() == tier) {
                    queue.add(np);
                }
            }
        }

        LongOpenHashSet memberSet = new LongOpenHashSet(members.size());
        for (BlockPos pos : members) {
            memberSet.add(pos.asLong());
        }
        SolarArray array = new SolarArray(tier, members, memberSet);
        for (BlockPos pos : members) {
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel) {
                panel.adopt(array);
            }
        }
        return array;
    }

    /** Generate this tick's pooled energy and re-balance the buffers. Runs at most once per game tick. */
    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime == this.lastTick) {
            return;
        }
        this.lastTick = gameTime;

        List<SolarPanelBlockEntity> panels = new ArrayList<>(this.members.size());
        for (BlockPos pos : this.members) {
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel && panel.tier() == this.tier) {
                panels.add(panel);
            } else {
                this.valid = false; // a member vanished/changed — members rebuild next tick
                return;
            }
        }
        if (panels.isEmpty()) {
            this.valid = false;
            return;
        }

        // Each panel contributes its own daylight-scaled output (a shaded panel adds less); the sum is
        // the array's generation. Add into the per-panel buffers, then balance them into one pool.
        long total = 0L;
        for (SolarPanelBlockEntity panel : panels) {
            panel.generate(panel.generationThisTick(level));
            total += panel.energy().getAmountAsInt();
        }
        int n = panels.size();
        int base = (int) (total / n);
        int remainder = (int) (total % n);
        for (int i = 0; i < n; i++) {
            panels.get(i).energy().setStored(base + (i < remainder ? 1 : 0));
        }
    }
}
