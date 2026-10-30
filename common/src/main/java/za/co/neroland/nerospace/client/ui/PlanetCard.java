package za.co.neroland.nerospace.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The planet card: a pure drawing + hit-test helper, not a widget — the host screen owns focus and
 * clicks. Standard 96×56 (selected destination) and compact 64×32 (list rows). Hazards, locks and gates are drawn
 * from the glyph sheet ({@link Glyphs}); the clock is a {@link ClockDial} lit by the planet's daylight curve.
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
            glyph(g, x + 1, y + 9, d, locked);
            text(g, font, ellipsis(font, nameLine(d), 44), x + 10, y + 9, locked ? SUBTLE : nameColour);
            if (d.hasDayLength()) {
                dial(g, x + 54, y + 9, 3, d, accent);
            }
            text(g, font, Component.literal(String.format(Locale.ROOT, "%.2fg", d.gravity())), x + 3, y + 21, VALUE);
            if (d.weatherNow() != WeatherEvent.NONE) {
                g.fill(x + 40, y + 22, x + 46, y + 28, weatherTint(d.weatherNow()));
            }
            if (d.state() == PlanetCardData.State.SELECTED) {
                selectedMarks(g, x, y, h, accent);
            }
        } else {
            glyph(g, x + 4, y + 15, d, locked);
            text(g, font, ellipsis(font, nameLine(d), 60), x + 16, y + 15, locked ? SUBTLE : nameColour);
            if (d.hasDayLength()) {
                dial(g, x + 82, y + 14, 5, d, accent);
            }
            text(g, font, Component.literal(String.format(Locale.ROOT, "%.2f g", d.gravity())), x + 3, y + 27, VALUE);
            text(g, font, Component.translatable(PlanetCardData.suitKey(true, d.hazard(), d.needsRadLining(),
                    d.requiredSuit())),
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

    /**
     * A selected compact card is marked by shape, not colour alone: a pointer notch left of the card
     * and a check mark bottom-right, both in the planet accent.
     */
    private static void selectedMarks(GuiGraphicsExtractor g, int x, int y, int h, int accent) {
        int cy = y + h / 2;
        g.fill(x - 6, cy - 3, x - 5, cy + 4, accent);
        g.fill(x - 5, cy - 2, x - 4, cy + 3, accent);
        g.fill(x - 4, cy - 1, x - 3, cy + 2, accent);
        int cx = x + 54;
        int by = y + 27;
        g.fill(cx, by - 2, cx + 1, by - 1, accent);
        g.fill(cx + 1, by - 1, cx + 2, by, accent);
        g.fill(cx + 2, by - 2, cx + 3, by - 1, accent);
        g.fill(cx + 3, by - 3, cx + 4, by - 2, accent);
        g.fill(cx + 4, by - 4, cx + 5, by - 3, accent);
        g.fill(cx + 5, by - 5, cx + 6, by - 4, accent);
    }

    /** The four lines the compact card's tooltip shows, so compact never hides information. */
    public static List<Component> tooltip(PlanetCardData d) {
        return tooltip(d, false);
    }

    /**
     * The compact card's tooltip; with {@code selectionHints} (a destination list) the second line says whether a
     * click selects the card, or why it cannot (an unselectable card always explains itself).
     */
    public static List<Component> tooltip(PlanetCardData d, boolean selectionHints) {
        List<Component> lines = new ArrayList<>();
        lines.add(d.name());
        Component reason = switch (d.state()) {
            case LOCKED_TIER -> Component.translatable("gui.nerospace.card.tier_tooltip", d.requiredTier());
            case LOCKED_GATE -> Component.translatable("gui.nerospace.card.gate_tooltip");
            case HERE -> selectionHints ? Component.translatable("gui.nerospace.dest.reason.here")
                    : Component.translatable("gui.nerospace.card.here");
            default -> null;
        };
        if (reason != null) {
            lines.add(selectionHints ? Component.translatable("gui.nerospace.dest.cant_select", reason)
                    .withStyle(ChatFormatting.GOLD) : reason);
        } else if (selectionHints) {
            lines.add(d.state() == PlanetCardData.State.SELECTED
                    ? Component.translatable("gui.nerospace.card.selected").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.nerospace.card.click_select").withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.literal(String.format(Locale.ROOT, "%.2f g  ", d.gravity()))
                .append(Component.translatable(PlanetCardData.suitKey(true, d.hazard(), d.needsRadLining(),
                        d.requiredSuit()))));
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

    /** The hazard glyph from the glyph sheet (LOCK / GATE when locked, the green NONE dot when safe). */
    private static void glyph(GuiGraphicsExtractor g, int x, int y, PlanetCardData d, boolean locked) {
        if (locked) {
            Glyphs.draw(g, d.state() == PlanetCardData.State.LOCKED_GATE ? Glyphs.Glyph.GATE : Glyphs.Glyph.LOCK,
                    x, y, SUBTLE);
            return;
        }
        int c = switch (d.hazard()) {
            case HEAT -> 0xFFF0703C;
            case COLD -> 0xFF78D2F0;
            case RADIATION -> 0xFFF5C542;
            default -> SAFE;
        };
        Glyphs.draw(g, Glyphs.hazard(d.hazard()), x, y, c);
    }

    /**
     * The card's clock dial ({@link ClockDial}): phase 0 is sunrise (hand to the right), 0.25 noon (up), the face
     * lit by the planet's daylight curve; a fixed-clock planet shows only the sun dot.
     */
    private static void dial(GuiGraphicsExtractor g, int x, int y, int r, PlanetCardData d, int accent) {
        java.util.Optional<PlanetDefinition> planet = Planets.byDimension(d.dimension());
        DaylightCurve curve = planet.map(PlanetDefinition::daylight).orElse(null);
        boolean fixed = planet.map(p -> p.traits().fixedClock()).orElse(Boolean.FALSE);
        za.co.neroland.nerospace.client.sky.SkyProfile profile = planet.map(
                p -> za.co.neroland.nerospace.client.sky.SkyProfiles.get(p.traits().skyProfile())).orElse(null);
        int sun = profile != null ? ClockDial.sunArgb(profile) : 0xFFFFE9A8;
        ClockDial.draw(g, x, y, r, d.clockPhase(), accent, sun, fixed,
                ClockDial.windowsFor(planet.orElse(null), profile), curve);
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
