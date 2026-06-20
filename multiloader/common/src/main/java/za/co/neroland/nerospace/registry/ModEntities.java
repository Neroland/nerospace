package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.entity.Greenling;
import za.co.neroland.nerospace.entity.QuartzCrawler;
import za.co.neroland.nerospace.entity.XertzStalker;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Entity types, ported cross-loader through {@link RegistrationProvider} over the vanilla
 * {@code ENTITY_TYPE} registry (the root used NeoForge's {@code DeferredRegister.Entities}). The
 * builder's {@code build(ResourceKey)} consumes the key the provider hands the factory. Attributes
 * are applied per-loader from {@link ModEntityAttributes}; renderers from
 * {@code client/ClientEntityRenderers}. Natural-spawn placement rules are deferred until the planet
 * dimensions land (the creatures are summonable meanwhile).
 */
public final class ModEntities {

    public static final RegistrationProvider<EntityType<?>> ENTITY_TYPES =
            RegistrationProvider.get(Registries.ENTITY_TYPE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<EntityType<XertzStalker>> XERTZ_STALKER = ENTITY_TYPES.register(
            "xertz_stalker",
            key -> EntityType.Builder.of(XertzStalker::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.9F).eyeHeight(1.6F).clientTrackingRange(8).build(key));

    public static final RegistryEntry<EntityType<QuartzCrawler>> QUARTZ_CRAWLER = ENTITY_TYPES.register(
            "quartz_crawler",
            key -> EntityType.Builder.of(QuartzCrawler::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.8F).eyeHeight(0.6F).clientTrackingRange(8).build(key));

    public static final RegistryEntry<EntityType<Greenling>> GREENLING = ENTITY_TYPES.register(
            "greenling",
            key -> EntityType.Builder.of(Greenling::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.6F).eyeHeight(0.45F).clientTrackingRange(8).build(key));

    private ModEntities() {
    }

    public static void init() {
    }
}
