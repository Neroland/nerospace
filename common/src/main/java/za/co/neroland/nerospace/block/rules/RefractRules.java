package za.co.neroland.nerospace.block.rules;

/**
 * Void Crystal refraction: a beam turns 90° toward the crystal's facing. A beam arriving along
 * the facing axis is not turned. Directions are the vanilla 3D-data indices (0 down, 1 up, 2 north, 3 south,
 * 4 west, 5 east) so this stays free of game classes.
 */
public final class RefractRules {

    private RefractRules() {
    }

    public static int outDirection(int in, int facing) {
        if (in < 0 || in > 5 || facing < 0 || facing > 5) {
            return in;
        }
        if ((in >> 1) == (facing >> 1)) {
            return in; // parallel to the facing axis: passes straight through
        }
        return facing;
    }
}
