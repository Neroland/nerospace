package za.co.neroland.nerospace.client.sky;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;

/**
 * What one sky frame shows, worked out from a {@link SkyProfile} with no rendering classes, so it is the same on every
 * loader and can be unit-tested. A loader's sky hook turns it into draw calls (NeoForge 26.3 today).
 *
 * <p><b>Directions</b> follow Minecraft: yaw 0 = south (+Z), 90 = west (-X), 180 = north, 270 = east (+X); pitch is
 * the elevation above the horizon (negative = below it).</p>
 *
 * <p><b>Body angle</b>: a body with {@code orbitPeriodDays > 0} has
 * {@code bodyPhase = frac(days / orbitPeriodDays + phaseOffset)}, where {@code days} is the continuous day count
 * (whole days plus the clock phase), so no body jumps when the phase wraps at sunrise; it rises in the east at bodyPhase 0, culminates at
 * 0.25 and sets in the west at 0.5 (a body with a period under half a day rises in the west instead, like Phobos).
 * Its elevation is {@code sin(2 pi bodyPhase) * 90 + inclination} at the horizon crossings; above the horizon the
 * path leans south (positive inclination) or north (negative) and culminates at {@code 90 - |inclination|}, so an
 * inclined body passes beside the zenith instead of stalling on it. {@code orbitPeriodDays == 0} or a {@code fixedAngle}
 * pins the body at that yaw / pitch.</p>
 */
public final class SkyScene {

    /** One textured body to draw (a sun, moon, planet or the homeworld disk). */
    public record BodyDraw(String id, Optional<Identifier> texture, float yaw, float pitch, float size, int rgb,
            float alpha, float glow, boolean emissive, float roll) {
    }

    public final int zenith;
    public final int horizon;
    public final int fog;
    /** Star alpha 0..1 and tint. */
    public final float starAlpha;
    public final int starTint;
    public final int starCount;
    public final float galaxyAlpha;
    public final Optional<Identifier> galaxyTexture;
    public final float galaxyYaw;
    public final float galaxyPitch;
    /** Aurora alpha 0..1 and its current colour (cycled through the profile's list). */
    public final float auroraAlpha;
    public final int auroraColor;
    public final float auroraShift;
    public final float hazeAlpha;
    public final int hazeColor;
    /** Draw order: suns (emissive) first, then planets and moons (they may cover a sun: the eclipse). */
    public final List<BodyDraw> bodies;

    private SkyScene(int zenith, int horizon, int fog, float starAlpha, int starTint, int starCount, float galaxyAlpha,
            Optional<SkyProfile.Galaxy> galaxy, float auroraAlpha, int auroraColor, float auroraShift, float hazeAlpha, int hazeColor, List<BodyDraw> bodies) {
        this.zenith = zenith;
        this.horizon = horizon;
        this.fog = fog;
        this.starAlpha = starAlpha;
        this.starTint = starTint;
        this.starCount = starCount;
        this.galaxyAlpha = galaxyAlpha;
        this.galaxyTexture = galaxy.flatMap(SkyProfile.Galaxy::texture);
        this.galaxyYaw = galaxy.map(SkyProfile.Galaxy::yaw).orElse(0.0F);
        this.galaxyPitch = galaxy.map(SkyProfile.Galaxy::pitch).orElse(0.0F);
        this.auroraAlpha = auroraAlpha;
        this.auroraColor = auroraColor;
        this.auroraShift = auroraShift;
        this.hazeAlpha = hazeAlpha;
        this.hazeColor = hazeColor;
        this.bodies = bodies;
    }

