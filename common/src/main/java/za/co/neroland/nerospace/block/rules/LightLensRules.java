package za.co.neroland.nerospace.block.rules;

/**
 * Crater Glass light lens: {@code LENS = clamp(max non-lens neighbour block light + 1, 0, 15)}. Vanilla
 * glass passes light at −1, so the lens is +2 relative. Other lenses never count (no chains).
 */
public final class LightLensRules {

    public static final int MAX = 15;
    /** Client sparkle threshold. */
    public static final int SPARKLE_MIN = 8;

    private LightLensRules() {
    }

    public static int lensLevel(int[] neighbourBlockLight, boolean[] neighbourIsLens) {
        int best = -1;
        for (int i = 0; i < neighbourBlockLight.length; i++) {
            boolean lens = neighbourIsLens != null && i < neighbourIsLens.length && neighbourIsLens[i];
            if (!lens && neighbourBlockLight[i] > best) {
                best = neighbourBlockLight[i];
            }
        }
        if (best <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(MAX, best + 1));
    }

    public static boolean sparkles(int lens) {
        return lens >= SPARKLE_MIN;
    }
}
