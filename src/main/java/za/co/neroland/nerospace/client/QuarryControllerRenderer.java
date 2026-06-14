package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryRegion;

/**
 * Draws the quarry's working machinery: glowing gantry rails around the claimed region, a moving
 * bridge rail tracking the dig column, a vertical drill shaft and a bright drill head at the cell
 * currently being mined (MINER_DESIGN — "build frame + drill head"). Geometry is emissive
 * position-colour quads via the lightning render type (same approach as the Universal Pipe streams).
 * The head position is server-synced (region + cursor + currentY) and smoothed client-side.
 */
public class QuarryControllerRenderer
        implements BlockEntityRenderer<QuarryControllerBlockEntity, QuarryControllerRenderState> {

    private static final double RAIL = 0.12;
    private static final double SHAFT = 0.05;
    private static final double HEAD = 0.18;

    @Override
    public QuarryControllerRenderState createRenderState() {
        return new QuarryControllerRenderState();
    }

    @Override
    public int getViewDistance() {
        return 128; // regions can stretch well past the default 64 from the controller
    }

    @Override
    public void extractRenderState(QuarryControllerBlockEntity be, QuarryControllerRenderState s,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, cameraPos, breakProgress);
        QuarryRegion r = be.renderRegion();
        QuarryControllerBlockEntity.State st = be.renderState();
        if (r == null || st == QuarryControllerBlockEntity.State.IDLE) {
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

        double tx;
        double ty;
        double tz;
        if (s.mining) {
            int cols = Math.max(1, r.columns());
            int cur = Mth.clamp(be.renderCursor(), 0, cols - 1);
            BlockPos col = r.columnPos(cur, be.renderCurrentY());
            tx = col.getX() + 0.5;
            ty = be.renderCurrentY() + 0.5;
            tz = col.getZ() + 0.5;
        } else {
            tx = (r.minX() + r.maxX() + 1) / 2.0;
            ty = r.refY() + 0.5;
            tz = (r.minZ() + r.maxZ() + 1) / 2.0;
        }
        if (!be.dispInit) {
            be.dispX = tx;
            be.dispY = ty;
            be.dispZ = tz;
            be.dispInit = true;
        } else {
            double k = 0.3;
            be.dispX += (tx - be.dispX) * k;
            be.dispY += (ty - be.dispY) * k;
            be.dispZ += (tz - be.dispZ) * k;
        }
        s.hx = be.dispX - p.getX();
        s.hy = be.dispY - p.getY();
        s.hz = be.dispZ - p.getZ();
    }

    @Override
    public void submit(QuarryControllerRenderState s, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!s.active) {
            return;
        }
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.lightning(),
                (pose, consumer) -> draw(s, pose, consumer));
    }

    private static void draw(QuarryControllerRenderState s, PoseStack.Pose pose, VertexConsumer consumer) {
        int ar = (s.accent >> 16) & 0xFF;
        int ag = (s.accent >> 8) & 0xFF;
        int ab = s.accent & 0xFF;
        int a = 170;
        double ty = s.topY;

        // The frame BLOCKS already draw the static perimeter, so the renderer only adds the MOVING
        // gantry parts: a bridge rail tracking the dig column, the drill shaft and the drill head.
        // Moving bridge rail (spans Z at the head's X).
        box(pose, consumer, s.hx - RAIL / 2, ty - RAIL, s.z0, s.hx + RAIL / 2, ty, s.z1, ar, ag, ab, a);

        // Vertical drill shaft from the head up to the bridge.
        box(pose, consumer, s.hx - SHAFT, s.hy, s.hz - SHAFT, s.hx + SHAFT, ty - RAIL, s.hz + SHAFT,
                120, 230, 255, 150);

        // Drill head.
        box(pose, consumer, s.hx - HEAD, s.hy - HEAD, s.hz - HEAD, s.hx + HEAD, s.hy + HEAD, s.hz + HEAD,
                200, 245, 255, 220);
    }

    /** Emit the 6 faces of an axis-aligned box as position-colour quads. */
    private static void box(PoseStack.Pose pose, VertexConsumer c, double x0, double y0, double z0,
            double x1, double y1, double z1, int r, int g, int b, int a) {
        quad(pose, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a); // -Z
        quad(pose, c, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b, a); // +Z
        quad(pose, c, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, a); // -X
        quad(pose, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a); // +X
        quad(pose, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); // +Y
        quad(pose, c, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, r, g, b, a); // -Y
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer c,
            double ax, double ay, double az, double bx, double by, double bz,
            double cx, double cy, double cz, double dx, double dy, double dz,
            int r, int g, int b, int a) {
        c.addVertex(pose, (float) ax, (float) ay, (float) az).setColor(r, g, b, a);
        c.addVertex(pose, (float) bx, (float) by, (float) bz).setColor(r, g, b, a);
        c.addVertex(pose, (float) cx, (float) cy, (float) cz).setColor(r, g, b, a);
        c.addVertex(pose, (float) dx, (float) dy, (float) dz).setColor(r, g, b, a);
    }
}
