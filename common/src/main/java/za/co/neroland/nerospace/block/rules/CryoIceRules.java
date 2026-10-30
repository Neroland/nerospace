package za.co.neroland.nerospace.block.rules;

/** Packed Cryo-Ice: the fastest surface in the mod; never melts. */
public final class CryoIceRules {

    /** Vanilla blue ice friction, for reference. */
    public static final float BLUE_ICE = 0.989F;

    private CryoIceRules() {
    }

    public static float friction() {
        return 0.989F;
    }

    public static boolean melts(int lightLevel) {
        return false;
    }
}
