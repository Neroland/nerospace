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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModDimensionTypes;
import za.co.neroland.nerospace.solar.SolarPanelBlock;
import za.co.neroland.nerospace.solar.SolarPanelBlockEntity;
import za.co.neroland.nerospace.solar.SolarTier;

/**
 * Draws the moving solar-panel deck above its static housing model. EVERY tier uses the SAME animation:
 * a deck that pivots on its T-pole and pitches east-west to track the sun. Tier 1 is a single 1×1
 * tracker; Tier 2/3 are N×N multiblocks where each cell renders its own identical tracker, and the
 * edge-to-edge seam joining ({@link SolarPanelRenderState#connect}) merges them into one continuous,
 * lockstep-moving tracking field (all cells read the same world time). On faces touching a power cable
 * or any other energy block (this or another mod), a small connector stub is drawn so the hookup butts
 * up against the cable arm with no blank gap ({@link SolarPanelRenderState#connector}).
 */
public class SolarPanelRenderer
        implements BlockEntityRenderer<SolarPanelBlockEntity, SolarPanelRenderState> {

    /** Pivot height = the cross-bar top (the T-pole). */
    private static final float POLE_TOP = 9.0F / 16.0F;
    /** Deck thickness: a real 1px slab. */
    private static final float THICK = 1.0F / 16.0F;
    /** Max east-west tracking tilt; capped so the deck clears the torque tube. */
    private static final float MAX_TILT = 40.0F;
    /** Half-extent of a deck edge with no neighbour (thin frame gap) / touching a neighbour. */
    private static final float INSET = 0.46F;
    private static final float EDGE = 0.5F;
    /** Connector stub cross-section (4..12px) — matches the cable arm so the joint reads as continuous. */
    private static final float CONN_LO = 4.0F / 16.0F;
    private static final float CONN_HI = 12.0F / 16.0F;
    /** How far the connector reaches in from a face (4px) to meet the cable arm at the shared face. */
    private static final float CONN_REACH = 4.0F / 16.0F;

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
        // Every cell renders its own tracker (Tier 1 animation at every tier) — no anchor-only branch.

        float openness;
        float track;
        if (level.dimensionTypeRegistration().is(ModDimensionTypes.SPACE)) {
            openness = 1.0F;  // permanent sun in orbit / on an airless moon
            track = 0.0F;
        } else {
            long tod = level.getOverworldClockTime() % 24000L; // 0 sunrise, 6000 noon, 18000 midnight
            float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
            openness = Mth.clamp((sun + 0.05F) / 0.3F, 0.0F, 1.0F); // eases to 0 at night
            track = (float) ((tod - 6000L) / 24000.0) * 360.0F; // -90 sunrise .. 0 noon .. +90 sunset
        }
        // East-west sun tracking for ALL tiers; cells move in lockstep on the same clock.
        state.angle = openness * Mth.clamp(track, -MAX_TILT, MAX_TILT);

        SolarTier tier = panel.tier();
        BlockPos pos = panel.getBlockPos();
        state.connect[0] = sameTier(level, pos.relative(Direction.NORTH), tier);
        state.connect[1] = sameTier(level, pos.relative(Direction.EAST), tier);
        state.connect[2] = sameTier(level, pos.relative(Direction.SOUTH), tier);
        state.connect[3] = sameTier(level, pos.relative(Direction.WEST), tier);

        // Connector stubs: any horizontal neighbour exposing an energy capability that ISN'T another
        // solar panel — a Nerospace universal cable, a battery/machine, or any other mod's power cable.
        state.connector[0] = energyHookup(level, pos.relative(Direction.NORTH), Direction.NORTH);
        state.connector[1] = energyHookup(level, pos.relative(Direction.EAST), Direction.EAST);
        state.connector[2] = energyHookup(level, pos.relative(Direction.SOUTH), Direction.SOUTH);
        state.connector[3] = energyHookup(level, pos.relative(Direction.WEST), Direction.WEST);
    }

    private static boolean sameTier(Level level, BlockPos pos, SolarTier tier) {
        return level.getBlockEntity(pos) instanceof SolarPanelBlockEntity neighbour && neighbour.tier() == tier;
    }

    /**
     * True when {@code pos} (the neighbour on {@code face}) accepts/provides energy and is not itself a
     * solar panel — i.e. a power cable or machine to hook up to. Capability-based, so it lights up for
     * any mod's energy block, making the connector "dynamic for all mods that have power cables".
     */
    private static boolean energyHookup(Level level, BlockPos pos, Direction face) {
        if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity) {
            return false; // don't grow a port between two adjacent panels
        }
        return Capabilities.Energy.BLOCK.getCapability(level, pos, null, null, face.getOpposite()) != null;
    }

    @Override
    public void submit(SolarPanelRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = state.lightCoords;
        Identifier texture = Identifier.fromNamespaceAndPath(
                Nerospace.MODID, "textures/block/solar_panel_t" + state.tier + ".png");

        // A centred deck on the T-pole, pitching east-west to follow the sun. Every cell (T1 and each
        // cell of a T2/T3 multiblock) draws this identically; the seam insets join neighbours edge-to-edge.
        float we = state.connect[3] ? EDGE : INSET;
        float ee = state.connect[1] ? EDGE : INSET;
        float no = state.connect[0] ? EDGE : INSET;
        float so = state.connect[2] ? EDGE : INSET;
        poseStack.pushPose();
        poseStack.translate(0.5F, POLE_TOP, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, consumer) -> box(consumer, pose, light,
                        -we, -THICK / 2.0F, -no, ee, THICK / 2.0F, so,
                        -we + 0.5F, -no + 0.5F, ee + 0.5F, so + 0.5F));
        poseStack.popPose();

        // Power connector stubs (drawn in block space, no deck rotation) on the `_base` housing sprite.
        if (state.connector[0] || state.connector[1] || state.connector[2] || state.connector[3]) {
            Identifier baseTexture = Identifier.fromNamespaceAndPath(
                    Nerospace.MODID, "textures/block/solar_panel_t" + state.tier + "_base.png");
            var rt = RenderTypes.entityCutout(baseTexture);
            if (state.connector[0]) { // NORTH (−Z)
                connector(collector, poseStack, rt, light, CONN_LO, CONN_LO, 0.0F, CONN_HI, CONN_HI, CONN_REACH);
            }
            if (state.connector[2]) { // SOUTH (+Z)
                connector(collector, poseStack, rt, light, CONN_LO, CONN_LO, 1.0F - CONN_REACH, CONN_HI, CONN_HI, 1.0F);
            }
            if (state.connector[1]) { // EAST (+X)
                connector(collector, poseStack, rt, light, 1.0F - CONN_REACH, CONN_LO, CONN_LO, 1.0F, CONN_HI, CONN_HI);
            }
            if (state.connector[3]) { // WEST (−X)
                connector(collector, poseStack, rt, light, 0.0F, CONN_LO, CONN_LO, CONN_REACH, CONN_HI, CONN_HI);
            }
        }
    }

    /** A small box stub from the housing out to a face, mapping a centre patch of the base sprite. */
    private static void connector(SubmitNodeCollector collector, PoseStack poseStack,
            RenderType rt, int light,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        collector.order(1).submitCustomGeometry(poseStack, rt,
                (pose, consumer) -> box(consumer, pose, light, x0, y0, z0, x1, y1, z1,
                        0.25F, 0.25F, 0.75F, 0.75F));
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
