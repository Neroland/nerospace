package za.co.neroland.nerospace.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientBlockEntityRenderers;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.ClientOxygenVisuals;
import za.co.neroland.nerospace.client.GalleryCaptureHarness;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.FuelRefineryScreen;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.HydrationModuleScreen;
import za.co.neroland.nerospace.client.MeteorTrackerHud;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.OxygenGeneratorScreen;
import za.co.neroland.nerospace.client.OxygenHud;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.client.PipeConfigScreen;
import za.co.neroland.nerospace.client.QuarryScreen;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.client.StarGuideScreen;
import za.co.neroland.nerospace.client.TerraformMonitorScreen;
import za.co.neroland.nerospace.client.TerraformerScreen;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Forge client-only wiring for screens, renderers, fluid models, HUD, and client ticks. */
public final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    public static void init(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onClientSetup);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClientSetup::onRegisterEntityRenderers);
        ModelEvent.BakeFluidModels.BUS.addListener(ForgeClientSetup::onBakeFluidModels);
        AddGuiOverlayLayersEvent.BUS.addListener(ForgeClientSetup::onAddGuiLayers);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            MeteorTrackerHud.tick();
            ClientOxygenVisuals.tick();
            GalleryCaptureHarness.tick();
        });
        // Client-side /nerospace capture command tree (drives the local camera; separate dispatcher
        // from the server-side /nerospace gallery builder).
        RegisterClientCommandsEvent.BUS.addListener(event ->
                GalleryCaptureHarness.registerClientCommands(event.getDispatcher()));
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ForgeClientSetup::registerScreens);
    }

    private static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "oxygen_hud"),
                (g, delta) -> OxygenHud.render(g));
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientEntityRenderers.registerAll(new ClientEntityRenderers.Sink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(type, provider);
            }
        });
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        });
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.OXYGEN_GENERATOR.get(), OxygenGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        MenuScreens.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.PIPE_CONFIG.get(), PipeConfigScreen::new);
        MenuScreens.register(ModMenuTypes.ADVANCED_FILTER.get(), za.co.neroland.nerospace.client.AdvancedFilterScreen::new);
        MenuScreens.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        MenuScreens.register(ModMenuTypes.QUARRY_CONTROLLER.get(), QuarryScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORMER.get(), TerraformerScreen::new);
        MenuScreens.register(ModMenuTypes.HYDRATION_MODULE.get(), HydrationModuleScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORM_MONITOR.get(), TerraformMonitorScreen::new);
        MenuScreens.register(ModMenuTypes.STAR_GUIDE.get(), StarGuideScreen::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_CONTROLLER.get(), za.co.neroland.nerospace.client.LaunchControllerScreen::new);
        MenuScreens.register(ModMenuTypes.STATION_CHARTER.get(), za.co.neroland.nerospace.client.StationCharterScreen::new);
    }

    private static void onBakeFluidModels(ModelEvent.BakeFluidModels event) {
        Material still = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_still"));
        Material flow = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_flow"));
        FluidModel model = new FluidModel.Unbaked(still, flow, still, BlockTintSources.constant(0xFFFFFFFF))
                .bake(event.materials(), () -> NerospaceCommon.MOD_ID + ":rocket_fuel");
        event.register(ModFluids.ROCKET_FUEL.get(), model);
        event.register(ModFluids.ROCKET_FUEL_FLOWING.get(), model);
    }
}
