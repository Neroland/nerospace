package za.co.neroland.nerospace.neoforge;

// NeoForge 26.3 planet sky: the CustomSkyboxRenderer registered as nerospace:planet. Every dimension type selects it
// through the neoforge:custom_skybox attribute in neoforge/src/main/resources-26.3 (tools/gen_sky_overlays.py). It
// turns PlanetSkyRenderer.scene(...) into draw calls with the vanilla sky pipelines and returns false (vanilla sky)
// for a level without a planet profile. Only this cell has a sky hook; the others show the dimension colours.
//? if >=26.3 {
/*import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.sky.PlanetSkyRenderer;
import za.co.neroland.nerospace.client.sky.SkyScene;

public final class NeoForgePlanetSky implements net.neoforged.neoforge.client.CustomSkyboxRenderer {

    private static final float R = 100.0F;
    private static final Identifier RAMP = tex("horizon_ramp");
    private static final Identifier STARS = tex("stars");
    private static final Identifier AURORA_0 = tex("aurora_0");
    private static final Identifier AURORA_1 = tex("aurora_1");

    private GpuBuffer disc;
    private GpuBuffer ramp;
    private int rampQuads;
    private GpuBuffer quadTex;
    private GpuBuffer quadTexColor;
    private GpuBuffer galaxy;
    private int galaxyQuads;
    private GpuBuffer aurora;
    private int auroraQuads;
    private final Map<Integer, GpuBuffer> stars = new HashMap<>();
    private final Map<Integer, Integer> starQuads = new HashMap<>();
    private boolean failed;

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/sky/" + name + ".png");
    }

    @Override
    public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState, Matrix4fc modelViewMatrix,
            GpuBufferSlice skyFog) {
        if (this.failed) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        PlanetSkyRenderer.Frame frame = PlanetSkyRenderer.frame(mc.level);
        if (frame == null) {
            return false;
        }
        Vec3 cam = levelRenderState.cameraRenderState.pos;
        double time = levelRenderState.gameTime + (double) levelRenderState.worldPartialTicks;
        SkyScene scene = PlanetSkyRenderer.scene(frame, PlanetSkyRenderer.blendPosition(frame, cam.x, cam.z), time);
        try {
            draw(mc, scene, new Matrix4f(modelViewMatrix), skyFog);
            return true;
        } catch (RuntimeException ex) {
            // Never crash a frame over the sky: fall back to the vanilla sky for the rest of the session.
            this.failed = true;
            NerospaceCommon.LOGGER.warn("[Nerospace] planet sky disabled after a render error: {}", ex.toString());
            return false;
        }
    }

    private void draw(Minecraft mc, SkyScene s, Matrix4f view, GpuBufferSlice skyFog) {
        build();
        RenderSystem.setShaderFog(skyFog);
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Nerospace sky",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            // Zenith colour disc (the vanilla sky shader fades it into the dimension's fog colour at the horizon).
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.SKY));
            pass.setUniform("DynamicTransforms", transform(view, s.zenith, 1.0F));
            pass.setVertexBuffer(0, this.disc.slice());
            pass.draw(10, 1, 0, 0);
            // Zenith-to-horizon gradient.
            drawTextured(pass, RenderPipelines.END_SKY, this.ramp, this.rampQuads, RAMP, view, s.horizon, 1.0F, mc);
            if (s.starAlpha > 0.01F) {
                GpuBuffer starBuf = starBuffer(s.starCount);
                drawTextured(pass, RenderPipelines.CELESTIAL, starBuf, this.starQuads.get(s.starCount), STARS, view,
                        s.starTint, s.starAlpha, mc);
            }
            // Galaxy band (translucent, faded towards its far side).
            if (s.galaxyAlpha > 0.01F && s.galaxyTexture.isPresent()) {
                Matrix4f m = new Matrix4f(view).rotateY((float) Math.toRadians(-s.galaxyYaw))
                        .rotateX((float) Math.toRadians(-s.galaxyPitch));
                drawTextured(pass, RenderPipelines.END_SKY, this.galaxy, this.galaxyQuads, s.galaxyTexture.get(), m,
                        0xFFFFFF, s.galaxyAlpha, mc);
            }
            if (s.auroraAlpha > 0.01F) {
                drawTextured(pass, RenderPipelines.CELESTIAL, this.aurora, this.auroraQuads, AURORA_0,
                        new Matrix4f(view).rotateY((float) Math.toRadians(-180.0F)), s.auroraColor, s.auroraAlpha, mc);
                drawTextured(pass, RenderPipelines.CELESTIAL, this.aurora, this.auroraQuads, AURORA_1,
                        new Matrix4f(view).rotateY((float) Math.toRadians(-180.0F + 25.0F + 30.0F * s.auroraShift))
                                .scale(1.0F, 1.25F, 1.0F),
                        s.auroraColor, s.auroraAlpha * 0.7F, mc);
            }
            for (SkyScene.BodyDraw b : s.bodies) {
                if (b.texture().isEmpty()) {
                    continue;
                }
                float half = (float) (R * Math.tan(Math.toRadians(Math.min(170.0F, Math.max(0.1F, b.size())) / 2.0)));
                Matrix4f m = new Matrix4f(view).rotateY((float) Math.toRadians(-b.yaw()))
                        .rotateX((float) Math.toRadians(-b.pitch())).rotateZ((float) Math.toRadians(b.roll()));
                if (b.emissive()) {
                    if (b.glow() > 0.0F) {
                        float h = half * (1.0F + b.glow() * 0.12F);
                        drawTextured(pass, RenderPipelines.CELESTIAL, this.quadTex, 2, b.texture().get(),
                                new Matrix4f(m).scale(h, h, R), b.rgb(), 0.35F * b.alpha(), mc);
                    }
                    drawTextured(pass, RenderPipelines.CELESTIAL, this.quadTex, 2, b.texture().get(),
                            new Matrix4f(m).scale(half, half, R), b.rgb(), b.alpha(), mc);
                } else {
                    drawTextured(pass, RenderPipelines.END_SKY, this.quadTexColor, 2, b.texture().get(),
                            new Matrix4f(m).scale(half, half, R), b.rgb(), b.alpha(), mc);
                }
            }
            if (s.hazeAlpha > 0.01F) {
                drawTextured(pass, RenderPipelines.END_SKY, this.ramp, this.rampQuads, RAMP,
                        new Matrix4f(view).scale(1.0F, 0.45F, 1.0F), s.hazeColor, s.hazeAlpha, mc);
            }
        }
    }

    private static GpuBufferSlice transform(Matrix4f m, int rgb, float alpha) {
        return RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(m),
                new Vector4f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, alpha));
    }

    private void drawTextured(RenderPass pass, RenderPipeline pipeline, GpuBuffer vertices, int quads, Identifier texture,
            Matrix4f m, int rgb, float alpha, Minecraft mc) {
        AbstractTexture t = mc.getTextureManager().getTexture(texture);
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        int count = quads * 6;
        pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", transform(m, rgb, alpha));
        pass.setUniform("Sampler0", t.getTextureView(), t.getSampler());
        pass.setVertexBuffer(0, vertices.slice());
        pass.setIndexBuffer(indices.getBuffer(count), indices.type());
        pass.drawIndexed(count, 1, 0, 0, 0);
    }

    // ------------------------------------------------------------------------------------------------ meshes

    private void build() {
        if (this.disc != null) {
            return;
        }
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(10 * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            b.addVertex(0.0F, 16.0F, 0.0F);
            for (int i = -180; i <= 180; i += 45) {
                b.addVertex(512.0F * (float) Math.cos(Math.toRadians(i)), 16.0F, 512.0F * (float) Math.sin(Math.toRadians(i)));
            }
            this.disc = upload("Nerospace sky disc", b);
        }
        // Horizon ramp: a ring of quads from 55 degrees down to the horizon (texture ramp), then down to -30 (solid).
        int seg = 32;
        float[][] rows = {{55.0F, 0.0F}, {0.0F, 1.0F}, {-30.0F, 1.0F}};
        this.rampQuads = seg * 2 * 2;
        this.ramp = mesh("Nerospace sky ramp", DefaultVertexFormat.POSITION_TEX_COLOR, this.rampQuads, b -> {
            for (int r = 0; r < 2; r++) {
                for (int i = 0; i < seg; i++) {
                    double a0 = Math.PI * 2.0 * i / seg;
                    double a1 = Math.PI * 2.0 * (i + 1) / seg;
                    float[] top = rows[r];
                    float[] bot = rows[r + 1];
                    float u0 = (float) i / seg;
                    float u1 = (float) (i + 1) / seg;
                    float[] p00 = ring(a0, top[0]);
                    float[] p10 = ring(a1, top[0]);
                    float[] p11 = ring(a1, bot[0]);
                    float[] p01 = ring(a0, bot[0]);
                    quadColor(b, p00, p10, p11, p01, u0, u1, top[1], bot[1], 0xFFFFFFFF, 0xFFFFFFFF);
                }
            }
        });
        // Unit quads at z = 1 facing the camera, both windings.
        this.quadTex = mesh("Nerospace sky body", DefaultVertexFormat.POSITION_TEX, 2, b -> {
            float[] a = {-1.0F, -1.0F, 1.0F};
            float[] c = {1.0F, -1.0F, 1.0F};
            float[] d = {1.0F, 1.0F, 1.0F};
            float[] e = {-1.0F, 1.0F, 1.0F};
            quadTex(b, a, c, d, e, 0.0F, 1.0F, 1.0F, 0.0F);
        });
        this.quadTexColor = mesh("Nerospace sky solid body", DefaultVertexFormat.POSITION_TEX_COLOR, 2, b -> {
            float[] a = {-1.0F, -1.0F, 1.0F};
            float[] c = {1.0F, -1.0F, 1.0F};
            float[] d = {1.0F, 1.0F, 1.0F};
            float[] e = {-1.0F, 1.0F, 1.0F};
            quadColor(b, a, c, d, e, 0.0F, 1.0F, 1.0F, 0.0F, 0xFFFFFFFF, 0xFFFFFFFF);
        });
        // Galaxy band: a great circle centred on local +Z, 36 degrees wide, fading to nothing 150 degrees away.
        int gseg = 64;
        this.galaxyQuads = gseg * 2;
        this.galaxy = mesh("Nerospace galaxy band", DefaultVertexFormat.POSITION_TEX_COLOR, this.galaxyQuads, b -> {
            for (int i = 0; i < gseg; i++) {
                double a0 = -Math.PI + Math.PI * 2.0 * i / gseg;
                double a1 = -Math.PI + Math.PI * 2.0 * (i + 1) / gseg;
                int c0 = fadeColor(a0);
                int c1 = fadeColor(a1);
                float u0 = (float) i / gseg;
                float u1 = (float) (i + 1) / gseg;
                float[] p00 = band(a0, 18.0);
                float[] p10 = band(a1, 18.0);
                float[] p11 = band(a1, -18.0);
                float[] p01 = band(a0, -18.0);
                quadColorPerColumn(b, p00, p10, p11, p01, u0, u1, c0, c1);
            }
        });
        // Aurora curtain: 160 degrees of arc centred on local +Z, from 12 to 50 degrees up.
        int aseg = 24;
        this.auroraQuads = aseg * 2;
        this.aurora = mesh("Nerospace aurora", DefaultVertexFormat.POSITION_TEX, this.auroraQuads, b -> {
            for (int i = 0; i < aseg; i++) {
                double a0 = Math.toRadians(-80.0 + 160.0 * i / aseg);
                double a1 = Math.toRadians(-80.0 + 160.0 * (i + 1) / aseg);
                float u0 = (float) i / aseg;
                float u1 = (float) (i + 1) / aseg;
                float[] p00 = curtain(a0, 50.0);
                float[] p10 = curtain(a1, 50.0);
                float[] p11 = curtain(a1, 12.0);
                float[] p01 = curtain(a0, 12.0);
                quadTex(b, p00, p10, p11, p01, u0, u1, 0.0F, 1.0F);
            }
        });
    }

    private GpuBuffer starBuffer(int count) {
        GpuBuffer existing = this.stars.get(count);
        if (existing != null) {
            return existing;
        }
        RandomSource random = RandomSource.createThreadLocalInstance(10842L + count);
        int[] made = {0};
        GpuBuffer buf = mesh("Nerospace stars", DefaultVertexFormat.POSITION_TEX, count * 2, b -> {
            for (int i = 0; i < count; i++) {
                double x = random.nextFloat() * 2.0F - 1.0F;
                double y = random.nextFloat() * 2.0F - 1.0F;
                double z = random.nextFloat() * 2.0F - 1.0F;
                double len = Math.sqrt(x * x + y * y + z * z);
                float size = 0.35F + random.nextFloat() * 0.45F;
                int cell = random.nextInt(16);
                if (len < 0.1 || len > 1.0) {
                    continue;
                }
                double dx = x / len;
                double dy = y / len;
                double dz = z / len;
                // Two axes perpendicular to the star direction.
                double ax = Math.abs(dy) < 0.9 ? 0.0 : 1.0;
                double ay = Math.abs(dy) < 0.9 ? 1.0 : 0.0;
                double ux = ay * dz;
                double uy = -ax * dz;
                double uz = ax * dy - ay * dx;
                double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
                ux /= ul;
                uy /= ul;
                uz /= ul;
                double vx = dy * uz - dz * uy;
                double vy = dz * ux - dx * uz;
                double vz = dx * uy - dy * ux;
                float cx = (float) (dx * R);
                float cy = (float) (dy * R);
                float cz = (float) (dz * R);
                float[] p0 = {cx + (float) ((-ux - vx) * size), cy + (float) ((-uy - vy) * size), cz + (float) ((-uz - vz) * size)};
                float[] p1 = {cx + (float) ((ux - vx) * size), cy + (float) ((uy - vy) * size), cz + (float) ((uz - vz) * size)};
                float[] p2 = {cx + (float) ((ux + vx) * size), cy + (float) ((uy + vy) * size), cz + (float) ((uz + vz) * size)};
                float[] p3 = {cx + (float) ((-ux + vx) * size), cy + (float) ((-uy + vy) * size), cz + (float) ((-uz + vz) * size)};
                float u0 = (cell % 4) / 4.0F;
                float v0 = (cell / 4) / 4.0F;
                quadTex(b, p0, p1, p2, p3, u0, u0 + 0.25F, v0 + 0.25F, v0);
                made[0]++;
            }
        });
        this.stars.put(count, buf);
        this.starQuads.put(count, made[0] * 2);
        return buf;
    }

    private static int fadeColor(double angle) {
        double f = Math.max(0.0, 1.0 - Math.pow(Math.abs(Math.toDegrees(angle)) / 150.0, 2.0));
        int a = (int) Math.round(255.0 * f);
        return (a << 24) | 0xFFFFFF;
    }

    private static float[] ring(double azimuth, float elevationDeg) {
        double e = Math.toRadians(elevationDeg);
        return new float[] {(float) (Math.sin(azimuth) * Math.cos(e) * R), (float) (Math.sin(e) * R),
                (float) (Math.cos(azimuth) * Math.cos(e) * R)};
    }

    private static float[] band(double along, double acrossDeg) {
        double w = Math.toRadians(acrossDeg);
        return new float[] {(float) (Math.sin(along) * Math.cos(w) * R), (float) (Math.sin(w) * R),
                (float) (Math.cos(along) * Math.cos(w) * R)};
    }

    private static float[] curtain(double azimuth, double elevationDeg) {
        return ring(azimuth, (float) elevationDeg);
    }

    private interface Filler {
        void fill(BufferBuilder b);
    }

    private static GpuBuffer mesh(String name, VertexFormat format, int quads, Filler filler) {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(Math.max(1, quads) * 4 * format.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
            filler.fill(b);
            return upload(name, b);
        }
    }

    private static GpuBuffer upload(String name, BufferBuilder b) {
        try (MeshData mesh = b.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> name, 32, mesh.vertexBuffer());
        }
    }

    // Each quad is written twice (both windings) so face culling never hides a sky part.
    private static void quadTex(BufferBuilder b, float[] p0, float[] p1, float[] p2, float[] p3, float u0, float u1,
            float v0, float v1) {
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, v0);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, v0);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, v1);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, v1);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, v1);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, v1);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, v0);
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, v0);
    }

    private static void quadColor(BufferBuilder b, float[] p0, float[] p1, float[] p2, float[] p3, float u0, float u1,
            float v0, float v1, int cTop, int cBottom) {
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, v0).setColor(cTop);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, v0).setColor(cTop);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, v1).setColor(cBottom);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, v1).setColor(cBottom);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, v1).setColor(cBottom);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, v1).setColor(cBottom);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, v0).setColor(cTop);
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, v0).setColor(cTop);
    }

    private static void quadColorPerColumn(BufferBuilder b, float[] p0, float[] p1, float[] p2, float[] p3, float u0,
            float u1, int cLeft, int cRight) {
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, 0.0F).setColor(cLeft);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, 0.0F).setColor(cRight);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, 1.0F).setColor(cRight);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, 1.0F).setColor(cLeft);
        b.addVertex(p3[0], p3[1], p3[2]).setUv(u0, 1.0F).setColor(cLeft);
        b.addVertex(p2[0], p2[1], p2[2]).setUv(u1, 1.0F).setColor(cRight);
        b.addVertex(p1[0], p1[1], p1[2]).setUv(u1, 0.0F).setColor(cRight);
        b.addVertex(p0[0], p0[1], p0[2]).setUv(u0, 0.0F).setColor(cLeft);
    }
}
*///?}
