package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.RocketGeometry.Pass;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.route.CargoRocketEntity;

/**
 * Entity renderer for the rocket. Draws the tier's {@link RocketModel} in four passes — lit cutout
 * hull, lit translucent glass, full-bright emissive lamps/beacons and the additive exhaust plume —
 * at the tier's visual scale, so bigger tiers genuinely look bigger and each carries its own livery.
 * The window band is cut out of the hull texture and glazed from the inside, so the rider sees out
 * through real glass and onlookers see the lit cockpit.
 *
 * <p>Cross-loader port note: geometry is baked directly from {@link RocketGeometry} (the same
 * approach the Greenxertz mob renderers use), so no model-layer registry is required.</p>
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/rocket_t1.png"),
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/rocket_t2.png"),
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/rocket_t3.png"),
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/rocket_t4.png"),
    };
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final Pass[] PASSES = Pass.values();

    /** [tier][pass] — the four passes of each tier share one skeleton. */
    private final RocketModel[][] models;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        RocketTier[] tiers = RocketTier.values();
        this.models = new RocketModel[tiers.length][PASSES.length];
        for (RocketTier tier : tiers) {
            for (Pass pass : PASSES) {
                this.models[tier.ordinal()][pass.ordinal()] = RocketModel.bake(tier, pass);
            }
        }
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extractRenderState(RocketEntity rocket, RocketRenderState state, float partialTick) {
        super.extractRenderState(rocket, state, partialTick);
        state.ageInTicks = rocket.tickCount + partialTick;
        state.scale = rocket.visualScale();
        state.tier = Math.min(TEXTURES.length - 1, rocket.getTier().ordinal());
        state.texture = TEXTURES[state.tier];
        state.launching = rocket.isLaunching() || rocket.isDescending(); // The descent replays the ascent reversed
        state.launch = rocket.launchProgress(partialTick);
        state.ignition = rocket.ignition(partialTick);
        state.hatch = rocket.hatchOpen(partialTick);
        state.cargo = rocket instanceof CargoRocketEntity;
    }

    @Override
    protected float getShadowRadius(RocketRenderState state) {
        return 0.55F * state.scale;
    }

    /**
     * The hitbox is a 1×3 column but the hull is several times taller and the plume and separated
     * boosters reach well below it; cull on the visual envelope so the rocket never pops out of view
     * while any part of it is on screen.
     */
    //? if >=26.3 {
    /*@Override
    protected AABB getBoundingBoxForCulling(RocketEntity rocket, float partialTick) {
        return rocket.visualBounds();
    }
    *///?} else {
    @Override
    protected AABB getBoundingBoxForCulling(RocketEntity rocket) {
        return rocket.visualBounds();
    }
    //?}

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        poseStack.pushPose();
        // Standard entity-model orientation: flip Y/X into model space at the tier's scale. The model
        // ground line sits at model-y 24 = 1.5, so -1.5 plants the legs on the pad at any scale.
        float s = state.scale;
        poseStack.scale(-s, -s, s);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        RocketModel[] tierModels = this.models[Math.min(this.models.length - 1, state.tier)];
        for (Pass pass : PASSES) {
            RocketModel model = tierModels[pass.ordinal()];
            model.setupAnim(state);
            RenderType renderType = model.renderType(state.texture);
            boolean lit = pass == Pass.HULL || pass == Pass.GLASS;
            //? if >=1.21.9 {
            int light = lit ? state.lightCoords : FULL_BRIGHT;
            //?} else {
            /*int light = lit ? this.light() : FULL_BRIGHT;
            *///?}
            int order = pass == Pass.HULL ? 0 : 1;
            //? if >=26.3 {
            /*collector.order(order).submitModel(model, state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, -1);
            *///?} else {
            collector.order(order).submitModel(model, state, poseStack, renderType,
                    light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
            //?}
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
