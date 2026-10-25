package za.co.neroland.nerospace.world.station;

import java.util.List;

import za.co.neroland.nerospace.block.rules.TelescopeSightline;

/**
 * Observatory reveal. The telescope's sightline is the vertical column
 * {@code (0, +10 .. +10+RANGE, 0)} above the station centre; each Void Crystal in it extends the reveal (capped).
 * A dormant telescope — the station is not RESEARCH — reveals nothing. Pure.
 */
public final class ObservatoryRules {

    /** Sightline length above the telescope's column start. */
    public static final int RANGE = 16;
    /** First sightline cell, relative to the station centre's Y (the telescope sits at +9). */
    public static final int START_DY = 10;
    /** How often the Station Core recomputes the cached reveal count. */
    public static final int RECHECK_TICKS = 200;

    private ObservatoryRules() {
    }

    public static int revealCount(int voidCrystals, boolean research) {
        return research ? TelescopeSightline.revealCount(voidCrystals) : 0;
    }

    /** As above over a sampled column: {@code true} where the cell holds a Void Crystal. */
    public static int revealCount(List<Boolean> columnIsVoidCrystal, boolean research) {
        int n = 0;
        for (Boolean b : columnIsVoidCrystal) {
            if (Boolean.TRUE.equals(b)) {
                n++;
            }
        }
        return revealCount(n, research);
    }
}
