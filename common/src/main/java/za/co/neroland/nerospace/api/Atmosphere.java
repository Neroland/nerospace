package za.co.neroland.nerospace.api;

/**
 * Coarse atmosphere state at a position, suitable for optional crop/environment integrations.
 *
 * <p><b>Public API — semver-stable.</b> Constants are only ever appended, never reordered or removed, so a
 * consumer may switch over them safely. Deliberately coarse: it describes what a plant or a life-support
 * system needs to know, not the internal oxygen field's cell values.</p>
 */
public enum Atmosphere {

    /** No breathable or pressurised air — open vacuum. */
    VACUUM,

    /** Some oxygen present but below the breathable threshold; survivable only with a suit. */
    PRESSURIZED,

    /** Under an active terraforming overlay but not yet breathable. */
    TERRAFORMING,

    /** Breathable without a suit. */
    BREATHABLE
}
