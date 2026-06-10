package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.entity.CinderStalker;
import za.co.neroland.nerospace.entity.EmberStrutter;
import za.co.neroland.nerospace.entity.FrostStrider;
import za.co.neroland.nerospace.entity.Greenling;
import za.co.neroland.nerospace.entity.MeadowLoper;
import za.co.neroland.nerospace.entity.QuartzCrawler;
import za.co.neroland.nerospace.entity.WoollyDrift;
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
                    // Seat the rider INSIDE the hull at cockpit height (the window band), not
                    // perched on the nose cone (the default attachment is the entity's top).
                    .passengerAttachments(2.2F)
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

    // --- Glacira creatures (NEW_DESTINATION_DESIGN.md) -----------------------

    /** Tall stilt-legged ice predator; freeze immunity lives in {@link FrostStrider#canFreeze()}. */
    public static final Supplier<EntityType<FrostStrider>> FROST_STRIDER = ENTITY_TYPES.registerEntityType(
            "frost_strider",
            FrostStrider::new,
            MobCategory.MONSTER,
            builder -> builder.sized(0.8F, 2.4F).eyeHeight(2.1F).clientTrackingRange(8));

    // --- Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5) -----------------

    /** Placid cow-analogue grazer of mature terraformed Greenxertz. */
    public static final Supplier<EntityType<MeadowLoper>> MEADOW_LOPER = ENTITY_TYPES.registerEntityType(
            "meadow_loper",
            MeadowLoper::new,
            MobCategory.CREATURE,
            builder -> builder.sized(1.1F, 1.3F).eyeHeight(1.1F).clientTrackingRange(8));

    /** Skittish chicken-analogue of mature terraformed Cindara (fire-proof like its homeworld). */
    public static final Supplier<EntityType<EmberStrutter>> EMBER_STRUTTER = ENTITY_TYPES.registerEntityType(
            "ember_strutter",
            EmberStrutter::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.5F, 0.9F).eyeHeight(0.7F).fireImmune().clientTrackingRange(8));

    /** Shaggy sheep-analogue of mature terraformed Glacira; cold-proof via {@code canFreeze()}. */
    public static final Supplier<EntityType<WoollyDrift>> WOOLLY_DRIFT = ENTITY_TYPES.registerEntityType(
            "woolly_drift",
            WoollyDrift::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.9F, 1.2F).eyeHeight(1.0F).clientTrackingRange(8));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
