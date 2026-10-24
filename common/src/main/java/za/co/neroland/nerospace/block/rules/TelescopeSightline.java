package za.co.neroland.nerospace.block.rules;

/** Telescope (002 §12, §11.2): reveal radius = 1 + min(2, Void Crystals in the line of sight). */
public final class TelescopeSightline {

    public static final int BASE = 1;
    public static final int MAX_CRYSTAL_BONUS = 2;

    private TelescopeSightline() {
    }

    public static int revealCount(int voidCrystals) {
        return BASE + Math.max(0, Math.min(MAX_CRYSTAL_BONUS, voidCrystals));
    }
}
