package za.co.neroland.nerospace.world.planet.mars;

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
 * Mars ({@code nerospace:mars}): rust under a butterscotch sky. A 24.6 h sol (24 600 ticks), a thin-atmosphere
 * daylight peak of 0.85 and dust storms. The dimension ships in a later stage, so the planet stays hidden
 * until it exists on the server.
 */
public final class MarsPlanet extends PlanetDefinition {

    public static final MarsPlanet INSTANCE = new MarsPlanet();

    public static final double PEAK = 0.85;

    private MarsPlanet() {
        super(new Spec(new PlanetTraits(planet("mars"), 0.38, true, Hazard.COLD,
                PlanetArchetype.DESERT, TemperatureBand.COLD, 1, 24_600L, id("mars"),
                Optional.of(biome("terraformed_mars_living")), Optional.empty(), id("martian_settlement")),
                "mars", "Mars")
                .unlockTier(2)
                .salt(0x4D415253L)
                .daylight(phase -> DaylightCurve.base(phase, 0.04, PEAK), 4.8)
                .weather(new WeatherParams(WeatherEvent.DUST_STORM, 61_500L, 6_000, 18_000))
                .terraform(TreeStyle.OAK_BIRCH, null));
    }
}
