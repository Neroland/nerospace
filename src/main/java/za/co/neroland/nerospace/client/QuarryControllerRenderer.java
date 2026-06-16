package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryRegion;

/**
 * Draws the quarry's working machinery on top of the (solid, block-based) frame ring — as real
 * textured geometry via {@link RenderTypes#entityCutout(Identifier)} with the frame's own texture, so
 * the gantry matches the frame and is depth-correct (entity-cutout writes depth, so it is properly
 * occluded by terrain — no see-through — and is cull-off so winding is a non-issue):
 * <ul>
 *   <li>a gantry that rides the frame: a bridge beam spanning one axis with end trucks, a carriage
 *       that tracks the dig column, and a support shaft down to the head;</li>
 *   <li>the drill head: a multi-segment bit (chuck collar, shaft, flared hub, four flutes, and a
 *       tapered spike with a glowing tip) that spins and sits on top of the block being mined.</li>
 * </ul>
 * The head follows the last block actually mined (server-synced) so it tracks the dig in order rather
 * than chasing the raw scan cursor; the position is smoothed client-side.
 */
public class QuarryControllerRenderer
        implements BlockEntityRenderer<QuarryControllerBlockEntity, QuarryControllerRenderState> {

    /** The frame texture, reused so the gantry + bit match the frame ring exactly. */
    private static final Identifier TEX =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/block/quarry_frame.png");
    /** Full-bright light (the frame is emissive); per-face lighting still shades by normal. */
    private static final int FULL_BRIGHT = 0x00F000F0;
    /** Top of the bit (chuck collar top, local Y) — the support shaft meets it here. */
    private static final float BIT_TOP = 0.72F;
    /** Spike apex (local Y); the head is placed so this sits on the mined block's top face. */
    private static final float TIP_Y = -0.6F;

    @Override
    public QuarryControllerRenderState createRenderState() {
        return new QuarryControllerRenderState();
    }

    @Override
    public int getViewDistance() {
        return 128; // regions can stretch well past the default 64 from the controller
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The gantry + drill render far from the controller block, so register as a global block entity:
        // otherwise the renderer is dropped whenever the controller's own chunk section is occlusion-
        // culled (e.g. when the camera is down inside the pit), and the whole gantry vanishes.
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(QuarryControllerBlockEntity be) {
        QuarryRegion r = be.renderRegion();
        if (r == null) {
            return new AABB(be.getBlockPos());
        }
        // The gantry + drill render across the whole region and down the dig shaft, far from the
        // controller block. Without this, the default unit-cube render box gets frustum-culled when the
        // camera is close and the controller block leaves view — and the gantry vanishes. Cover the
        // whole region down to the world floor.
        int floor = be.getLevel() != null ? be.getLevel().getMinY() : r.refY() - 64;
        return new AABB(r.minX(), floor, r.minZ(), r.maxX() + 1, r.refY() + 2, r.maxZ() + 1);
    }

    @Override
    public void extractRenderState(QuarryControllerBlockEntity be, QuarryControllerRenderState s,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, cameraPos, breakProgress);
        QuarryRegion r = be.renderRegion();
        QuarryControllerBlockEntity.State st = be.renderState();
        if (r == null || st == QuarryControllerBlockEntity.State.IDLE || be.getLevel() == null) {
            s.active = false;
            return;
        }
        s.active = true;
        BlockPos p = be.getBlockPos();
        s.x0 = r.minX() - p.getX();
        s.x1 = r.maxX() + 1 - p.getX();
        s.z0 = r.minZ() - p.getZ();
        s.z1 = r.maxZ() + 1 - p.getZ();
        s.topY = r.refY() + 1 - p.getY();
        s.accent = be.tier().accentColor();
        s.mining = st == QuarryControllerBlockEntity.State.MINING;

        double cx = (r.minX() + r.maxX() + 1) / 2.0;
        double cz = (r.minZ() + r.maxZ() + 1) / 2.0;
        double now = be.getLevel().getGameTime() + partialTick;
        if (s.mining && be.renderHasMine()) {
            // Sweep the head continuously from the previously-mined block to the current one over the
            // real elapsed interval (1 block at a time), so motion is smooth rather than stop-start.
            // The spike apex sits on the block's top face. dur self-adapts to the actual dig pace.
            double dur = Math.max(1.0, Math.min(20.0, be.renderMineTime() - be.renderPrevMineTime()));
            double a = Math.max(0.0, Math.min(1.0, (now - be.renderMineTime()) / dur));
            be.dispX = lerp(be.renderPrevMineX() + 0.5, be.renderMineX() + 0.5, a);
            be.dispY = lerp(be.renderPrevMineY(), be.renderMineY(), a) + 1.0 - TIP_Y;
            be.dispZ = lerp(be.renderPrevMineZ() + 0.5, be.renderMineZ() + 0.5, a);
            be.dispInit = true;
        } else {
            // Idle / pre-first-block: hover at the region centre, lightly eased.
            double tx = cx;
            double ty = s.mining ? r.refY() + 1.0 - TIP_Y : r.refY() + 0.5;
            double tz = cz;
            if (!be.dispInit) {
                be.dispX = tx;
                be.dispY = ty;
                be.dispZ = tz;
                be.dispInit = true;
            } else {
                be.dispX += (tx - be.dispX) * 0.2;
                be.dispY += (ty - be.dispY) * 0.2;
                be.dispZ += (tz - be.dispZ) * 0.2;
            }
        }
        s.hx = be.dispX - p.getX();
        s.hy = be.dispY - p.getY();
        s.hz = be.dispZ - p.getZ();

        s.headSpin = (float) ((now * 18.0) % 360.0); // fast spin reads as a working drill
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public void submit(QuarryControllerRenderState s, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!s.active) {
            return;
        }
        RenderType rt = RenderTypes.entityCutout(TEX);
        // Gantry: world-relative, at the base pose.
        collector.order(1).submitCustomGeometry(poseStack, rt, (pose, c) -> drawGantry(s, pose, c));
        // Drill head: spun around the vertical axis at the dig cell.
        poseStack.pushPose();
        poseStack.translate(s.hx, s.hy, s.hz);
        poseStack.mulPose(Axis.YP.rotationDegrees(s.headSpin));
        collector.order(1).submitCustomGeometry(poseStack, rt, (pose, c) -> drawBit(pose, c));
        poseStack.popPose();
    }

    /** The gantry crane that rides the frame: bridge + end trucks + carriage + support shaft. */
    private static void drawGantry(QuarryControllerRenderState s, PoseStack.Pose pose, VertexConsumer c) {
        float ty = (float) s.topY;
        float hx = (float) s.hx;
        float hz = (float) s.hz;
        float z0 = (float) s.z0;
        float z1 = (float) s.z1;
        // Bridge beam: spans Z at the head's X, sitting on the frame plane (moves in X with the dig).
        texBeam(c, pose, hx - 0.15F, ty, z0, hx + 0.15F, ty + 0.22F, z1);
        // End trucks: ride the two side rails of the frame.
        texBox(c, pose, hx - 0.22F, ty - 0.05F, z0, hx + 0.22F, ty + 0.28F, z0 + 0.35F);
        texBox(c, pose, hx - 0.22F, ty - 0.05F, z1 - 0.35F, hx + 0.22F, ty + 0.28F, z1);
        // Carriage: the moving drill mount that tracks the dig column.
        texBox(c, pose, hx - 0.26F, ty - 0.06F, hz - 0.26F, hx + 0.26F, ty + 0.30F, hz + 0.26F);
        // Support shaft from the carriage down to the top of the spinning bit.
        texBeam(c, pose, hx - 0.07F, (float) s.hy + BIT_TOP, hz - 0.07F, hx + 0.07F, ty, hz + 0.07F);
    }

    /** The drill head (local space, origin at the head centre, Y up): a detailed frame-textured bit. */
    private static void drawBit(PoseStack.Pose pose, VertexConsumer c) {
        texBox(c, pose, -0.24F, 0.58F, -0.24F, 0.24F, BIT_TOP, 0.24F); // chuck collar
        texBox(c, pose, -0.12F, 0.22F, -0.12F, 0.12F, 0.58F, 0.12F);   // shaft
        texBox(c, pose, -0.22F, 0.05F, -0.22F, 0.22F, 0.22F, 0.22F);   // flared hub
        texBox(c, pose, 0.20F, 0.05F, -0.05F, 0.34F, 0.30F, 0.05F);    // flute +x
        texBox(c, pose, -0.34F, 0.05F, -0.05F, -0.20F, 0.30F, 0.05F);  // flute -x
        texBox(c, pose, -0.05F, 0.05F, 0.20F, 0.05F, 0.30F, 0.34F);    // flute +z
        texBox(c, pose, -0.05F, 0.05F, -0.34F, 0.05F, 0.30F, -0.20F);  // flute -z
        // tapering spike with a glowing tip
        float bw = 0.22F;
        spikeFace(c, pose, -bw, -bw, bw, -bw, 0, 0, -1); // north
        spikeFace(c, pose, bw, -bw, bw, bw, 1, 0, 0);    // east
        spikeFace(c, pose, bw, bw, -bw, bw, 0, 0, 1);    // south
        spikeFace(c, pose, -bw, bw, -bw, -bw, -1, 0, 0); // west
    }

    /** One tapering spike face: two base corners (at y=0.05) up to the apex, base white, tip glowing. */
    private static void spikeFace(VertexConsumer c, PoseStack.Pose pose,
            float ax, float az, float bx, float bz, float nx, float ny, float nz) {
        tv(c, pose, ax, 0.05F, az, 0.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, bx, 0.05F, bz, 1.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, 0.0F, TIP_Y, 0.0F, 0.5F, 1.0F, nx, ny, nz, 255, 176, 230);
        tv(c, pose, 0.0F, TIP_Y, 0.0F, 0.5F, 1.0F, nx, ny, nz, 255, 176, 230);
    }

    // --- textured geometry helpers (entityCutout = QUADS, cull off, depth-correct) -------------

    private static void tv(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, float nx, float ny, float nz, int r, int g, int b) {
        c.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /** A textured quad (4 corners, one normal, UV from (0,0) to (uMax,vMax)), drawn white. */
    private static void quad(VertexConsumer c, PoseStack.Pose pose,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float nx, float ny, float nz, float uMax, float vMax) {
        tv(c, pose, ax, ay, az, 0.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, bx, by, bz, uMax, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, cx, cy, cz, uMax, vMax, nx, ny, nz, 255, 255, 255);
        tv(c, pose, dx, dy, dz, 0.0F, vMax, nx, ny, nz, 255, 255, 255);
    }

    /** A textured box; UVs map 1 texture tile per block (dims are assumed ≤ ~1, so no wrap needed). */
    private static void texBox(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, dx, dz); // down
        quad(c, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, dx, dz);  // up
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, dx, dy); // north
        quad(c, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, dx, dy);  // south
        quad(c, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, dz, dy); // west
        quad(c, pose, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, dz, dy);  // east
    }

    /** A textured beam: subdivides the longest axis into ≤1-block segments so the texture tiles. */
    private static void texBeam(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        if (dx >= dy && dx >= dz) {
            for (float x = x0; x < x1 - 1.0e-4F; x += 1.0F) {
                texBox(c, pose, x, y0, z0, Math.min(x + 1.0F, x1), y1, z1);
            }
        } else if (dz >= dx && dz >= dy) {
            for (float z = z0; z < z1 - 1.0e-4F; z += 1.0F) {
                texBox(c, pose, x0, y0, z, x1, y1, Math.min(z + 1.0F, z1));
            }
        } else {
            for (float y = y0; y < y1 - 1.0e-4F; y += 1.0F) {
                texBox(c, pose, x0, y, z0, x1, Math.min(y + 1.0F, y1), z1);
            }
        }
    }
}
