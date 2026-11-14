package za.co.neroland.nerospace.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
//? if >=1.21.8 {
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//?}
//? if >=26.1 {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else if >=1.21.8 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

//? if >=1.21.8 {
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

import za.co.neroland.nerospace.client.sky.PlanetSkyRenderer;
//?}

/**
 * Applies the planet sky profile's fog colour after vanilla computes it. The fog renderer
 * returns the colour on 1.21.x and writes it to an output vector on 26.x.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
public abstract class FogColorPlanetMixin {

    //? if >=26.1 {
    @Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At("TAIL"), require = 0)
    private void nerospace$planetFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
            float darkenWorldAmount, Vector4f color, CallbackInfo ci) {
        nerospace$applyPlanetFog(camera, partialTick, color);
    }
    //?} else if >=1.21.11 {
    /*@Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)Lorg/joml/Vector4f;",
            at = @At("RETURN"), require = 0)
    private void nerospace$planetFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
            float darkenWorldAmount, CallbackInfoReturnable<Vector4f> ci) {
        nerospace$applyPlanetFog(camera, partialTick, ci.getReturnValue());
    }
    *///?} else if >=1.21.8 {
    /*@Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFZ)Lorg/joml/Vector4f;",
            at = @At("RETURN"), require = 0)
    private void nerospace$planetFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
            float darkenWorldAmount, boolean thickFog, CallbackInfoReturnable<Vector4f> ci) {
        nerospace$applyPlanetFog(camera, partialTick, ci.getReturnValue());
    }
    *///?}

    //? if >=1.21.8 {
    @Unique
    private static void nerospace$applyPlanetFog(Camera camera, float partialTick, Vector4f color) {
        int rgb = PlanetSkyRenderer.fogRgb(camera, partialTick);
        if (rgb >= 0) {
            color.set(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, color.w());
        }
    }
    //?}
}
