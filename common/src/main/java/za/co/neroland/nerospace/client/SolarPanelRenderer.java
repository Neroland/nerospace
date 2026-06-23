package za.co.neroland.nerospace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.machine.SolarPanelBlock;
import za.co.neroland.nerospace.machine.SolarPanelBlockEntity;

/**
 * Draws the moving solar-panel deck above its static housing model. Every tier uses the SAME animation:
 * a deck that pitches east-west to track the sun and folds flat at night. Tier 1 is a single 1×1 deck on
 * the model's pole; Tier 2/3 are N×N multiblocks drawn as ONE big deck on a central mast (only the
 * anchor cell renders it), with the tilt scaled down as the footprint grows so the wider deck's
 * descending edge never dips below the housings.
 *
 * <p>Cross-loader port: the standalone renderer minus the per-face connector stubs (they needed
 * client-side energy-cap queries — dropped for this slice). The deck angle is driven by the real
 * day-of-time clock ({@link #dayOfTime}), falling back to the game clock where the data-driven clock
 * isn't loaded, and the airless 2× "permanent sun" case keys off
 * {@link SolarPanelBlockEntity#isAirless}.</p>
 */
public class SolarPanelRenderer
        implements BlockEntityRenderer<SolarPanelBlockEntity, SolarPanelRenderState> {

    /** Pivot height = the cross-bar top (the T-pole). */
    private static final float POLE_TOP = 9.0F / 16.0F;
    /** Static housing top (3px) — the floor the deck must stay clear of when tilted. */
    private static final float HOUSING_TOP = 3.0F / 16.0F;
    /** Deck thickness: a real 1px slab. */
    private static final float THICK = 1.0F / 16.0F;
    /** Max east-west tracking tilt; capped so the deck clears the torque tube. */
    private static final float MAX_TILT = 40.0F;
    /** Central-mast dimensions for the multiblock deck. */
    private static final float MAST_HALF = 1.0F / 16.0F;
    private static final float POST_TOP = 7.0F / 16.0F;
    private static final float TUBE_TOP = 8.0F / 16.0F;
    private static final float TUBE_HALF = 4.0F / 16.0F;

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
        state.footprint = panel.tier().footprint;
        state.anchor = panel.getBlockState().getValue(SolarPanelBlock.ANCHOR);

        float openness;
        float track;
        if (SolarPanelBlockEntity.isAirless(level)) {
            openness = 1.0F; // permanent sun in orbit / on an airless moon
            track = 0.0F;
        } else {
            // Day-of-time for sun tracking. 0 sunrise, 6000 noon, 18000 midnight.
            long tod = dayOfTime(level);
            float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
            openness = Mth.clamp((sun + 0.05F) / 0.3F, 0.0F, 1.0F); // eases to 0 at night → folds flat
            track = (float) ((tod - 6000L) / 24000.0) * 360.0F; // -90 sunrise .. 0 noon .. +90 sunset
        }
        state.angle = openness * Mth.clamp(track, -MAX_TILT, MAX_TILT);
    }

    @Override
    public void submit(SolarPanelRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        // Full-bright: the PV deck reads as a lit/reflective surface, and (like the quarry renderer)
        // we can't rely on state.lightCoords being populated for submitted custom geometry — when it is
        // left at 0 the deck draws nearly black against the dark housing and looks like it's missing.
        int light = 0x00F000F0;
        Identifier texture = Identifier.fromNamespaceAndPath(
                NerospaceCommon.MOD_ID, "textures/block/solar_panel" + tierSuffix(state.tier) + ".png");

        if (state.footprint > 1) {
            if (!state.anchor) {
                return; // one big panel per multiblock — only the anchor (min-corner) draws the deck
            }
            submitMultiblockDeck(state, poseStack, collector, light, texture);
            return;
        }

        // Tier 1: a centred 1×1 deck on the model's T-pole, pitching east-west to follow the sun.
        poseStack.pushPose();
        poseStack.translate(0.5F, POLE_TOP, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, consumer) -> box(consumer, pose, light,
                        -0.5F, -THICK / 2.0F, -0.5F, 0.5F, THICK / 2.0F, 0.5F,
                        0.0F, 0.0F, 1.0F, 1.0F));
        poseStack.popPose();
    }

    /**
     * Tier 2/3: ONE big N×N deck on a central mast, pitching east-west like Tier 1, drawn in the anchor's
     * (min-corner) space. The tilt is reduced as the footprint grows ({@link #maxTiltFor}) so the wider
     * deck's descending edge clears the housings.
     */
    private void submitMultiblockDeck(SolarPanelRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, int light, Identifier texture) {
        int n = state.footprint;
        float centre = n / 2.0F;
        float cap = maxTiltFor(n);
        float angle = Mth.clamp(state.angle, -cap, cap);

        // Central mast (post + N-S torque tube) on the `_base` sprite, supporting the deck at the centre.
        Identifier baseTexture = Identifier.fromNamespaceAndPath(
                NerospaceCommon.MOD_ID, "textures/block/solar_panel" + tierSuffix(state.tier) + "_base.png");
        RenderType baseRt = RenderTypes.entityCutout(baseTexture);
        collector.order(1).submitCustomGeometry(poseStack, baseRt, (pose, consumer) -> {
            box(consumer, pose, light, centre - MAST_HALF, HOUSING_TOP, centre - MAST_HALF,
                    centre + MAST_HALF, POST_TOP, centre + MAST_HALF, 0.25F, 0.25F, 0.75F, 0.75F);
            box(consumer, pose, light, centre - MAST_HALF, POST_TOP, centre - TUBE_HALF,
                    centre + MAST_HALF, TUBE_TOP, centre + TUBE_HALF, 0.25F, 0.25F, 0.75F, 0.75F);
        });

        // The single deck, pivoting east-west about the central mast; fills the footprint edge-to-edge.
        float half = centre;
        poseStack.pushPose();
        poseStack.translate(centre, POLE_TOP, centre);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, consumer) -> box(consumer, pose, light,
                        -half, -THICK / 2.0F, -half, half, THICK / 2.0F, half,
                        0.0F, 0.0F, 1.0F, 1.0F));
        poseStack.popPose();
    }

    /** Once a world's data-driven clock markers are found missing, stop calling the throwing path. */
    private static boolean clockAvailable = true;

    /**
     * Real day-of-time (0..23999) for sun tracking. Prefers the 26.x data-driven clock
     * ({@code getDefaultClockTime()}), which is sun-accurate and matches the standalone — but that throws
     * when a world's clock time-markers aren't loaded (seen in some dev/data setups: "Time marker ...
     * does not exist for clock minecraft:overworld"). We try it once and fall back permanently to the
     * free-running game clock if it's unavailable, so the deck still animates without erroring.
     */
    private static long dayOfTime(Level level) {
        if (clockAvailable) {
            try {
                return level.getDefaultClockTime() % 24000L;
            } catch (RuntimeException ex) {
                clockAvailable = false;
            }
        }
        return level.getGameTime() % 24000L;
    }

    /** Texture suffix per tier: T1 reuses the base "solar_panel" sprite, T2/T3 use "_t2"/"_t3". */
    private static String tierSuffix(int tier) {
        return tier <= 1 ? "" : "_t" + tier;
    }

    /**
     * The east-west tilt cap for a footprint. Tier 1 gets the full {@link #MAX_TILT}; wider multiblock
     * decks are capped so the descending edge's underside never drops below the housing top.
     */
    private static float maxTiltFor(int footprint) {
        if (footprint <= 1) {
            return MAX_TILT;
        }
        double half = footprint / 2.0;
        double sin = (POLE_TOP - HOUSING_TOP - THICK / 2.0F) / half;
        return (float) Math.toDegrees(Math.asin(Math.min(1.0, sin)));
    }

    /**
     * A 1px-thick textured deck box. The PV sprite maps across the top/bottom by {@code x -> [u0,u1]} and
     * {@code z -> [v0,v1]}; every face is double-sided so it shows from any angle through the cutout cull.
     */
    private static void box(VertexConsumer c, PoseStack.Pose pose, int light,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float u0, float v0, float u1, float v1) {
        // top (+Y) / bottom (-Y)
        face(c, pose, light, 0, 1, 0, x0, y1, z0, u0, v0, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0);
        face(c, pose, light, 0, -1, 0, x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1);
        // north (-Z) / south (+Z)
        face(c, pose, light, 0, 0, -1, x0, y0, z0, u0, v0, x0, y1, z0, u0, v0, x1, y1, z0, u1, v0, x1, y0, z0, u1, v0);
        face(c, pose, light, 0, 0, 1, x1, y0, z1, u1, v1, x1, y1, z1, u1, v1, x0, y1, z1, u0, v1, x0, y0, z1, u0, v1);
        // west (-X) / east (+X)
        face(c, pose, light, -1, 0, 0, x0, y0, z1, u0, v1, x0, y1, z1, u0, v1, x0, y1, z0, u0, v0, x0, y0, z0, u0, v0);
        face(c, pose, light, 1, 0, 0, x1, y0, z0, u1, v0, x1, y1, z0, u1, v0, x1, y1, z1, u1, v1, x1, y0, z1, u1, v1);
    }

    /** Emit a quad both ways (front with the given normal, back reversed) so it shows from both sides. */
    private static void face(VertexConsumer c, PoseStack.Pose pose, int light, float nx, float ny, float nz,
            float ax, float ay, float az, float au, float av,
            float bx, float by, float bz, float bu, float bv,
            float cx, float cy, float cz, float cu, float cv,
            float dx, float dy, float dz, float du, float dv) {
        vertex(c, pose, ax, ay, az, au, av, light, nx, ny, nz);
        vertex(c, pose, bx, by, bz, bu, bv, light, nx, ny, nz);
        vertex(c, pose, cx, cy, cz, cu, cv, light, nx, ny, nz);
        vertex(c, pose, dx, dy, dz, du, dv, light, nx, ny, nz);
        vertex(c, pose, dx, dy, dz, du, dv, light, -nx, -ny, -nz);
        vertex(c, pose, cx, cy, cz, cu, cv, light, -nx, -ny, -nz);
        vertex(c, pose, bx, by, bz, bu, bv, light, -nx, -ny, -nz);
        vertex(c, pose, ax, ay, az, au, av, light, -nx, -ny, -nz);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int light, float nx, float ny, float nz) {
        c.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
