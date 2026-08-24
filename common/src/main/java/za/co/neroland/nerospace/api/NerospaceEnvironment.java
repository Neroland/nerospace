package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.machine.TerraformConversion;
import za.co.neroland.nerospace.world.OxygenFieldManager;

/**
 * Public read-only atmosphere, oxygen, hazard, gravity and terraforming query facade.
 *
 * <p><b>Public API — semver-stable.</b> This is the supported way for another Neroland mod (NeroAgriculture's
 * crop environment model, for example) to ask "what is it like here?" without touching Nerospace's internal
 * managers. It is a pure read: nothing is created, mutated or persisted, and no player data is involved.</p>
 *
 * <p><b>Server-side only.</b> Every query needs a {@link ServerLevel}.</p>
 */
public final class NerospaceEnvironment {

    private NerospaceEnvironment() {
    }

    /**
     * The environment at a position.
     *
     * <p><b>Fail-closed on unloaded chunks.</b> When {@code pos}'s chunk is not loaded the result is a
     * vacuum snapshot with {@link EnvironmentSnapshot#loaded()} {@code false} and the planet's flat default
     * gravity. Callers should treat that as "unknown, assume hostile" rather than as a reading.</p>
     *
     * <p>The unloaded check deliberately runs <em>before</em> any chunk-backed lookup. Exact gravity goes
     * through {@code GravityManager.factorAt}, which reads the biome and can call {@code getChunkAt} — so
     * resolving it first would let a read-only probe synchronously load or even generate a chunk. The flat
     * dimension default used instead needs no chunk at all.</p>
     *
     * @throws IllegalArgumentException if {@code level} or {@code pos} is null
     */
    public static EnvironmentSnapshot at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            throw new IllegalArgumentException("Environment query requires a level and position");
        }
        Optional<PlanetId> planet = NerospacePlanets.byDimension(level.dimension());
        PlanetTraits traits = planet.map(NerospacePlanets::traits).orElse(null);
        Hazard hazard = traits == null ? Hazard.NONE : traits.hazard();

        if (!level.hasChunkAt(pos)) {
            double flatGravity = traits == null ? 1.0D : traits.defaultGravity();
            return EnvironmentRules.resolve(false, planet, hazard, flatGravity, 0, 0, false, false);
        }

        double gravity = traits == null ? 1.0D : NerospacePlanets.gravityAt(level, pos);
        OxygenFieldManager field = OxygenFieldManager.get(level);
        int oxygen = field.concentrationAt(pos) + NerospaceOxygen.pressureAt(level, pos);
        int physicalStage = TerraformConversion.effectiveStage(level.getChunkAt(pos));
        Optional<TerraformRegion> overlay = NerospaceTerraforming.at(level, pos);
        int stage = Math.max(physicalStage, overlay.map(TerraformRegion::stage).orElse(0));
        // An overlay that exists but has not yet reached stage 1 is work in progress, not yet air.
        boolean underway = overlay.isPresent() && stage < 1;
        return EnvironmentRules.resolve(true, planet, hazard, gravity, oxygen, stage, underway,
                field.isBreathable(pos));
    }
}
