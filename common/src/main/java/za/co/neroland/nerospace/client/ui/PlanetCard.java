package za.co.neroland.nerospace.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The planet card (004 §5): a pure drawing + hit-test helper, not a widget — the host screen owns focus and
 * clicks. Standard 96×56 (selected destination) and compact 64×32 (list rows). Functional art: Stage 6 swaps
 * the procedural strip and glyph squares for the sky-profile strip and the glyph sheet; sizes, fields and states
 * are final.
 */
public final class PlanetCard {

    public static final int W = 96;
    public static final int H = 56;
    public static final int COMPACT_W = 64;
    public static final int COMPACT_H = 32;

    private static final int HULL = 0xFF0D1119;
    private static final int TROUGH = 0xFF0B1119;
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;
    private static final int VALUE = 0xFFB9C6D4;
    private static final int LABEL = 0xFFCFE7FF;
    private static final int IDLE_BORDER = 0xFF2E4A5A;
    private static final int TIER_BORDER = 0xFF262B33;
    private static final int GATE_BORDER = 0xFF582D70;
    private static final int SAFE = 0xFF54D46A;
    private static final int TIER1 = 0xFF8FD3FF;
    private static final int TIER2 = 0xFFF5C542;

    private PlanetCard() {
    }

    /** Whether {@code (mx, my)} is inside a card drawn at {@code (x, y)}. */
    public static boolean hit(int x, int y, boolean compact, double mx, double my) {
        int w = compact ? COMPACT_W : W;
        int h = compact ? COMPACT_H : H;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, PlanetCardData d, boolean compact,
            boolean hovered, long nowMs) {
        int w = compact ? COMPACT_W : W;
        int h = compact ? COMPACT_H : H;
        int accent = PlanetAccents.of(d.dimension());
        border(g, x, y, w, h, d.state(), accent, hovered);
        g.fill(x, y, x + w, y + h, HULL);
        int strip = compact ? 8 : 12;
        skyStrip(g, x, y, w, strip, d, accent, nowMs);

        boolean locked = d.state() == PlanetCardData.State.LOCKED_TIER || d.state() == PlanetCardData.State.LOCKED_GATE;
        int nameColour = d.state() == PlanetCardData.State.SELECTED ? 0xFFFFFFFF : TITLE;
        if (compact) {
            glyph(g, x + 2, y + 10, 6, d, locked);
            text(g, font, ellipsis(font, nameLine(d), 44), x + 10, y + 9, locked ? SUBTLE : nameColour);
            if (d.hasDayLength()) {
                dial(g, x + 55, y + 10, 7, d.clockPhase(), accent);
            }
            text(g, font, Component.literal(String.format(Locale.ROOT, "%.2fg", d.gravity())), x + 3, y + 21, VALUE);
            if (d.weatherNow() != WeatherEvent.NONE) {
                g.fill(x + 40, y + 22, x + 46, y + 28, weatherTint(d.weatherNow()));
            }
        } else {
            glyph(g, x + 3, y + 15, 9, d, locked);
            text(g, font, ellipsis(font, nameLine(d), 60), x + 16, y + 15, locked ? SUBTLE : nameColour);
            if (d.hasDayLength()) {
                dial(g, x + 82, y + 14, 11, d.clockPhase(), accent);
            }
            text(g, font, Component.literal(String.format(Locale.ROOT, "%.2f g", d.gravity())), x + 3, y + 27, VALUE);
            text(g, font, Component.translatable(PlanetCardData.suitKey(true, d.hazard(), d.needsRadLining())),
                    x + 46, y + 27, d.hazard() == Hazard.NONE && !d.needsRadLining() ? TIER1 : TIER2);
            if (d.weatherNow() == WeatherEvent.NONE) {
                text(g, font, Component.translatable("weather.nerospace.none"), x + 3, y + 37, SUBTLE);
            } else {
                text(g, font, ellipsis(font, weatherName(d.weatherNow()), 62), x + 3, y + 37, weatherTint(d.weatherNow()));
                int lit = Math.round(d.weatherIntensity() * 4.0F);
                for (int i = 0; i < 4; i++) {
                    int cx = x + 70 + i * 6;
                    g.fill(cx, y + 38, cx + 4, y + 42, i < lit ? weatherTint(d.weatherNow()) : TROUGH);
                }
            }
            Component pad = d.chosenPad().orElse(null);
            if (pad != null) {
                text(g, font, ellipsis(font, pad, 90), x + 3, y + 47, LABEL);
            } else {
                text(g, font, ellipsis(font, Component.translatable("gui.nerospace.card.no_pad"), 90), x + 3, y + 47, SUBTLE);
            }
            if (d.state() == PlanetCardData.State.SELECTED) {
                g.fill(x + w / 2 - 2, y + h - 2, x + w / 2 + 2, y + h, accent);
            }
        }
        if (locked) {
            g.fill(x, y, x + w, y + h, 0x800D1119); // desaturate: 50 % hull over the card
        }
        if (d.state() == PlanetCardData.State.HERE) {
            g.fill(x, y, x + w, y + strip, 0xA00D1119);
            text(g, font, Component.translatable("gui.nerospace.card.here"), x + 2, y + (compact ? 0 : 2), SUBTLE);
        }
    }

