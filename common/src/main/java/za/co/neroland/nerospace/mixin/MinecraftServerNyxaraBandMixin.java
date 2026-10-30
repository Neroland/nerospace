package za.co.neroland.nerospace.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand;

/**
 * Binds Nyxara's stored band width and seed as the server prepares its levels: every level exists by then, but no
 * chunk (spawn or force-loaded) has been generated yet, so no Nyxara chunk is ever built on the datapack fallback
 * width. {@code require = 0}: if the target ever moves, the first server tick still binds it (the previous path).
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerNyxaraBandMixin {

    @Inject(method = "prepareLevels", at = @At("HEAD"), require = 0)
    private void nerospace$bindNyxaraBand(CallbackInfo ci) {
        NyxaraBand.bind((MinecraftServer) (Object) this);
    }
}
