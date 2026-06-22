package za.co.neroland.nerospace.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.pipe.TravellingItem;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/**
 * Renders the item stacks physically travelling through a Universal Pipe segment: each packet slides
 * from the face it entered, through the centre, to the face it exits, spinning as it goes. The
 * translucent tube itself is the (static, batched) block model; this renderer only draws what is
 * MOVING, so idle pipes cost nothing.
 *
 * <p>Cross-loader port: the standalone mod's pipe renderer, reduced to the travelling-item lane (the
 * coloured energy/fluid/gas stream pulses are deferred — they need the per-face connection blockstates
 * the multiloader pipe's single-cube model doesn't have yet). Uses the vanilla BER submission API, the
 * item-model resolver, and the {@link ClientBlockEntityRenderers} seam — all proven cross-version by the
 * Star Guide hologram and solar deck renderers.</p>
 */
public class UniversalPipeRenderer
        implements BlockEntityRenderer<UniversalPipeBlockEntity, UniversalPipeRenderState> {

    private static final int MAX_RENDERED_ITEMS = UniversalPipeBlockEntity.MAX_TRAVELLING;

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
            return;
        }
        float now = pipe.getLevel().getGameTime() + partialTick;
        state.time = now;

        // Advance progress locally for smooth motion between the (throttled) server syncs.
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

    @Override
    public void submit(UniversalPipeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        for (int i = 0; i < state.visibleItems; i++) {
            UniversalPipeRenderState.TravellingItemEntry entry = state.items.get(i);
            poseStack.pushPose();
            poseStack.translate(entry.x, entry.y - 0.12F, entry.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(entry.spin));
            poseStack.scale(0.55F, 0.55F, 0.55F);
            entry.renderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
