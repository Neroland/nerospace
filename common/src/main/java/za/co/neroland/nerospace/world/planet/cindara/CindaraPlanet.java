package za.co.neroland.nerospace.world.planet.cindara;

import java.util.Optional;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.PlanetArchetype;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.TreeStyle;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherParams;

/**
 * Cindara ({@code nerospace:cindara}): ember-cracked ash under a red giant that eclipses into dusk. A two-day
 * clock with an eclipse window in the late afternoon (0.40–0.50) that runs straight into dusk, and ash fall.
 *
 * <p>Gravity, hazard, terraform result, trees, livestock and quarry numbers are the 1.x values, unchanged.</p>
 */
public final class CindaraPlanet extends PlanetDefinition {

    public static final CindaraPlanet INSTANCE = new CindaraPlanet();

    public static final double ECLIPSE_START = 0.40;
    public static final double ECLIPSE_END = 0.50;
    private static final double ECLIPSE_CENTRE = 0.45;
    private static final double ECLIPSE_HALF_WIDTH = 0.05;
    private static final double ECLIPSE_DEPTH = 0.9;

    private CindaraPlanet() {
        super(new Spec(new PlanetTraits(planet("cindara"), 0.8, true, Hazard.HEAT,
                PlanetArchetype.VOLCANIC, TemperatureBand.INFERNAL, 1, 48_000L, id("cindara"),
                Optional.of(biome("terraformed_savanna")), Optional.of(id("ember_kin")), id("ember_forge")),
                "cindara", "Cindara")
                .unlockTier(3)
                .daylight(CindaraPlanet::daylightAt, 4.6)
                .weather(new WeatherParams(WeatherEvent.ASH_FALL, 36_000L, 3_000, 9_000))
                .mining(0.8, 2)
                .terraform(TreeStyle.ACACIA,
                        () -> za.co.neroland.nerospace.registry.ModEntities.EMBER_STRUTTER.get()));
    }

    static double daylightAt(double phase) {
        double p = DaylightCurve.wrap(phase);
        double v = DaylightCurve.base(p, 0.06, 1.0);
        return v * (1.0 - ECLIPSE_DEPTH * DaylightCurve.bump((p - ECLIPSE_CENTRE) / ECLIPSE_HALF_WIDTH));
    }

    /** Ash fall never starts within 0.03 phase of the eclipse (excluded 0.37–0.53). */
    @Override
    public boolean weatherMayStart(double phase) {
        double p = DaylightCurve.wrap(phase);
        return p < ECLIPSE_START - 0.03 || p > ECLIPSE_END + 0.03;
    }

    /** Whether {@code phase} is inside the eclipse window. */
    public static boolean inEclipse(double phase) {
        double p = DaylightCurve.wrap(phase);
        return p >= ECLIPSE_START && p <= ECLIPSE_END;
    }
}