    /**
     * @param phase       planet clock phase 0..1 (0 sunrise, 0.25 noon, 0.5 sunset)
     * @param days        continuous day count whose fractional part is {@code phase} (body angles only)
     * @param daylight    0..1 from the planet's daylight curve
     * @param weather     current weather event name ({@code "NONE"} when clear)
     * @param intensity   weather intensity 0..1
     * @param position    player coordinate on the profile's {@code positionBlend} axis
     * @param bandWidth   full band width for {@code positionBlend} (blocks)
     * @param timeTicks   game time plus partial tick, for slow animation (aurora drift, disk roll)
     */
    public static SkyScene compute(SkyProfile p, double phase, double days, double daylight, String weather,
            float intensity, double position, int bandWidth, double timeTicks) {
        double d = clamp01(daylight);
        double ph = frac(phase);
        int[] c = baseColors(p, ph, d);
        if (p.positionBlend().isPresent()) {
            c = positionColors(p.positionBlend().get(), position, bandWidth);
        }
        SkyProfile.WeatherTint tint = p.weatherTints().get(weather);
        if (tint != null && intensity > 0.0F) {
            float s = clamp01(intensity) * tint.strengthAtPeak();
            int sky = SkyProfile.rgb(tint.sky());
            c = new int[] {lerpRgb(c[0], sky, s), lerpRgb(c[1], sky, s), lerpRgb(c[2], SkyProfile.rgb(tint.fog()), s)};
        }
        float eclipseLight = eclipseLight(p, ph);
        if (eclipseLight < 1.0F) {
            float k = 1.0F - (1.0F - eclipseLight) * 0.85F;
            c = new int[] {scaleRgb(c[0], k), scaleRgb(c[1], k), scaleRgb(c[2], k)};
        }
        double night = 1.0 - d;
        float starAlpha = p.stars().alwaysVisible() ? p.stars().brightness()
                : (float) (p.stars().brightness() * night * night);
        float galaxyAlpha = p.galaxy().map(g -> g.opacity()
                * (float) ("constant".equals(p.background().blend()) ? 1.0 : Math.pow(night, 1.5))).orElse(0.0F);
        float auroraAlpha = 0.0F;
        int auroraColor = 0xFFFFFF;
        float auroraShift = 0.0F;
        if (p.aurora().isPresent()) {
            SkyProfile.Aurora a = p.aurora().get();
            float vis = a.nightOnly() ? (float) clamp01(1.0 - Math.max(0.0, d - 0.12) / 0.25) : 1.0F;
            if (a.nightOnly() && d >= 0.37) {
                vis = 0.0F;
            }
            float boost = a.boostOnWeather().map(w -> w.equals(weather) ? 1.0F + 0.4F * clamp01(intensity) : 1.0F)
                    .orElse(1.0F);
            auroraAlpha = Math.min(1.0F, a.opacity() * boost * vis);
            double t = timeTicks / 20.0 * 0.05 * a.speed();
            auroraColor = cycle(a.colors(), t);
            auroraShift = (float) frac(t * 0.25);
        }
        float hazeAlpha = p.horizonHaze().map(SkyProfile.Haze::strength).orElse(0.0F);
        int hazeColor = p.horizonHaze().map(h -> SkyProfile.rgb(h.color())).orElse(0);
        float roll = p.terminatorScroll().filter(t -> t.periodTicks() > 0)
                .map(t -> (float) (frac(timeTicks / t.periodTicks()) * 360.0)).orElse(0.0F);
        List<BodyDraw> bodies = new ArrayList<>();
        float[] sunDir = null;
        for (SkyProfile.Body b : p.sun()) {
            float[] yp = bodyAngles(b, days);
            if (sunDir == null) {
                sunDir = direction(yp[0], yp[1]);
            }
            bodies.add(new BodyDraw(b.id(), b.texture(), yp[0], yp[1], b.size(), SkyProfile.rgb(b.color()), 1.0F,
                    b.glow(), true, 0.0F));
        }
        addSolid(bodies, p.bodies(), days, sunDir, roll);
        addSolid(bodies, p.moons(), days, sunDir, roll);
        return new SkyScene(c[0], c[1], c[2], clamp01(starAlpha), SkyProfile.rgb(p.stars().tint()),
                Math.max(0, Math.min(8000, p.stars().count())), clamp01(galaxyAlpha), p.galaxy(), auroraAlpha, auroraColor,
                auroraShift, clamp01(hazeAlpha), hazeColor, List.copyOf(bodies));
    }

    private static void addSolid(List<BodyDraw> out, List<SkyProfile.Body> list, double days, float[] sunDir,
            float roll) {
        for (SkyProfile.Body b : list) {
            float[] yp = bodyAngles(b, days);
            float lit = 1.0F;
            if (b.phases() && sunDir != null) {
                float[] dir = direction(yp[0], yp[1]);
                double cos = dir[0] * sunDir[0] + dir[1] * sunDir[1] + dir[2] * sunDir[2];
                // Full when opposite the sun, a thin crescent next to it.
                lit = (float) (0.3 + 0.7 * (1.0 - cos) / 2.0);
            }
            boolean pinned = b.orbitPeriodDays() == 0.0F || b.fixedAngle().isPresent();
            out.add(new BodyDraw(b.id(), b.texture(), yp[0], yp[1], b.size(), scaleRgb(SkyProfile.rgb(b.color()), lit),
                    1.0F, b.glow(), false, pinned ? roll : 0.0F));
        }
    }

