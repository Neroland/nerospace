package za.co.neroland.nerospace.block.rules;

/** Sulfur Crust (002 §1.3): fire next to it ignites up to 5 flammable neighbours, no chaining in a tick. */
public final class IgniteRules {

    public static final int MAX_TARGETS = 5;
    public static final int FLAMMABILITY = 30;
    public static final int FIRE_SPREAD = 60;

    private IgniteRules() {
    }

    /** Mask of neighbours (0..5) that get fire above them; at most {@code min(cap, 5)} bits. */
    public static int targets(boolean[] adjacentFlammable, int cap) {
        int limit = Math.min(cap, MAX_TARGETS);
        int mask = 0;
        int n = 0;
        for (int i = 0; i < Math.min(6, adjacentFlammable.length) && n < limit; i++) {
            if (adjacentFlammable[i]) {
                mask |= 1 << i;
                n++;
            }
        }
        return mask;
    }
}
