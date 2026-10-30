package za.co.neroland.nerospace.block.rules;

/** Hull Window frost: frosted in the station when any horizontal neighbour is air (faces the void). */
public final class FrostRules {

    private FrostRules() {
    }

    public static boolean frosted(boolean inStation, boolean[] horizontalNeighbourIsAir) {
        if (!inStation) {
            return false;
        }
        for (boolean air : horizontalNeighbourIsAir) {
            if (air) {
                return true;
            }
        }
        return false;
    }
}