    /**
     * {yaw, pitch} in degrees for a body at the continuous day count {@code days} (see the class note). Within one day
     * a one-day body only needs the phase, but any other period must be given the unwrapped count.
     */
    public static float[] bodyAngles(SkyProfile.Body b, double days) {
        if (b.orbitPeriodDays() == 0.0F || b.fixedAngle().isPresent()) {
            SkyProfile.FixedAngle f = b.fixedAngle().orElse(new SkyProfile.FixedAngle(0.0F, 90.0F));
            return new float[] {f.yaw(), f.pitch()};
        }
        double bodyPhase = frac(days / b.orbitPeriodDays() + b.phaseOffset());
        double theta = 2.0 * Math.PI * bodyPhase;
        double sin = Math.sin(theta);
        double incl = b.inclination();
        // Contract elevation at the horizon crossings (inclination), capped at 90 - |inclination| at culmination so
        // an inclined body passes beside the zenith instead of parking on it.
        double elevation = sin >= 0.0 ? sin * (90.0 - Math.abs(incl)) + incl * (1.0 - sin) : sin * 90.0 + incl;
        double el = Math.toRadians(Math.max(-90.0, Math.min(90.0, elevation)));
        // East-west component: +1 = east at bodyPhase 0, -1 = west at 0.5; fast moons run the other way.
        double ew = Math.cos(theta);
        if (b.orbitPeriodDays() < 0.5F) {
            ew = -ew;
        }
        // Inclined paths lean south (positive) or north (negative) while above the horizon.
        double ns = Math.signum(incl) * Math.max(0.0, sin);
        double len = Math.sqrt(ew * ew + ns * ns);
        double horiz = Math.cos(el);
        double x = len < 1e-9 ? 0.0 : ew / len * horiz;
        double z = len < 1e-9 ? 0.0 : ns / len * horiz;
        double y = Math.sin(el);
        return anglesOf(x, y, z);
    }

    /** Unit direction (x east, y up, z south) for a yaw / pitch in degrees. */
    public static float[] direction(float yaw, float pitch) {
        double ry = Math.toRadians(yaw);
        double rp = Math.toRadians(pitch);
        double h = Math.cos(rp);
        return new float[] {(float) (-Math.sin(ry) * h), (float) Math.sin(rp), (float) (Math.cos(ry) * h)};
    }

    private static float[] anglesOf(double x, double y, double z) {
        double h = Math.sqrt(x * x + z * z);
        float pitch = (float) Math.toDegrees(Math.atan2(y, h));
        float yaw = h < 1e-9 ? 0.0F : (float) Math.toDegrees(Math.atan2(-x, z));
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
        return new float[] {yaw, pitch};
    }

    /** Eclipse light factor 1 (none) .. minLight (totality, the middle of the window). */
    static float eclipseLight(SkyProfile p, double phase) {
        if (p.eclipse().isEmpty()) {
            return 1.0F;
        }
        SkyProfile.Eclipse e = p.eclipse().get();
        if (e.phaseEnd() <= e.phaseStart() || phase < e.phaseStart() || phase > e.phaseEnd()) {
            return 1.0F;
        }
        double mid = (e.phaseStart() + e.phaseEnd()) / 2.0;
        double half = (e.phaseEnd() - e.phaseStart()) / 2.0;
        double t = 1.0 - Math.abs(phase - mid) / half;
        double k = t * t * (3.0 - 2.0 * t);
        return (float) (1.0 - (1.0 - e.minLight()) * k);
    }

    /** {zenith, horizon, fog} from the gradient keyframes, or from the background / fog day-night pair. */
    static int[] baseColors(SkyProfile p, double phase, double daylight) {
        List<SkyProfile.GradientKey> g = p.gradient();
        if (!g.isEmpty()) {
            return gradientAt(g, phase);
        }
        SkyProfile.Background bg = p.background();
        SkyProfile.Fog fog = p.fog();
        if ("constant".equals(bg.blend())) {
            return new int[] {SkyProfile.rgb(bg.zenith()), SkyProfile.rgb(bg.horizon()), SkyProfile.rgb(fog.color())};
        }
        float d = (float) daylight;
        return new int[] {lerpRgb(SkyProfile.rgb(bg.nightZenith()), SkyProfile.rgb(bg.zenith()), d),
                lerpRgb(SkyProfile.rgb(bg.nightHorizon()), SkyProfile.rgb(bg.horizon()), d),
                lerpRgb(SkyProfile.rgb(fog.nightColor().orElse(fog.color())), SkyProfile.rgb(fog.color()), d)};
    }

