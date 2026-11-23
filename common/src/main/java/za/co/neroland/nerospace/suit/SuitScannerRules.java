package za.co.neroland.nerospace.suit;

/**
 * The survey scanner may name a block only when it was placed by world generation. Player-placed blocks,
 * containers and anything that is not an ore-tagged generated block are ignored.
 */
public final class SuitScannerRules {

    private SuitScannerRules() {
    }

    public static boolean visible(boolean oreTag, boolean worldGenerated, boolean container, boolean playerPlaced) {
        return oreTag && worldGenerated && !container && !playerPlaced;
    }

    public static int radius(int configured) {
        return Math.max(8, Math.min(64, configured));
    }

    public static int cooldown(int configured) {
        return Math.max(20, Math.min(1200, configured));
    }
}
