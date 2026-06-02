package za.co.neroland.nerospace;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import za.co.neroland.nerospace.client.CinderStalkerModel;
import za.co.neroland.nerospace.client.GreenlingModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.client.QuartzCrawlerModel;
import za.co.neroland.nerospace.client.XertzStalkerModel;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.OxygenGeneratorScreen;
import za.co.neroland.nerospace.client.RocketModel;
import za.co.neroland.nerospace.client.RocketRenderer;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModMenuTypes;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Nerospace.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Nerospace.MODID, value = Dist.CLIENT)
public class NerospaceClient {
    public NerospaceClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Nerospace.LOGGER.info("Nerospace client setup complete.");
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        event.register(ModMenuTypes.OXYGEN_GENERATOR.get(), OxygenGeneratorScreen::new);
        event.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        event.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        // Each creature now has its own model geometry; the scale just fine-tunes size.
        event.registerEntityRenderer(ModEntities.XERTZ_STALKER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new XertzStalkerModel(context.bakeLayer(XertzStalkerModel.LAYER)),
                        entityTexture("xertz_stalker"), 1.0F, 1.0F, 1.0F, 0.5F));
        event.registerEntityRenderer(ModEntities.QUARTZ_CRAWLER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new QuartzCrawlerModel(context.bakeLayer(QuartzCrawlerModel.LAYER)),
                        entityTexture("quartz_crawler"), 1.0F, 1.0F, 1.0F, 0.5F));
        event.registerEntityRenderer(ModEntities.GREENLING.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new GreenlingModel(context.bakeLayer(GreenlingModel.LAYER)),
                        entityTexture("greenling"), 1.0F, 1.0F, 1.0F, 0.3F));
        event.registerEntityRenderer(ModEntities.CINDER_STALKER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new CinderStalkerModel(context.bakeLayer(CinderStalkerModel.LAYER)),
                        entityTexture("cinder_stalker"), 1.0F, 1.0F, 1.0F, 0.6F));
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GreenxertzCreatureModel.LAYER, GreenxertzCreatureModel::createBodyLayer);
        event.registerLayerDefinition(XertzStalkerModel.LAYER, XertzStalkerModel::createBodyLayer);
        event.registerLayerDefinition(QuartzCrawlerModel.LAYER, QuartzCrawlerModel::createBodyLayer);
        event.registerLayerDefinition(GreenlingModel.LAYER, GreenlingModel::createBodyLayer);
        event.registerLayerDefinition(CinderStalkerModel.LAYER, CinderStalkerModel::createBodyLayer);
        event.registerLayerDefinition(RocketModel.LAYER, RocketModel::createBodyLayer);
    }

    private static Identifier entityTexture(String name) {
        return Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/" + name + ".png");
    }
}
