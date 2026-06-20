package za.co.neroland.nerospace.neoforge;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.FuelRefineryScreen;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.client.QuarryScreen;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** NeoForge client-only wiring (screen + fluid-model registration). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterFluidModels);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterEntityRenderers);
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientEntityRenderers.registerAll(new ClientEntityRenderers.Sink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(type, provider);
            }
        });
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        event.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        event.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        event.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        event.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        event.register(ModMenuTypes.QUARRY_CONTROLLER.get(), QuarryScreen::new);
    }

    /** Rocket fuel renders as itself (amber still/flow) instead of the default missing art. */
    private static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_still"));
        Material flow = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_flow"));
        event.register(
                new FluidModel.Unbaked(still, flow, still, FluidTintSources.constant(0xFFFFFFFF)),
                ModFluids.ROCKET_FUEL, ModFluids.ROCKET_FUEL_FLOWING);
    }
}
