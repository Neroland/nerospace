package za.co.neroland.nerospace.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import za.co.neroland.nerospace.block.light.NyxIndexHolder;
import za.co.neroland.nerospace.block.light.NyxLightIndex;
import za.co.neroland.nerospace.block.rules.LightAbsorbRules;

/**
 * Nyx Stone light absorption, part 2 of 2 (decision 002 §11.1): when block light spreads into a cell within 1 block of
 * a Nyx Stone it loses {@link LightAbsorbRules#ABSORB} extra levels. In {@code propagateIncrease} the engine has just
 * pointed {@code mutablePos} at the destination cell when it asks for that cell's opacity, so the redirect reads the
 * destination from there. Cost: one field read when no Nyx Stone is indexed, else one hash lookup + one array read
 * per propagation step. Decreases need no hook (they zero what the increase wrote and re-pull from sources).
 */
@SuppressWarnings("cast") // the (Object) casts are required by javac for mixin self-checks
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineNyxMixin {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutablePos;

    @Redirect(method = "propagateIncrease(JJI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/BlockLightEngine;getOpacity(Lnet/minecraft/world/level/block/state/BlockState;)I"),
            require = 0)
    private int nerospace$absorb(BlockLightEngine engine, BlockState state) {
        int opacity = Math.max(1, state.getLightDampening());
        NyxLightIndex index = ((NyxIndexHolder) (Object) this).nerospace$nyxIndex();
        if (index.isEmpty()) {
            return opacity;
        }
        BlockPos to = this.mutablePos;
        if (!index.near(to.getX(), to.getY(), to.getZ())) {
            return opacity;
        }
        // absorbedLevel(incoming) = incoming − 2, expressed as 2 extra opacity on top of the vanilla step.
        return opacity + LightAbsorbRules.ABSORB;
    }
}
