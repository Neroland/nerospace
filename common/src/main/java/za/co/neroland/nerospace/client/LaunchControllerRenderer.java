package za.co.neroland.nerospace.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.rocket.LaunchControllerBlockEntity;

/**
 * Draws the Launch Controller's holographic pad preview: a glowing footprint outline + a vertical
 * "laser" at each block the selected tier's formation still needs (green = Launch Pad, cyan = Station
 * Wall, red = Launch Gantry). Texture-free wireframe via the lines render type; the ghosts disappear as
 * the real blocks get placed. Registered through the {@link ClientBlockEntityRenderers} seam.
 */
public class LaunchControllerRenderer
        implements BlockEntityRenderer<LaunchControllerBlockEntity, LaunchControllerRenderState> {

    @Override
    public LaunchControllerRenderState createRenderState() {
        return new LaunchControllerRenderState();
    }

    @Override
    public void extractRenderState(LaunchControllerBlockEntity controller, LaunchControllerRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(controller, state, partialTick, cameraPos, breakProgress);
        state.cells.clear();
        List<int[]> cells = controller.previewCells();
        state.visible = !cells.isEmpty();
        state.cells.addAll(cells);
    }

    @Override
    public void submit(LaunchControllerRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.visible) {
            return;
        }
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
            for (int[] cell : state.cells) {
                drawGhost(consumer, pose, cell[0], cell[1], cell[2], cell[3]);
            }
        });
    }

    /** A footprint outline + vertical laser at offset {@code (dx, dy, dz)} from the controller. */
    private static void drawGhost(VertexConsumer c, PoseStack.Pose pose, int dx, int dy, int dz, int color) {
        float x0 = dx + 0.12F;
        float x1 = dx + 0.88F;
        float z0 = dz + 0.12F;
        float z1 = dz + 0.88F;
        float y = dy + 0.06F;
        // Footprint square.
        line(c, pose, x0, y, z0, x1, y, z0, color);
        line(c, pose, x1, y, z0, x1, y, z1, color);
        line(c, pose, x1, y, z1, x0, y, z1, color);
        line(c, pose, x0, y, z1, x0, y, z0, color);
        // Vertical laser beam.
        float cx = dx + 0.5F;
        float cz = dz + 0.5F;
        line(c, pose, cx, y, cz, cx, dy + 1.6F, cz, color);
    }

    private static void line(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float nx = x1 - x0;
        float ny = y1 - y0;
        float nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-4F) {
            len = 1.0F;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        c.addVertex(pose, x0, y0, z0).setColor(color).setNormal(pose, nx, ny, nz);
        c.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz);
    }
}
