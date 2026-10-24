package za.co.neroland.nerospace.world.planet.station;

import java.util.Optional;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.PlanetArchetype;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.TreeStyle;

/**
 * The orbital station void ({@code nerospace:station}): near-zero gravity, airless, a fixed clock with the
 * planet disk lit below, no weather and nothing to terraform.
 */
public final class StationPlanet extends PlanetDefinition {

    public static final StationPlanet INSTANCE = new StationPlanet();

    private StationPlanet() {
        super(new Spec(new PlanetTraits(planet("station"), 0.1, true, Hazard.NONE,
                PlanetArchetype.ORBITAL, TemperatureBand.TEMPERATE, 1, 0L, id("station"),
                Optional.empty(), Optional.empty(), id("generic")), "station", "Orbital Station")
                .unlockTier(1)
                .clock(ClockSource.PLANET_CLOCK, 0.30)
                .daylight(DaylightCurve.constant(1.0), 2.0)
                .noMeteors()
                .terraform(TreeStyle.OAK_BIRCH, null));
    }
}
