package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.meteor.FallingMeteorEntity;

/**
 * Entity renderer for the falling meteor (meteor-events design §4). Draws {@link FallingMeteorModel}
 * via the 26.1 submit pipeline, tumbling it on its age, at full brightness so the molten rock glows
 * against the sky. The flame/smoke trail is spawned by the entity itself.
 *
 * <p>Cross-loader port note: the model geometry is baked directly from {@code createBodyLayer()}
 * (the same approach the rocket + mob renderers use), so no model-layer registry is required.</p>
 */
public class FallingMeteorRenderer extends EntityRenderer<FallingMeteorEntity, FallingMeteorRenderState> {

    private static final @org.jspecify.annotations.NonNull Identifier TEXTURE =
            NerospaceCommon.id("textures/entity/falling_meteor.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final @org.jspecify.annotations.NonNull FallingMeteorModel model;

    public FallingMeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FallingMeteorModel(FallingMeteorModel.createBodyLayer().bakeRoot());
    }

    @Override
    public FallingMeteorRenderState createRenderState() {
        return new FallingMeteorRenderState();
    }

    @Override
    public void extractRenderState(FallingMeteorEntity meteor, FallingMeteorRenderState state, float partialTick) {
        super.extractRenderState(meteor, state, partialTick);
        state.ticks = meteor.tickCount + partialTick;
    }

    @Override
    public void submit(FallingMeteorRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        // Standard entity-model orientation (flip into model space), then tumble on the age so the
        // rock spins as it falls.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -0.7F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks * 7.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.ticks * 5.0F));

        RenderType renderType = this.model.renderType(TEXTURE);
        collector.order(0).submitModel(this.model, state, poseStack, renderType,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
