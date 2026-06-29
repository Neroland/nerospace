package za.co.neroland.nerospace.api;

/**
 * Read-only, immutable snapshot of a planet's basic gameplay traits.
 *
 * <p><b>Public API — semver-stable.</b> A flat DTO over traits already modelled internally
 * ({@code world.gravity.GravityManager}, {@code world.OxygenManager}); none of those internal classes are
 * exposed. Obtain via {@link NerospacePlanets#traits(PlanetId)}.</p>
 *
 * @param planet         the planet these traits describe
 * @param defaultGravity the flat dimension-default gravity factor ({@code 1.0} = Earth-normal), config-scaled,
 *                       with no per-position biome-tag or terraforming override applied. For an exact
 *                       per-position reading use {@link NerospacePlanets#gravityAt}.
 * @param airless        {@code true} if the world has no breathable atmosphere (all current Nerospace planets do)
 * @param hazard         the environmental {@link Hazard} the world carries
 */
public record PlanetTraits(PlanetId planet, double defaultGravity, boolean airless, Hazard hazard) {

    public PlanetTraits {
        if (planet == null) {
            throw new IllegalArgumentException("PlanetTraits planet must not be null");
        }
        if (hazard == null) {
            throw new IllegalArgumentException("PlanetTraits hazard must not be null");
        }
    }
}
