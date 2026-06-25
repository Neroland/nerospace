package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Entity renderer for the rocket. Renders {@link RocketModel} via the 26.1 submit pipeline with a
 * PER-TIER scale and texture: bigger tiers genuinely look bigger, and each tier carries its accent
 * livery. The window band is punched out of the texture (alpha cutout), so the standing rider can see
 * out of the hull.
 *
 * <p>Cross-loader port note: each tier's geometry is baked directly from its {@code createBodyLayer()}
 * (the same approach the Greenxertz mob renderers use), so no model-layer registry is required.</p>
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {

    private static final Identifier [] TEXTURES = {
            NerospaceCommon.id("textures/entity/rocket_t1.png"),
            NerospaceCommon.id("textures/entity/rocket_t2.png"),
            NerospaceCommon.id("textures/entity/rocket_t3.png"),
            NerospaceCommon.id("textures/entity/rocket_t4.png"),
    };
    private static final int FULL_BRIGHT = 0x00F000F0;

    /** Per-tier geometry: T1 classic, T2 boosters, T3 ring, T4 heavy. */
    private final RocketModel [] models;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.models = new RocketModel[] {
                new RocketModel(RocketModel.createBodyLayer().bakeRoot()),
                new RocketModel(RocketT2Model.createBodyLayer().bakeRoot()),
                new RocketModel(RocketT3Model.createBodyLayer().bakeRoot()),
                new RocketModel(RocketT4Model.createBodyLayer().bakeRoot()),
        };
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extractRenderState(RocketEntity rocket, RocketRenderState state, float partialTick) {
        super.extractRenderState(rocket, state, partialTick);
        state.scale = rocket.visualScale();
        state.tier = Math.min(TEXTURES.length - 1, rocket.getTier().ordinal());
        state.texture = NerospaceCommon.requireNonNull(TEXTURES[state.tier]);
    }

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        // Standard entity-model orientation: flip Y/X into model space at the tier's scale. The model
        // bottom (fins) sits at model-y 24 = 1.5, so -1.5 plants the fins on the pad at any scale.
        float s = state.scale;
        poseStack.scale(-s, -s, s);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        RocketModel model = NerospaceCommon.requireNonNull(this.models[Math.min(this.models.length - 1, state.tier)]);
        model.setupAnim(state);
        RenderType renderType = NerospaceCommon.requireNonNull(model.renderType(state.texture));
        collector.order(0).submitModel(model, state, poseStack, renderType,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
