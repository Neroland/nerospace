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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryRegion;

/**
 * Draws the quarry's working machinery on top of the (solid, block-based) frame ring — a gantry crane
 * that rides the frame and a spinning drill head that tracks the dig column — as real textured geometry
 * via {@link RenderTypes#entityCutout(Identifier)} (depth-correct, cull-off).
 *
 * <p>Cross-loader port of the standalone mod's quarry renderer. The gantry + bit geometry is verbatim;
 * the only simplification is the head MOTION: it eases toward the current dig cell
 * ({@code region.columnPos(cursor, currentY)}, server-synced via the BE update tag) instead of replaying
 * the root's per-block mined-history time-lerp — so the head still glides down the pit with the dig
 * without the extra synced state. Uses the proven submission API + {@link ClientBlockEntityRenderers} seam.</p>
 */
public class QuarryControllerRenderer
        implements BlockEntityRenderer<QuarryControllerBlockEntity, QuarryControllerRenderState> {

    /** Purple strut texture for the moving gantry (bridge / trucks / carriage / shaft). */
    private static final Identifier GANTRY_TEX =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/quarry_gantry.png");
    /** Red/steel strut texture for the drill head. */
    private static final Identifier DRILL_TEX =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/block/quarry_drill.png");
    /** Full-bright light (the frame is emissive); per-face lighting still shades by normal. */
    private static final int FULL_BRIGHT = 0x00F000F0;
    /** Top of the bit (chuck collar top, local Y) — the support shaft meets it here. */
    private static final float BIT_TOP = 0.72F;
    /** Spike apex (local Y); the head is placed so this sits on the mined block's top face. */
    private static final float TIP_Y = -0.6F;

    @Override
    public QuarryControllerRenderState createRenderState() {
        return new QuarryControllerRenderState();
    }

    @Override
    public int getViewDistance() {
        return 512; // max-size claims can put the gantry far from the controller block
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The gantry + drill render far from the controller block, so always render this BE (a "global"
        // block entity): otherwise the renderer is dropped whenever the controller's chunk section is
        // occlusion-culled (e.g. when the camera is down in the pit) and the gantry vanishes. This alone
        // is sufficient on Fabric and Forge (their BE render-state extraction does NOT frustum-cull); see
        // getRenderBoundingBox for the extra NeoForge-only step.
        return true;
    }

    /**
     * Widen the per-BE frustum-cull box to the whole working volume.
     *
     * <p>NeoForge (only) routes block-entity render-state extraction through a frustum-aware overload that
     * culls via {@code renderer.getRenderBoundingBox(be)} → {@code Frustum.isVisible(aabb)}, where the
     * default box is just the single controller block. So the gantry/drill — which draw across the whole
     * claim, often metres from the controller — get dropped the instant the controller leaves the view
     * frustum (look slightly away → the machine vanishes), independent of {@link #shouldRenderOffScreen()}
     * (that only governs section culling, not this frustum test). Returning a box that spans the claim and
     * the dig shaft keeps the assembly visible while any of it is on screen.</p>
     *
     * <p>Cross-loader: this is NeoForge's {@code IBlockEntityRendererExtension} method; vanilla's
     * {@code BlockEntityRenderer} has no such method, so on Fabric and Forge this is simply an unused
     * public method (their dispatchers never call it) — hence no {@code @Override} (it would not resolve
     * there). {@code AABB} is vanilla, so it compiles on all six cells.</p>
     */
    public AABB getRenderBoundingBox(QuarryControllerBlockEntity be) {
        QuarryRegion r = be.renderRegion();
        if (r == null) {
            // Unclaimed / nothing extra drawn — the controller block alone (matches the vanilla default).
            return new AABB(be.getBlockPos());
        }
        // Claim footprint padded by the gantry overhang (it sits ~1 block beyond the frame on X), up to
        // the frame plane and down past any world floor (refY - 512 clears even deep custom worlds). The
        // box only feeds a frustum visibility test, so a generous span just guarantees we never wrongly
        // cull — negligible for a singular machine.
        return new AABB(
                r.minX() - 1, r.refY() - 512, r.minZ() - 1,
                r.maxX() + 2, r.refY() + 3, r.maxZ() + 2);
    }

    @Override
    public boolean shouldRender(QuarryControllerBlockEntity blockEntity, Vec3 cameraPos) {
        return true;
    }

    @Override
    public void extractRenderState(QuarryControllerBlockEntity be, QuarryControllerRenderState s,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, cameraPos, breakProgress);
        QuarryRegion r = be.renderRegion();
        QuarryControllerBlockEntity.State st = be.renderState();
        if (r == null || st == QuarryControllerBlockEntity.State.IDLE || be.getLevel() == null) {
            s.active = false;
            return;
        }
        s.active = true;
        BlockPos p = be.getBlockPos();
        s.x0 = r.minX() - p.getX();
        s.x1 = r.maxX() + 1 - p.getX();
        s.z0 = r.minZ() - p.getZ();
        s.z1 = r.maxZ() + 1 - p.getZ();
        s.topY = r.refY() + 1 - p.getY();
        s.accent = be.tier().accentColor();
        s.mining = st == QuarryControllerBlockEntity.State.MINING;

        double cx = (r.minX() + r.maxX() + 1) / 2.0;
        double cz = (r.minZ() + r.maxZ() + 1) / 2.0;
        double tx;
        double ty;
        double tz;
        if (s.mining) {
            // Head target: the current dig cell. The spike apex sits on the cell's top face.
            BlockPos cell = r.columnPos(be.renderCursor(), be.renderCurrentY());
            tx = cell.getX() + 0.5;
            ty = be.renderCurrentY() + 1.0 - TIP_Y;
            tz = cell.getZ() + 0.5;
        } else {
            // Idle / pre-first-block: hover at the region centre.
            tx = cx;
            ty = r.refY() + 0.5;
            tz = cz;
        }
        long tick = be.getLevel().getGameTime();
        if (!be.dispInit) {
            be.dispX = be.prevDispX = tx;
            be.dispY = be.prevDispY = ty;
            be.dispZ = be.prevDispZ = tz;
            be.dispInit = true;
            be.lastDispTick = tick;
        } else if (tick != be.lastDispTick) {
            // Ease ONCE per tick (FPS-independent), saving the previous-tick position so the render below
            // interpolates across the tick — smooth, jitter-free motion at any frame rate.
            be.lastDispTick = tick;
            be.prevDispX = be.dispX;
            be.prevDispY = be.dispY;
            be.prevDispZ = be.dispZ;
            be.dispX += (tx - be.dispX) * 0.4;
            be.dispY += (ty - be.dispY) * 0.4;
            be.dispZ += (tz - be.dispZ) * 0.4;
        }
        double rx = be.prevDispX + (be.dispX - be.prevDispX) * partialTick;
        double ry = be.prevDispY + (be.dispY - be.prevDispY) * partialTick;
        double rz = be.prevDispZ + (be.dispZ - be.prevDispZ) * partialTick;
        // The gantry/drill otherwise render exactly +1 block on the X axis relative to the frame; nudge
        // the whole assembly (bridge, carriage, shaft and spinning bit all key off hx) back by one.
        s.hx = rx - p.getX() - 1.0;
        s.hy = ry - p.getY();
        s.hz = rz - p.getZ();

        double now = tick + partialTick;
        s.headSpin = (float) ((now * 18.0) % 360.0); // fast spin reads as a working drill
    }

    @Override
    public void submit(QuarryControllerRenderState s, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!s.active) {
            return;
        }
        // Gantry (purple): world-relative, at the base pose.
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GANTRY_TEX),
                (pose, c) -> drawGantry(s, pose, c));
        // Drill head (red): spun around the vertical axis at the dig cell.
        poseStack.pushPose();
        poseStack.translate(s.hx, s.hy, s.hz);
        poseStack.mulPose(Axis.YP.rotationDegrees(s.headSpin));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(DRILL_TEX),
                (pose, c) -> drawBit(pose, c));
        poseStack.popPose();
    }

    /** The gantry crane that rides the frame: bridge + end trucks + carriage + support shaft. */
    private static void drawGantry(QuarryControllerRenderState s, PoseStack.Pose pose, VertexConsumer c) {
        float ty = (float) s.topY;
        float hx = (float) s.hx;
        float hz = (float) s.hz;
        float z0 = (float) s.z0;
        float z1 = (float) s.z1;
        // Bridge beam: spans Z at the head's X, sitting on the frame plane (moves in X with the dig).
        texBeam(c, pose, hx - 0.15F, ty, z0, hx + 0.15F, ty + 0.22F, z1);
        // End trucks: ride the two side rails of the frame.
        texBox(c, pose, hx - 0.22F, ty - 0.05F, z0, hx + 0.22F, ty + 0.28F, z0 + 0.35F);
        texBox(c, pose, hx - 0.22F, ty - 0.05F, z1 - 0.35F, hx + 0.22F, ty + 0.28F, z1);
        // Carriage: the moving drill mount that tracks the dig column.
        texBox(c, pose, hx - 0.26F, ty - 0.06F, hz - 0.26F, hx + 0.26F, ty + 0.30F, hz + 0.26F);
        // Support shaft from the carriage down to the top of the spinning bit.
        texBeam(c, pose, hx - 0.07F, (float) s.hy + BIT_TOP, hz - 0.07F, hx + 0.07F, ty, hz + 0.07F);
    }

    /** The drill head (local space, origin at the head centre, Y up): a detailed frame-textured bit. */
    private static void drawBit(PoseStack.Pose pose, VertexConsumer c) {
        texBox(c, pose, -0.24F, 0.58F, -0.24F, 0.24F, BIT_TOP, 0.24F); // chuck collar
        texBox(c, pose, -0.12F, 0.22F, -0.12F, 0.12F, 0.58F, 0.12F);   // shaft
        texBox(c, pose, -0.22F, 0.05F, -0.22F, 0.22F, 0.22F, 0.22F);   // flared hub
        texBox(c, pose, 0.20F, 0.05F, -0.05F, 0.34F, 0.30F, 0.05F);    // flute +x
        texBox(c, pose, -0.34F, 0.05F, -0.05F, -0.20F, 0.30F, 0.05F);  // flute -x
        texBox(c, pose, -0.05F, 0.05F, 0.20F, 0.05F, 0.30F, 0.34F);    // flute +z
        texBox(c, pose, -0.05F, 0.05F, -0.34F, 0.05F, 0.30F, -0.20F);  // flute -z
        // tapering spike with a glowing tip
        float bw = 0.22F;
        spikeFace(c, pose, -bw, -bw, bw, -bw, 0, 0, -1); // north
        spikeFace(c, pose, bw, -bw, bw, bw, 1, 0, 0);    // east
        spikeFace(c, pose, bw, bw, -bw, bw, 0, 0, 1);    // south
        spikeFace(c, pose, -bw, bw, -bw, -bw, -1, 0, 0); // west
    }

    /** One tapering spike face: two base corners (at y=0.05) up to the apex, base white, tip glowing. */
    private static void spikeFace(VertexConsumer c, PoseStack.Pose pose,
            float ax, float az, float bx, float bz, float nx, float ny, float nz) {
        tv(c, pose, ax, 0.05F, az, 0.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, bx, 0.05F, bz, 1.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, 0.0F, TIP_Y, 0.0F, 0.5F, 1.0F, nx, ny, nz, 255, 176, 230);
        tv(c, pose, 0.0F, TIP_Y, 0.0F, 0.5F, 1.0F, nx, ny, nz, 255, 176, 230);
    }

    // --- textured geometry helpers (entityCutout = QUADS, cull off, depth-correct) -------------

    private static void tv(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, float nx, float ny, float nz, int r, int g, int b) {
        c.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /** A textured quad (4 corners, one normal, UV from (0,0) to (uMax,vMax)), drawn white. */
    private static void quad(VertexConsumer c, PoseStack.Pose pose,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float nx, float ny, float nz, float uMax, float vMax) {
        tv(c, pose, ax, ay, az, 0.0F, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, bx, by, bz, uMax, 0.0F, nx, ny, nz, 255, 255, 255);
        tv(c, pose, cx, cy, cz, uMax, vMax, nx, ny, nz, 255, 255, 255);
        tv(c, pose, dx, dy, dz, 0.0F, vMax, nx, ny, nz, 255, 255, 255);
    }

    /** A textured box; UVs map 1 texture tile per block (dims are assumed &le; ~1, so no wrap needed). */
    private static void texBox(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, dx, dz); // down
        quad(c, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, dx, dz);  // up
        quad(c, pose, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, dx, dy); // north
        quad(c, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, dx, dy);  // south
        quad(c, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, dz, dy); // west
        quad(c, pose, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, dz, dy);  // east
    }

    /** A textured beam: subdivides the longest axis into &le;1-block segments so the texture tiles. */
    private static void texBeam(VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        if (dx >= dy && dx >= dz) {
            for (float x = x0; x < x1 - 1.0e-4F; x += 1.0F) {
                texBox(c, pose, x, y0, z0, Math.min(x + 1.0F, x1), y1, z1);
            }
        } else if (dz >= dx && dz >= dy) {
            for (float z = z0; z < z1 - 1.0e-4F; z += 1.0F) {
                texBox(c, pose, x0, y0, z, x1, y1, Math.min(z + 1.0F, z1));
            }
        } else {
            for (float y = y0; y < y1 - 1.0e-4F; y += 1.0F) {
                texBox(c, pose, x0, y, z0, x1, Math.min(y + 1.0F, y1), z1);
            }
        }
    }
}
