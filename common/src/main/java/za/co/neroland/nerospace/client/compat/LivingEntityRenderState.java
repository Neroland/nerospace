package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line): the living-entity render state look-alike (see EntityRenderState).

//? if <1.21.2 {
/*import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class LivingEntityRenderState extends EntityRenderState {

    public float bodyRot;
    public float yRot;
    public float xRot;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float scale = 1.0F;
    public boolean isBaby;
    public float deathTime;

    // The vanilla 1.21.2+ LivingEntityRenderer.extractRenderState(..) fields the models read.
    public static void extractLiving(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        extractBase(entity, state, partialTick);
        state.bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        state.yRot = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - state.bodyRot);
        state.xRot = entity.getViewXRot(partialTick);
        state.isBaby = entity.isBaby();
        state.walkAnimationPos = entity.walkAnimation.position(partialTick);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);
        state.scale = entity.getScale();
        state.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTick : 0.0F;
    }
}
*///?}
