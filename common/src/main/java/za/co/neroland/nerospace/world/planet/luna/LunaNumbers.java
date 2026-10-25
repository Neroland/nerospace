package za.co.neroland.nerospace.world.planet.luna;

import za.co.neroland.nerospace.block.rules.CheeseRules;
import za.co.neroland.nerospace.block.rules.GasPocketRules;

/** Pure numbers behind Luna's blocks (no game classes), shared by the blocks and their tests. */
public final class LunaNumbers {

    /** Helium-3 canisters the pocket's loot table drops on every harvest (the leaked yield). */
    public static final int HE3_LOOT_YIELD = 1;

    private LunaNumbers() {
    }

    /** Canisters added in code on top of {@link #HE3_LOOT_YIELD}, so the total equals {@link GasPocketRules#yield}. */
    public static int he3ExtraYield(boolean silkTouch, boolean gasTankAdjacent) {
        return Math.max(0, GasPocketRules.yield(silkTouch, gasTankAdjacent, GasPocketRules.BASE_YIELD) - HE3_LOOT_YIELD);
    }

    /** West edge (in 1/16 block) of the Cheese Wheel after {@code bites} slices; shapes and models share it. */
    public static int wheelMinX(int bites) {
        int b = Math.max(0, Math.min(CheeseRules.MAX_BITES, bites));
        return 2 + Math.round(b * 12.0F / CheeseRules.SLICES);
    }
}
