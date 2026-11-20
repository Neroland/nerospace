package za.co.neroland.nerospace.mixin.shader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import za.co.neroland.nerospace.client.shader.ShaderCompat;

/**
 * Optional shader-mod compatibility: lets an airless Nerospace world ask a shader pack for its End programs
 * instead of the overworld ones a pack gives any dimension it does not know (blue sky, the pack's clouds).
 *
 * <p>The shader mod has no public API for this, so this is the one hook into its internals: the method that
 * resolves the current dimension for the pack. It has had the same name and shape on every supported version.
 * Fail-soft by construction: the whole config is skipped when the shader mod is absent
 * ({@link ShaderCompatMixinPlugin}), the target is {@code @Pseudo}, and the injection is {@code require = 0},
 * so a version that moves the method starts normally with the compatibility inactive.</p>
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisDimensionMixin {

    @Inject(method = "getCurrentDimension", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void nerospace$shaderDimension(CallbackInfoReturnable<Object> cir) {
        Object mapped = ShaderCompat.remap(cir.getReturnValue());
        if (mapped != null) {
            cir.setReturnValue(mapped);
        }
    }
}
