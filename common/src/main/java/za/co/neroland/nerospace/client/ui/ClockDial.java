package za.co.neroland.nerospace.client.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.client.sky.SkyProfile;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.glacira.GlaciraPlanet;
import za.co.neroland.nerospace.world.planet.luna.LunaPlanet;

/**
 * The planet clock dial, drawn pixel by pixel (no sprite) so the lit face IS the planet's daylight curve: an
 * eclipse notch, a long dim plateau or a hard day/night all show in the face. Phase convention shared with
 * {@code ClockMath}: 0 sunrise = right, 0.25 noon = up, 0.5 sunset = left, 0.75 midnight = down.
 *
 * <p>Sizes: r = 7 (HUD, 16x16 box), r = 5 (standard card, 12x12), r = 3 (compact card, 8x8). The geometry lives in
 * pure static methods so it is unit-tested without a renderer.</p>
 */
public final class ClockDial {

    public static final int INK = 0xFF05080D;
    public static final int TROUGH = 0xFF0B1119;
    public static final int TITLE = 0xFFD6ECFF;
    /** Eclipse windows: dotted ring. */
    public static final int BAD = 0xFFF06060;
    /** Twilight / Earthrise windows: solid ring. */
    public static final int VALUE = 0xFFB9C6D4;

    public static final String EARTHRISE = "hud.nerospace.clock.earthrise";
    public static final String TWILIGHT = "hud.nerospace.clock.twilight";
    public static final String ECLIPSE = "hud.nerospace.clock.eclipse";

    /**
     * A tinted span of the ring, {@code from..to} in phase units; {@code dotted} = every other pixel. While the phase
     * is inside it, the dial's phase name is {@code labelKey} (null = the plain quarter name).
     */
    public record Window(double from, double to, int argb, boolean dotted, @Nullable String labelKey) {

        /** A window named by its look: an eclipse ({@link #BAD}) reads Eclipse, any other window has no name. */
        public Window(double from, double to, int argb, boolean dotted) {
            this(from, to, argb, dotted, argb == BAD ? ECLIPSE : null);
        }

        public boolean contains(double phase) {
            double p = DaylightCurve.wrap(phase);
            return this.from <= this.to ? p >= this.from && p < this.to : p >= this.from || p < this.to;
        }
    }

    /** A dial face does not depend on the phase, so it is built once per look and drawn as row runs. */
    private record FaceKey(int r, int accent, @Nullable DaylightCurve curve, List<Window> windows) {
    }

