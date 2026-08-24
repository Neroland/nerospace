package za.co.neroland.nerospace.api;

import java.util.Optional;

/**
 * Pure snapshot resolver, kept separate from world lookup so the atmosphere rules are unit-testable
 * without a server. Package-private: the rules are an implementation detail of
 * {@link NerospaceEnvironment}, and only {@link EnvironmentSnapshot}'s bounds are public contract.
 */
@org.jetbrains.annotations.ApiStatus.Internal
final class EnvironmentRules {

    /** Oxygen concentration scale, matching {@code OxygenFieldManager}'s cell values. */
    static final int MAX_OXYGEN = EnvironmentSnapshot.MAX_OXYGEN;

    /** Terraforming stages, matching {@code TerraformConversion}. */
    static final int MAX_STAGE = EnvironmentSnapshot.MAX_TERRAFORM_STAGE;

    private EnvironmentRules() {
    }

    /**
     * Maps raw readings to a snapshot.
     *
     * <p><b>Why terraforming needs its own flag.</b> Stage 1 is the point at which
     * {@code TerraformConversion.convertColumn} flags the chunk permanently breathable, so
     * "stage &gt;= 1" and "breathable" mean the same thing and a stage alone can never describe the
     * in-between state. {@code terraformingUnderway} carries it: a region that exists and is working but
     * has not yet reached stage 1. Without it {@link Atmosphere#TERRAFORMING} would be unreachable.</p>
     *
     * @param loaded              whether the position's chunk is loaded; when false everything else is
     *                            ignored and the fail-closed vacuum snapshot is returned
     * @param terraformingUnderway an overlay region covers this position but has not yet made it breathable
     * @param fieldBreathable     the oxygen field's own verdict, which already accounts for sealed rooms
     */
    static EnvironmentSnapshot resolve(boolean loaded, Optional<PlanetId> planet, Hazard hazard,
            double gravity, int oxygen, int terraformStage, boolean terraformingUnderway,
            boolean fieldBreathable) {
        if (!loaded) {
            return new EnvironmentSnapshot(false, planet, Atmosphere.VACUUM, hazard, gravity, 0, 0, false);
        }
        int boundedOxygen = Math.clamp(oxygen, 0, MAX_OXYGEN);
        int boundedStage = Math.clamp(terraformStage, 0, MAX_STAGE);
        boolean breathable = fieldBreathable
                || boundedOxygen >= NerospaceOxygen.BREATHABLE_PRESSURE
                || boundedStage >= 1;
        Atmosphere atmosphere = breathable ? Atmosphere.BREATHABLE
                : terraformingUnderway ? Atmosphere.TERRAFORMING
                : boundedOxygen > 0 ? Atmosphere.PRESSURIZED
                : Atmosphere.VACUUM;
        return new EnvironmentSnapshot(true, planet, atmosphere, hazard, gravity, boundedOxygen,
                boundedStage, breathable);
    }
}
