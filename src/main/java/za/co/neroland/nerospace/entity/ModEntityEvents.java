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
        event.put(ModEntities.FROST_STRIDER.get(), FrostStrider.createAttributes().build());
        // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5).
        event.put(ModEntities.MEADOW_LOPER.get(), MeadowLoper.createAttributes().build());
        event.put(ModEntities.EMBER_STRUTTER.get(), EmberStrutter.createAttributes().build());
        event.put(ModEntities.WOOLLY_DRIFT.get(), WoollyDrift.createAttributes().build());
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

        event.register(ModEntities.FROST_STRIDER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5): graze only on grassed (Living)
        // ground — the mature biomes are runtime-written, so grass is the reliable signal.
        registerLivestockPlacement(event, ModEntities.MEADOW_LOPER.get());
        registerLivestockPlacement(event, ModEntities.EMBER_STRUTTER.get());
        registerLivestockPlacement(event, ModEntities.WOOLLY_DRIFT.get());
    }

    private static void registerLivestockPlacement(RegisterSpawnPlacementsEvent event,
            net.minecraft.world.entity.EntityType<? extends TerraformLivestock> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) ->
                        level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                                && level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
