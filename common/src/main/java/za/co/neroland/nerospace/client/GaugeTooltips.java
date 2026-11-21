package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import org.jetbrains.annotations.Nullable;

/**
 * The shared gauge tooltips for every Nerospace machine screen: each gauge names itself, gives amount /
 * capacity with units, and its rate. Builders append lines to a list; {@link TexturedContainerScreen}
 * positions and emits them through {@link #show}. Numbers come from {@link GaugeMath}; every word is an
 * English source lang key. Machine-scoped numbers only, already on screen: no identities, no coordinates.
 */
public final class GaugeTooltips {

    /** Tank change per second treated as steady. */
    public static final double TANK_DEADBAND_PER_SECOND = 0.5D;
    /** Generic level change per second treated as steady. */
    public static final double LEVEL_DEADBAND_PER_SECOND = 0.05D;

    private GaugeTooltips() {
    }

    /** A gauge lang line ({@code gui.nerospace.gauge.<key>}). */
    public static Component tr(String key, Object... args) {
        return Component.translatable("gui.nerospace.gauge." + key, args);
    }

    // --- energy -----------------------------------------------------------------------------------

    /** Energy with the default name ("Energy"). */
    public static void energy(List<Component> out, long stored, long capacity, double netPerTick,
            long usedPerTick, long deliveredPerTick) {
        energy(out, null, stored, capacity, netPerTick, usedPerTick, deliveredPerTick);
    }

    /**
     * Energy: name, stored / capacity in FE, and the rate. With a known draw ({@code usedPerTick > 0})
     * the net change splits into "in" and "used"; otherwise the net change alone. {@code deliveredPerTick}
     * adds its own line when non-negative.
     */
    public static void energy(List<Component> out, @Nullable Component name, long stored, long capacity,
            double netPerTick, long usedPerTick, long deliveredPerTick) {
        out.add(name != null ? name : tr("energy"));
        if (capacity <= 0) {
            out.add(tr("energy.no_buffer"));
            return;
        }
        out.add(tr("energy.amount", GaugeMath.compact(stored), GaugeMath.compact(capacity),
                GaugeMath.percent(stored, capacity)));
        if (usedPerTick > 0) {
            double in = Math.max(0D, netPerTick + usedPerTick);
            out.add(tr("energy.flow", GaugeMath.signedRate(in), GaugeMath.signedRate(-usedPerTick)));
        } else {
            out.add(tr("energy.net", GaugeMath.signedRate(netPerTick)));
        }
        if (deliveredPerTick >= 0) {
            out.add(tr("energy.delivered", GaugeMath.rate(deliveredPerTick)));
        }
        if (stored >= capacity) {
            out.add(tr("energy.full"));
        }
    }

    // --- progress ---------------------------------------------------------------------------------

    /**
     * Progress: name, percent, time left at the current speed (a falling gauge reads as fuel burning
     * down), the stall cause when one is given, and the operation rate when positive.
     */
    public static void progress(List<Component> out, boolean working, int permille, double permillePerTick,
            @Nullable Component stallCause, int opsPerMinute) {
        out.add(tr("progress"));
        if (!working) {
            out.add(stallCause != null ? stallCause : tr("progress.idle"));
            if (opsPerMinute > 0) {
                out.add(tr("progress.ops", GaugeMath.grouped(opsPerMinute)));
            }
            return;
        }
        out.add(tr("progress.percent", Math.max(0, Math.min(100, Math.round(permille / 10F)))));
        if (stallCause != null) {
            out.add(stallCause);
        } else {
            long ticks = GaugeMath.ticksRemaining(permille, permillePerTick);
            if (ticks >= 0) {
                out.add(tr(permillePerTick < 0 ? "progress.burn_left" : "progress.left", GaugeMath.duration(ticks)));
            }
        }
        if (opsPerMinute > 0) {
            out.add(tr("progress.ops", GaugeMath.grouped(opsPerMinute)));
        }
    }

    // --- tanks ------------------------------------------------------------------------------------

    /**
     * A tank: its role (optional), the medium (omitted when {@code null}; "Empty" when there is none),
     * amount / capacity in mB, and the fill or drain rate.
     */
    public static void tank(List<Component> out, @Nullable Component role, @Nullable Component medium,
            long amount, long capacity, double mbPerTick) {
        if (role != null) {
            out.add(role);
        }
        if (amount <= 0) {
            out.add(tr("tank.empty"));
        } else if (medium != null) {
            out.add(medium);
        }
        out.add(tr("tank.amount", GaugeMath.grouped(Math.max(0L, amount)), GaugeMath.grouped(Math.max(0L, capacity)),
                GaugeMath.percent(amount, capacity)));
        double perSecond = mbPerTick * GaugeMath.TICKS_PER_SECOND;
        int trend = GaugeMath.trend(perSecond, TANK_DEADBAND_PER_SECOND);
        out.add(trend > 0 ? tr("tank.filling", GaugeMath.rate(perSecond))
                : trend < 0 ? tr("tank.draining", GaugeMath.rate(perSecond)) : tr("tank.steady"));
    }

    // --- generic levels ---------------------------------------------------------------------------

    /** A unitless level (hydration, a percentage): name, amount / capacity (and %), and its trend per second. */
    public static void level(List<Component> out, Component name, long amount, long capacity, double perSecond) {
        out.add(name);
        out.add(tr("level.amount", GaugeMath.grouped(Math.max(0L, amount)), GaugeMath.grouped(Math.max(0L, capacity)),
                GaugeMath.percent(amount, capacity)));
        int trend = GaugeMath.trend(perSecond, LEVEL_DEADBAND_PER_SECOND);
        out.add(trend > 0 ? tr("level.rising", GaugeMath.rate(perSecond))
                : trend < 0 ? tr("level.falling", GaugeMath.rate(perSecond)) : tr("level.steady"));
    }

    // --- emission ---------------------------------------------------------------------------------

    /**
     * Wrap every line to {@code width} pixels so a long cause never runs off a small window (GUI scale 4
     * on 1280x720 leaves 320 px). Wrapped lines lose styling; these tooltips use none.
     */
    public static List<Component> wrap(Font font, List<Component> lines, int width) {
        List<Component> wrapped = new ArrayList<>(lines.size());
        int w = Math.max(60, width);
        for (Component line : lines) {
            if (font.width(line) <= w) {
                wrapped.add(line);
                continue;
            }
            for (FormattedText part : font.getSplitter().splitLines(line, w, Style.EMPTY)) {
                wrapped.add(Component.literal(part.getString()));
            }
        }
        return wrapped;
    }

    /** The wrap width for a screen {@code screenWidth} wide: half the window, 100..220 px. */
    public static int wrapWidth(int screenWidth) {
        return Math.max(100, Math.min(220, screenWidth / 2));
    }

    /** Emit a tooltip at the mouse (deferred on 1.21.6+; the legacy rename draws it immediately before that). */
    public static void show(GuiGraphicsExtractor extractor, Font font, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }
        extractor.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
