package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.entity.CinderStalker;
import za.co.neroland.nerospace.entity.Greenling;
import za.co.neroland.nerospace.entity.QuartzCrawler;
import za.co.neroland.nerospace.entity.XertzStalker;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Entity types (Phase 4). The rocket is a non-living, rideable vehicle, so it uses
 * {@link MobCategory#MISC} (never naturally spawned) and is summoned only via its item.
 */
public final class ModEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(Nerospace.MODID);

    public static final Supplier<EntityType<RocketEntity>> ROCKET = ENTITY_TYPES.registerEntityType(
            "rocket",
            RocketEntity::new,
            MobCategory.MISC,
            builder -> builder
                    // ~3x3 footprint (matches the launch pad) and tall enough to seat the rider inside.
                    .sized(2.6F, 5.0F)
                    .eyeHeight(2.4F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    // --- Greenxertz creatures (Phase 5) -------------------------------------

    public static final Supplier<EntityType<XertzStalker>> XERTZ_STALKER = ENTITY_TYPES.registerEntityType(
            "xertz_stalker",
            XertzStalker::new,
            MobCategory.MONSTER,
            builder -> builder.sized(0.7F, 1.9F).eyeHeight(1.6F).clientTrackingRange(8));

    public static final Supplier<EntityType<QuartzCrawler>> QUARTZ_CRAWLER = ENTITY_TYPES.registerEntityType(
            "quartz_crawler",
            QuartzCrawler::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.9F, 0.8F).eyeHeight(0.6F).clientTrackingRange(8));

    public static final Supplier<EntityType<Greenling>> GREENLING = ENTITY_TYPES.registerEntityType(
            "greenling",
            Greenling::new,
            MobCategory.AMBIENT,
            builder -> builder.sized(0.5F, 0.6F).eyeHeight(0.45F).clientTrackingRange(8));

    // --- Cindara creatures (Phase 7) ----------------------------------------

    public static final Supplier<EntityType<CinderStalker>> CINDER_STALKER = ENTITY_TYPES.registerEntityType(
            "cinder_stalker",
            CinderStalker::new,
            MobCategory.MONSTER,
            builder -> builder.sized(0.8F, 2.0F).eyeHeight(1.7F).fireImmune().clientTrackingRange(8));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
