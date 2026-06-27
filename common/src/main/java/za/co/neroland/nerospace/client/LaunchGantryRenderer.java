package za.co.neroland.nerospace.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.LaunchGantryBlockEntity;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Draws the launch gantry as a tall service tower (a 1×3 lattice with a swing arm) above its block, and
 * animates it around launches: when a rocket on the adjacent pad lights its engines the tower reclines
 * away to release it, then swings back upright once the rocket has climbed past the tower top. Purely
 * cosmetic — the gantry's gameplay role (forming the Heavy Launch Complex, opening the console) is
 * unchanged.
 *
 * <p>Cross-loader port: reuses the same vanilla submission/custom-geometry vertex path proven on both
 * 26.1.2 and 26.2 by {@link SolarPanelRenderer}; registered via the {@link ClientBlockEntityRenderers}
 * seam. The recline is eased on the client only (no networking) from the rocket's synced launch state.</p>
 */
public class LaunchGantryRenderer
        implements BlockEntityRenderer<LaunchGantryBlockEntity, LaunchGantryRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/launch_gantry.png");
    /** Custom geometry's light coords aren't reliably populated; draw self-lit like the other BERs. */
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final float TOWER_H = 3.0F;     // three blocks tall
    private static final float MAX_LEAN = 32.0F;   // recline angle at full swing
    private static final float EASE = 0.15F;        // per-frame approach toward the target swing
    private static final float PHW = 1.5F / 16.0F;  // post half-width
    private static final float CI = 2.5F / 16.0F;   // corner inset
    private static final float BHW = 0.8F / 16.0F;  // brace half-thickness

    @Override
    public LaunchGantryRenderState createRenderState() {
        return new LaunchGantryRenderState();
    }

    @Override
    public void extractRenderState(LaunchGantryBlockEntity gantry, LaunchGantryRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(gantry, state, partialTick, cameraPos, breakProgress);
        Level level = gantry.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = gantry.getBlockPos();
        double bx = pos.getX() + 0.5;
        double bz = pos.getZ() + 0.5;

        // Find the nearest rocket around the pad column this gantry serves.
        RocketEntity rocket = null;
        double best = Double.MAX_VALUE;
        AABB box = new AABB(pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3,
                pos.getX() + 4, pos.getY() + 14, pos.getZ() + 4);
        List<RocketEntity> rockets = level.getEntitiesOfClass(RocketEntity.class, box);
        for (RocketEntity r : rockets) {
            double d = (r.getX() - bx) * (r.getX() - bx) + (r.getZ() - bz) * (r.getZ() - bz);
            if (d < best && d <= 12.0) {
                best = d;
                rocket = r;
            }
        }

        int adx = gantry.getArmDx();
        int adz = gantry.getArmDz();
        float target = 0.0F;
        boolean attached = false;
        if (rocket != null) {
            double dxr = rocket.getX() - bx;
            double dzr = rocket.getZ() - bz;
            if (Math.abs(dxr) >= Math.abs(dzr)) {
                adx = dxr >= 0 ? 1 : -1;
                adz = 0;
            } else {
                adz = dzr >= 0 ? 1 : -1;
                adx = 0;
            }
            gantry.setArm(adx, adz);
            boolean cleared = rocket.getY() > pos.getY() + TOWER_H + 0.5;
            target = (rocket.isLaunching() && !cleared) ? 1.0F : 0.0F;
            attached = !rocket.isLaunching();
        }

        float swing = gantry.getSwing() + (target - gantry.getSwing()) * EASE;
        swing = Mth.clamp(swing, 0.0F, 1.0F);
        gantry.setSwing(swing);

        state.armDx = adx;
        state.armDz = adz;
        state.attached = attached;
        // Recline AWAY from the pad: about Z when the pad is east/west, about X when north/south.
        if (adx != 0) {
            state.axisX = false;
            state.lean = swing * MAX_LEAN * (adx > 0 ? 1.0F : -1.0F);
        } else {
            state.axisX = true;
            state.lean = swing * MAX_LEAN * (adz > 0 ? -1.0F : 1.0F);
        }
    }

    @Override
    public void submit(LaunchGantryRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        RenderType rt = RenderTypes.entityCutout(TEXTURE);
        poseStack.pushPose();
        // Recline about the tower base centre.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose((state.axisX ? Axis.XP : Axis.ZP).rotationDegrees(state.lean));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        collector.order(1).submitCustomGeometry(poseStack, rt,
                (pose, consumer) -> drawTower(consumer, pose, state));
        poseStack.popPose();
    }

    private void drawTower(VertexConsumer c, PoseStack.Pose pose, LaunchGantryRenderState state) {
        int light = FULL_BRIGHT;
        // Four corner posts, full height.
        post(c, pose, light, CI, CI);
        post(c, pose, light, 1.0F - CI, CI);
        post(c, pose, light, CI, 1.0F - CI);
        post(c, pose, light, 1.0F - CI, 1.0F - CI);
        // Brace rings binding the posts at three heights.
        ring(c, pose, light, 0.55F);
        ring(c, pose, light, 1.55F);
        ring(c, pose, light, 2.55F);
        // A finial cap.
        box(c, pose, light, 0.5F - PHW, TOWER_H, 0.5F - PHW, 0.5F + PHW, TOWER_H + 0.12F, 0.5F + PHW);
        // The service arm reaching toward the pad near the top, with a clamp at its tip.
        drawArm(c, pose, light, state);
    }

    /** A vertical post centred on (cx, cz), full tower height. */
    private void post(VertexConsumer c, PoseStack.Pose pose, int light, float cx, float cz) {
        box(c, pose, light, cx - PHW, 0.0F, cz - PHW, cx + PHW, TOWER_H, cz + PHW);
    }

    /** A square brace ring (four bars) at height {@code h} linking the four posts. */
    private void ring(VertexConsumer c, PoseStack.Pose pose, int light, float h) {
        float a = CI;
        float b = 1.0F - CI;
        // Bars along X at z=a and z=b.
        box(c, pose, light, a - PHW, h - BHW, a - BHW, b + PHW, h + BHW, a + BHW);
        box(c, pose, light, a - PHW, h - BHW, b - BHW, b + PHW, h + BHW, b + BHW);
        // Bars along Z at x=a and x=b.
        box(c, pose, light, a - BHW, h - BHW, a - PHW, a + BHW, h + BHW, b + PHW);
        box(c, pose, light, b - BHW, h - BHW, a - PHW, b + BHW, h + BHW, b + PHW);
    }

    /** The swing arm + clamp, extending from the tower's pad-side face toward the rocket. */
    private void drawArm(VertexConsumer c, PoseStack.Pose pose, int light, LaunchGantryRenderState state) {
        float y0 = 2.35F;
        float y1 = 2.55F;
        float reach = 0.5F;
        if (state.armDx != 0) {
            float inner = state.armDx > 0 ? 1.0F - CI : CI;
            float outer = inner + state.armDx * reach;
            float x0 = Math.min(inner, outer);
            float x1 = Math.max(inner, outer);
            box(c, pose, light, x0, y0, 0.5F - BHW, x1, y1, 0.5F + BHW);
            float tip = state.armDx > 0 ? x1 : x0;
            box(c, pose, light, tip - 1.5F / 16.0F, y0 - 1.5F / 16.0F, 0.5F - 2.5F / 16.0F,
                    tip + 1.5F / 16.0F, y1 + 1.5F / 16.0F, 0.5F + 2.5F / 16.0F); // clamp
        } else {
            float inner = state.armDz > 0 ? 1.0F - CI : CI;
            float outer = inner + state.armDz * reach;
            float z0 = Math.min(inner, outer);
            float z1 = Math.max(inner, outer);
            box(c, pose, light, 0.5F - BHW, y0, z0, 0.5F + BHW, y1, z1);
            float tip = state.armDz > 0 ? z1 : z0;
            box(c, pose, light, 0.5F - 2.5F / 16.0F, y0 - 1.5F / 16.0F, tip - 1.5F / 16.0F,
                    0.5F + 2.5F / 16.0F, y1 + 1.5F / 16.0F, tip + 1.5F / 16.0F); // clamp
        }
    }

    // --- Geometry helpers (verbatim pattern from SolarPanelRenderer; maps the sprite 0..1 per box) ---

    private static void box(VertexConsumer c, PoseStack.Pose pose, int light,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 1.0F;
        face(c, pose, light, 0, 1, 0, x0, y1, z0, u0, v0, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0);
        face(c, pose, light, 0, -1, 0, x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1);
        face(c, pose, light, 0, 0, -1, x0, y0, z0, u0, v0, x0, y1, z0, u0, v0, x1, y1, z0, u1, v0, x1, y0, z0, u1, v0);
        face(c, pose, light, 0, 0, 1, x1, y0, z1, u1, v1, x1, y1, z1, u1, v1, x0, y1, z1, u0, v1, x0, y0, z1, u0, v1);
        face(c, pose, light, -1, 0, 0, x0, y0, z1, u0, v1, x0, y1, z1, u0, v1, x0, y1, z0, u0, v0, x0, y0, z0, u0, v0);
        face(c, pose, light, 1, 0, 0, x1, y0, z0, u1, v0, x1, y1, z0, u1, v0, x1, y1, z1, u1, v1, x1, y0, z1, u1, v1);
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, int light, float nx, float ny, float nz,
            float ax, float ay, float az, float au, float av,
            float bx, float by, float bz, float bu, float bv,
            float cx, float cy, float cz, float cu, float cv,
            float dx, float dy, float dz, float du, float dv) {
        vertex(c, pose, ax, ay, az, au, av, light, nx, ny, nz);
        vertex(c, pose, bx, by, bz, bu, bv, light, nx, ny, nz);
        vertex(c, pose, cx, cy, cz, cu, cv, light, nx, ny, nz);
        vertex(c, pose, dx, dy, dz, du, dv, light, nx, ny, nz);
        vertex(c, pose, dx, dy, dz, du, dv, light, -nx, -ny, -nz);
        vertex(c, pose, cx, cy, cz, cu, cv, light, -nx, -ny, -nz);
        vertex(c, pose, bx, by, bz, bu, bv, light, -nx, -ny, -nz);
        vertex(c, pose, ax, ay, az, au, av, light, -nx, -ny, -nz);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int light, float nx, float ny, float nz) {
        c.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
