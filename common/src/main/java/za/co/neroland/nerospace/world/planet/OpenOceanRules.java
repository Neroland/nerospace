package za.co.neroland.nerospace.world.planet;

import java.util.List;

/**
 * Pure rules for {@link TerraformTable.Rule#OPEN_OCEAN}: at stage 3 the top surface block and up to
 * {@link #DEPTH} convertible blocks under it become the table liquid. Loose cover on top of a column (ice blades,
 * snow layers) is looked through and removed; anything else on top leaves the column alone.
 */
public final class OpenOceanRules {

    /** Blocks under the top block that may also open. */
    public static final int DEPTH = 3;
    /** The most cells one column opens (the top block plus {@link #DEPTH}). */
    public static final int MAX_CELLS = DEPTH + 1;

    /** Loose cover a column is read through, by block id. */
    public static final List<String> LOOK_THROUGH = List.of("nerospace:penitente_blade", "minecraft:snow");

    private OpenOceanRules() {
    }

    /** Whether a block id is loose cover that is looked through and removed. */
    public static boolean lookThrough(String blockId) {
        return LOOK_THROUGH.contains(blockId);
    }

    /**
     * Index of the real top block in a column listed from the top down (block ids): the first entry that is not
     * loose cover. {@code -1} when the column is empty or is cover all the way.
     */
    public static int topIndex(List<String> columnFromTopDown) {
        for (int i = 0; i < columnFromTopDown.size(); i++) {
            if (!lookThrough(columnFromTopDown.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * How many cells open in one column: 0 when the top block is not the table's surface top, else the top block
     * plus the run of convertible blocks under it, capped at {@link #MAX_CELLS}.
     *
     * @param columnTopIsSurfaceTop the real top block is the table's {@code surfaceTop}
     * @param convertibleRunLength  consecutive convertible blocks directly under the top block
     */
    public static int cells(boolean columnTopIsSurfaceTop, int convertibleRunLength) {
        if (!columnTopIsSurfaceTop) {
            return 0;
        }
        return 1 + Math.max(0, Math.min(DEPTH, convertibleRunLength));
    }
}
