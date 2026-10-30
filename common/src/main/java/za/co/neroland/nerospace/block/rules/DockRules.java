package za.co.neroland.nerospace.block.rules;

/** Docking Ring: the rocket sits at {@code centre + (0.5, surfaceHeight, 0.5)}; 3×3 footprint. */
public final class DockRules {

    public static final int HALF = 1;

    private DockRules() {
    }

    /** {x, y, z} of the docked rocket for a ring centre. */
    public static double[] dockPosition(int cx, int cy, int cz, double surfaceHeight) {
        return new double[] {cx + 0.5, cy + surfaceHeight, cz + 0.5};
    }

    /** {@code PART} index 0..8 (row-major, 4 = centre) for an offset from the centre, or −1 outside the 3×3. */
    public static int partIndex(int dx, int dz) {
        if (Math.abs(dx) > HALF || Math.abs(dz) > HALF) {
            return -1;
        }
        return (dz + 1) * 3 + (dx + 1);
    }

    public static boolean dropsSelf(int partIndex) {
        return partIndex == 4;
    }
}
