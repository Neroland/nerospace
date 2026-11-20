package za.co.neroland.nerospace.rocket;

/**
 * Pure ascent numbers: how long a climb of a configured height lasts and how fast the rocket moves on each
 * tick of it. The server drives {@link RocketEntity} from these; the client normalises its launch animation
 * against the same duration (synced), so both sides agree without a second formula.
 *
 * <p>The curve keeps the original ease-in shape, {@code 0.08 + 0.5·(tick/duration)²}, scaled so it sums to
 * the configured height. At the {@link #BASE_HEIGHT 24-block} baseline it is tick-for-tick the pre-2.4.0
 * climb ({@link #BASE_DURATION 100} ticks, unscaled).
 */
public final class AscentMath {

    /** The nominal climb before the height became configurable. */
    public static final int BASE_HEIGHT = 24;
    /** Ticks of the baseline climb ({@code RocketEntity.LAUNCH_DURATION}). */
    public static final int BASE_DURATION = 100;
    /** {@code rockets.ascentHeight} bounds and default (4× the baseline). */
    public static final int MIN_HEIGHT = 24;
    public static final int MAX_HEIGHT = 256;
    public static final int DEFAULT_HEIGHT = 96;
    /** No climb lasts longer than this, however high. */
    public static final int MAX_DURATION = 240;

    private static final double BASE_SPEED = 0.08D;
    private static final double EASE = 0.5D;
    /** What the unscaled baseline curve really sums to (24.4175): the baseline climbs this, not a flat 24. */
    private static final double BASE_SUM = curveSum(BASE_DURATION);

    private AscentMath() {
    }

    /** The configured height clamped to the supported range. */
    public static int clampHeight(int height) {
        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
    }

    /**
     * Ascent length in ticks for a nominal height. Grows with the cube root of the height so a taller climb
     * reads as a climb rather than a snap: 100 ticks at 24 blocks, 159 at 96, 220 at 256, capped at 240.
     */
    public static int duration(int height) {
        int h = clampHeight(height);
        if (h == BASE_HEIGHT) {
            return BASE_DURATION;
        }
        long ticks = Math.round(BASE_DURATION * Math.cbrt((double) h / BASE_HEIGHT));
        return (int) Math.max(BASE_DURATION, Math.min(MAX_DURATION, ticks));
    }

    /** A synced or saved duration made safe: anything out of range falls back to the baseline. */
    public static int sanitizeDuration(int duration) {
        return duration < BASE_DURATION || duration > MAX_DURATION ? BASE_DURATION : duration;
    }

    /**
     * Blocks climbed on {@code tick} (0-based) of an ascent of {@code duration} ticks to {@code height}, at
     * gravity factor 1. Zero outside the ascent.
     */
    public static double speed(int tick, int duration, int height) {
        if (duration <= 0 || tick < 0 || tick >= duration) {
            return 0.0D;
        }
        double t = (double) tick / duration;
        double shape = BASE_SPEED + EASE * (t * t);
        int h = clampHeight(height);
        if (h == BASE_HEIGHT && duration == BASE_DURATION) {
            return shape;
        }
        return shape * (h + (BASE_SUM - BASE_HEIGHT)) / curveSum(duration);
    }

    /** Total blocks climbed over a whole ascent at gravity factor 1. */
    public static double totalClimb(int duration, int height) {
        double sum = 0.0D;
        for (int tick = 0; tick < duration; tick++) {
            sum += speed(tick, duration, height);
        }
        return sum;
    }

    /**
     * The cosmetic low-gravity multiplier on the climb (unchanged from 2.3.1): 1 at gravity factor 1, up to
     * 1.5 in free fall, never below 0.75.
     */
    public static double gravityBonus(double gravityFactor) {
        double factor = Math.max(0.0D, Math.min(1.5D, gravityFactor));
        return 1.5D - 0.5D * factor;
    }

    private static double curveSum(int duration) {
        double sum = 0.0D;
        for (int tick = 0; tick < duration; tick++) {
            double t = (double) tick / duration;
            sum += BASE_SPEED + EASE * (t * t);
        }
        return sum;
    }
}
