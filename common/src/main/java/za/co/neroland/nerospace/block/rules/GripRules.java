package za.co.neroland.nerospace.block.rules;

/**
 * Cut Europa Ice grip: scored ice that is quicker to walk on and does not slide. The block carries the speed factor
 * as a registered property and keeps the default friction; the other ices keep the default speed.
 */
public final class GripRules {

    /** Walk-speed factor on cut ice. */
    public static final float CUT_SPEED = 1.15F;
    /** Walk-speed factor on every other block (the game default). */
    public static final float DEFAULT_SPEED = 1.0F;
    /** Friction of an ordinary block (no slide). */
    public static final float DEFAULT_FRICTION = 0.6F;

    private GripRules() {
    }

    public static float speedFactor(boolean cut) {
        return cut ? CUT_SPEED : DEFAULT_SPEED;
    }

    /** Cut ice never slides: its friction is the default one whatever the block is. */
    public static float friction(boolean cut) {
        return DEFAULT_FRICTION;
    }
}
