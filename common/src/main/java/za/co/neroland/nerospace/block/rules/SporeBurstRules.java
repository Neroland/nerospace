package za.co.neroland.nerospace.block.rules;

/** Spore Pod area cloud (002 §1.5, §2.1.1): radius 2.5, 100 ticks, Nausea I 8 s; the pod is consumed. */
public final class SporeBurstRules {

    public static final double RADIUS = 2.5;
    public static final int DURATION_TICKS = 100;
    public static final int NAUSEA_TICKS = 160;
    public static final int PARTICLES = 30;
    /** Recipe: 4 pods + 1 gunpowder → 2 spore bombs. */
    public static final int BOMB_PODS = 4;
    public static final int BOMB_OUTPUT = 2;

    private SporeBurstRules() {
    }

    public record Cloud(double radius, int durationTicks, int nauseaTicks) {
    }

    public static Cloud cloud(double radius, int durationTicks) {
        return new Cloud(radius, durationTicks, NAUSEA_TICKS);
    }

    public static Cloud cloud() {
        return cloud(RADIUS, DURATION_TICKS);
    }

    /** Shears or silk touch take the pod without bursting. */
    public static boolean bursts(boolean shearsOrSilk) {
        return !shearsOrSilk;
    }
}
