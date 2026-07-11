package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.machine.TerraformConversion;
import za.co.neroland.nerospace.world.OxygenFieldManager;

/** Public read-only atmosphere, oxygen, hazard, gravity, and terraforming query facade. */
public final class NerospaceEnvironment {

    private NerospaceEnvironment() {
    }

    public static EnvironmentSnapshot at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            throw new IllegalArgumentException("Environment query requires a level and position");
        }
        Optional<PlanetId> planet = NerospacePlanets.byDimension(level.dimension());
        PlanetTraits traits = planet.map(NerospacePlanets::traits).orElse(null);
        Hazard hazard = traits == null ? Hazard.NONE : traits.hazard();
        double gravity = planet.isEmpty() ? 1.0D : NerospacePlanets.gravityAt(level, pos);
        if (!level.hasChunkAt(pos)) {
            return EnvironmentRules.resolve(false, planet, hazard, gravity, 0, 0, false);
        }

        int field = OxygenFieldManager.get(level).concentrationAt(pos);
        int contributed = NerospaceOxygen.pressureAt(level, pos);
        int oxygen = Math.clamp(field + contributed, 0, 15);
        int physicalStage = TerraformConversion.effectiveStage(level.getChunkAt(pos));
        int overlayStage = NerospaceTerraforming.at(level, pos).map(TerraformRegion::stage).orElse(0);
        int stage = Math.max(physicalStage, overlayStage);
        return EnvironmentRules.resolve(true, planet, hazard, gravity, oxygen, stage,
                OxygenFieldManager.get(level).isBreathable(pos));
    }
}
