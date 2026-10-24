package za.co.neroland.nerospace.world.planet;

/**
 * A planet's daylight as a pure function of its clock phase: {@code phase} in [0,1) maps to a daylight value
 * in [0,1]. Phase convention shared by every planet: 0.00 sunrise, 0.25 noon, 0.50 sunset, 0.75 midnight.
 *
 * <p>Daylight drives the sky, solar output and villager schedules. It never drives block or sky light.</p>
 */
@FunctionalInterface
public interface DaylightCurve {

    /** Daylight in [0,1] at {@code phase} (any real value; it is wrapped into [0,1)). */
    double at(double phase);

    /** A constant curve (fixed-clock planets and the station). */
    static DaylightCurve constant(double value) {
        double v = clamp01(value);
        return phase -> v;
    }

    /**
     * The shared base curve: a sine day whose sunrise and sunset transitions each take {@code dawnWidth} of the
     * day, scaled to {@code peak}. Symmetric, so its time-average is {@code peak / 2}.
     */
    static double base(double phase, double dawnWidth, double peak) {
        double w = Math.max(1.0E-4, dawnWidth);
        double k = Math.max(1.0, 1.0 / (2.0 * Math.PI * w));
        return peak * clamp01(0.5 + 0.5 * k * Math.sin(2.0 * Math.PI * wrap(phase)));
    }

    /** Wraps any phase into [0,1). */
    static double wrap(double phase) {
        double f = phase - Math.floor(phase);
        return f >= 1.0 ? 0.0 : f;
    }

    static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    /** A smooth bump: 1 at 0, falling to 0 at |x| = 1. */
    static double bump(double x) {
        return Math.max(0.0, 1.0 - x * x);
    }
}
