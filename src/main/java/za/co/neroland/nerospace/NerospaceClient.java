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

import za.co.neroland.nerospace.client.GreenxertzCreatureModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
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
        event.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.XERTZ_STALKER.get(),
                context -> new GreenxertzCreatureRenderer(context, entityTexture("xertz_stalker")));
        event.registerEntityRenderer(ModEntities.QUARTZ_CRAWLER.get(),
                context -> new GreenxertzCreatureRenderer(context, entityTexture("quartz_crawler")));
        event.registerEntityRenderer(ModEntities.GREENLING.get(),
                context -> new GreenxertzCreatureRenderer(context, entityTexture("greenling")));
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GreenxertzCreatureModel.LAYER, GreenxertzCreatureModel::createBodyLayer);
        event.registerLayerDefinition(RocketModel.LAYER, RocketModel::createBodyLayer);
    }

    private static Identifier entityTexture(String name) {
        return Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/" + name + ".png");
    }
}
