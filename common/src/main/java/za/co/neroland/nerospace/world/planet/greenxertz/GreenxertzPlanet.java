package za.co.neroland.nerospace.world.planet.greenxertz;

import java.util.Optional;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.PlanetArchetype;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.TreeStyle;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherParams;

/**
 * Greenxertz ({@code nerospace:greenxertz}): the bioluminescent jungle under twin suns. It keeps its real
 * vanilla day (beds, raids, mob darkness), so its clock mirrors the level time and is never authoritative.
 *
 * <p>Gravity, airless flag, hazard, terraform result and livestock are the 1.x values, unchanged.</p>
 */
public final class GreenxertzPlanet extends PlanetDefinition {

    public static final GreenxertzPlanet INSTANCE = new GreenxertzPlanet();

    /** The second (pale green) sun is up between these phases and adds this much daylight. */
    public static final double SECOND_SUN_START = 0.05;
    public static final double SECOND_SUN_END = 0.45;
    public static final double SECOND_SUN = 0.1;
    /** Bio-glow floor: night never drops below this. */
    public static final double NIGHT_FLOOR = 0.08;

    private GreenxertzPlanet() {
        super(new Spec(new PlanetTraits(planet("greenxertz"), 0.6, true, Hazard.NONE,
                PlanetArchetype.JUNGLE, TemperatureBand.TEMPERATE, 0, 24_000L, id("greenxertz"),
                Optional.of(biome("terraformed_greenxertz_living")), Optional.of(id("xertz")), id("canopy_commune")),
                "greenxertz", "Greenxertz")
                .unlockTier(2)
                .clock(ClockSource.LEVEL_TIME, 0.25)
                .daylight(GreenxertzPlanet::daylightAt, 3.8)
                .weather(new WeatherParams(WeatherEvent.SPORE_BLOOM, 3L * 24_000L, 2_400, 6_000))
                .mining(1.0, 1)
                .terraform(TreeStyle.OAK_BIRCH,
                        () -> za.co.neroland.nerospace.registry.ModEntities.MEADOW_LOPER.get()));
    }

    static double daylightAt(double phase) {
        double p = DaylightCurve.wrap(phase);
        double v = DaylightCurve.base(p, 0.08, 1.0);
        if (p >= SECOND_SUN_START && p <= SECOND_SUN_END) {
            v += SECOND_SUN;
        }
        return Math.max(NIGHT_FLOOR, DaylightCurve.clamp01(v));
    }
}
