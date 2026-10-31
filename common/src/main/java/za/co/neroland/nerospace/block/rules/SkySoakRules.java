package za.co.neroland.nerospace.block.rules;

/**
 * Twilight Crystal: its glow is its own sky light. {@code GLOW = clamp((skyLight - 6) / 3, 0, 3)} read just above the
 * block, emitting 0 / 3 / 5 / 7. A sky-lit floor glows softly; the glow dies out underground.
 */
public final class SkySoakRules {

    public static final int MAX_GLOW = 3;
    private static final int[] LIGHT = {0, 3, 5, 7};

    private SkySoakRules() {
    }

    public static int glow(int skyLight) {
        return Math.max(0, Math.min(MAX_GLOW, (skyLight - 6) / 3));
    }

    public static int light(int glow) {
        return LIGHT[Math.max(0, Math.min(MAX_GLOW, glow))];
    }
}
