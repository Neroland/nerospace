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
 * Draws the moving solar-panel deck above its static housing model: a 1px-thick photovoltaic slab
 * hinged along its NORTH edge at the housing top. It tilts up to face the sky during the day and folds
 * flat onto the housing at night — the hinge means it only ever rotates UP, so it never dips into the
 * base or ground (no clipping). Every panel reads the SAME world time, so a whole array opens and folds
 * in lockstep, and connected same-tier neighbours extend their decks edge-to-edge into one surface.
 */
public class SolarPanelRenderer
        implements BlockEntityRenderer<SolarPanelBlockEntity, SolarPanelRenderState> {

    /** Pivot height = centre of the cross-bar (the T-pole top); the deck tilts about it on the post. */
    private static final float POLE_TOP = 9.0F / 16.0F;
    /** Deck thickness: a real 1px slab (not a zero-width plane). */
    private static final float THICK = 1.0F / 16.0F;
    /** Max tracking tilt either side of flat; capped so the deck's dip stays clear of the torque tube. */
    private static final float MAX_TILT = 40.0F;
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

        // Sun tracking: the deck pitches to follow the sun's east-west arc (flat at noon, tilted toward
        // the low sun at dawn/dusk), scaled by openness so it eases back to flat and parks at night.
        float openness;
        float track;
        if (level.dimensionTypeRegistration().is(ModDimensionTypes.SPACE)) {
            openness = 1.0F;  // permanent sun in orbit / on an airless moon
            track = 0.0F;     // no celestial cycle: rest facing straight up
        } else {
            long tod = level.getOverworldClockTime() % 24000L; // 0 sunrise, 6000 noon, 18000 midnight
            float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
            openness = Mth.clamp((sun + 0.05F) / 0.3F, 0.0F, 1.0F); // eases to 0 at night
            track = (float) ((tod - 6000L) / 24000.0) * 360.0F; // -90 sunrise .. 0 noon .. +90 sunset
        }
        state.angle = openness * Mth.clamp(track, -MAX_TILT, MAX_TILT);

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
        float we = state.connect[3] ? EDGE : INSET; // west  (-X)
        float ee = state.connect[1] ? EDGE : INSET; // east  (+X)
        float no = state.connect[0] ? EDGE : INSET; // north (-Z)
        float so = state.connect[2] ? EDGE : INSET; // south (+Z)
        Identifier texture = Identifier.fromNamespaceAndPath(
                Nerospace.MODID, "textures/block/solar_panel_t" + state.tier + ".png");

        poseStack.pushPose();
        // Pivot on the cross-bar; rotating about Z pitches the deck east-west to follow the sun.
        poseStack.translate(0.5F, POLE_TOP, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        float w = we;
        float e = ee;
        float n = no;
        float s = so;
        int light = state.lightCoords;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, consumer) -> drawDeck(pose, consumer, w, e, n, s, light));
        poseStack.popPose();
    }

    /** A 1px-thick deck box centred on the pivot: x in [-w,e], z in [-n,s], y straddling 0. */
    private static void drawDeck(PoseStack.Pose pose, VertexConsumer consumer,
            float w, float e, float n, float s, int light) {
        float x0 = -w;
        float x1 = e;
        float y0 = -THICK / 2.0F;
        float y1 = THICK / 2.0F;
        float z0 = -n;
        float z1 = s;
        // Six faces, each double-sided so the slab shows from any angle through the cutout's culling.
        face(consumer, pose, light, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0);  // top
        face(consumer, pose, light, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0); // bottom
        face(consumer, pose, light, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1); // north
        face(consumer, pose, light, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1);  // south
        face(consumer, pose, light, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0); // west
        face(consumer, pose, light, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0);  // east
    }

    /** Emit a quad both ways (front with the given normal, back reversed) so it's visible from both sides. */
    private static void face(VertexConsumer consumer, PoseStack.Pose pose, int light,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float nx, float ny, float nz) {
        vertex(consumer, pose, ax, ay, az, light, nx, ny, nz);
        vertex(consumer, pose, bx, by, bz, light, nx, ny, nz);
        vertex(consumer, pose, cx, cy, cz, light, nx, ny, nz);
        vertex(consumer, pose, dx, dy, dz, light, nx, ny, nz);
        vertex(consumer, pose, dx, dy, dz, light, -nx, -ny, -nz);
        vertex(consumer, pose, cx, cy, cz, light, -nx, -ny, -nz);
        vertex(consumer, pose, bx, by, bz, light, -nx, -ny, -nz);
        vertex(consumer, pose, ax, ay, az, light, -nx, -ny, -nz);
    }

    /** One vertex; UVs project the PV sprite onto the deck (u from x, v from z along the panel). */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            int light, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(x + 0.5F, z + 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
