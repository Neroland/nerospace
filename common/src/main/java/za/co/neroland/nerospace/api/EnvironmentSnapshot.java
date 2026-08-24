package za.co.neroland.nerospace.api;

import java.util.Optional;

/**
 * Immutable read-only environment result returned by {@link NerospaceEnvironment#at}.
 *
 * <p><b>Public API — semver-stable.</b> It carries no manager, no mutable collection and no player
 * identity, so it is safe to hand to any other mod and safe to cache. Values are clamped on
 * construction, so a malformed or hand-edited saved-data row cannot push a consumer out of range.</p>
 *
 * @param loaded         false when the position's chunk is not loaded; every other field is then the
 *                       documented fail-closed default rather than a real reading (see
 *                       {@link NerospaceEnvironment#at})
 * @param planet         the Nerospace planet, or empty on Earth / any non-Nerospace dimension
 * @param atmosphere     coarse atmosphere state
 * @param hazard         the planet's ambient hazard
 * @param gravity        gravity factor, {@code 1.0} = Earth-normal
 * @param oxygen         oxygen concentration, 0-15
 * @param terraformStage terraforming stage, 0-3
 * @param breathable     whether a player can breathe here without a suit
 */
public record EnvironmentSnapshot(boolean loaded, Optional<PlanetId> planet, Atmosphere atmosphere,
        Hazard hazard, double gravity, int oxygen, int terraformStage, boolean breathable) {

    /** Top of the oxygen scale, matching Nerospace's internal oxygen field. */
    public static final int MAX_OXYGEN = 15;

    /** Highest terraforming stage. */
    public static final int MAX_TERRAFORM_STAGE = 3;

    public EnvironmentSnapshot {
        planet = planet == null ? Optional.empty() : planet;
        if (atmosphere == null || hazard == null || !Double.isFinite(gravity)) {
            throw new IllegalArgumentException("Invalid environment snapshot");
        }
        oxygen = Math.clamp(oxygen, 0, MAX_OXYGEN);
        terraformStage = Math.clamp(terraformStage, 0, MAX_TERRAFORM_STAGE);
    }
}
