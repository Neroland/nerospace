package za.co.neroland.nerospace.api;

import java.util.Optional;

/** Immutable read-only environment result. It carries no manager, mutable collection, or identity. */
public record EnvironmentSnapshot(boolean loaded, Optional<PlanetId> planet, Atmosphere atmosphere,
        Hazard hazard, double gravity, int oxygen, int terraformStage, boolean breathable) {

    public EnvironmentSnapshot {
        planet = planet == null ? Optional.empty() : planet;
        if (atmosphere == null || hazard == null || !Double.isFinite(gravity)) {
            throw new IllegalArgumentException("Invalid environment snapshot");
        }
        oxygen = Math.clamp(oxygen, 0, 15);
        terraformStage = Math.clamp(terraformStage, 0, 3);
    }
}
