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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraft.client.player.LocalPlayer;

import za.co.neroland.nerospace.item.ConfiguratorItem;
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

        // Configurator overlay: face shading + the floating installed-filter indicator.
        LocalPlayer player = Minecraft.getInstance().player;
        state.configuratorHeld = player != null
                && (player.getMainHandItem().getItem() instanceof ConfiguratorItem
                        || player.getOffhandItem().getItem() instanceof ConfiguratorItem);
        state.hasFilterIndicator = false;
        if (state.configuratorHeld) {
            ItemStack indicator = ItemStack.EMPTY;
            for (Direction dir : Direction.values()) {
                ItemStack filter = pipe.filterItem(dir);
                state.faceFiltered[dir.get3DDataValue()] = !filter.isEmpty();
                if (indicator.isEmpty()) {
                    indicator = filter;
                }
            }
            if (!indicator.isEmpty()) {
                state.hasFilterIndicator = true;
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                        state.filterIndicator, indicator, ItemDisplayContext.GROUND, pipe.getLevel(), null,
                        (int) pipe.getBlockPos().asLong() + 63);
            }
        }

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
        int rendered = 0;
        for (int i = 0; i < items.size() && rendered < MAX_RENDERED_ITEMS; i++) {
            TravellingItem item = items.get(i);
            if (!item.isParked()) {
                item.advance(dt * step * 0.999F); // hold just shy of 1.0 until the server expires it
            }
            if (!item.isVisible()) {
                continue; // path-staggered packet that hasn't entered this segment yet
            }
            UniversalPipeRenderState.TravellingItemEntry entry = state.entry(rendered++);

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
            // Spin phase from the packet's route (stable), not its list index (which shifts as
            // earlier packets expire and made items visibly snap).
            int spinSeed = item.from().get3DDataValue() * 61
                    + (to == null ? 3 : to.get3DDataValue()) * 97;
            entry.spin = (now * 4.0F + spinSeed) % 360.0F;

            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                    entry.renderState, item.stack(), ItemDisplayContext.GROUND, pipe.getLevel(), null,
                    (int) pipe.getBlockPos().asLong() + i);
        }
        state.visibleItems = rendered;

        for (int slot = 0; slot < pipe.getContainerSize() && state.visibleItems < MAX_RENDERED_ITEMS; slot++) {
            ItemStack stack = pipe.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int i = state.visibleItems++;
            UniversalPipeRenderState.TravellingItemEntry entry = state.entry(i);
            // Lane by SLOT (stable) — deriving it from the render index made buffered items hop
            // lanes whenever a travelling packet expired.
            entry.x = 0.5F + ((slot % 2) == 0 ? -0.12F : 0.12F);
            entry.y = 0.5F + (slot / 2) * 0.12F;
            entry.z = 0.5F;
            entry.spin = (now * 2.0F + slot * 45.0F) % 360.0F;
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                    entry.renderState, stack, ItemDisplayContext.GROUND, pipe.getLevel(), null,
                    (int) pipe.getBlockPos().asLong() + slot + 31);
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

        // Floating installed-filter indicator (Configurator in hand): a slow-spinning, bobbing
        // copy of the face's filter item above the pipe — filtered segments read at a glance.
        if (state.hasFilterIndicator) {
            poseStack.pushPose();
            float bob = Mth.sin(state.time * 0.12F) * 0.04F;
            poseStack.translate(0.5F, 1.05F + bob, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees((state.time * 2.5F) % 360.0F));
            poseStack.scale(0.45F, 0.45F, 0.45F);
            state.filterIndicator.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // Configurator face shading: normal alpha blending (debugQuads) — the additive lightning
        // blend washed the colours out to pastel against bright backgrounds.
        if (state.configuratorHeld) {
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                    (pose, consumer) -> renderFaceShading(state, pose, consumer));
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

    /**
     * Configurator face shading, hugging the PHYSICAL pipe model (colours shared with the config
     * GUI via {@link PipeFaceColors}): a colour plate on each hub face, plus — where the arm is
     * connected — an opaque colour sleeve wrapped around that arm out to the block edge. Faces
     * with an installed filter render brighter. This tints the pipe itself rather than boxing it
     * in, so the tube stays visible (the old full-block overlay hid the pipe entirely).
     */
    private static void renderFaceShading(UniversalPipeRenderState state, PoseStack.Pose pose,
            VertexConsumer consumer) {
        for (Direction dir : Direction.values()) {
            int d = dir.get3DDataValue();
            int color = PipeFaceColors.ARGB[d];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = state.faceFiltered[d] ? 240 : 170;

            // The two axes perpendicular to the face normal:
            // X-normal -> P1=(0,1,0), P2=(0,0,1); Y-normal -> P1=(1,0,0), P2=(0,0,1);
            // Z-normal -> P1=(1,0,0), P2=(0,1,0).
            float p1x = dir.getAxis() == Direction.Axis.X ? 0 : 1;
            float p1y = dir.getAxis() == Direction.Axis.X ? 1 : 0;
            float p2y = dir.getAxis() == Direction.Axis.Z ? 1 : 0;
            float p2z = dir.getAxis() == Direction.Axis.Z ? 0 : 1;

            // Colour plate sitting just off the 6x6 hub face (hub half-size = 0.1875).
            float plate = 0.192F;
            float ps = 0.175F;
            quad(pose, consumer,
                    0.5F + dir.getStepX() * plate, 0.5F + dir.getStepY() * plate, 0.5F + dir.getStepZ() * plate,
                    p1x * ps, p1y * ps, 0, 0, p2y * ps, p2z * ps, r, g, b, a);

            if (!state.connections[d]) {
                continue;
            }
            // Sleeve around the connected arm: four walls from the hub edge to the block face,
            // wrapped just outside the arm's cross-section.
            float mid = 0.346F;   // midpoint of the arm run (0.192 .. 0.5)
            float half = 0.154F;  // half-length of the run
            float sh = 0.20F;     // sleeve half-width (arm is ~0.1875)
            float cx = 0.5F + dir.getStepX() * mid;
            float cy = 0.5F + dir.getStepY() * mid;
            float cz = 0.5F + dir.getStepZ() * mid;
            float ax = dir.getStepX() * half;
            float ay = dir.getStepY() * half;
            float az = dir.getStepZ() * half;
            // Walls offset along ±P1, spanned by (arm axis, P2).
            quad(pose, consumer, cx + p1x * sh, cy + p1y * sh, cz,
                    ax, ay, az, 0, p2y * sh, p2z * sh, r, g, b, a);
            quad(pose, consumer, cx - p1x * sh, cy - p1y * sh, cz,
                    ax, ay, az, 0, p2y * sh, p2z * sh, r, g, b, a);
            // Walls offset along ±P2, spanned by (arm axis, P1).
            quad(pose, consumer, cx, cy + p2y * sh, cz + p2z * sh,
                    ax, ay, az, p1x * sh, p1y * sh, 0, r, g, b, a);
            quad(pose, consumer, cx, cy - p2y * sh, cz - p2z * sh,
                    ax, ay, az, p1x * sh, p1y * sh, 0, r, g, b, a);
        }
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
                    // Ease the pulse in and out over its run (sin envelope) so it fades at the arm
                    // ends instead of popping into and out of existence — the visible "jitter".
                    int alpha = (int) (170 * Mth.sin(phase * (float) Math.PI));
                    if (alpha <= 10) {
                        continue;
                    }
                    float along = 0.08F + 0.42F * phase; // centre -> face
                    float cx = 0.5F + dir.getStepX() * along;
                    float cy = 0.5F + dir.getStepY() * along + (l - 1) * 0.09F * (dir.getAxis().isHorizontal() ? 1 : 0);
                    float cz = 0.5F + dir.getStepZ() * along;
                    crossQuads(pose, consumer, dir, cx, cy, cz, r, g, b, alpha);
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
