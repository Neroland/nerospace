package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
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
                    .sized(0.9F, 2.6F)
                    .eyeHeight(1.6F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
