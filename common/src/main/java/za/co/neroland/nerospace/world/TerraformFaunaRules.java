package za.co.neroland.nerospace.world;

/**
 * Pure rule for seeding the stage-3 starter herd. On a world whose terraform table opens an ocean the herd is a
 * water herd, so it is seeded only when the surface cell is the table liquid; every other world seeds as before.
 */
public final class TerraformFaunaRules {

    private TerraformFaunaRules() {
    }

    /**
     * @param hasOpenOcean the table carries {@code OPEN_OCEAN}
     * @param cellIsLiquid the cell at the surface position holds the table liquid
     */
    public static boolean maySeed(boolean hasOpenOcean, boolean cellIsLiquid) {
        return !hasOpenOcean || cellIsLiquid;
    }
}
