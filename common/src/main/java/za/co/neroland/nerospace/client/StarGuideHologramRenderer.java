package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.progression.StarGuideBlockEntity;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Star Guide pedestal hologram: a slowly spinning, bobbing icon floating above a LOADED pedestal
 * showing the nearest player's next incomplete progression step (server-computed, BE-synced) — or the
 * Star Guide Book itself once everything is complete.
 *
 * <p>Cross-loader port: vanilla BER submission API; identical to the standalone mod. Registered via the
 * {@link ClientBlockEntityRenderers} seam.</p>
 */
public class StarGuideHologramRenderer
        implements BlockEntityRenderer<StarGuideBlockEntity, StarGuideHologramRenderState> {

    /** Packed full-bright light coords (same constant the RocketRenderer uses for its glow). */
    private static final int FULL_BRIGHT = 0x00F000F0;

    @Override
    public StarGuideHologramRenderState createRenderState() {
        return new StarGuideHologramRenderState();
    }

    @Override
    public void extractRenderState(StarGuideBlockEntity guide, StarGuideHologramRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(guide, state, partialTick, cameraPos, breakProgress);
        Level level = guide.getLevel();
        if (!guide.hasBook() || level == null) {
            state.visible = false;
            return;
        }
        state.visible = true;
        float now = level.getGameTime() + partialTick;
        state.spin = (now * 1.5F) % 360.0F;
        state.bob = Mth.sin(now * 0.06F) * 0.05F;

        ItemStack icon = guide.getHologram();
        if (icon.isEmpty()) {
            icon = new ItemStack(ModItems.STAR_GUIDE_BOOK.get()); // all complete (or no player near)
        }
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                NerospaceCommon.requireNonNull(state.renderState), icon, ItemDisplayContext.GROUND, level, null,
                (int) guide.getBlockPos().asLong());
    }

    @Override
    public void submit(StarGuideHologramRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.visible) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.35F + state.bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        // Emissive: a hologram is its own light source, so render full-bright instead of with the
        // pedestal's world light (it read pitch-black at night).
        state.renderState.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
