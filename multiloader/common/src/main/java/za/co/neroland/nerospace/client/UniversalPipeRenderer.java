package za.co.neroland.nerospace.client;

import java.util.List;

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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.TravellingItem;
import za.co.neroland.nerospace.pipe.UniversalPipeBlock;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/**
 * Renders the dynamic contents of a Universal Pipe: the item stacks physically travelling through the
 * segment, and colour-coded stream packets pulsing along each active arm (red = energy, blue = fluid,
 * cyan = gas). The translucent tube itself is the (static, batched) multipart block model; this
 * renderer only draws what is MOVING, so idle pipes cost nothing beyond the connection checks.
 *
 * <p>Cross-loader port: the standalone mod's pipe renderer, on the vanilla BER submission API + the
 * item-model resolver + the {@link ClientBlockEntityRenderers} seam (all proven cross-version by the
 * Star Guide hologram and solar deck). Reads buffered amounts via the cross-loader storage interfaces
 * and connections from the pipe's {@code CONNECTIONS} blockstate.</p>
 */
public class UniversalPipeRenderer
        implements BlockEntityRenderer<UniversalPipeBlockEntity, UniversalPipeRenderState> {

    private static final int MAX_RENDERED_ITEMS = UniversalPipeBlockEntity.MAX_TRAVELLING;
    private static final float STREAM_SPEED = 0.6F;   // pulses per tick fraction (scaled below)
    private static final float STREAM_HALF = 0.055F;  // half-size of a stream packet quad

    @Override
    public UniversalPipeRenderState createRenderState() {
        return new UniversalPipeRenderState();
    }

    @Override
    public void extractRenderState(UniversalPipeBlockEntity pipe, UniversalPipeRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTick, cameraPos, breakProgress);
        if (pipe.getLevel() == null) {
            state.visibleItems = 0;
            clearStreams(state);
            return;
        }
        float now = pipe.getLevel().getGameTime() + partialTick;
        state.time = now;

        // Stream layers from the buffered contents + face modes + connection blockstate.
        boolean hasEnergy = pipe.getEnergy().getAmount() > 0;
        boolean hasFluid = pipe.getFluidTank().getAmount() > 0;
        boolean hasGas = pipe.getGas().getAmount() > 0;
        state.streamColors[0] = PipeResourceType.ENERGY.color();
        state.streamColors[1] = PipeResourceType.FLUID.color();
        state.streamColors[2] = PipeResourceType.GAS.color();

        PipeResourceType[] layerTypes = {PipeResourceType.ENERGY, PipeResourceType.FLUID, PipeResourceType.GAS};
        boolean[] layerHas = {hasEnergy, hasFluid, hasGas};
        for (Direction dir : Direction.values()) {
            int d = dir.get3DDataValue();
            state.connections[d] = pipe.getBlockState().getValue(UniversalPipeBlock.CONNECTIONS[d]);
            for (int l = 0; l < 3; l++) {
                PipeIoMode mode = pipe.mode(dir, layerTypes[l]);
                state.streams[d][l] = state.connections[d] && layerHas[l] && mode.isConnected();
                state.inward[d][l] = mode == PipeIoMode.IN;
            }
        }

        // Travelling items: advance progress locally for smooth motion between the (throttled) server syncs.
        float dt = pipe.clientItemTime == 0.0F ? 0.0F : Math.max(0.0F, now - pipe.clientItemTime);
        pipe.clientItemTime = now;
        float step = 1.0F / pipe.itemTicksPerBlock();

        List<TravellingItem> items = pipe.travelling();
        int count = Math.min(MAX_RENDERED_ITEMS, items.size());
        state.visibleItems = count;
        for (int i = 0; i < count; i++) {
            TravellingItem item = items.get(i);
            if (!item.isParked()) {
                item.advance(dt * step * 0.999F); // hold just shy of 1.0 until the server expires it
            }
            UniversalPipeRenderState.TravellingItemEntry entry = state.entry(i);

            float t = item.progress();
            float fx = item.from().getStepX();
            float fy = item.from().getStepY();
            float fz = item.from().getStepZ();
            Direction to = item.to(); // local so the null check holds for the analyzer
            if (item.isParked() || to == null) {
                entry.x = 0.5F;
                entry.y = 0.5F;
                entry.z = 0.5F;
            } else if (t < 0.5F) {
                float k = 0.5F - t; // from face (0.5) -> centre (0)
                entry.x = 0.5F + fx * k;
                entry.y = 0.5F + fy * k;
                entry.z = 0.5F + fz * k;
            } else {
                float k = t - 0.5F; // centre -> to face
                entry.x = 0.5F + to.getStepX() * k;
                entry.y = 0.5F + to.getStepY() * k;
                entry.z = 0.5F + to.getStepZ() * k;
            }
            entry.spin = (now * 4.0F + i * 45.0F) % 360.0F;

            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                    entry.renderState, item.stack(), ItemDisplayContext.GROUND, pipe.getLevel(), null,
                    (int) pipe.getBlockPos().asLong() + i);
        }
    }

    private static void clearStreams(UniversalPipeRenderState state) {
        for (int d = 0; d < 6; d++) {
            for (int l = 0; l < 3; l++) {
                state.streams[d][l] = false;
            }
        }
    }

    @Override
    public void submit(UniversalPipeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        // Travelling items.
        for (int i = 0; i < state.visibleItems; i++) {
            UniversalPipeRenderState.TravellingItemEntry entry = state.items.get(i);
            poseStack.pushPose();
            poseStack.translate(entry.x, entry.y - 0.12F, entry.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(entry.spin));
            poseStack.scale(0.55F, 0.55F, 0.55F);
            entry.renderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // Stream packets (position-colour translucent quads via the lightning render type).
        boolean any = false;
        for (int d = 0; d < 6 && !any; d++) {
            for (int l = 0; l < 3 && !any; l++) {
                any = state.streams[d][l];
            }
        }
        if (!any) {
            return;
        }
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.lightning(),
                (pose, consumer) -> renderStreams(state, pose, consumer));
    }

    private static void renderStreams(UniversalPipeRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        for (Direction dir : Direction.values()) {
            int d = dir.get3DDataValue();
            for (int l = 0; l < 3; l++) {
                if (!state.streams[d][l]) {
                    continue;
                }
                int color = state.streamColors[l];
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                // Two pulses per arm, staggered per layer so parallel lanes read clearly.
                for (int pulse = 0; pulse < 2; pulse++) {
                    float phase = Mth.frac(state.time * STREAM_SPEED * 0.12F + pulse * 0.5F + l * 0.21F);
                    if (state.inward[d][l]) {
                        phase = 1.0F - phase;
                    }
                    float along = 0.08F + 0.42F * phase; // centre -> face
                    float cx = 0.5F + dir.getStepX() * along;
                    float cy = 0.5F + dir.getStepY() * along + (l - 1) * 0.09F * (dir.getAxis().isHorizontal() ? 1 : 0);
                    float cz = 0.5F + dir.getStepZ() * along;
                    crossQuads(pose, consumer, dir, cx, cy, cz, r, g, b, 170);
                }
            }
        }
    }

    /** Two crossed quads centred on the point, aligned along the arm's axis (visible from all sides). */
    private static void crossQuads(PoseStack.Pose pose, VertexConsumer consumer, Direction dir,
            float cx, float cy, float cz, int r, int g, int b, int a) {
        float s = STREAM_HALF;
        float vx = dir.getStepX() * s * 1.8F;
        float vy = dir.getStepY() * s * 1.8F;
        float vz = dir.getStepZ() * s * 1.8F;
        // Perpendicular axes.
        float u1x = dir.getAxis() == Direction.Axis.X ? 0 : s;
        float u1y = dir.getAxis() == Direction.Axis.X ? s : 0;
        float u2x = 0;
        float u2y = dir.getAxis() == Direction.Axis.Y ? 0 : s;
        float u2z = dir.getAxis() == Direction.Axis.Y ? s : (dir.getAxis() == Direction.Axis.Z ? 0 : s);
        // Quad 1: spanned by (v, u1).
        quad(pose, consumer, cx, cy, cz, vx, vy, vz, u1x, u1y, 0, r, g, b, a);
        // Quad 2: spanned by (v, u2).
        quad(pose, consumer, cx, cy, cz, vx, vy, vz, u2x, u2y, u2z, r, g, b, a);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer,
            float cx, float cy, float cz, float vx, float vy, float vz,
            float ux, float uy, float uz, int r, int g, int b, int a) {
        consumer.addVertex(pose, cx - vx - ux, cy - vy - uy, cz - vz - uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx - vx + ux, cy - vy + uy, cz - vz + uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx + vx + ux, cy + vy + uy, cz + vz + uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx + vx - ux, cy + vy - uy, cz + vz - uz).setColor(r, g, b, a);
        // Back face so the packet is visible from both sides.
        consumer.addVertex(pose, cx + vx - ux, cy + vy - uy, cz + vz - uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx + vx + ux, cy + vy + uy, cz + vz + uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx - vx + ux, cy - vy + uy, cz - vz + uz).setColor(r, g, b, a);
        consumer.addVertex(pose, cx - vx - ux, cy - vy - uy, cz - vz - uz).setColor(r, g, b, a);
    }
}
