package za.co.neroland.nerospace.api;

/**
 * The broad kind of world a planet is. Read from {@link PlanetTraits#archetype()}.
 *
 * <p><b>Public API — semver-stable.</b> Append-only: new constants may be added at the end in a future minor
 * version; existing ones are never removed, renamed or reordered.</p>
 */
public enum PlanetArchetype {
    /** An orbital habitat in open space (the station). */
    ORBITAL,
    /** A grey, airless moon. */
    BARREN_MOON,
    /** A cold rust desert. */
    DESERT,
    /** Ash, embers and a red giant. */
    VOLCANIC,
    /** Ice, methane and aurora. */
    FROZEN,
    /** A bioluminescent jungle. */
    JUNGLE,
    /** A tidally locked world with a habitable twilight band. */
    TWILIGHT
}
