package za.co.neroland.nerospace.neoforge;

// NeoForge 26.3 planet sky hook: the CustomSkyboxRenderer registered as nerospace:planet. Every dimension type
// selects it through the neoforge:custom_skybox attribute in neoforge/src/main/resources-26.3
// (tools/gen_sky_overlays.py). A thin adapter: the drawing is the shared client.sky.PlanetSkyDraw, which returns
// false (vanilla sky) for a level without a planet profile. NeoForge 26.1 and 26.2 use the shared sky mixin
// (mixin.sky.SkyRendererPlanetMixin) like Fabric and Forge, so this class only exists on 26.3 and later.
//? if >=26.3 {
/*import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;

import org.joml.Matrix4fc;

import za.co.neroland.nerospace.client.sky.PlanetSkyDraw;

public final class NeoForgePlanetSky implements net.neoforged.neoforge.client.CustomSkyboxRenderer {

    @Override
    public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState, Matrix4fc modelViewMatrix,
            GpuBufferSlice skyFog) {
        CameraRenderState camera = levelRenderState.cameraRenderState;
        return PlanetSkyDraw.INSTANCE.render(levelRenderState.worldPartialTicks, levelRenderState.gameTime,
                camera.pos, camera.projectionMatrix, modelViewMatrix, skyFog);
    }
}
*///?}
