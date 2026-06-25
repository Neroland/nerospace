package za.co.neroland.nerospace.machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * A connected run of same-tier solar panel <b>units</b> treated as ONE machine: total storage is the
 * sum of every unit's buffer and total generation the sum of every unit's (sky-/weather-/dimension-
 * scaled) output. The pooled energy is kept balanced across the unit buffers, so a pipe pulling from
 * ANY panel face drains the whole array.
 *
 * <p>Built by flood-fill across all connected same-tier cells; membership is rebuilt lazily so placing
 * or breaking a panel needs no explicit hooks. Only the SAME {@link SolarTier} is adopted — different
 * tiers stay separate arrays.</p>
 *
 * <p>Cross-loader port: identical to the standalone {@code solar.SolarArray} except the per-unit energy
 * store is the multiloader {@link za.co.neroland.nerospace.energy.EnergyBuffer} (raw int accessors)
 * rather than the NeoForge transfer handler.</p>
 */
public final class SolarArray {

    private static final int MAX_CELLS = 16_384;

    private final SolarTier tier;
    /** Member <b>anchor</b> positions (one per unit; every 1×1 cell is its own anchor). */
    private final List<BlockPos> anchors;
    private boolean valid = true;
    private long lastTick = -1L;

    private SolarArray(SolarTier tier, List<BlockPos> anchors) {
        this.tier = tier;
        this.anchors = anchors;
    }

    public boolean isValid() {
        return this.valid;
    }

    public SolarTier tier() {
        return this.tier;
    }

    /** Number of pooled units in the array. */
    public int size() {
        return this.anchors.size();
    }

    /** Flood-fill the connected same-tier cells from {@code seed}, collect the distinct unit anchors. */
    public static SolarArray getOrBuild(ServerLevel level, BlockPos seed, SolarTier tier) {
        List<BlockPos> anchors = new ArrayList<>();
        LongOpenHashSet anchorSet = new LongOpenHashSet();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        seen.add(seed.asLong());

        int visited = 0;
        while (!queue.isEmpty() && visited < MAX_CELLS) {
            BlockPos pos = java.util.Objects.requireNonNull(queue.poll());
            if (!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity cell) || cell.tier() != tier) {
                continue;
            }
            visited++;
            BlockPos anchor = cell.anchorPos();
            if (anchorSet.add(anchor.asLong())) {
                anchors.add(anchor);
            }
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(java.util.Objects.requireNonNull(dir));
                if (seen.add(np.asLong())
                        && level.getBlockEntity(np) instanceof SolarPanelBlockEntity neighbour
                        && neighbour.tier() == tier) {
                    queue.add(np);
                }
            }
        }

        SolarArray array = new SolarArray(tier, anchors);
        for (BlockPos anchor : anchors) {
            if (level.getBlockEntity(NerospaceCommon.requireNonNull(anchor)) instanceof SolarPanelBlockEntity a) {
                a.adopt(array);
            }
        }
        return array;
    }

    /** Generate this tick's pooled energy and re-balance the buffers. Runs once per game tick. */
    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime == this.lastTick) {
            return;
        }
        this.lastTick = gameTime;

        List<SolarPanelBlockEntity> units = new ArrayList<>(this.anchors.size());
        for (BlockPos anchor : this.anchors) {
            if (level.getBlockEntity(NerospaceCommon.requireNonNull(anchor)) instanceof SolarPanelBlockEntity a
                    && a.isAnchor() && a.tier() == this.tier) {
                units.add(a);
            } else {
                this.valid = false; // a unit vanished/changed — members rebuild next tick
                return;
            }
        }
        if (units.isEmpty()) {
            this.valid = false;
            return;
        }

        // Each unit contributes its own daylight-scaled output; the sum is the array's generation. Add
        // into the per-unit buffers, then balance them into one pool.
        long total = 0L;
        for (SolarPanelBlockEntity unit : units) {
            unit.generate(unit.generationThisTick(level));
            total += unit.energy().getRaw();
        }
        int n = units.size();
        int base = (int) (total / n);
        int remainder = (int) (total % n);
        for (int i = 0; i < n; i++) {
            units.get(i).energy().setRaw(base + (i < remainder ? 1 : 0));
        }
    }
}
