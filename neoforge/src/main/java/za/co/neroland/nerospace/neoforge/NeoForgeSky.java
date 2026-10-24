package za.co.neroland.nerospace.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.sky.SkyProfiles;

/**
 * NeoForge client sky wiring: the sky-profile reload listener (all versions) and, on 26.3, a
 * {@code CustomSkyboxRenderer} registered as {@code nerospace:planet} that delegates to
 * {@code PlanetSkyRenderer}. The renderer is only used by a dimension type whose {@code neoforge:custom_skybox}
 * environment attribute names it; Stage 1 changes no dimension type, so it is registered but inert, and even
 * when attached it returns false (vanilla sky) until the Stage-6 profiles ship.
 */
public final class NeoForgeSky {

    /** The custom skybox id a dimension type attribute refers to. */
    public static final Identifier SKY_ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet");

    private NeoForgeSky() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener((AddClientReloadListenersEvent event) ->
                event.addListener(SkyProfiles.ID, SkyProfiles.INSTANCE));
        //? if >=26.3 {
        /*modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent event) ->
                event.registerSkyboxRenderer(SKY_ID, new net.neoforged.neoforge.client.CustomSkyboxRenderer() {
                    @Override
                    public boolean renderSky(net.minecraft.client.renderer.state.level.LevelRenderState levelRenderState,
                            net.minecraft.client.renderer.state.level.SkyRenderState skyRenderState,
                            org.joml.Matrix4fc modelViewMatrix,
                            com.mojang.renderpearl.api.buffers.GpuBufferSlice skyFog) {
                        return za.co.neroland.nerospace.client.sky.PlanetSkyRenderer.renderCurrent(
                                net.minecraft.client.Minecraft.getInstance().level);
                    }
                }));
        *///?}
    }
}
