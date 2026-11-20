package za.co.neroland.nerospace.block.rules;

/**
 * Europium Lamp: a lamp toggled by use. Lit it gives a red light of 14, one short of full so it never replaces a
 * proper light source for anything that asks for 15; unlit it gives none.
 */
public final class PhosphorRules {

    /** The lit level: deliberately one below the maximum of 15. */
    public static final int NEAR_MISS_OF_FULL = 14;

    private PhosphorRules() {
    }

    public static int light(boolean lit) {
        return lit ? NEAR_MISS_OF_FULL : 0;
    }

    public static boolean toggle(boolean lit) {
        return !lit;
    }
}
