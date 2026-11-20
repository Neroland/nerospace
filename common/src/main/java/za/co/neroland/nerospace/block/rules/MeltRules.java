package za.co.neroland.nerospace.block.rules;

/**
 * Europa Ice melt: a heat source in any of the 6 neighbours turns the block into water, once. Only Europa Ice melts;
 * the other ices (chaos, cut, bricks, shell and the ore-bearing ices) never do, and neither does light alone.
 */
public final class MeltRules {

    /** Steam puffs shown when a block melts. */
    public static final int STEAM_PARTICLES = 6;

    private MeltRules() {
    }

    /** True when any of the adjacent cells is a heat source. */
    public static boolean anyHeat(boolean[] adjacentIsHeat) {
        for (boolean h : adjacentIsHeat) {
            if (h) {
                return true;
            }
        }
        return false;
    }

    /** Whether the block melts: it must be Europa Ice itself and touch a heat source. */
    public static boolean melts(boolean isEuropaIce, boolean heatAdjacent) {
        return isEuropaIce && heatAdjacent;
    }
}
