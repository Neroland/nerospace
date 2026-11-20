package za.co.neroland.nerospace.world.planet.europa;

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
 * Europa ({@code nerospace:europa}): a cracked ice shell over a dark ocean, under a fixed Jupiter. One Europa day is
 * one orbit of Jupiter (85,200 ticks). The sun passes behind Jupiter at noon, so every day has an eclipse centred on
 * phase 0.25; at night Jupiter is full and lights the ice a little.
 *
 * <p>Two weather events share the one machine: plume fall (diamond dust) at any time, and a radiation storm (flare)
 * that may only begin while Europa crosses Jupiter's magnetotail, around the eclipse.</p>
 */
public final class EuropaPlanet extends PlanetDefinition {

    /** A small sun at 5.2 AU with no air to scatter it. */
    public static final double PEAK = 0.50;
    /** Night floor: Jupiter is full at midnight. */
    public static final double JUPITER_SHINE = 0.06;
    public static final double ECLIPSE_CENTRE = 0.25;
    public static final double ECLIPSE_HALF_WIDTH = 0.064;
    /** Equals {@code eclipse.phaseStart} in the sky profile. */
    public static final double ECLIPSE_START = 0.186;
    /** Equals {@code eclipse.phaseEnd} in the sky profile. */
    public static final double ECLIPSE_END = 0.314;
    public static final double ECLIPSE_DEPTH = 0.95;
    /** Europa crosses Jupiter's magnetotail around the eclipse: the only time a radiation storm may begin. */
    public static final double TAIL_START = 0.15;
    public static final double TAIL_END = 0.35;
    /** A ground panel's factor at full daylight; day average times this is at least the legacy 2.0. */
    public static final double PLANET_SOLAR = 8.5;

    /** Plume fall: fine ice from a cryovolcanic plume settling back. */
    public static final WeatherParams PLUME_FALL = new WeatherParams(WeatherEvent.DIAMOND_DUST, 60_000L, 3_000, 9_000);
    /** Radiation storm: about one per two Europa days inside the tail window. */
    public static final WeatherParams RADIATION_STORM = new WeatherParams(WeatherEvent.FLARE, 68_160L, 1_800, 3_600);

    public static final EuropaPlanet INSTANCE = new EuropaPlanet();

    private EuropaPlanet() {
        super(new Spec(new PlanetTraits(planet("europa"), 0.13, true, Hazard.COLD,
                PlanetArchetype.FROZEN, TemperatureBand.FRIGID, 2, 85_200L, id("europa"),
                Optional.of(biome("terraformed_europa_living")), Optional.empty(), id("ice_shell_habitat")),
                "europa", "Europa")
                .unlockTier(4)
                .salt(0x4555524FL)
                .daylight(EuropaPlanet::daylightAt, PLANET_SOLAR)
                .weather(PLUME_FALL)
                .alsoWeather(RADIATION_STORM)
                .mining(0.7, 3)
                .terraform(TreeStyle.SPRUCE, () -> EuropaContent.LANTERN_DRIFTER.get()));
    }

    static double daylightAt(double phase) {
        double p = DaylightCurve.wrap(phase);
        double v = DaylightCurve.base(p, 0.03, PEAK);
        v *= 1.0 - ECLIPSE_DEPTH * DaylightCurve.bump((p - ECLIPSE_CENTRE) / ECLIPSE_HALF_WIDTH);
        return DaylightCurve.clamp01(Math.max(JUPITER_SHINE, v));
    }

    /** A radiation storm only begins inside the tail window; plume fall at any time. */
    @Override
    public boolean weatherMayStart(WeatherEvent event, double phase) {
        if (event == WeatherEvent.FLARE) {
            return inTail(phase);
        }
        return true;
    }

    /** Whether {@code phase} is inside the magnetotail window. */
    public static boolean inTail(double phase) {
        double p = DaylightCurve.wrap(phase);
        return p >= TAIL_START && p <= TAIL_END;
    }

    /** Whether {@code phase} is inside the eclipse window. */
    public static boolean inEclipse(double phase) {
        double p = DaylightCurve.wrap(phase);
        return p >= ECLIPSE_START && p <= ECLIPSE_END;
    }
}
