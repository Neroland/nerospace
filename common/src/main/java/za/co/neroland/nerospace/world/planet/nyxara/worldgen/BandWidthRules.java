package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

/**
 * Which band width a Nyxara world uses (pure). A stored value wins (clamped to the config range); with nothing stored
 * (first load of the level) the current config value is used and then stored. So a config change applies to new
 * worlds only and never reshapes an existing one.
 */
public final class BandWidthRules {

    private BandWidthRules() {
    }

    /** @param stored the saved width, {@code <= 0} if none; @param config {@code nyxara.twilightBandWidth} */
    public static int resolve(int stored, int config) {
        return FixedXBands.clampWidth(stored > 0 ? stored : config);
    }

    /** A reconciled band width and the game time it was first bound ({@code -1} = unknown, written before). */
    public record Bound(int width, long boundAt) {
    }

    /**
     * Reconciles the two stored copies of the width: the Nyxara level's own store and the overworld backup. A missing
     * copy ({@code width <= 0}, e.g. after a recovery reset of one store) is filled from the other, so a reset never
     * re-reads the config and shifts an existing world's band. When both exist and disagree the <b>older</b> copy wins
     * (smaller {@code boundAt}; an unknown {@code -1} predates both and counts as oldest; a tie keeps the primary).
     * With neither stored, the config width (clamped) is bound at {@code now}.
     */
    public static Bound reconcile(int primary, long primaryAt, int backup, long backupAt, int config, long now) {
        boolean hasPrimary = primary > 0;
        boolean hasBackup = backup > 0;
        if (!hasPrimary && !hasBackup) {
            return new Bound(FixedXBands.clampWidth(config), now);
        }
        if (!hasBackup) {
            return new Bound(FixedXBands.clampWidth(primary), primaryAt);
        }
        if (!hasPrimary) {
            return new Bound(FixedXBands.clampWidth(backup), backupAt);
        }
        if (primary == backup) {
            return new Bound(FixedXBands.clampWidth(primary), older(primaryAt, backupAt) ? primaryAt : backupAt);
        }
        return older(backupAt, primaryAt) && backupAt != primaryAt
                ? new Bound(FixedXBands.clampWidth(backup), backupAt)
                : new Bound(FixedXBands.clampWidth(primary), primaryAt);
    }

    /** Whether time {@code a} is at or before {@code b}; {@code -1} (unknown) is the oldest. */
    private static boolean older(long a, long b) {
        long x = a < 0 ? Long.MIN_VALUE : a;
        long y = b < 0 ? Long.MIN_VALUE : b;
        return x <= y;
    }
}
