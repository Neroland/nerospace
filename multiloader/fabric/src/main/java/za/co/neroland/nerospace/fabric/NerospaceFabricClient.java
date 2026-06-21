package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.ClientOxygenVisuals;
import za.co.neroland.nerospace.client.MeteorTrackerHud;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.FuelRefineryScreen;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.client.HydrationModuleScreen;
import za.co.neroland.nerospace.client.QuarryScreen;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.client.TerraformMonitorScreen;
import za.co.neroland.nerospace.client.TerraformerScreen;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Fabric client entry point — screen + entity-renderer registration. */
public final class NerospaceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric client bootstrap");
        FabricNetwork.registerClient();
        MenuScreens.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        MenuScreens.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        MenuScreens.register(ModMenuTypes.QUARRY_CONTROLLER.get(), QuarryScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORMER.get(), TerraformerScreen::new);
        MenuScreens.register(ModMenuTypes.HYDRATION_MODULE.get(), HydrationModuleScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORM_MONITOR.get(), TerraformMonitorScreen::new);

        ClientEntityRenderers.registerAll(new ClientEntityRenderers.Sink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });

        // Meteor Tracker readout + oxygen-field visuals — counterpart to NeoForge's ClientTickEvent.Post.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            MeteorTrackerHud.tick();
            ClientOxygenVisuals.tick();
        });
    }
}
