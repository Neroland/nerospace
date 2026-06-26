package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
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

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.world.OxygenFieldEvents;
import za.co.neroland.nerospace.client.ClientBlockEntityRenderers;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.ClientOxygenVisuals;
import za.co.neroland.nerospace.client.MeteorTrackerHud;
import za.co.neroland.nerospace.client.OxygenHud;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.OxygenGeneratorScreen;
import za.co.neroland.nerospace.client.TrashCanScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.FuelRefineryScreen;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.client.PipeConfigScreen;
import za.co.neroland.nerospace.client.HydrationModuleScreen;
import za.co.neroland.nerospace.client.QuarryScreen;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.client.StarGuideScreen;
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
        MenuScreens.register(ModMenuTypes.OXYGEN_GENERATOR.get(), OxygenGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.TRASH_CAN.get(), TrashCanScreen::new);
        MenuScreens.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        MenuScreens.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.PIPE_CONFIG.get(), PipeConfigScreen::new);
        MenuScreens.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        MenuScreens.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        MenuScreens.register(ModMenuTypes.QUARRY_CONTROLLER.get(), QuarryScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORMER.get(), TerraformerScreen::new);
        MenuScreens.register(ModMenuTypes.HYDRATION_MODULE.get(), HydrationModuleScreen::new);
        MenuScreens.register(ModMenuTypes.TERRAFORM_MONITOR.get(), TerraformMonitorScreen::new);
        MenuScreens.register(ModMenuTypes.STAR_GUIDE.get(), StarGuideScreen::new);
        MenuScreens.register(ModMenuTypes.LAUNCH_CONTROLLER.get(), za.co.neroland.nerospace.client.LaunchControllerScreen::new);

        ClientEntityRenderers.registerAll(new ClientEntityRenderers.Sink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
        });

        registerFluidRendering();
        registerOxygenHud();

        // Meteor Tracker readout + oxygen-field visuals — counterpart to NeoForge's ClientTickEvent.Post.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            MeteorTrackerHud.tick();
            ClientOxygenVisuals.tick();
        });
    }

    /**
     * Bespoke oxygen/hazard HUD gauge — Fabric counterpart to NeoForge's {@code RegisterGuiLayersEvent}
     * in {@code NeoForgeClientSetup}. Draws the shared {@link OxygenHud} on top of the vanilla HUD, and
     * suppresses the vanilla air-bubble element on airless dimensions so the gauge doesn't double up (the
     * server still mirrors oxygen onto the air supply — that mirror IS the client sync the gauge reads).
     */
    private static void registerOxygenHud() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "oxygen_hud"),
                (graphics, delta) -> OxygenHud.render(graphics));
        HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, original -> (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null
                    && OxygenFieldEvents.FIELD_DIMENSIONS.contains(mc.player.level().dimension())) {
                return; // suppress vanilla air bubbles on airless dims
            }
            original.extractRenderState(graphics, delta);
        });
    }

    /**
     * Rocket fuel renders as itself (amber still/flow) in the world instead of the missing-texture
     * checkerboard. Counterpart to NeoForge's {@code RegisterFluidModelsEvent} wiring in
     * {@code NeoForgeClientSetup}: same still/flow sprites, same constant (untinted) colour — here via
     * Fabric API's {@code FluidRenderingRegistry} + the vanilla {@code FluidModel.Unbaked} / vanilla
     * {@code BlockTintSources.constant(...)} (the Fabric-rendering-fluids-v1 module ships in fabric-api).
     */
    private static void registerFluidRendering() {
        Material still = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_still"));
        Material flow = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_flow"));
        FluidModel.Unbaked model = new FluidModel.Unbaked(still, flow, still, BlockTintSources.constant(0xFFFFFFFF));
        FluidRenderingRegistry.register(ModFluids.ROCKET_FUEL.get(), ModFluids.ROCKET_FUEL_FLOWING.get(), model);
    }
}
