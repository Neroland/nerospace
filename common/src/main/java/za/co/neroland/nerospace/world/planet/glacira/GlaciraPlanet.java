package za.co.neroland.nerospace.world.planet.glacira;

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
 * Glacira ({@code nerospace:glacira}): blue ice, methane lakes and aurora. A three-day clock on the shared phase
 * convention (0.25 = noon) with long linear twilights (dawn 0.95→0.05, dusk 0.45→0.55), a pale-sun plateau of 0.75
 * and an aurora night floor; diamond dust.
 *
 * <p>Gravity, hazard, terraform result, trees, livestock and quarry numbers are the 1.x values, unchanged.</p>
 */
public final class GlaciraPlanet extends PlanetDefinition {

    public static final GlaciraPlanet INSTANCE = new GlaciraPlanet();

    public static final double PEAK = 0.75;
    public static final double NIGHT_FLOOR = 0.12;
    /** The long twilights (phase): dawn ramps up across 0.95 to 0.05, dusk down across 0.45 to 0.55. */
    public static final double DAWN_START = 0.95;
    public static final double DAWN_END = 0.05;
    public static final double DUSK_START = 0.45;
    public static final double DUSK_END = 0.55;

    private GlaciraPlanet() {
        super(new Spec(new PlanetTraits(planet("glacira"), 0.4, true, Hazard.COLD,
                PlanetArchetype.FROZEN, TemperatureBand.FRIGID, 0, 72_000L, id("glacira"),
                Optional.of(biome("terraformed_tundra")), Optional.of(id("frostbound")), id("cryo_station")),
                "glacira", "Glacira")
                .unlockTier(4)
                // Glacira had no structures before 2.0, so it gets its own placement salt (only Greenxertz keeps 0).
                .salt(0x474C4143L)
                .daylight(GlaciraPlanet::daylightAt, 4.8)
                .weather(new WeatherParams(WeatherEvent.DIAMOND_DUST, 48_000L, 4_000, 12_000))
                .mining(0.7, 3)
                .terraform(TreeStyle.SPRUCE,
                        () -> za.co.neroland.nerospace.registry.ModEntities.WOOLLY_DRIFT.get()));
    }

    /** Diamond dust only forms in night or twilight (daylight at most 0.3). */
    @Override
    public boolean weatherMayStart(double phase) {
        return daylightAt(phase) <= 0.3;
    }

    static double daylightAt(double phase) {
        // The long-twilight profile is authored with its plateau on 0.30–0.70; shifting by a quarter day puts the
        // plateau on 0.05–0.45 so full daylight is centred on the shared noon (0.25).
        double p = DaylightCurve.wrap(phase + 0.25);
        double v;
        if (p >= 0.30 && p <= 0.70) {
            v = PEAK;
        } else if (p >= 0.20 && p < 0.30) {
            v = PEAK * (p - 0.20) / 0.10;
        } else if (p > 0.70 && p <= 0.80) {
            v = PEAK * (0.80 - p) / 0.10;
        } else {
            v = 0.0;
        }
        return Math.max(NIGHT_FLOOR, v);
    }
}
