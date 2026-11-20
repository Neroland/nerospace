package za.co.neroland.nerospace.block.rules;

import java.util.Arrays;

/**
 * Chaos Ice rafts: when a small connected group of chaos ice (6 blocks at most, joined face to face) has nothing solid
 * under any of its members, the whole group falls. A group that is larger than the cap, or has one supported member,
 * stays. Blocks are identified by an opaque key so the rule needs no world.
 */
public final class RaftFallRules {

    /** Largest group that can fall. */
    public static final int CAP = 6;
    /** Fall plans carried out per chunk per game tick. */
    public static final int MAX_PLANS_PER_CHUNK_TICK = 2;
    /** Delay from a neighbour change to the check, in ticks. */
    public static final int CHECK_DELAY = 2;

    private static final long[] NONE = new long[0];

    private RaftFallRules() {
    }

    /**
     * The blocks that fall, lowest first then by key, or none.
     *
     * @param keys      one key per member of the connected group
     * @param ys        each member's Y
     * @param supported whether each member has a solid block other than chaos ice directly below
     * @param truncated whether the flood fill stopped at the cap with more group left
     */
    public static long[] plan(long[] keys, int[] ys, boolean[] supported, boolean truncated) {
        if (truncated || keys.length == 0 || keys.length > CAP) {
            return NONE;
        }
        for (boolean s : supported) {
            if (s) {
                return NONE;
            }
        }
        Integer[] order = new Integer[keys.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> ys[a] != ys[b] ? Integer.compare(ys[a], ys[b]) : Long.compare(keys[a], keys[b]));
        long[] out = new long[keys.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = keys[order[i]];
        }
        return out;
    }

    /** Whether one more plan may run in a chunk that has already run {@code plansThisTick} this game tick. */
    public static boolean allowed(int plansThisTick) {
        return plansThisTick < MAX_PLANS_PER_CHUNK_TICK;
    }
}
