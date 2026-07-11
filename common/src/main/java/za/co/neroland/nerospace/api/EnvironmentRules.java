package za.co.neroland.nerospace.api;

import java.util.Optional;

/** Pure snapshot resolver kept separate from world lookup so atmosphere rules are unit-testable. */
@org.jetbrains.annotations.ApiStatus.Internal
final class EnvironmentRules {

    private EnvironmentRules() {
    }

    static EnvironmentSnapshot resolve(boolean loaded, Optional<PlanetId> planet, Hazard hazard,
            double gravity, int oxygen, int terraformStage, boolean fieldBreathable) {
        if (!loaded) {
            return new EnvironmentSnapshot(false, planet, Atmosphere.VACUUM, hazard, gravity, 0, 0, false);
        }
        int boundedOxygen = Math.clamp(oxygen, 0, 15);
        int boundedStage = Math.clamp(terraformStage, 0, 3);
        boolean breathable = fieldBreathable || boundedOxygen >= 6 || boundedStage >= 1;
        Atmosphere atmosphere = breathable ? Atmosphere.BREATHABLE
                : boundedStage > 0 ? Atmosphere.TERRAFORMING
                : boundedOxygen > 0 ? Atmosphere.PRESSURIZED
                : Atmosphere.VACUUM;
        return new EnvironmentSnapshot(true, planet, atmosphere, hazard, gravity, boundedOxygen,
                boundedStage, breathable);
    }
}
