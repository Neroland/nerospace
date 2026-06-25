package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModEntities;

/**
 * Cross-loader entity-renderer wiring. The renderer set is identical on both loaders, so it lives
 * here once and each loader passes its own registration function ({@link Sink}) — NeoForge's
 * {@code RegisterRenderers} event, Fabric's {@code EntityRendererRegistry}. Models are baked directly
 * via {@code createBodyLayer().bakeRoot()} so no model-layer registry is needed on either loader
 * (Fabric's {@code EntityModelLayerRegistry} isn't on the de-obf classpath here).
 */
public final class ClientEntityRenderers {

    /** A loader's renderer-registration entry point. */
    public interface Sink {
        <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider);
    }

    public static void registerAll(Sink sink) {
        sink.register(ModEntities.XERTZ_STALKER.get(), context -> new GreenxertzCreatureRenderer(context,
                new XertzStalkerModel(XertzStalkerModel.createBodyLayer().bakeRoot()),
                tex("xertz_stalker"), 1.0F, 1.0F, 1.0F, 0.5F, glow("xertz_stalker")));
        sink.register(ModEntities.QUARTZ_CRAWLER.get(), context -> new GreenxertzCreatureRenderer(context,
                new QuartzCrawlerModel(QuartzCrawlerModel.createBodyLayer().bakeRoot()),
                tex("quartz_crawler"), 1.0F, 1.0F, 1.0F, 0.5F, glow("quartz_crawler")));
        sink.register(ModEntities.GREENLING.get(), context -> new GreenxertzCreatureRenderer(context,
                new GreenlingModel(GreenlingModel.createBodyLayer().bakeRoot()),
                tex("greenling"), 1.0F, 1.0F, 1.0F, 0.3F, glow("greenling")));
        sink.register(ModEntities.RUIN_WARDEN.get(), context -> new GreenxertzCreatureRenderer(context,
                new RuinWardenModel(RuinWardenModel.createBodyLayer().bakeRoot()),
                tex("ruin_warden"), 1.4F, 1.4F, 1.4F, 0.9F, glow("ruin_warden")));
        sink.register(ModEntities.CINDER_STALKER.get(), context -> new GreenxertzCreatureRenderer(context,
                new CinderStalkerModel(CinderStalkerModel.createBodyLayer().bakeRoot()),
                tex("cinder_stalker"), 1.0F, 1.0F, 1.0F, 0.6F, glow("cinder_stalker")));
        sink.register(ModEntities.FROST_STRIDER.get(), context -> new GreenxertzCreatureRenderer(context,
                new FrostStriderModel(FrostStriderModel.createBodyLayer().bakeRoot()),
                tex("frost_strider"), 1.0F, 1.0F, 1.0F, 0.5F, glow("frost_strider")));
        sink.register(ModEntities.MEADOW_LOPER.get(), context -> new GreenxertzCreatureRenderer(context,
                new MeadowLoperModel(MeadowLoperModel.createBodyLayer().bakeRoot()),
                tex("meadow_loper"), 1.0F, 1.0F, 1.0F, 0.6F, glow("meadow_loper")));
        sink.register(ModEntities.EMBER_STRUTTER.get(), context -> new GreenxertzCreatureRenderer(context,
                new EmberStrutterModel(EmberStrutterModel.createBodyLayer().bakeRoot()),
                tex("ember_strutter"), 1.0F, 1.0F, 1.0F, 0.3F, glow("ember_strutter")));
        sink.register(ModEntities.WOOLLY_DRIFT.get(), context -> new GreenxertzCreatureRenderer(context,
                new WoollyDriftModel(WoollyDriftModel.createBodyLayer().bakeRoot()),
                tex("woolly_drift"), 1.0F, 1.0F, 1.0F, 0.5F, glow("woolly_drift")));
        sink.register(ModEntities.ALIEN_VILLAGER.get(), AlienVillagerRenderer::new);

        sink.register(ModEntities.ROCKET.get(), RocketRenderer::new);
        sink.register(ModEntities.FALLING_METEOR.get(), FallingMeteorRenderer::new);
    }

    private static Identifier tex(String name) {
        return NerospaceCommon.id("textures/entity/" + name + ".png");
    }

    private static Identifier glow(String name) {
        return NerospaceCommon.id("textures/entity/" + name + "_glow.png");
    }

    private ClientEntityRenderers() {
    }
}
