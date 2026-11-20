package za.co.neroland.nerospace.mixin.sky;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
//? if >=26.1 {
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import za.co.neroland.nerospace.client.sky.PlanetSkyDraw;
//?}
//? if >=26.3 {
/*import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import net.minecraft.client.renderer.state.level.SkyRenderState;
*///?} else if >=26.1 {
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.level.MoonPhase;
//?}

/**
 * Draws the planet sky in place of the vanilla sky on Minecraft 26.1 and later, through the shared
 * {@link PlanetSkyDraw}. Listed in {@code nerospace-sky.mixins.json} (client only, from 26.1.2), which Fabric,
 * Forge and NeoForge all declare. The vanilla sky renders untouched whenever the shared path returns false: a
 * level without a planet sky profile, the planet sky turned off for shader packs, or after a render error.
 *
 * <p>26.3: the sky pass is the single {@code render(GpuBufferSlice, SkyRenderState)} call, cancelled when the
 * planet sky drew. 26.1 and 26.2: the pass calls {@code renderSkyDisc} or {@code renderEndSky} first, then the
 * sunrise, sun/moon/stars, dark disc and End flash methods; the first call draws the planet sky and records
 * whether it did, and the later calls of that frame are cancelled while it did. The fog is already bound there.</p>
 *
 * <p>Stays out when a loader registered its own sky hook ({@link PlanetSkyDraw#mixinDraws()}), so NeoForge 26.3
 * (a {@code CustomSkyboxRenderer}) never draws the sky twice. Every injection is {@code require = 0}: a version
 * that moves a method keeps the vanilla sky. Below 26.1 this class is an empty shell and is not listed.</p>
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.SkyRenderer")
public abstract class SkyRendererPlanetMixin {

    //? if >=26.3 {
    /*@Inject(method = "render(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$planetSky(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
        if (PlanetSkyDraw.mixinDraws() && PlanetSkyDraw.renderCurrent(skyFog)) {
            ci.cancel();
        }
    }
    *///?} else if >=26.1 {
    // Whether the planet sky replaced this frame's sky; written by the first call of every sky pass.
    @Unique
    private static boolean nerospace$handled;

    @Inject(method = "renderSkyDisc(I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$planetSky(int skyColor, CallbackInfo ci) {
        nerospace$begin(ci);
    }

    @Inject(method = "renderEndSky()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$planetEndSky(CallbackInfo ci) {
        nerospace$begin(ci);
    }

    @Inject(method = "renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void nerospace$skipSunrise(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor,
            CallbackInfo ci) {
        nerospace$skip(ci);
    }

    @Inject(method = "renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$skipSunMoonAndStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
            MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        nerospace$skip(ci);
    }

    @Inject(method = "renderDarkDisc()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$skipDarkDisc(CallbackInfo ci) {
        nerospace$skip(ci);
    }

    @Inject(method = "renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("HEAD"), cancellable = true,
            require = 0)
    private void nerospace$skipEndFlash(PoseStack poseStack, float intensity, float xAngle, float yAngle,
            CallbackInfo ci) {
        nerospace$skip(ci);
    }

    @Unique
    private static void nerospace$begin(CallbackInfo ci) {
        nerospace$handled = PlanetSkyDraw.mixinDraws() && PlanetSkyDraw.renderCurrent(null);
        if (nerospace$handled) {
            ci.cancel();
        }
    }

    @Unique
    private static void nerospace$skip(CallbackInfo ci) {
        if (nerospace$handled) {
            ci.cancel();
        }
    }
    //?}
}
