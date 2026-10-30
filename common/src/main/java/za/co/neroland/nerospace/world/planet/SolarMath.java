package za.co.neroland.nerospace.world.planet;

/**
 * Pure solar-output arithmetic, unit-tested by {@code SolarAverageTest}:
 * {@code factor = daylight(phase) × planetSolar × weatherFactor}. The constants are chosen so the time-average
 * over a planet day is at least the 1.x flat 2.0 on every planet; the station stays a constant 2.0. With
 * {@code solar.clockDriven=false} every Nerospace world returns the 1.x flat {@link #LEGACY_FLAT}.
 */
public final class SolarMath {

    /** The 1.x factor on every Nerospace world (permanent sun, no weather). */
    public static final double LEGACY_FLAT = 2.0;

    private SolarMath() {
    }

    /** The panel factor at a phase, before any roof check. */
    public static double factor(PlanetDefinition planet, double phase, double weatherFactor, boolean clockDriven) {
        if (!clockDriven) {
            return LEGACY_FLAT;
        }
        double w = Math.max(0.0, Math.min(1.0, weatherFactor));
        return planet.daylight().at(phase) * planet.planetSolar() * w;
    }

    /** Mean of {@link #factor} over one day, sampled {@code samples} times at a constant weather factor. */
    public static double dayAverage(PlanetDefinition planet, double weatherFactor, int samples) {
        int n = Math.max(1, samples);
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += factor(planet, (i + 0.5) / n, weatherFactor, true);
        }
        return sum / n;
    }
}
