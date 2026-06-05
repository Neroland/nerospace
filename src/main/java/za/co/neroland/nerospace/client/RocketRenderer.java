package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Entity renderer for the rocket (cockpit rework). Renders {@link RocketModel} via the 26.1
 * submit pipeline with a PER-TIER scale and texture: bigger tiers genuinely look bigger, and each
 * tier carries its accent livery. The window band is punched out of the texture (alpha cutout),
 * so the standing rider can see out of the hull.
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket_t1.png"),
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket_t2.png"),
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket_t3.png"),
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket_t4.png"),
    };
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final RocketModel model;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RocketModel(context.bakeLayer(RocketModel.LAYER));
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extractRenderState(RocketEntity rocket, RocketRenderState state, float partialTick) {
        super.extractRenderState(rocket, state, partialTick);
        state.scale = rocket.visualScale();
        state.texture = TEXTURES[Math.min(TEXTURES.length - 1, rocket.getTier().ordinal())];
    }

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        // Standard entity-model orientation: flip Y/X into model space at the tier's scale. The
        // translate is in MODEL units (applied after the scale): the model bottom (fins) sits at
        // model-y 24 = 1.5, so -1.5 plants the fins on the pad at any scale.
        float s = state.scale;
        poseStack.scale(-s, -s, s);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);
        // Replicate LivingEntityRenderer EXACTLY (the path the textured Greenxertz mobs use):
        // renderType = model.renderType(texture) (the default EntityModel function = entityCutout,
        // which binds the texture AND discards the window band's transparent pixels — the cockpit
        // cutout), submitted via the full 10-arg overload.
        RenderType renderType = this.model.renderType(state.texture);
        collector.order(0).submitModel(this.model, state, poseStack, renderType,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0, null);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
