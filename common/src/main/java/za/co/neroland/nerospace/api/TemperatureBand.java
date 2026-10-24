package za.co.neroland.nerospace.api;

/**
 * Coarse surface temperature of a planet. Read from {@link PlanetTraits#temperatureBand()}.
 *
 * <p><b>Public API — semver-stable.</b> Append-only.</p>
 */
public enum TemperatureBand {
    FRIGID,
    COLD,
    TEMPERATE,
    HOT,
    INFERNAL
}