    /** Piecewise-linear keyframe lookup that wraps from the last key back to the first. */
    static int[] gradientAt(List<SkyProfile.GradientKey> keys, double phase) {
        List<SkyProfile.GradientKey> k = sorted(keys) ? keys : sortedCopy(keys);
        if (k.size() == 1) {
            SkyProfile.GradientKey only = k.get(0);
            return colorsOf(only);
        }
        SkyProfile.GradientKey prev = k.get(k.size() - 1);
        double prevPhase = prev.phase() - 1.0;
        for (SkyProfile.GradientKey next : k) {
            if (phase <= next.phase()) {
                double span = next.phase() - prevPhase;
                float t = span <= 1e-9 ? 0.0F : (float) ((phase - prevPhase) / span);
                return lerp3(colorsOf(prev), colorsOf(next), t);
            }
            prev = next;
            prevPhase = next.phase();
        }
        SkyProfile.GradientKey first = k.get(0);
        double span = first.phase() + 1.0 - prevPhase;
        float t = span <= 1e-9 ? 0.0F : (float) ((phase - prevPhase) / span);
        return lerp3(colorsOf(prev), colorsOf(first), t);
    }

    /** Whether the keys are already in phase order (the shipped profiles are), so a frame copies nothing. */
    private static boolean sorted(List<SkyProfile.GradientKey> keys) {
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i - 1).phase() > keys.get(i).phase()) {
                return false;
            }
        }
        return true;
    }

    private static List<SkyProfile.GradientKey> sortedCopy(List<SkyProfile.GradientKey> keys) {
        List<SkyProfile.GradientKey> k = new ArrayList<>(keys);
        k.sort((a, b) -> Float.compare(a.phase(), b.phase()));
        return k;
    }

    /** Colours by position: west below -half, band inside, east above +half; each edge fades over 128 blocks. */
    static int[] positionColors(SkyProfile.PositionBlend b, double position, int bandWidth) {
        double half = Math.max(1, bandWidth) / 2.0;
        double fade = 128.0;
        int[] west = colorsOf(b.west());
        int[] band = colorsOf(b.band());
        int[] east = colorsOf(b.east());
        if (position < 0.0) {
            float t = (float) clamp01((position + half + fade / 2.0) / fade);
            return lerp3(west, band, t);
        }
        float t = (float) clamp01((position - half + fade / 2.0) / fade);
        return lerp3(band, east, t);
    }

    private static int[] colorsOf(SkyProfile.GradientKey k) {
        return new int[] {SkyProfile.rgb(k.zenith()), SkyProfile.rgb(k.horizon()), SkyProfile.rgb(k.fog())};
    }

    private static int[] colorsOf(SkyProfile.SkyColors k) {
        return new int[] {SkyProfile.rgb(k.zenith()), SkyProfile.rgb(k.horizon()), SkyProfile.rgb(k.fog())};
    }

    private static int[] lerp3(int[] a, int[] b, float t) {
        return new int[] {lerpRgb(a[0], b[0], t), lerpRgb(a[1], b[1], t), lerpRgb(a[2], b[2], t)};
    }

    private static int cycle(List<String> colors, double t) {
        if (colors.isEmpty()) {
            return 0x60E0A0;
        }
        double f = frac(t) * colors.size();
        int i = (int) Math.floor(f);
        return lerpRgb(SkyProfile.rgb(colors.get(i % colors.size())), SkyProfile.rgb(colors.get((i + 1) % colors.size())),
                (float) (f - i));
    }

    public static int lerpRgb(int a, int b, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    static int scaleRgb(int c, float k) {
        int r = Math.min(255, Math.round(((c >> 16) & 0xFF) * k));
        int g = Math.min(255, Math.round(((c >> 8) & 0xFF) * k));
        int b = Math.min(255, Math.round((c & 0xFF) * k));
        return (r << 16) | (g << 8) | b;
    }

    static double frac(double v) {
        return v - Math.floor(v);
    }

    static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
