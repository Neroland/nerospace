package za.co.neroland.nerospace.world.planet;

/**
 * Pure planet-clock arithmetic (no level, no server) so it is unit-testable and shared verbatim by the server
 * and the client. Phase convention: 0.00 sunrise, 0.25 noon, 0.50 sunset, 0.75 midnight.
 */
public final class ClockMath {

    /** Vanilla day length; a {@link ClockSource#LEVEL_TIME} planet mirrors this. */
    public static final long VANILLA_DAY = 24_000L;
    /** The phase every {@link ClockSource#PLANET_CLOCK} planet is pinned to while {@code clock.enabled=false}. */
    public static final double NOON = 0.25;

    private ClockMath() {
    }

    /**
     * The clock phase in [0,1).
     *
     * @param gameTime     the level's monotonic game time
     * @param epochTicks   game time at which the clock started
     * @param offsetTicks  ops adjustment ({@code /nerospace clock set})
     * @param dayLength    ticks per planet day; {@code <= 0} means fixed
     * @param pinnedPhase  phase of a fixed clock
     * @param frozen       whether the clock is frozen
     * @param frozenAt     game time at which it froze (read only when frozen)
     */
    public static double phase(long gameTime, long epochTicks, long offsetTicks, long dayLength, double pinnedPhase,
            boolean frozen, long frozenAt) {
        if (dayLength <= 0L) {
            return DaylightCurve.wrap(pinnedPhase);
        }
        long now = frozen ? frozenAt : gameTime;
        long elapsed = now - epochTicks - offsetTicks;
        return (double) Math.floorMod(elapsed, dayLength) / (double) dayLength;
    }

    /**
     * Continuous days since the clock started: whole days plus {@link #phase}, so it never wraps (the sky's orbiting
     * bodies read it; a body whose period is not one day would jump at every sunrise on the wrapped phase). A fixed
     * clock stays at its pinned phase; a frozen clock keeps its day.
     */
    public static double days(long gameTime, long epochTicks, long offsetTicks, long dayLength, double pinnedPhase,
            boolean frozen, long frozenAt) {
        if (dayLength <= 0L) {
            return DaylightCurve.wrap(pinnedPhase);
        }
        long now = frozen ? frozenAt : gameTime;
        long elapsed = now - epochTicks - offsetTicks;
        return Math.floorDiv(elapsed, dayLength) + (double) Math.floorMod(elapsed, dayLength) / (double) dayLength;
    }

    /** {@link #days} for a vanilla-time mirror: day time over the vanilla day length. */
    public static double mirrorDays(long dayTime) {
        return Math.floorDiv(dayTime, VANILLA_DAY) + mirrorPhase(dayTime);
    }

    /** The phase a vanilla-time mirror shows for a given day time. */
    public static double mirrorPhase(long dayTime) {
        return (double) Math.floorMod(dayTime, VANILLA_DAY) / (double) VANILLA_DAY;
    }

    /**
     * The offset that makes {@link #phase} return {@code target} at {@code gameTime} (unfrozen).
     * {@code target} is wrapped into [0,1).
     */
    public static long offsetFor(long gameTime, long epochTicks, long dayLength, double target) {
        if (dayLength <= 0L) {
            return 0L;
        }
        long want = Math.round(DaylightCurve.wrap(target) * dayLength);
        return Math.floorMod(gameTime - epochTicks - want, dayLength);
    }

    /**
     * The 1-based day count ("Sol N" on Mars) since the clock started, following the same timeline as
     * {@link #phase}. Never below 1. Fixed clocks are always day 1.
     */
    public static long dayNumber(long gameTime, long epochTicks, long offsetTicks, long dayLength, boolean frozen,
            long frozenAt) {
        if (dayLength <= 0L) {
            return 1L;
        }
        long now = frozen ? frozenAt : gameTime;
        long elapsed = now - epochTicks - offsetTicks;
        return Math.max(1L, Math.floorDiv(elapsed, dayLength) + 1L);
    }
}
