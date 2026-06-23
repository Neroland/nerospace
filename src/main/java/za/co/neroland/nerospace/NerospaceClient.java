package za.co.neroland.nerospace;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;

import za.co.neroland.nerospace.client.ClientOxygenField;
import za.co.neroland.nerospace.client.OxygenHudLayer;

import za.co.neroland.nerospace.client.CinderStalkerModel;
import za.co.neroland.nerospace.client.FrostStriderModel;
import za.co.neroland.nerospace.client.GreenlingModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.client.QuartzCrawlerModel;
import za.co.neroland.nerospace.client.XertzStalkerModel;
import za.co.neroland.nerospace.client.FuelRefineryScreen;
import za.co.neroland.nerospace.client.FuelTankScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.OxygenGeneratorScreen;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.client.ClientMeteorTracker;
import za.co.neroland.nerospace.client.FallingMeteorModel;
import za.co.neroland.nerospace.client.FallingMeteorRenderer;
import za.co.neroland.nerospace.client.RocketModel;
import za.co.neroland.nerospace.client.RocketRenderer;
import za.co.neroland.nerospace.meteor.MeteorSite;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.client.UniversalPipeRenderer;
import za.co.neroland.nerospace.client.RocketScreen;
import za.co.neroland.nerospace.client.TerraformerScreen;
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

    /**
     * Fluid client visuals (ART_OVERHAUL_DESIGN.md §5): rocket_fuel finally renders as itself in
     * the world — animated amber still/flow strips instead of the default missing art. 26.1 routes
     * fluid sprites through {@code RegisterFluidModelsEvent} + {@code FluidModel.Unbaked} (the old
     * {@code IClientFluidTypeExtensions} texture hooks are gone — javap-confirmed).
     */
    @SubscribeEvent
    static void onRegisterFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        var still = new net.minecraft.client.resources.model.sprite.Material(
                Identifier.fromNamespaceAndPath(Nerospace.MODID, "block/rocket_fuel_still"));
        var flow = new net.minecraft.client.resources.model.sprite.Material(
                Identifier.fromNamespaceAndPath(Nerospace.MODID, "block/rocket_fuel_flow"));
        event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                        still, flow, still,
                        net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xFFFFFFFF)),
                za.co.neroland.nerospace.fluid.ModFluids.ROCKET_FUEL,
                za.co.neroland.nerospace.fluid.ModFluids.ROCKET_FUEL_FLOWING);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        event.register(ModMenuTypes.QUARRY_CONTROLLER.get(),
                za.co.neroland.nerospace.client.QuarryScreen::new);
        event.register(ModMenuTypes.OXYGEN_GENERATOR.get(), OxygenGeneratorScreen::new);
        event.register(ModMenuTypes.FUEL_TANK.get(), FuelTankScreen::new);
        event.register(ModMenuTypes.FUEL_REFINERY.get(), FuelRefineryScreen::new);
        event.register(ModMenuTypes.TERRAFORMER.get(), TerraformerScreen::new);
        event.register(ModMenuTypes.HYDRATION_MODULE.get(),
                za.co.neroland.nerospace.client.HydrationModuleScreen::new);
        event.register(ModMenuTypes.TERRAFORM_MONITOR.get(),
                za.co.neroland.nerospace.client.TerraformMonitorScreen::new);
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
        event.register(ModMenuTypes.ROCKET.get(), RocketScreen::new);
        event.register(ModMenuTypes.STAR_GUIDE.get(), za.co.neroland.nerospace.client.StarGuideScreen::new);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_hud"), new OxygenHudLayer());
    }

    /**
     * The bespoke {@link OxygenHudLayer} is the oxygen readout on airless dimensions, so hide the
     * vanilla air-bubble row there — the server still mirrors oxygen onto the air supply (that
     * mirror IS the client sync the gauge reads), it just no longer double-renders as bubbles.
     */
    @SubscribeEvent
    static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null
                && za.co.neroland.nerospace.world.OxygenFieldEvents.FIELD_DIMENSIONS
                        .contains(mc.player.level().dimension())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        // Meteor events (meteor-events-design.md): the tumbling, trailing falling meteor.
        event.registerEntityRenderer(ModEntities.FALLING_METEOR.get(), FallingMeteorRenderer::new);
        // Each creature now has its own model geometry; the scale just fine-tunes size.
        event.registerEntityRenderer(ModEntities.XERTZ_STALKER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new XertzStalkerModel(context.bakeLayer(XertzStalkerModel.LAYER)),
                        entityTexture("xertz_stalker"), 1.0F, 1.0F, 1.0F, 0.5F, entityGlow("xertz_stalker")));
        event.registerEntityRenderer(ModEntities.QUARTZ_CRAWLER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new QuartzCrawlerModel(context.bakeLayer(QuartzCrawlerModel.LAYER)),
                        entityTexture("quartz_crawler"), 1.0F, 1.0F, 1.0F, 0.5F, entityGlow("quartz_crawler")));
        event.registerEntityRenderer(ModEntities.GREENLING.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new GreenlingModel(context.bakeLayer(GreenlingModel.LAYER)),
                        entityTexture("greenling"), 1.0F, 1.0F, 1.0F, 0.3F, entityGlow("greenling")));
        event.registerEntityRenderer(ModEntities.CINDER_STALKER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new CinderStalkerModel(context.bakeLayer(CinderStalkerModel.LAYER)),
                        entityTexture("cinder_stalker"), 1.0F, 1.0F, 1.0F, 0.6F, entityGlow("cinder_stalker")));
        event.registerEntityRenderer(ModEntities.FROST_STRIDER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new FrostStriderModel(context.bakeLayer(FrostStriderModel.LAYER)),
                        entityTexture("frost_strider"), 1.0F, 1.0F, 1.0F, 0.5F, entityGlow("frost_strider")));
        // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5).
        event.registerEntityRenderer(ModEntities.MEADOW_LOPER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new za.co.neroland.nerospace.client.MeadowLoperModel(
                                context.bakeLayer(za.co.neroland.nerospace.client.MeadowLoperModel.LAYER)),
                        entityTexture("meadow_loper"), 1.0F, 1.0F, 1.0F, 0.6F, entityGlow("meadow_loper")));
        event.registerEntityRenderer(ModEntities.EMBER_STRUTTER.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new za.co.neroland.nerospace.client.EmberStrutterModel(
                                context.bakeLayer(za.co.neroland.nerospace.client.EmberStrutterModel.LAYER)),
                        entityTexture("ember_strutter"), 1.0F, 1.0F, 1.0F, 0.3F, entityGlow("ember_strutter")));
        event.registerEntityRenderer(ModEntities.WOOLLY_DRIFT.get(),
                context -> new GreenxertzCreatureRenderer(context,
                        new za.co.neroland.nerospace.client.WoollyDriftModel(
                                context.bakeLayer(za.co.neroland.nerospace.client.WoollyDriftModel.LAYER)),
                        entityTexture("woolly_drift"), 1.0F, 1.0F, 1.0F, 0.5F, entityGlow("woolly_drift")));

        // Universal Pipe: streams + travelling items (the tube itself is the multipart block model).
        event.registerBlockEntityRenderer(
                za.co.neroland.nerospace.registry.ModBlockEntities.UNIVERSAL_PIPE.get(),
                context -> new UniversalPipeRenderer());

        // Solar panel: the sun-tracking, night-folding deck drawn above the housing.
        event.registerBlockEntityRenderer(
                za.co.neroland.nerospace.registry.ModBlockEntities.SOLAR_PANEL.get(),
                context -> new za.co.neroland.nerospace.client.SolarPanelRenderer());

        // Star Guide pedestal: the floating next-step hologram.
        event.registerBlockEntityRenderer(
                za.co.neroland.nerospace.registry.ModBlockEntities.STAR_GUIDE.get(),
                context -> new za.co.neroland.nerospace.client.StarGuideHologramRenderer());

        // Quarry controller: glowing gantry + moving drill head (MINER_DESIGN).
        event.registerBlockEntityRenderer(
                za.co.neroland.nerospace.registry.ModBlockEntities.QUARRY_CONTROLLER.get(),
                context -> new za.co.neroland.nerospace.client.QuarryControllerRenderer());
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(XertzStalkerModel.LAYER, XertzStalkerModel::createBodyLayer);
        event.registerLayerDefinition(QuartzCrawlerModel.LAYER, QuartzCrawlerModel::createBodyLayer);
        event.registerLayerDefinition(GreenlingModel.LAYER, GreenlingModel::createBodyLayer);
        event.registerLayerDefinition(CinderStalkerModel.LAYER, CinderStalkerModel::createBodyLayer);
        event.registerLayerDefinition(FrostStriderModel.LAYER, FrostStriderModel::createBodyLayer);
        event.registerLayerDefinition(za.co.neroland.nerospace.client.MeadowLoperModel.LAYER,
                za.co.neroland.nerospace.client.MeadowLoperModel::createBodyLayer);
        event.registerLayerDefinition(za.co.neroland.nerospace.client.EmberStrutterModel.LAYER,
                za.co.neroland.nerospace.client.EmberStrutterModel::createBodyLayer);
        event.registerLayerDefinition(za.co.neroland.nerospace.client.WoollyDriftModel.LAYER,
                za.co.neroland.nerospace.client.WoollyDriftModel::createBodyLayer);
        event.registerLayerDefinition(RocketModel.LAYER, RocketModel::createBodyLayer);
        event.registerLayerDefinition(FallingMeteorModel.LAYER, FallingMeteorModel::createBodyLayer);
        // Per-tier rocket geometry (ART_OVERHAUL_DESIGN.md §4.2).
        event.registerLayerDefinition(za.co.neroland.nerospace.client.RocketT2Model.LAYER,
                za.co.neroland.nerospace.client.RocketT2Model::createBodyLayer);
        event.registerLayerDefinition(za.co.neroland.nerospace.client.RocketT3Model.LAYER,
                za.co.neroland.nerospace.client.RocketT3Model::createBodyLayer);
        event.registerLayerDefinition(za.co.neroland.nerospace.client.RocketT4Model.LAYER,
                za.co.neroland.nerospace.client.RocketT4Model::createBodyLayer);
    }

    /** Tracks whether the local player was in breathable air last tick (for the boundary sound). */
    private static boolean wasBreathable;
    /** Counts client ticks so the (cheap) sound check runs every tick but particles run rarely. */
    private static int fxTick;
    /** Spawn ambient particles only every Nth tick — heavy iteration, kept sparse for performance. */
    private static final int PARTICLE_INTERVAL_TICKS = 8;

    /**
     * Per-tick oxygen visual FX (terraform design §1.7): drifting ambient particles (layer 1),
     * boundary-membrane shimmer + a soft crossfade sound when the breathable state flips (layer 3).
     * Each layer is independently config-gated; the haze tint (layer 2) is in {@link #onComputeFogColor}
     * and the HUD gauge (layer 4) rides the vanilla air-supply mirror driven server-side.
     */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Config.OXYGEN_VISUAL_QUALITY.get() == Config.OxygenVisualQuality.OFF) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        int threshold = Config.OXYGEN_BREATHABLE_THRESHOLD.get();
        int max = Config.OXYGEN_MAX_CONCENTRATION.get();
        BlockPos playerPos = mc.player.blockPosition();

        // Layer 3 (sound): crossfade an ambient note when the player crosses the breathable boundary.
        boolean breathingNow = ClientOxygenField.concentrationAt(playerPos.above()) >= threshold
                || ClientOxygenField.concentrationAt(playerPos) >= threshold;
        if (breathingNow != wasBreathable && Config.OXYGEN_BOUNDARY_INTENSITY.get() > 0.0D) {
            level.playLocalSound(playerPos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.AMBIENT,
                    0.25F, breathingNow ? 1.3F : 0.8F, false);
        }
        wasBreathable = breathingNow;

        // Particles are the expensive layer (field iteration + spawns), so run them only every Nth tick
        // and keep the budget tiny. A small sample near the player is plenty for the ambient cue.
        if (++fxTick % PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }
        double particleIntensity = Config.OXYGEN_PARTICLE_INTENSITY.get();
        double boundaryIntensity = Config.OXYGEN_BOUNDARY_INTENSITY.get();
        if (particleIntensity <= 0.0D && boundaryIntensity <= 0.0D) {
            return;
        }
        Long2ByteMap field = ClientOxygenField.view();
        if (field.isEmpty()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        boolean full = Config.OXYGEN_VISUAL_QUALITY.get() == Config.OxygenVisualQuality.FULL;
        int budget = full ? 2 : 1;
        int spawned = 0;
        long maxDistSq = 18L * 18L; // only spawn near the player

        for (Long2ByteMap.Entry e : field.long2ByteEntrySet()) {
            if (spawned >= budget) {
                break;
            }
            int conc = e.getByteValue() & 0xFF;
            if (conc < threshold) {
                continue;
            }
            BlockPos p = BlockPos.of(e.getLongKey());
            if (p.distSqr(playerPos) > maxDistSq) {
                continue;
            }
            // Layer 1: a single drifting ambient GLOW, rate proportional to concentration.
            if (particleIntensity > 0.0D && rnd.nextDouble() < particleIntensity * (conc / (double) max) * 0.08D) {
                level.addParticle(ParticleTypes.GLOW,
                        p.getX() + rnd.nextDouble(), p.getY() + rnd.nextDouble(), p.getZ() + rnd.nextDouble(),
                        0.0D, 0.004D, 0.0D);
                spawned++;
            }
        }
    }

    /**
     * Soft haze tint (terraform design §1.7, layer 2): nudge the fog colour toward cyan while the camera
     * is inside breathable air, scaled by local concentration and the haze-intensity slider.
     */
    @SubscribeEvent
    static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (Config.OXYGEN_VISUAL_QUALITY.get() != Config.OxygenVisualQuality.FULL) {
            return;
        }
        double haze = Config.OXYGEN_HAZE_INTENSITY.get();
        if (haze <= 0.0D) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
        int conc = Math.max(ClientOxygenField.concentrationAt(eye), ClientOxygenField.concentrationAt(eye.above()));
        int threshold = Config.OXYGEN_BREATHABLE_THRESHOLD.get();
        if (conc < threshold) {
            return;
        }
        float max = Config.OXYGEN_MAX_CONCENTRATION.get();
        float a = (float) Math.min(0.6D, haze * 0.18D * (conc / max));
        event.setRed(lerp(event.getRed(), 0.30F, a));
        event.setGreen(lerp(event.getGreen(), 0.80F, a));
        event.setBlue(lerp(event.getBlue(), 0.95F, a));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static final String[] COMPASS_8 = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    /**
     * Meteor Tracker readout (meteor-events design §6): while the player holds a tracker, show the
     * nearest meteor's state (incoming / landed), compass heading and distance in the action bar.
     * Server-authoritative — the data arrives via {@link ClientMeteorTracker}; this only presents it.
     */
    @SubscribeEvent
    static void onMeteorTrackerTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean holding = mc.player.getMainHandItem().is(ModItems.METEOR_TRACKER.get())
                || mc.player.getOffhandItem().is(ModItems.METEOR_TRACKER.get());
        if (!holding) {
            return;
        }
        if (!ClientMeteorTracker.isPresent()) {
            mc.gui.setOverlayMessage(
                    Component.translatable("item.nerospace.meteor_tracker.none"), false);
            return;
        }
        BlockPos target = ClientMeteorTracker.pos();
        Vec3 p = mc.player.position();
        double dx = target.getX() + 0.5D - p.x;
        double dz = target.getZ() + 0.5D - p.z;
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        // Bearing where North = -Z, East = +X (Minecraft convention).
        double deg = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0D) % 360.0D;
        String heading = COMPASS_8[(int) Math.round(deg / 45.0D) & 7];
        Component state = Component.translatable(ClientMeteorTracker.state() == MeteorSite.LANDED
                ? "item.nerospace.meteor_tracker.landed"
                : "item.nerospace.meteor_tracker.incoming");
        mc.gui.setOverlayMessage(
                Component.translatable("item.nerospace.meteor_tracker.readout", state, heading, dist), false);
    }

    private static Identifier entityTexture(String name) {
        return Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/" + name + ".png");
    }

    private static Identifier entityGlow(String name) {
        return Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/" + name + "_glow.png");
    }
}
