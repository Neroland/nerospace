package za.co.neroland.nerospace.world.planet.mars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.planet.mars.entity.DustDevil;

/**
 * Draws the Mars dust devil's vortex core: two crossed, translucent vertical quads (1 block wide, 2.5 tall) that spin
 * about the entity and step through the four 16 x 64 frames of {@code textures/entity/dust_devil.png} (one every three
 * ticks). The quads grow in over the first second and fade over the last two before the 600-tick despawn. The entity's
 * own falling-dust and cloud particles still draw around it.
 */
public class DustDevilRenderer extends EntityRenderer<DustDevil, EntityRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/dust_devil.png");
    private static final float HEIGHT = 2.5F;
    private static final float HALF_WIDTH = 0.5F;
    /** Swirl speed: degrees per tick. */
    private static final float SPIN = 12.0F;

    public DustDevilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    /** Frame index 0..3 for an age in ticks. */
    static int frame(float ageInTicks) {
        return Math.floorMod((int) ageInTicks / 3, 4);
    }

    /** Width scale: grows in over the first 20 ticks. */
    static float grow(float ageInTicks) {
        return Math.max(0.0F, Math.min(1.0F, ageInTicks / 20.0F));
    }

    /** Alpha scale: fades over ticks 560..600. */
    static float fade(float ageInTicks) {
        return Math.max(0.0F, Math.min(1.0F, 1.0F - Math.max(0.0F, ageInTicks - 560.0F) / 40.0F));
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float alpha = fade(age);
        float half = HALF_WIDTH * grow(age);
        if (alpha > 0.0F && half > 0.0F) {
            int light = state.lightCoords;
            int a = Math.round(alpha * 255.0F);
            float u0 = frame(age) * 0.25F;
            float u1 = u0 + 0.25F;
            double spin = Math.toRadians(age * SPIN);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE),
                    (pose, consumer) -> {
                        for (int k = 0; k < 2; k++) {
                            double angle = spin + k * Math.PI / 2.0;
                            float dx = (float) Math.cos(angle) * half;
                            float dz = (float) Math.sin(angle) * half;
                            quad(consumer, pose, dx, dz, u0, u1, a, light);
                        }
                    });
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    /** One vertical quad from (-dx, -dz) to (dx, dz), written in both windings so it shows from either side. */
    private static void quad(VertexConsumer c, PoseStack.Pose pose, float dx, float dz, float u0, float u1, int a,
            int light) {
        vertex(c, pose, -dx, 0.0F, -dz, u0, 1.0F, a, light);
        vertex(c, pose, dx, 0.0F, dz, u1, 1.0F, a, light);
        vertex(c, pose, dx, HEIGHT, dz, u1, 0.0F, a, light);
        vertex(c, pose, -dx, HEIGHT, -dz, u0, 0.0F, a, light);
        vertex(c, pose, -dx, HEIGHT, -dz, u0, 0.0F, a, light);
        vertex(c, pose, dx, HEIGHT, dz, u1, 0.0F, a, light);
        vertex(c, pose, dx, 0.0F, dz, u1, 1.0F, a, light);
        vertex(c, pose, -dx, 0.0F, -dz, u0, 1.0F, a, light);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            int a, int light) {
        c.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
