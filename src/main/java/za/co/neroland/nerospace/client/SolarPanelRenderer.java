package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModDimensionTypes;
import za.co.neroland.nerospace.solar.SolarPanelBlockEntity;
import za.co.neroland.nerospace.solar.SolarTier;

/**
 * Draws the moving solar-panel surface above its (static) housing model: a textured deck that tilts to
 * track the sun across the day and folds flat at night. Every panel reads the SAME world time, so a
 * whole array stays in lockstep, and connected same-tier neighbours extend their decks edge-to-edge so
 * a row joins into one continuous, seamless surface.
 */
public class SolarPanelRenderer
        implements BlockEntityRenderer<SolarPanelBlockEntity, SolarPanelRenderState> {

    /** Hinge height (just above the 4px housing) so the tilted deck clears the block top. */
    private static final float PIVOT_Y = 0.27F;
    /** Tracking tilt is clamped so the array stays low-profile (a tilting field, not standing poles). */
    private static final float MAX_TILT = 35.0F;
    /** Half-extent of a deck edge that has NO neighbour (leaves a thin frame gap between arrays). */
    private static final float INSET = 0.46F;
    /** Half-extent of a deck edge that DOES touch a same-tier neighbour (meets at the block border). */
    private static final float EDGE = 0.5F;

    @Override
    public SolarPanelRenderState createRenderState() {
        return new SolarPanelRenderState();
    }

    @Override
    public void extractRenderState(SolarPanelBlockEntity panel, SolarPanelRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(panel, state, partialTick, cameraPos, breakProgress);
        Level level = panel.getLevel();
        if (level == null) {
            return;
        }
        state.tier = panel.tier().tier;

        boolean space = level.dimensionTypeRegistration().is(ModDimensionTypes.SPACE);
        if (space) {
            // Permanent sun in orbit / on an airless moon: stay open, facing up.
            state.angle = 0.0F;
        } else {
            long tod = level.getOverworldClockTime() % 24000L; // 0 sunrise, 6000 noon, 18000 midnight
            float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
            float openness = Mth.clamp((sun + 0.02F) / 0.25F, 0.0F, 1.0F); // 0 at night -> folds flat
            float deg = (float) ((tod - 6000L) / 24000.0) * 360.0F; // 0 noon, -90 sunrise, +90 sunset
            state.angle = openness * Mth.clamp(deg, -MAX_TILT, MAX_TILT);
        }

        SolarTier tier = panel.tier();
        BlockPos pos = panel.getBlockPos();
        state.connect[0] = sameTier(level, pos.relative(Direction.NORTH), tier);
        state.connect[1] = sameTier(level, pos.relative(Direction.EAST), tier);
        state.connect[2] = sameTier(level, pos.relative(Direction.SOUTH), tier);
        state.connect[3] = sameTier(level, pos.relative(Direction.WEST), tier);
    }

    private static boolean sameTier(Level level, BlockPos pos, SolarTier tier) {
        return level.getBlockEntity(pos) instanceof SolarPanelBlockEntity neighbour && neighbour.tier() == tier;
    }

    @Override
    public void submit(SolarPanelRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        Identifier texture = Identifier.fromNamespaceAndPath(
                Nerospace.MODID, "textures/block/solar_panel_t" + state.tier + ".png");
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, consumer) -> drawDeck(state, pose, consumer));
        poseStack.popPose();
    }

    private static void drawDeck(SolarPanelRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float no = state.connect[0] ? EDGE : INSET; // north (-Z)
        float ee = state.connect[1] ? EDGE : INSET; // east  (+X)
        float so = state.connect[2] ? EDGE : INSET; // south (+Z)
        float we = state.connect[3] ? EDGE : INSET; // west  (-X)
        int light = state.lightCoords;

        // Front face (normal +Y). Back face (normal -Y, reversed winding) so the steeply-tilted deck is
        // visible from below too. The render type is no-cull, so both always draw.
        vertex(consumer, pose, -we, -no, light, 1.0F);
        vertex(consumer, pose, ee, -no, light, 1.0F);
        vertex(consumer, pose, ee, so, light, 1.0F);
        vertex(consumer, pose, -we, so, light, 1.0F);

        vertex(consumer, pose, -we, so, light, -1.0F);
        vertex(consumer, pose, ee, so, light, -1.0F);
        vertex(consumer, pose, ee, -no, light, -1.0F);
        vertex(consumer, pose, -we, -no, light, -1.0F);
    }

    /**
     * One deck vertex. {@code x}/{@code z} are pivot-local half-offsets (the block square is
     * [-0.5,0.5]); UVs are derived from them so each panel shows the full sprite. {@code ny} is the
     * face normal's Y sign.
     */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, int light,
            float ny) {
        consumer.addVertex(pose, x, 0.0F, z)
                .setColor(255, 255, 255, 255)
                .setUv(x + 0.5F, z + 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, ny, 0.0F);
    }
}
