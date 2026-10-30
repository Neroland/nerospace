package za.co.neroland.nerospace.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTables;

/**
 * Starter-herd seeding for Living terraformed ground (DEEPER_TERRAFORM_DESIGN.md §5). Natural
 * CREATURE spawning alone is too slow post-worldgen, so a sparse fraction of stage-3 columns actively
 * spawns a pair of the planet's livestock — capped by a nearby-population count so herds never balloon.
 * Biome spawn settings on the mature biomes remain the long-term backstop.
 *
 * <p>2.0: the species comes from the planet's terraform table (its fauna set). A species that is not
 * registered yet seeds nothing — never a stand-in from another world. Off-planet dimensions keep the 1.x Meadow
 * Loper.</p>
 *
 * <p>Cross-loader port note: the herd config (enable/chance/cap/radius) is inlined to the root's
 * shipped defaults (config seam deferred).</p>
 */
public final class TerraformFauna {

    private static final boolean FAUNA_ENABLED = true;
    private static final double HERD_CHANCE = 0.02D;
    private static final int HERD_RADIUS = 48;
    private static final int HERD_CAP = 8;

    private TerraformFauna() {
    }

    /** The livestock species seeded on this dimension's Living ground (§5), or null for none. */
    @Nullable
    public static EntityType<?> livestockFor(ServerLevel level) {
        TerraformTable table = TerraformTables.resolve(Planets.get(level.dimension()));
        return table == null ? null : livestockFor(table);
    }

    /** The first registered species of {@code table}'s fauna set, or null. */
    @Nullable
    public static EntityType<?> livestockFor(TerraformTable table) {
        for (Identifier id : table.fauna()) {
            // ENTITY_TYPE is a defaulted registry (pig): check the key before reading the value.
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return BuiltInRegistries.ENTITY_TYPE.getValue(id);
            }
        }
        return null;
    }

    /** Maybe seed a starter pair on a freshly Living column (sparse, population-capped). */
    public static void seedHerd(ServerLevel level, int x, int surfaceY, int z) {
        TerraformTable table = TerraformTables.resolve(Planets.get(level.dimension()));
        if (table != null) {
            seedHerd(level, table, x, surfaceY, z);
        }
    }

    /** {@link #seedHerd(ServerLevel, int, int, int)} with the planet's table already resolved. */
    public static void seedHerd(ServerLevel level, TerraformTable table, int x, int surfaceY, int z) {
        if (!FAUNA_ENABLED) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= HERD_CHANCE) {
            return;
        }
        EntityType<?> species = livestockFor(table);
        if (species == null) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY, z);
        int nearby = level.getEntities(species, new AABB(ground).inflate(HERD_RADIUS, HERD_RADIUS, HERD_RADIUS),
                e -> e.isAlive()).size();
        if (nearby >= HERD_CAP) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            species.spawn(level, ground, EntitySpawnReason.EVENT);
        }
    }
}
