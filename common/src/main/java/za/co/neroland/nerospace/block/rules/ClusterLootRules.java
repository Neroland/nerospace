package za.co.neroland.nerospace.block.rules;

/**
 * Nyx Crystal Cluster: a full cluster (stage 3) gives 2 shards to a pickaxe (Fortune adds more), 1 to anything else;
 * stages 0-2 give nothing. The loot tables carry the numbers; this table is what they are tested against. Light by
 * stage 1 / 2 / 4 / 5.
 */
public final class ClusterLootRules {

    public static final int MAX_STAGE = 3;
    private static final int[] LIGHT = {1, 2, 4, 5};

    private ClusterLootRules() {
    }

    public static int count(int stage, boolean pickaxe) {
        if (stage < MAX_STAGE) {
            return 0;
        }
        return pickaxe ? 2 : 1;
    }

    public static int light(int stage) {
        return LIGHT[Math.max(0, Math.min(MAX_STAGE, stage))];
    }

    /** Shape height in pixels by stage (bud to full cluster). */
    public static int height(int stage) {
        return new int[] {3, 4, 5, 7}[Math.max(0, Math.min(MAX_STAGE, stage))];
    }

    /** Inset from each side in pixels by stage. */
    public static int inset(int stage) {
        return new int[] {4, 3, 3, 3}[Math.max(0, Math.min(MAX_STAGE, stage))];
    }
}
