package za.co.neroland.nerospace.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import za.co.neroland.nerospace.client.ClientPlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The six ambient planet particles (one class, tuned per {@link Kind}). Sprites are mostly white and tinted by a
 * colour ramp over the particle's life, so one 4-frame sprite set serves every palette. All are translucent; embers
 * and nebula motes are full-bright. The spawner ({@code WeatherAmbience}) passes the drift velocity.
 */
public class AmbientParticle extends SingleQuadParticle {

    /** Lifetime, size, ramp and motion of one type. Colours are 0xRRGGBB. */
    public enum Kind {
        DUST(60, 120, 0.10F, 0.18F, 0.55F, 0.0F, false, new int[] {0xD9A066, 0x7A4A2E}),
        ASH(100, 200, 0.10F, 0.18F, 0.9F, 0.3F, false, new int[] {0xFF6B35, 0x4A2A22, 0x1A1210}),
        SPORE(160, 320, 0.10F, 0.18F, 0.7F, 0.0F, false, new int[] {0x3FBF6F, 0xA6FF4D}),
        DIAMOND_DUST(80, 140, 0.06F, 0.10F, 1.0F, 0.3F, false, new int[] {0xFFFFFF, 0xA9E3FF}),
        EMBER(30, 60, 0.05F, 0.09F, 1.0F, 0.0F, true, new int[] {0xFFF2B0, 0xFF6B35, 0xA3321C}),
        NEBULA_MOTE(200, 400, 0.12F, 0.20F, 0.5F, 0.0F, true, new int[] {0x9B5DE5, 0xFF6EC7, 0x4DD0E1});

        final int minLife;
        final int maxLife;
        final float minSize;
        final float maxSize;
        final float alphaStart;
        final float alphaEnd;
        final boolean emissive;
        final int[] ramp;

        Kind(int minLife, int maxLife, float minSize, float maxSize, float alphaStart, float alphaEnd, boolean emissive,
                int[] ramp) {
            this.minLife = minLife;
            this.maxLife = maxLife;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.alphaStart = alphaStart;
            this.alphaEnd = alphaEnd;
            this.emissive = emissive;
            this.ramp = ramp;
        }
    }

    private static final int[] BLOOM = {0xFF7AC8, 0xFFD1EA};
    private static final int FLARE = 0xFFB86B;

    private final SpriteSet sprites;
    private final Kind kind;
    private final int[] ramp;
    private final float phase;

    protected AmbientParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet sprites, Kind kind) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.kind = kind;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = switch (kind) {
            case ASH -> 0.08F;
            case SPORE -> 0.01F;
            case DIAMOND_DUST -> 0.04F;
            default -> 0.0F;
        };
        this.lifetime = kind.minLife + this.random.nextInt(kind.maxLife - kind.minLife + 1);
        this.quadSize = kind.minSize + this.random.nextFloat() * (kind.maxSize - kind.minSize);
        this.phase = this.random.nextFloat() * 6.2831855F;
        WeatherEvent event = ClientPlanetWeather.event(level);
        if (kind == Kind.SPORE && event == WeatherEvent.SPORE_BLOOM) {
            this.ramp = BLOOM;
        } else if (kind == Kind.NEBULA_MOTE && event == WeatherEvent.FLARE) {
            this.ramp = new int[] {FLARE, kind.ramp[1], FLARE};
        } else {
            this.ramp = kind.ramp;
        }
        this.roll = this.random.nextFloat() * 6.2831855F;
        this.oRoll = this.roll;
        apply();
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float t = this.age / (float) Math.max(1, this.lifetime);
        switch (this.kind) {
            case DUST -> this.yd = Math.sin(this.age * 0.2 + this.phase) * 0.01;
            case ASH -> {
                this.yd = Math.max(this.yd, -0.06);
                this.xd += (this.random.nextFloat() - 0.5F) * 0.01F;
                this.zd += (this.random.nextFloat() - 0.5F) * 0.01F;
                this.xd *= 0.9;
                this.zd *= 0.9;
                this.oRoll = this.roll;
                this.roll += 0.02F;
            }
            case SPORE -> {
                this.xd += (this.random.nextFloat() - 0.5F) * 0.006F;
                this.zd += (this.random.nextFloat() - 0.5F) * 0.006F;
                this.yd += 0.002 + (this.random.nextFloat() - 0.5F) * 0.006F;
                this.xd *= 0.92;
                this.yd *= 0.92;
                this.zd *= 0.92;
            }
            case DIAMOND_DUST -> this.yd = Math.max(this.yd, -0.03);
            case EMBER -> {
                this.yd = 0.04 * (1.0F - t);
                this.xd += (this.random.nextFloat() - 0.5F) * 0.02F;
                this.zd += (this.random.nextFloat() - 0.5F) * 0.02F;
                this.xd *= 0.8;
                this.zd *= 0.8;
            }
            case NEBULA_MOTE -> this.yd = Math.sin(this.age * 0.05 + this.phase) * 0.005;
        }
        apply();
        if (this.kind == Kind.DIAMOND_DUST) {
            this.setSprite(this.sprites.get((this.age / 4) % 4, 3));
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    /** Colour ramp + alpha for the current age. */
    private void apply() {
        float t = this.age / (float) Math.max(1, this.lifetime);
        int rgb = this.kind == Kind.NEBULA_MOTE ? cycle(this.ramp, this.age / 120.0F) : ramp(this.ramp, t);
        this.setColor(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        float a = this.kind.alphaStart + (this.kind.alphaEnd - this.kind.alphaStart) * t;
        if (this.kind == Kind.DIAMOND_DUST) {
            a = 0.3F + 0.7F * (0.5F + 0.5F * (float) Math.sin(this.age * 0.8 + this.phase));
        }
        this.setAlpha(Math.max(0.0F, Math.min(1.0F, a)));
    }

    @Override
    protected int getLightCoords(float partialTick) {
        if (this.kind.emissive || this.kind == Kind.ASH && this.age < 10) {
            return 0xF000F0;
        }
        return super.getLightCoords(partialTick);
    }

    static int ramp(int[] stops, float t) {
        if (stops.length == 1) {
            return stops[0];
        }
        float f = Math.max(0.0F, Math.min(1.0F, t)) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) f);
        return lerp(stops[i], stops[i + 1], f - i);
    }

    static int cycle(int[] stops, float t) {
        float f = (t - (float) Math.floor(t)) * stops.length;
        int i = (int) f % stops.length;
        return lerp(stops[i], stops[(i + 1) % stops.length], f - (int) f);
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** One provider per type, created by each loader with the type's sprite set. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        @org.jetbrains.annotations.Nullable
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
            // The config switch gates every source of these particles, not only the ambience spawner.
            if (!za.co.neroland.nerospace.config.NerospaceConfig.particlesEnabled()) {
                return null;
            }
            return new AmbientParticle(level, x, y, z, xd, yd, zd, this.sprites, this.kind);
        }
    }
}