    private static final int FACE_CACHE = 32;
    private static final Map<FaceKey, int[]> FACES = new LinkedHashMap<>(16, 0.75F, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<FaceKey, int[]> eldest) {
            return size() > FACE_CACHE;
        }
    };

    private ClockDial() {
    }

    /** The tip-dot colour for a sky profile: its first sun's colour, white when it has none. */
    public static int sunArgb(@Nullable SkyProfile profile) {
        return profile == null || profile.sun().isEmpty() ? 0xFFFFFFFF
                : 0xFF000000 | SkyProfile.rgb(profile.sun().get(0).color());
    }

    /** Box side for radius {@code r}. */
    public static int size(int r) {
        return 2 * r + 2;
    }

    /** The box centre (same on both axes) for radius {@code r}. */
    public static double centre(int r) {
        return (size(r) - 1) / 2.0;
    }

    /** The clock phase a box pixel faces, in [0,1): 0 to the right, 0.25 up. */
    public static double phaseAt(int px, int py, int r) {
        double c = centre(r);
        double a = Math.atan2(-(py - c), px - c) / (2.0 * Math.PI);
        return DaylightCurve.wrap(a);
    }

    /** Hand pixel at distance {@code t} from the centre for {@code phase}, as {x, y} box offsets. */
    public static int[] handPixel(int r, double phase, double t) {
        double c = centre(r);
        double a = 2.0 * Math.PI * phase;
        return new int[] { (int) Math.round(c + Math.cos(a) * t), (int) Math.round(c - Math.sin(a) * t) };
    }

    /** The hand's tip (where the sun dot sits), as {x, y} box offsets. */
    public static int[] tip(int r, double phase) {
        return handPixel(r, phase, r - 2);
    }

    /**
     * Draws the dial with its box top-left at {@code (x, y)}.
     *
     * @param accent planet accent: the lit part of the face
     * @param sunArgb colour of the tip dot (the planet's sun)
     * @param fixed a fixed-clock planet: no hand, only the sun dot
     * @param curve the planet's daylight curve, or null for a plain half-lit face
     */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int r, double phase, int accent, int sunArgb,
            boolean fixed, List<Window> windows, @Nullable DaylightCurve curve) {
        int s = size(r);
        int[] runs = face(r, accent, curve, windows);
        for (int i = 0; i < runs.length; i += 4) {
            g.fill(x + runs[i], y + runs[i + 1], x + runs[i] + runs[i + 2], y + runs[i + 1] + 1, runs[i + 3]);
        }
        if (!fixed) {
            for (int t = 0; t <= r - 2; t++) {
                int[] p = handPixel(r, phase, t);
                g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, TITLE);
            }
        }
        int[] tip = tip(r, phase);
        int dot = r >= 5 ? 2 : 1;
        int tx = Math.min(s - dot, Math.max(0, tip[0] - (dot - 1) / 2));
        int ty = Math.min(s - dot, Math.max(0, tip[1] - (dot - 1) / 2));
        g.fill(x + tx, y + ty, x + tx + dot, y + ty + dot, sunArgb);
    }

    /** The cached face as {x, y, length, colour} runs along each row. */
    private static int[] face(int r, int accent, @Nullable DaylightCurve curve, List<Window> windows) {
        synchronized (FACES) {
            int[] cached = FACES.get(new FaceKey(r, accent, curve, windows));
            if (cached != null) {
                return cached;
            }
        }
        int[] px = facePixels(r, accent, curve, windows);
        int s = size(r);
        int[] out = new int[s * s * 4];
        int n = 0;
        for (int py = 0; py < s; py++) {
            int start = 0;
            while (start < s) {
                int col = px[py * s + start];
                int end = start + 1;
                while (end < s && px[py * s + end] == col) {
                    end++;
                }
                if (col != 0) {
                    out[n++] = start;
                    out[n++] = py;
                    out[n++] = end - start;
                    out[n++] = col;
                }
                start = end;
            }
        }
        int[] runs = java.util.Arrays.copyOf(out, n);
        synchronized (FACES) {
            FACES.put(new FaceKey(r, accent, curve, List.copyOf(windows)), runs);
        }
        return runs;
    }

    /** The face pixels of a box of side {@code size(r)}, row-major; 0 = outside the dial. */
    static int[] facePixels(int r, int accent, @Nullable DaylightCurve curve, List<Window> windows) {
        int s = size(r);
        double c = centre(r);
        int[] out = new int[s * s];
        for (int py = 0; py < s; py++) {
            for (int px = 0; px < s; px++) {
                double d = Math.hypot(px - c, py - c);
                if (d > r) {
                    continue;
                }
                int col;
                if (d > r - 1) {
                    col = INK;
                } else {
                    double phi = phaseAt(px, py, r);
                    double lit = curve == null ? (phi < 0.5 ? 1.0 : 0.0) : DaylightCurve.clamp01(curve.at(phi));
                    col = mix(TROUGH, accent, (float) (0.40 * lit));
                    if (d > r - 2) {
                        for (Window w : windows) {
                            if (w.contains(phi) && (!w.dotted() || ((px + py) & 1) == 0)) {
                                col = w.argb();
                            }
                        }
                    }
                }
                out[py * s + px] = col;
            }
        }
        return out;
    }

    /**
     * The named windows on a planet's dial, eclipse first: the sky profile's eclipse (Cindara), Luna's Earthrise and
     * Glacira's two long twilights. Empty for a planet without any.
     */
    public static List<Window> windowsFor(@Nullable PlanetDefinition planet, @Nullable SkyProfile profile) {
        List<Window> out = new ArrayList<>();
        if (profile != null) {
            profile.eclipse().ifPresent(e -> out.add(new Window(e.phaseStart(), e.phaseEnd(), BAD, true, ECLIPSE)));
        }
        if (planet == Planets.LUNA) {
            out.add(new Window(LunaPlanet.EARTHRISE_START, LunaPlanet.EARTHRISE_END, VALUE, false, EARTHRISE));
        } else if (planet == Planets.GLACIRA) {
            out.add(new Window(GlaciraPlanet.DAWN_START, GlaciraPlanet.DAWN_END, VALUE, false, TWILIGHT));
            out.add(new Window(GlaciraPlanet.DUSK_START, GlaciraPlanet.DUSK_END, VALUE, false, TWILIGHT));
        }
        return List.copyOf(out);
    }

    /**
     * The lang key naming the phase ({@code hud.nerospace.clock.*}): the name of the first named window the phase is
     * in (Eclipse, Earthrise, Twilight), else sunrise / day / sunset / night by quarter, the transitions taking 0.05
     * either side of 0 and 0.5.
     */
    public static String phaseKey(double phase, List<Window> windows) {
        double p = DaylightCurve.wrap(phase);
        for (Window w : windows) {
            if (w.labelKey() != null && w.contains(p)) {
                return w.labelKey();
            }
        }
        if (p < 0.05 || p >= 0.95) {
            return "hud.nerospace.clock.sunrise";
        }
        if (p < 0.45) {
            return "hud.nerospace.clock.day";
        }
        if (p < 0.55) {
            return "hud.nerospace.clock.sunset";
        }
        return "hud.nerospace.clock.night";
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int r = Math.round(ar + (((b >> 16) & 0xFF) - ar) * t);
        int gg = Math.round(ag + (((b >> 8) & 0xFF) - ag) * t);
        int bl = Math.round(ab + ((b & 0xFF) - ab) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | bl;
    }
}
