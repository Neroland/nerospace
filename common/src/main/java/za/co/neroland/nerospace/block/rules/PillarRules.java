package za.co.neroland.nerospace.block.rules;

/** Basalt Column self-extension: grows upward one segment per 20 ticks, to height 4. */
public final class PillarRules {

    public static final int MAX_HEIGHT = 4;
    public static final int DELAY_TICKS = 20;

    private PillarRules() {
    }

    /** The {@code SEGMENT} value to place above (= currentHeight, 0-based), or −1 to stop. */
    public static int nextSegment(int currentHeight, int max, boolean aboveIsAir) {
        if (!aboveIsAir || currentHeight < 1 || currentHeight >= Math.min(max, MAX_HEIGHT)) {
            return -1;
        }
        return currentHeight;
    }
}
