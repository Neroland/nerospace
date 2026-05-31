package za.co.neroland.nerospace.entity;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModEntities;

/**
 * Mod-bus registration for the Phase 5 creatures: their attribute suppliers and their natural-spawn
 * placement rules. Each creature spawns on solid ground with open space above; the Xertz Stalker
 * (hostile) deliberately uses a light-independent rule so the planet is dangerous day and night.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class ModEntityEvents {

    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.XERTZ_STALKER.get(), XertzStalker.createAttributes().build());
        event.put(ModEntities.QUARTZ_CRAWLER.get(), QuartzCrawler.createAttributes().build());
        event.put(ModEntities.GREENLING.get(), Greenling.createAttributes().build());
        event.put(ModEntities.CINDER_STALKER.get(), CinderStalker.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.XERTZ_STALKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.QUARTZ_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.GREENLING.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.CINDER_STALKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
