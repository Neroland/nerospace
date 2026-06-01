package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Shared renderer for the Phase 5 Greenxertz creatures. One renderer class serves all four mob types
 * via the common {@link GreenxertzCreatureModel} mesh, but each is given a distinct <b>silhouette</b>
 * through a per-creature non-uniform scale + shadow (Phase 8d): a tall stalker, a low wide crawler, a
 * small greenling, a bulky cinder stalker. (Bespoke per-creature geometry/animation is a deeper art
 * pass; this gives them readable, different shapes now.)
 */
public class GreenxertzCreatureRenderer extends MobRenderer<Mob, LivingEntityRenderState, GreenxertzCreatureModel> {

    private final Identifier texture;
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;

    public GreenxertzCreatureRenderer(EntityRendererProvider.Context context, Identifier texture,
                                      float scaleX, float scaleY, float scaleZ, float shadow) {
        super(context, new GreenxertzCreatureModel(context.bakeLayer(GreenxertzCreatureModel.LAYER)), shadow);
        this.texture = texture;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return this.texture;
    }
}
