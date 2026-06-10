package za.co.neroland.nerospace.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Tuning;

/**
 * Starter-herd seeding for Living terraformed ground (DEEPER_TERRAFORM_DESIGN.md §5). Natural
 * CREATURE spawning alone is too slow post-worldgen, so a sparse fraction of stage-3 columns
 * actively spawns a pair of the planet's livestock — capped by a nearby-population count so herds
 * never balloon. Biome spawn settings on the mature biomes remain the long-term backstop.
 */
public final class TerraformFauna {

    private TerraformFauna() {
    }

    /** The livestock species seeded on this dimension's Living ground (§5), or null for none. */
    @Nullable
    public static EntityType<?> livestockFor(ServerLevel level) {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = level.dimension();
        if (za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL.equals(dimension)) {
            return za.co.neroland.nerospace.registry.ModEntities.EMBER_STRUTTER.get();
        }
        if (za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL.equals(dimension)) {
            return za.co.neroland.nerospace.registry.ModEntities.WOOLLY_DRIFT.get();
        }
        return za.co.neroland.nerospace.registry.ModEntities.MEADOW_LOPER.get();
    }

    /** Maybe seed a starter pair on a freshly Living column (sparse, population-capped). */
    public static void seedHerd(ServerLevel level, int x, int surfaceY, int z) {
        if (!Config.TERRAFORM_FAUNA_ENABLED.get()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= Tuning.TERRAFORM_HERD_CHANCE) {
            return;
        }
        EntityType<?> species = livestockFor(level);
        if (species == null) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY, z);
        int r = Tuning.TERRAFORM_HERD_RADIUS;
        int nearby = level.getEntities(species, new AABB(ground).inflate(r, r, r), e -> e.isAlive()).size();
        if (nearby >= Tuning.TERRAFORM_HERD_CAP) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            species.spawn(level, ground, EntitySpawnReason.EVENT);
        }
    }
}
