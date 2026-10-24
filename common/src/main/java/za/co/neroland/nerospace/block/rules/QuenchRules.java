package za.co.neroland.nerospace.block.rules;

/** Ember-Cracked Ashstone quench (002 §1.3): water in any of the 6 neighbours cools it to Ashstone. */
public final class QuenchRules {

    public static final int SMOKE_PARTICLES = 8;

    private QuenchRules() {
    }

    public static boolean quenches(boolean[] adjacentIsWater) {
        for (boolean w : adjacentIsWater) {
            if (w) {
                return true;
            }
        }
        return false;
    }
}