    /** The four lines the compact card's tooltip shows, so compact never hides information (004 §5.2). */
    public static List<Component> tooltip(PlanetCardData d) {
        List<Component> lines = new ArrayList<>();
        lines.add(d.name());
        switch (d.state()) {
            case LOCKED_TIER -> lines.add(Component.translatable("gui.nerospace.card.tier_tooltip", d.requiredTier()));
            case LOCKED_GATE -> lines.add(Component.translatable("gui.nerospace.card.gate_tooltip"));
            case HERE -> lines.add(Component.translatable("gui.nerospace.card.here"));
            default -> {
            }
        }
        lines.add(Component.literal(String.format(Locale.ROOT, "%.2f g  ", d.gravity()))
                .append(Component.translatable(PlanetCardData.suitKey(true, d.hazard(), d.needsRadLining()))));
        lines.add(d.weatherNow() == WeatherEvent.NONE ? Component.translatable("weather.nerospace.none")
                : weatherName(d.weatherNow()));
        lines.add(d.chosenPad().orElse(Component.translatable("gui.nerospace.card.no_pad")));
        return lines;
    }

    private static Component nameLine(PlanetCardData d) {
        return switch (d.state()) {
            case LOCKED_TIER -> Component.translatable("gui.nerospace.card.tier", d.requiredTier());
            case LOCKED_GATE -> Component.translatable("gui.nerospace.card.gate");
            default -> d.name();
        };
    }

    private static Component weatherName(WeatherEvent e) {
        return Component.translatable("weather.nerospace." + e.name().toLowerCase(Locale.ROOT));
    }

