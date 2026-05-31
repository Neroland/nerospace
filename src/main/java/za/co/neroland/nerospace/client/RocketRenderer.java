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
        // Standard entity-model orientation: flip Y/X into model space, then drop to stand on the ground.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);
        RenderType renderType = this.model.renderType(TEXTURE);
        collector.order(0).submitModel(this.model, state, poseStack, renderType,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
