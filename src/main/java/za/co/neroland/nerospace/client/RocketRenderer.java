package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Entity renderer for the rocket (Phase 4 follow-up). Renders a {@link RocketModel} via the 26.1
 * submit pipeline so the deployed rocket is actually visible. The transform mirrors the standard
 * entity-model orientation ({@code scale(-1,-1,1)}); the vertical offset and proportions are a first
 * pass to be fine-tuned live in {@code runClient}.
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, EntityRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final RocketModel model;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RocketModel(context.bakeLayer(RocketModel.LAYER));
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        // Standard entity-model orientation: flip Y/X into model space, scaled up so the rocket is
        // big enough to seat the rider, then dropped to stand on the ground. (Scale/offset are a
        // first pass — fine-tune in runClient.)
        // Model bottom (fins) is at model-y 24 = 1.5 blocks. After scale(-1.6) the offset that plants
        // the fins on the ground is -1.5 (gives a ~4.8-block-tall rocket; nose ends near the 5-block
        // entity height). The previous -2.4 floated the rocket ~1.4 blocks above the pad.
        poseStack.scale(-1.6F, -1.6F, 1.6F);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);
        // Replicate LivingEntityRenderer EXACTLY (the path the textured Greenxertz mobs use):
        // renderType = model.renderType(texture) (the default EntityModel function = RenderTypes.entityCutout,
        // which binds the texture), submitted via the full 10-arg overload (sprite=null, outlineColor=0,
        // crumbling=null). The Identifier and entitySolid overloads both rendered the rocket solid white.
        RenderType renderType = this.model.renderType(TEXTURE);
        collector.order(0).submitModel(this.model, state, poseStack, renderType,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