    private static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, PlanetCardData.State state,
            int accent, boolean hovered) {
        int c = switch (state) {
            case SELECTED -> accent;
            case LOCKED_TIER -> TIER_BORDER;
            case LOCKED_GATE -> GATE_BORDER;
            default -> hovered ? accent : IDLE_BORDER;
        };
        boolean dotted = state == PlanetCardData.State.LOCKED_TIER || state == PlanetCardData.State.LOCKED_GATE;
        if (!dotted) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, c);
            if (state == PlanetCardData.State.SELECTED) {
                g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x55FFFFFF);
            }
            return;
        }
        for (int i = -2; i < w + 2; i += 2) {
            g.fill(x + i, y - 2, x + i + 1, y, c);
            g.fill(x + i, y + h, x + i + 1, y + h + 2, c);
        }
        for (int j = -2; j < h + 2; j += 2) {
            g.fill(x - 2, y + j, x, y + j + 1, c);
            g.fill(x + w, y + j, x + w + 2, y + j + 1, c);
        }
    }

    private static void skyStrip(GuiGraphicsExtractor g, int x, int y, int w, int h, PlanetCardData d, int accent,
            long nowMs) {
        boolean station = d.dimension().equals(ModDimensions.STATION_LEVEL);
        int left = station ? 0xFF02040A : PlanetAccents.dark(d.dimension());
        int right = station ? 0xFF060A14 : mix(accent, 0xFF0D1119, 0.45F);
        for (int i = 0; i < w; i++) {
            g.fill(x + i, y, x + i + 1, y + h - 1, mix(left, right, i / (float) Math.max(1, w - 1)));
        }
        if (station) {
            for (int s = 0; s < 6; s++) {
                int sx = x + 3 + (s * 37 + 11) % (w - 6);
                int sy = y + 1 + (s * 5 + 2) % Math.max(1, h - 3);
                g.fill(sx, sy, sx + 1, sy + 1, ((nowMs / 300L + s) & 3) == 0 ? 0xFF6E84A0 : 0xFFFFFFFF);
            }
        } else {
            int sun = x + 4 + Math.round((w - 8) * Math.max(0.0F, Math.min(1.0F, d.clockPhase())));
            g.fill(sun, y + 2, sun + 2, y + 4, 0xFFFFE9A8);
        }
        g.fill(x, y + h - 1, x + w, y + h, accent);
    }

    /** Hazard glyph square (Stage 6 replaces it with the glyph sheet): NONE shows the green safe dot. */
    private static void glyph(GuiGraphicsExtractor g, int x, int y, int size, PlanetCardData d, boolean locked) {
        int c;
        if (locked) {
            c = SUBTLE;
        } else {
            c = switch (d.hazard()) {
                case HEAT -> 0xFFF0703C;
                case COLD -> 0xFF78D2F0;
                case RADIATION -> 0xFFF5C542;
                default -> SAFE;
            };
        }
        if (!locked && d.hazard() == Hazard.NONE) {
            int m = size / 2;
            g.fill(x + m - 2, y + m - 2, x + m + 2, y + m + 2, c);
            return;
        }
        g.fill(x, y, x + size, y + size, c);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF0D1119);
        g.fill(x + size / 2 - 1, y + 2, x + size / 2 + 1, y + size - 2, c);
    }

    /** A small clock dial: ring plus a hand at the phase (0 = midnight at the bottom, 0.5 = noon at the top). */
    private static void dial(GuiGraphicsExtractor g, int x, int y, int size, float phase, int accent) {
        g.fill(x, y, x + size, y + size, TROUGH);
        g.fill(x, y, x + size, y + 1, accent);
        g.fill(x, y + size - 1, x + size, y + size, accent);
        double a = phase * Math.PI * 2.0D;
        int cx = x + size / 2;
        int cy = y + size / 2;
        int hx = cx + (int) Math.round(Math.sin(a) * (size / 2 - 1));
        int hy = cy + (int) Math.round(Math.cos(a) * (size / 2 - 1));
        g.fill(hx, hy, hx + 1, hy + 1, 0xFFFFE9A8);
        g.fill(cx, cy, cx + 1, cy + 1, TITLE);
    }

    private static int weatherTint(WeatherEvent e) {
        return switch (e) {
            case SPORE_BLOOM -> 0xFF3FBF6F;
            case DUST_STORM -> 0xFFD9A066;
            case ASH_FALL -> 0xFFB0A8A0;
            case DIAMOND_DUST -> 0xFF9FE8FF;
            case FLARE -> 0xFFFF6B35;
            default -> SUBTLE;
        };
    }

    private static Component ellipsis(Font font, Component text, int maxWidth) {
        String s = text.getString();
        if (font.width(s) <= maxWidth) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…");
    }

    private static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int colour) {
        g.text(font, text, x, y, colour, false);
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int gg = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | bl;
    }
}
