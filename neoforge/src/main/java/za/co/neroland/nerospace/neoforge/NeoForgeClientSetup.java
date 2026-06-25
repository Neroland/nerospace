package za.co.neroland.nerospace.neoforge;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
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
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModMenuTypes;
import za.co.neroland.nerospace.world.OxygenFieldEvents;

/** NeoForge client-only wiring (screen + fluid-model registration). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterFluidModels);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterEntityRenderers);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterGuiLayers);
        // Suppress the vanilla air-bubble row on airless dimensions (the bespoke gauge is the readout there).
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onRenderGuiLayer);
        // Meteor Tracker readout + oxygen-field visuals — game-bus client tick (counterpart to Fabric's END_CLIENT_TICK).
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            MeteorTrackerHud.tick();
            ClientOxygenVisuals.tick();
        });
    }

    /** Register the bespoke oxygen/hazard HUD gauge on top of the vanilla HUD (shared draw in {@code common}). */
    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                NerospaceCommon.id("oxygen_hud"),
                (g, delta) -> OxygenHud.render(g));
    }

    /**
     * Hide the vanilla air-bubble row on airless Nerospace dimensions — the bespoke {@link OxygenHud} is
     * the oxygen readout there. The server still mirrors oxygen onto the air supply (that mirror IS the
     * client sync the gauge reads); it just no longer double-renders as bubbles.
     */
    private static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player != null
                && OxygenFieldEvents.FIELD_DIMENSIONS.contains(player.level().dimension())) {
            event.setCanceled(true);
        }
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientEntityRenderers.registerAll(new ClientEntityRenderers.Sink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(
                        NerospaceCommon.requireNonNull(type),
                        NerospaceCommon.requireNonNull(provider));
            }
        });
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(
                        NerospaceCommon.requireNonNull(type),
                        NerospaceCommon.requireNonNull(provider));
            }
        });
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.OXYGEN_GENERATOR.get(), OxygenGeneratorScreen::new);
        event.register(ModMenuTypes.TRASH_CAN.get(), TrashCanScreen::new);
        event.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        event.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        event.register(ModMenuTypes.PIPE_CONFIG.get(), PipeConfigScreen::new);
        event.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        event.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        event.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        event.register(ModMenuTypes.QUARRY_CONTROLLER.get(), QuarryScreen::new);
        event.register(ModMenuTypes.TERRAFORMER.get(), TerraformerScreen::new);
        event.register(ModMenuTypes.HYDRATION_MODULE.get(), HydrationModuleScreen::new);
        event.register(ModMenuTypes.TERRAFORM_MONITOR.get(), TerraformMonitorScreen::new);
        event.register(ModMenuTypes.STAR_GUIDE.get(), StarGuideScreen::new);
    }

    /** Rocket fuel renders as itself (amber still/flow) instead of the default missing art. */
    private static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(NerospaceCommon.id("block/rocket_fuel_still"));
        Material flow = new Material(NerospaceCommon.id("block/rocket_fuel_flow"));
        @NonNull Supplier<? extends Fluid> source = NerospaceCommon.requireNonNull(ModFluids.ROCKET_FUEL);
        @NonNull Supplier<? extends Fluid> flowing = NerospaceCommon.requireNonNull(ModFluids.ROCKET_FUEL_FLOWING);
        event.register(
                new FluidModel.Unbaked(still, flow, still, FluidTintSources.constant(0xFFFFFFFF)),
                source, flowing);
    }
}
