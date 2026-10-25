package za.co.neroland.nerospace.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.BlockLightEngine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import za.co.neroland.nerospace.block.light.NyxIndexHolder;
import za.co.neroland.nerospace.block.light.NyxLightIndex;
import za.co.neroland.nerospace.block.rules.LightAbsorbRules;

/**
 * Nyx Stone light absorption, part 2 of 2: when block light spreads into a cell within 1 block of a Nyx Stone it loses
 * {@link LightAbsorbRules#ABSORB} extra levels. In {@code propagateIncrease} vanilla computes
 * {@code newToLevel = fromLevel - getOpacity(toState)} with {@code mutablePos} pointing at the destination cell; this
 * adjusts the stored {@code newToLevel} <b>after</b> the original computation (so any other mod's opacity change or
 * redirect still applies and several such modifiers compose). Cost: one field read when no Nyx Stone is indexed, else
 * one hash lookup + one array read per step. Decreases need no hook. {@code require = 0}: if the local cannot be found
 * the block falls back to a plain light blocker. Identical on 26.1.2 / 26.2 / 26.3.
 */
@SuppressWarnings("cast") // the (Object) cast is required to reach the mixed-in interface
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineNyxMixin {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutablePos;

    @ModifyVariable(method = "propagateIncrease(JJI)V", at = @At("STORE"), name = "newToLevel", require = 0)
    private int nerospace$absorb(int newToLevel) {
        NyxLightIndex index = ((NyxIndexHolder) (Object) this).nerospace$nyxIndex();
        if (index.isEmpty()) {
            return newToLevel;
        }
        BlockPos to = this.mutablePos;
        if (!index.near(to.getX(), to.getY(), to.getZ())) {
            return newToLevel;
        }
        return LightAbsorbRules.absorbedLevel(newToLevel, true);
    }
}
