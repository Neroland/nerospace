package za.co.neroland.nerospace.block.rules;

/** Frost Crystal Cluster: grows one stage when a diamond-dust event ends; stages 0–3, light 3/5/7/9. */
public final class CrystalStageRules {

    public static final int MAX_STAGE = 3;
    /** Clusters advanced per chunk per event end (config may lower). */
    public static final int PER_CHUNK_CAP = 64;

    private CrystalStageRules() {
    }

    public static int nextStage(int stage, boolean skyExposed, boolean eventEnded) {
        if (!skyExposed || !eventEnded || stage >= MAX_STAGE) {
            return Math.max(0, Math.min(MAX_STAGE, stage));
        }
        return Math.max(0, stage) + 1;
    }

    public static int light(int stage) {
        return 3 + 2 * Math.max(0, Math.min(MAX_STAGE, stage));
    }

    /** Drops without silk touch: {@code frost_shard × (1 + stage)}. */
    public static int shardDrops(int stage) {
        return 1 + Math.max(0, Math.min(MAX_STAGE, stage));
    }
}
