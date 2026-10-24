package za.co.neroland.nerospace.world.planet.nyxara;

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
 * Nyxara ({@code nerospace:nyxara}): tidally locked, a fixed sun on one horizon and a galaxy on the other.
 * Fixed clock, flares, and a {@code deep_space} gate read at launch. The dimension ships in a later stage, so
 * the planet stays hidden until it exists on the server.
 */
public final class NyxaraPlanet extends PlanetDefinition {

    public static final NyxaraPlanet INSTANCE = new NyxaraPlanet();

    private NyxaraPlanet() {
        super(new Spec(new PlanetTraits(planet("nyxara"), 0.9, true, Hazard.RADIATION,
                PlanetArchetype.TWILIGHT, TemperatureBand.TEMPERATE, 3, 0L, id("nyxara"),
                Optional.of(biome("terraformed_nyxara_living")), Optional.of(id("duskborn")),
                id("twilight_enclave")), "nyxara", "Nyxara")
                .unlockTier(4)
                .coreGate(za.co.neroland.nerolandcore.progression.CoreGates.DEEP_SPACE)
                .salt(0x4E595841L)
                .clock(ClockSource.PLANET_CLOCK, 0.25)
                .daylight(DaylightCurve.constant(0.6), 3.4)
                .weather(new WeatherParams(WeatherEvent.FLARE, 96_000L, 1_200, 2_400))
                .terraform(TreeStyle.OAK_BIRCH, null));
    }
}
