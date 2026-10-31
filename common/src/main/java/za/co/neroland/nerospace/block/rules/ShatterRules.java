package za.co.neroland.nerospace.block.rules;

/**
 * Crazed Crystal: an impact from 3 or more blocks (a living entity, a falling block or a rocket) shatters the natural
 * crystal it lands on and up to 7 connected Crazed Crystal around it (flood fill, 8 per impact). Player-placed crystal
 * never shatters. The faller takes half the fall damage (safety glass).
 */
public final class ShatterRules {

    public static final double MIN_FALL = 3.0;
    /** Blocks broken per impact, the one landed on included. */
    public static final int CAP = 8;
    /** Block particles per impact. */
    public static final int PARTICLES = 12;

    private ShatterRules() {
    }

    public static boolean shatters(double fallDistance, boolean impactor, boolean playerPlaced) {
        return impactor && !playerPlaced && fallDistance >= MIN_FALL;
    }

    /**
     * One flood-fill step: which neighbours (by index) join the cascade, given which neighbours are natural Crazed
     * Crystal and how many more blocks the impact may still break. Returns a mask with at most {@code cap} bits set.
     */
    public static boolean[] cascade(boolean[] neighbourIsCrazed, int cap) {
        boolean[] out = new boolean[neighbourIsCrazed.length];
        int left = Math.max(0, cap);
        for (int i = 0; i < neighbourIsCrazed.length && left > 0; i++) {
            if (neighbourIsCrazed[i]) {
                out[i] = true;
                left--;
            }
        }
        return out;
    }

    public static float fallDamageFactor() {
        return 0.5F;
    }
}
