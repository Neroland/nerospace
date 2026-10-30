package za.co.neroland.nerospace.world.planet.luna;

import java.util.Optional;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.PlanetArchetype;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.TreeStyle;

/**
 * The Moon ({@code nerospace:luna}): grey, silent and slow. A 28-day clock with a hard terminator and an
 * Earthrise window that adds a little earthshine; no weather. The dimension ships in a later stage, so the
 * planet stays hidden until it exists on the server.
 */
public final class LunaPlanet extends PlanetDefinition {

    public static final LunaPlanet INSTANCE = new LunaPlanet();

    /** Earthrise window (phase) and the earthshine it adds. */
    public static final double EARTHRISE_START = 0.78;
    public static final double EARTHRISE_END = 0.86;
    public static final double EARTHSHINE = 0.05;

    private LunaPlanet() {
        super(new Spec(new PlanetTraits(planet("luna"), 0.17, true, Hazard.NONE,
                PlanetArchetype.BARREN_MOON, TemperatureBand.COLD, 1, 672_000L, id("luna"),
                Optional.of(biome("terraformed_luna_living")), Optional.empty(), id("lunar_outpost")),
                "luna", "Luna")
                .unlockTier(1)
                .salt(0x4C554E41L)
                .daylight(LunaPlanet::daylightAt, 4.0)
                .terraform(TreeStyle.OAK_BIRCH, () -> LunaContent.MOON_HOPPER.get()));
    }

    static double daylightAt(double phase) {
        double p = DaylightCurve.wrap(phase);
        double v = DaylightCurve.base(p, 0.01, 1.0);
        if (p >= EARTHRISE_START && p <= EARTHRISE_END) {
            v += EARTHSHINE;
        }
        return DaylightCurve.clamp01(v);
    }
}
