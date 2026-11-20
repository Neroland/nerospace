package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure plan for an ice fracture on Europa: a straight slot two blocks wide and 9 to 17 long, cut from the surface
 * down to the lowest level the underside of the shell can reach, so its bottom always opens onto the ocean or an air
 * dome. Below sea level the slot holds water, above it air.
 */
public final class FractureRules {

    public static final int LENGTH_MIN = 9;
    public static final int LENGTH_MAX = 17;
    public static final int WIDTH = 2;
    /** The lowest Y a fracture is cut to: the shell is always open at and below it. */
    public static final int BOTTOM_Y = 84;
    /** Cells below this Y fill with water. */
    public static final int SEA_LEVEL = 88;
    /** No planned column is further than this from the origin on either axis (inside the 3 x 3 chunk write region). */
    public static final int MAX_REACH = 9;

    public static final String WATER = "minecraft:water";
    public static final String AIR = "minecraft:air";

    /** One planned column. */
    public record Column(int x, int z) {
    }

    private FractureRules() {
    }

    /** A length in range from a roll in {@code [0, 9)}. */
    public static int length(int roll) {
        return Math.max(LENGTH_MIN, Math.min(LENGTH_MAX, LENGTH_MIN + roll));
    }

    /**
     * The columns of one fracture, centred on the origin: {@code length} cells along one axis, {@link #WIDTH} across.
     *
     * @param alongX true to run along X, false along Z
     */
    public static List<Column> plan(int originX, int originZ, boolean alongX, int length) {
        int n = Math.max(LENGTH_MIN, Math.min(LENGTH_MAX, length));
        List<Column> out = new ArrayList<>(n * WIDTH);
        int start = -(n / 2);
        for (int i = 0; i < n; i++) {
            for (int w = 0; w < WIDTH; w++) {
                int along = start + i;
                out.add(alongX ? new Column(originX + along, originZ + w) : new Column(originX + w, originZ + along));
            }
        }
        return out;
    }

    /** What a cut cell holds at {@code y}: water below sea level, air at and above it. */
    public static String fill(int y) {
        return y < SEA_LEVEL ? WATER : AIR;
    }
}
