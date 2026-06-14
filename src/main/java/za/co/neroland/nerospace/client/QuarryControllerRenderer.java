package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryRegion;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Draws the quarry's working machinery on top of the (solid, block-based) frame ring: a moving
 * bridge rail + drill shaft drawn as depth-tested lines, and the drill head itself rendered as a
 * spinning item model (the proven Star-Guide-hologram path — solid, lit, depth-correct). The head
 * position is server-synced (region + cursor + currentY) and smoothed client-side.
 */
public class QuarryControllerRenderer
        implements BlockEntityRenderer<QuarryControllerBlockEntity, QuarryControllerRenderState> {

    /** Per-vertex line width required by the lines vertex format (POSITION_COLOR_NORMAL_LINE_WIDTH). */
    private static final float LINE_WIDTH = 3.0F;
    /** Full-bright light so the head glows like a powered tool. */
    private static final int FULL_BRIGHT = 0x00F000F0;

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

        float now = be.getLevel().getGameTime() + partialTick;
        s.headSpin = (now * 6.0F) % 360.0F;
        // Render the drill head as a spinning pickaxe item (solid/lit/depth-correct).
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                s.head, new ItemStack(ModItems.NEROSIUM_PICKAXE.get()), ItemDisplayContext.GROUND,
                be.getLevel(), null, (int) p.asLong());
    }

    @Override
    public void submit(QuarryControllerRenderState s, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!s.active) {
            return;
        }
        // Depth-tested lines for the moving bridge + drill shaft.
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.lines(),
                (pose, consumer) -> draw(s, pose, consumer));

        // The drill head: a spinning, full-bright item model at the dig cell. Spin around the vertical
        // axis, then flip 180° so the pickaxe head points DOWN into the dig face (was rendering upside
        // down — GROUND context keeps the sprite head-up by default).
        poseStack.pushPose();
        poseStack.translate(s.hx, s.hy, s.hz);
        poseStack.mulPose(Axis.YP.rotationDegrees(s.headSpin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        s.head.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void draw(QuarryControllerRenderState s, PoseStack.Pose pose, VertexConsumer consumer) {
        int ar = (s.accent >> 16) & 0xFF;
        int ag = (s.accent >> 8) & 0xFF;
        int ab = s.accent & 0xFF;
        double ty = s.topY;

        // The frame itself is the solid frame BLOCKS — the renderer only adds the MOVING gantry parts.
        // Moving bridge: a rail spanning Z at the head's X, along the top of the frame plane.
        line(pose, consumer, s.hx, ty, s.z0, s.hx, ty, s.z1, ar, ag, ab, 255);
        // Drill shaft: from the head up to the frame plane.
        line(pose, consumer, s.hx, s.hy, s.hz, s.hx, ty - 1.0, s.hz, 120, 230, 255, 255);
    }

    /** A single line segment (RenderTypes.lines needs a per-vertex normal = the segment direction). */
    private static void line(PoseStack.Pose pose, VertexConsumer c, double x1, double y1, double z1,
            double x2, double y2, double z2, int r, int g, int b, int a) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-5f) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        c.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
        c.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
    }
}
