package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;


import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Shared renderer for the Greenxertz/Cindara creatures. As of Phase 10 each creature has its OWN
 * model geometry (a distinct {@link EntityModel} passed in — tall stalker, low six-legged crawler,
 * small greenling, horned cinder brute); this renderer just carries the per-creature texture plus a
 * fine-tuning scale + shadow. (Walk/idle animation is the next slice.)
 */
public class GreenxertzCreatureRenderer extends MobRenderer<Mob, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {

    private final Identifier texture;
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;

    public GreenxertzCreatureRenderer(EntityRendererProvider.Context context,
                                      EntityModel<LivingEntityRenderState> model, Identifier texture,
                                      float scaleX, float scaleY, float scaleZ, float shadow) {
        this(context, model, texture, scaleX, scaleY, scaleZ, shadow, null);
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public GreenxertzCreatureRenderer(EntityRendererProvider.Context context,
                                      EntityModel<LivingEntityRenderState> model, Identifier texture,
                                      float scaleX, float scaleY, float scaleZ, float shadow,
                                      Identifier glowTexture) {
        super(context, model, shadow);
        this.texture = NerospaceCommon.requireNonNull(texture);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        if (glowTexture != null) {
            this.addLayer(new GlowEyesLayer<LivingEntityRenderState>(this, glowTexture));
        }
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
