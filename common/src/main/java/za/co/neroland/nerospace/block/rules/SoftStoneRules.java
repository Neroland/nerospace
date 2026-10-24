package za.co.neroland.nerospace.block.rules;

/** Ashstone (002 §1.3): instant-mine with any pickaxe. */
public final class SoftStoneRules {

    private SoftStoneRules() {
    }

    public static float destroyProgress(boolean holdingPickaxe, float base) {
        return holdingPickaxe ? 1.0F : base;
    }
}
