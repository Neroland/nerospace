package za.co.neroland.nerospace.block.rules;

/**
 * Vent Sulfide Ore: mined wet (waterlogged surroundings: water or brine in any of the 6 neighbours) it gives its ore;
 * mined dry the sulfide oxidises and one sulfur is all that is left.
 */
public final class SulfideRules {

    private SulfideRules() {
    }

    public static boolean wet(boolean anyNeighbourLiquid, boolean waterlogged) {
        return anyNeighbourLiquid || waterlogged;
    }

    public static boolean dropsOre(boolean wet) {
        return wet;
    }
}
