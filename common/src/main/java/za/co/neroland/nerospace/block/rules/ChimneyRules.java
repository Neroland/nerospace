package za.co.neroland.nerospace.block.rules;

/**
 * Smoker Chimney growth: a stack standing on an active vent adds one block on top on a random tick, up to
 * {@link #MAX} blocks tall, while the cell above is free.
 */
public final class ChimneyRules {

    /** Tallest stack. */
    public static final int MAX = 5;
    /** Chance per random tick that the top block grows. */
    public static final float GROW_CHANCE = 0.08F;

    private ChimneyRules() {
    }

    public static boolean mayGrow(int heightOfStack, boolean baseIsActiveVent, boolean spaceAbove) {
        return baseIsActiveVent && spaceAbove && heightOfStack >= 1 && heightOfStack < MAX;
    }

    /** Whether this random tick grows, from a roll in {@code [0, 1)}. */
    public static boolean grows(float roll) {
        return roll < GROW_CHANCE;
    }
}
