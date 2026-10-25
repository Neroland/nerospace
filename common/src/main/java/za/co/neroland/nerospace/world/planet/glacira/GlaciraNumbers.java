package za.co.neroland.nerospace.world.planet.glacira;

import za.co.neroland.nerospace.block.rules.CrystalStageRules;

/**
 * Glacira's tunable numbers in one place (pure, tested): the frost-crystal outline per stage and the diamond-dust
 * freeze budget (003 §4: exposed water to ice, one cell per chunk per 100 ticks, at most 32 per chunk per event).
 */
public final class GlaciraNumbers {

    /** Freeze attempt cadence while diamond dust is active, in ticks. */
    public static final int FREEZE_INTERVAL_TICKS = 100;
    /** Water cells frozen per chunk per event, at most. */
    public static final int FREEZE_CAP_PER_EVENT = 32;

    private GlaciraNumbers() {
    }

    /** Horizontal inset (in 1/16 block) of the cluster outline at {@code stage}: 5, 4, 3, 2. */
    public static double clusterInset(int stage) {
        return 5.0 - clamp(stage);
    }

    /** Height (in 1/16 block) of the cluster outline at {@code stage}: 5, 7, 10, 13. */
    public static double clusterHeight(int stage) {
        return switch (clamp(stage)) {
            case 0 -> 5.0;
            case 1 -> 7.0;
            case 2 -> 10.0;
            default -> 13.0;
        };
    }

    /**
     * Whether one diamond-dust freeze step turns the sampled cell to ice: only a still water source with open sky
     * above, while the chunk is under its per-event cap.
     */
    public static boolean freezes(boolean waterSource, boolean skyExposed, int frozenThisEvent, int cap) {
        return waterSource && skyExposed && frozenThisEvent < Math.min(cap, FREEZE_CAP_PER_EVENT);
    }

    private static int clamp(int stage) {
        return Math.max(0, Math.min(CrystalStageRules.MAX_STAGE, stage));
    }
}
