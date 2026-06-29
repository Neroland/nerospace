package za.co.neroland.nerospace.api;

/**
 * The environmental hazard a planet carries (what a worn suit variant must counter).
 *
 * <p><b>Public API — semver-stable.</b> A stable mirror of the internal hazard model so consumers never
 * depend on {@code world.OxygenManager}. New constants may be appended in future minor versions; existing
 * ones will not be removed or renamed.</p>
 */
public enum Hazard {
    /** No environmental hazard (Greenxertz, the orbital station, and any non-Nerospace dimension). */
    NONE,
    /** Extreme heat (Cindara). Countered by a full Heat suit variant. */
    HEAT,
    /** Extreme cold (Glacira). Countered by a full Cryo suit variant. */
    COLD
}
