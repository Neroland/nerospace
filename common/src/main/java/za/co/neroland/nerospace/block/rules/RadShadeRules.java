package za.co.neroland.nerospace.block.rules;

/**
 * Sunglass Crystal radiation shade: a player with a {@code #nerospace:radiation_shade} block in any of the three cells
 * directly above their head is shaded, and the ambient radiation level counts one lower for them (a flare's +1 still
 * applies on top). Dayside 2 becomes 1, so the dayside still needs the Rad Lining in the open.
 */
public final class RadShadeRules {

    /** Cells checked above the head (feet + 2 .. feet + 4). */
    public static final int CELLS = 3;

    private RadShadeRules() {
    }

    /** True when any of the cells above the head holds a shade block. */
    public static boolean shaded(boolean[] above3InTag) {
        if (above3InTag == null) {
            return false;
        }
        for (int i = 0; i < Math.min(CELLS, above3InTag.length); i++) {
            if (above3InTag[i]) {
                return true;
            }
        }
        return false;
    }

    /** The ambient level after the shade: one lower when shaded, never below 0. */
    public static int effectiveRadiation(int ambient, boolean shaded) {
        return Math.max(0, shaded ? ambient - 1 : ambient);
    }
}
