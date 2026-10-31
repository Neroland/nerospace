package za.co.neroland.nerospace.fabric.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

import za.co.neroland.nerospace.client.sky.PlanetSkyRenderer;

/**
 * Fabric world fog: Fabric API has no fog-colour event on 26.x, so this pushes the planet sky profile's fog colour
 * (phase, position, weather, eclipse; see {@link PlanetSkyRenderer#fogRgb}) into the colour vanilla's
 * {@code FogRenderer.computeFogColor} has just written, like NeoForge and Forge do through
 * {@code ViewportEvent.ComputeFogColor}. Optional by design ({@code @Pseudo}, {@code require = 0}): if a Minecraft
 * line moves the method, the dimension's fog attribute simply stays the colour.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
public abstract class FogColorPlanetMixin {

    @Inject(method = "computeFogColor", at = @At("TAIL"), require = 0)
    private void nerospace$planetFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
            float darkenWorldAmount, Vector4f color, CallbackInfo ci) {
        int rgb = PlanetSkyRenderer.fogRgb(camera, partialTick);
        if (rgb >= 0) {
            color.set(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, color.w());
        }
    }
}
