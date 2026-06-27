package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.LaunchControllerBlock;
import za.co.neroland.nerospace.rocket.LaunchControllerBlockEntity;

/**
 * Draws the Launch Controller's sleek console face on top of its solid 3×2 gunmetal chassis: a big
 * animated mission-control screen (rotated to the operator side), two projector arms reaching toward the
 * pad with glowing emitter heads, a top light band and corner lights — all in a green / blue / purple
 * glow palette. When the hologram is on it also projects a ghost of the real block in each still-missing
 * pad cell. Coordinates are in the core block's local space (core = bottom-centre of the structure); the
 * structure spans x ∈ [-1, 2], y ∈ [0, 2], z ∈ [0, 1] in the canonical (north) frame.
 */
public class LaunchControllerRenderer
        implements BlockEntityRenderer<LaunchControllerBlockEntity, LaunchControllerRenderState> {

    private static final Identifier GLOW =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/glow.png");
    private static final Identifier BODY =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/controller_body.png");
    private static final Identifier PAD_TEX =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/rocket_launch_pad.png");
    private static final Identifier WALL_TEX =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/station_wall.png");
    private static final Identifier GANTRY_TEX =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/launch_gantry.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float PAD_H = 3.0F / 16.0F;

    @Override
    public LaunchControllerRenderState createRenderState() {
        return new LaunchControllerRenderState();
    }

    @Override
    public int getViewDistance() {
        return 256; // the pad hologram projects several blocks out from the controller
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // Keep rendering even when the controller's own chunk section is culled — otherwise the hologram
        // (drawn well in front of the block) vanishes the moment the player looks toward the pad.
        return true;
    }

    @Override
    public boolean shouldRender(LaunchControllerBlockEntity blockEntity, Vec3 cameraPos) {
        return true;
    }

    @Override
    public void extractRenderState(LaunchControllerBlockEntity controller, LaunchControllerRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(controller, state, partialTick, cameraPos, breakProgress);
        state.facing = controller.getBlockState().getValue(LaunchControllerBlock.FACING);
        state.cells.clear();
        state.cells.addAll(controller.previewCells());
        state.visible = !state.cells.isEmpty();
    }

    @Override
    public void submit(LaunchControllerRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        long now = System.currentTimeMillis();

        // Console face — rotate so the screen faces the operator side and the arms reach the pad side.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-blockYRot(state.facing)));
        poseStack.translate(-0.5, 0.0, -0.5);
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(BODY),
                (pose, c) -> drawArms(c, pose));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLOW),
                (pose, c) -> drawConsole(c, pose, now));
        poseStack.popPose();

        if (!state.visible) {
            return;
        }
        submitGhosts(state, poseStack, collector, PAD_TEX, 0);
        submitGhosts(state, poseStack, collector, WALL_TEX, 1);
        submitGhosts(state, poseStack, collector, GANTRY_TEX, 2);
    }

    private static float blockYRot(Direction facing) {
        switch (facing) {
            case EAST:  return 90.0F;
            case SOUTH: return 180.0F;
            case WEST:  return 270.0F;
            default:    return 0.0F; // NORTH
        }
    }

    // --- projector arms (gunmetal, body texture) -----------------------------------------------

    /** Two arms rising from the top corners and booming out toward the pad side (−z), holding emitters. */
    private static void drawArms(VertexConsumer c, PoseStack.Pose pose) {
        arm(c, pose, -1.0F); // left
        arm(c, pose, 1.95F); // right (x of the outer face)
    }

    private static void arm(VertexConsumer c, PoseStack.Pose pose, float x) {
        float w = 0.16F;
        // vertical riser above the top corner
        box(c, pose, x, 1.9F, 0.42F, x + w, 2.45F, 0.58F, 255, 255, 255);
        // boom angling out over the pad side
        box(c, pose, x, 2.3F, -0.45F, x + w, 2.46F, 0.5F, 255, 255, 255);
    }

    // --- the glowing console (screen + band + lights + emitter heads) ---------------------------

    private static void drawConsole(VertexConsumer c, PoseStack.Pose pose, long now) {
        float fz = 1.0F; // front (operator) face of the structure, at z = 1
        // Screen frame (green).
        int[] frame = green(0.5F + 0.5F * Mth.sin(now / 600.0F));
        float sx0 = -0.85F;
        float sx1 = 1.85F;
        float sy0 = 0.30F;
        float sy1 = 1.82F;
        frameRect(c, pose, sx0, sy0, sx1, sy1, fz + 0.005F, 0.05F, frame);

        // Dim base panel (blue).
        int[] base = blue(0.16F);
        box(c, pose, sx0 + 0.06F, sy0 + 0.06F, fz + 0.006F, sx1 - 0.06F, sy1 - 0.06F, fz + 0.012F,
                base[0], base[1], base[2]);

        // Live data bars (cyan), each growing/shrinking at its own phase.
        for (int i = 0; i < 4; i++) {
            float y0 = 0.55F + i * 0.30F;
            float grow = 0.30F + 0.70F * (0.5F + 0.5F * Mth.sin(now / 220.0F + i * 1.3F));
            int[] bar = cyan(0.55F + 0.45F * grow);
            box(c, pose, sx0 + 0.18F, y0, fz + 0.013F, sx0 + 0.18F + 2.3F * grow, y0 + 0.16F, fz + 0.020F,
                    bar[0], bar[1], bar[2]);
        }
        // Green progress strip along the bottom.
        float prog = 0.5F + 0.5F * Mth.sin(now / 900.0F);
        int[] pg = green(0.9F);
        box(c, pose, sx0 + 0.18F, sy0 + 0.10F, fz + 0.013F, sx0 + 0.18F + 2.3F * prog, sy0 + 0.24F, fz + 0.020F,
                pg[0], pg[1], pg[2]);
        // Purple status ticks across the top.
        int[] pk = purple(0.6F + 0.4F * (0.5F + 0.5F * Mth.sin(now / 180.0F)));
        for (int i = 0; i < 5; i++) {
            float tx = sx0 + 0.25F + i * 0.55F;
            box(c, pose, tx, sy1 - 0.22F, fz + 0.013F, tx + 0.18F, sy1 - 0.10F, fz + 0.020F, pk[0], pk[1], pk[2]);
        }

        // Top light band ringing the structure.
        int[] bd = blue(0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(now / 380.0F + 1.0F)));
        float yb0 = 1.90F;
        float yb1 = 1.97F;
        box(c, pose, -1.0F, yb0, 0.99F, 2.0F, yb1, 1.01F, bd[0], bd[1], bd[2]); // front
        box(c, pose, -1.0F, yb0, -0.01F, 2.0F, yb1, 0.01F, bd[0], bd[1], bd[2]); // back
        box(c, pose, -1.01F, yb0, 0.0F, -0.99F, yb1, 1.0F, bd[0], bd[1], bd[2]); // left
        box(c, pose, 1.99F, yb0, 0.0F, 2.01F, yb1, 1.0F, bd[0], bd[1], bd[2]);   // right

        // Glowing emitter heads at the ends of the projector arms (purple, strong pulse).
        int[] em = purple(0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(now / 200.0F)));
        box(c, pose, -1.06F, 2.28F, -0.55F, -0.84F, 2.50F, -0.33F, em[0], em[1], em[2]);
        box(c, pose, 1.89F, 2.28F, -0.55F, 2.11F, 2.50F, -0.33F, em[0], em[1], em[2]);
    }

    /** A glowing rectangular frame (four thin bars) on the z=plane facing +z. */
    private static void frameRect(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float x1, float y1, float z, float t, int[] rgb) {
        box(c, pose, x0, y1 - t, z, x1, y1, z + 0.012F, rgb[0], rgb[1], rgb[2]); // top
        box(c, pose, x0, y0, z, x1, y0 + t, z + 0.012F, rgb[0], rgb[1], rgb[2]); // bottom
        box(c, pose, x0, y0, z, x0 + t, y1, z + 0.012F, rgb[0], rgb[1], rgb[2]); // left
        box(c, pose, x1 - t, y0, z, x1, y1, z + 0.012F, rgb[0], rgb[1], rgb[2]); // right
    }

    private static int[] green(float p)  { return new int[] {(int) (60 * p), (int) (230 * p), (int) (95 * p)}; }
    private static int[] blue(float p)   { return new int[] {(int) (60 * p), (int) (140 * p), (int) (235 * p)}; }
    private static int[] cyan(float p)   { return new int[] {(int) (70 * p), (int) (215 * p), (int) (235 * p)}; }
    private static int[] purple(float p) { return new int[] {(int) (170 * p), (int) (80 * p), (int) (235 * p)}; }

    // --- holographic ghost blocks --------------------------------------------------------------

    private void submitGhosts(LaunchControllerRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            Identifier texture, int type) {
        boolean any = false;
        for (int[] cell : state.cells) {
            if (cell[3] == type) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        RenderType rt = RenderTypes.entityCutout(texture);
        collector.order(1).submitCustomGeometry(poseStack, rt, (pose, c) -> {
            for (int[] cell : state.cells) {
                if (cell[3] != type) {
                    continue;
                }
                int dx = cell[0];
                int dy = cell[1];
                int dz = cell[2];
                if (type == 0) {
                    box(c, pose, dx + 0.06F, dy, dz + 0.06F, dx + 0.94F, dy + PAD_H, dz + 0.94F, 255, 255, 255);
                } else {
                    box(c, pose, dx + 0.08F, dy + 0.02F, dz + 0.08F, dx + 0.92F, dy + 0.96F, dz + 0.92F, 255, 255, 255);
                }
            }
        });
    }

    // --- textured geometry (entityCutout = QUADS, cull off; complete vertices) ------------------

    private static void box(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1, int r, int g, int b) {
        face(c, pose, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b);
        face(c, pose, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b);
        face(c, pose, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b);
        face(c, pose, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b);
        face(c, pose, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b);
        face(c, pose, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b);
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz, int r, int g, int b) {
        vertex(c, pose, ax, ay, az, 0.0F, 0.0F, nx, ny, nz, r, g, b);
        vertex(c, pose, bx, by, bz, 1.0F, 0.0F, nx, ny, nz, r, g, b);
        vertex(c, pose, cx, cy, cz, 1.0F, 1.0F, nx, ny, nz, r, g, b);
        vertex(c, pose, dx, dy, dz, 0.0F, 1.0F, nx, ny, nz, r, g, b);
        vertex(c, pose, dx, dy, dz, 0.0F, 1.0F, -nx, -ny, -nz, r, g, b);
        vertex(c, pose, cx, cy, cz, 1.0F, 1.0F, -nx, -ny, -nz, r, g, b);
        vertex(c, pose, bx, by, bz, 1.0F, 0.0F, -nx, -ny, -nz, r, g, b);
        vertex(c, pose, ax, ay, az, 0.0F, 0.0F, -nx, -ny, -nz, r, g, b);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, float nx, float ny, float nz, int r, int g, int b) {
        c.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
