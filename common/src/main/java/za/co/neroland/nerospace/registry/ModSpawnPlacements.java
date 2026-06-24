package za.co.neroland.nerospace.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Cross-loader natural-spawn placement rules for the ported creatures. The loaders apply them
 * differently — NeoForge through {@code RegisterSpawnPlacementsEvent} (which adds a REPLACE/AND/OR
 * {@code Operation}), Fabric through the vanilla {@code SpawnPlacements#register} static — so this
 * exposes each rule (entity type + placement type + heightmap + predicate) through a {@link Sink}
 * and lets each loader register it its own way.
 *
 * <p>Every creature spawns on solid ground with open space above. The Xertz Stalker (hostile)
 * deliberately keeps a light-independent rule so Greenxertz is dangerous day and night; the
 * terraform livestock graze only on grassed (matured) ground. The Ruin Warden has no natural rule
 * — it is a structure/event boss only.
 */
public final class ModSpawnPlacements {

    /** Receives one placement rule for loader-specific registration. */
    public interface Sink {
        <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placementType,
                Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);
    }

    public static void registerAll(Sink sink) {
        ground(sink, ModEntities.XERTZ_STALKER);
        ground(sink, ModEntities.QUARTZ_CRAWLER);
        ground(sink, ModEntities.GREENLING);
        ground(sink, ModEntities.CINDER_STALKER);
        ground(sink, ModEntities.FROST_STRIDER);
        ground(sink, ModEntities.ALIEN_VILLAGER);
        grass(sink, ModEntities.MEADOW_LOPER);
        grass(sink, ModEntities.EMBER_STRUTTER);
        grass(sink, ModEntities.WOOLLY_DRIFT);
    }

    /** Solid ground below, open air at the spawn position; light-independent. */
    private static <T extends Mob> void ground(Sink sink, RegistryEntry<EntityType<T>> entry) {
        sink.register(entry.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir());
    }

    /** Grassed (matured/terraformed) ground only — the reliable runtime signal for mature biomes. */
    private static <T extends Mob> void grass(Sink sink, RegistryEntry<EntityType<T>> entry) {
        sink.register(entry.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                                && level.getBlockState(pos).isAir());
    }

    private ModSpawnPlacements() {
    }
}
