package za.co.neroland.nerospace.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.station.ModuleKind;

/**
 * The HUD / screen glyph sheet ({@code textures/gui/hud_glyphs.png}, 128x32): four rows of sixteen 8x8 white
 * glyphs on transparent, tinted at draw time. Hazards differ by silhouette, not only by hue, so the sheet stays
 * readable for colour-blind players. Drawn at 8 or 16 px (nearest scaling) only.
 */
public final class Glyphs {

    public static final Identifier SHEET =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/hud_glyphs.png");
    public static final int SHEET_W = 128;
    public static final int SHEET_H = 32;
    public static final int CELL = 8;

    /** Every glyph on the sheet, with its cell (column, row). Row order is the sheet's. */
    public enum Glyph {
        // row 0: hazards and status
        HEAT(0, 0), COLD(1, 0), TOXIC(2, 0), RADIATION(3, 0), AIRLESS(4, 0), NONE(5, 0), SUIT(6, 0), LOCK(7, 0),
        GATE(8, 0), PAD(9, 0), DOCK(10, 0), GRAVITY_1(11, 0), GRAVITY_2(12, 0), GRAVITY_3(13, 0), GRAVITY_4(14, 0),
        MEMBER(15, 0),
        // row 1: weather and clock quarters
        W_NONE(0, 1), DUST_STORM(1, 1), ASH_FALL(2, 1), DIAMOND_DUST(3, 1), SPORE_BLOOM(4, 1), FLARE(5, 1),
        CLOCK_SUNRISE(6, 1), CLOCK_NOON(7, 1), CLOCK_SUNSET(8, 1), CLOCK_MIDNIGHT(9, 1),
        // row 2: UI
        ARROW_L(0, 2), ARROW_R(1, 2), CHEV_UP(2, 2), CHEV_DOWN(3, 2), CHECK(4, 2), STAR(5, 2), DOT_FILLED(6, 2),
        DOT_HOLLOW(7, 2), ROCKET(8, 2), SCROLL(9, 2), TOOLTIP(10, 2), REQUIRED(11, 2), OPTIONAL(12, 2), WALL(13, 2),
        GANTRY(14, 2),
        // row 3: station modules (ModuleKind tile order 0..6), then resources
        MOD_CORE(0, 3), MOD_HABITAT(1, 3), MOD_DOCKING(2, 3), MOD_SOLAR(3, 3), MOD_HYDRO(4, 3), MOD_GRAVITY(5, 3),
        MOD_OBSERVATORY(6, 3), FUEL(7, 3), O2(8, 3), POWER(9, 3);

        private final int col;
        private final int row;

        Glyph(int col, int row) {
            this.col = col;
            this.row = row;
        }

        public int u() {
            return this.col * CELL;
        }

        public int v() {
            return this.row * CELL;
        }
    }

    private static final Glyph[] MODULES = {
        Glyph.MOD_CORE, Glyph.MOD_HABITAT, Glyph.MOD_DOCKING, Glyph.MOD_SOLAR, Glyph.MOD_HYDRO, Glyph.MOD_GRAVITY,
        Glyph.MOD_OBSERVATORY };

    private Glyphs() {
    }

    /** Blits {@code glyph} at {@code (x, y)}, {@code size} 8 or 16 px, tinted with {@code argb}. */
    public static void draw(GuiGraphicsExtractor g, Glyph glyph, int x, int y, int size, int argb) {
        int s = size >= 16 ? 16 : CELL;
        g.blit(RenderPipelines.GUI_TEXTURED, SHEET, x, y, glyph.u(), glyph.v(), s, s, CELL, CELL, SHEET_W, SHEET_H,
                argb);
    }

    /** An 8 px glyph. */
    public static void draw(GuiGraphicsExtractor g, Glyph glyph, int x, int y, int argb) {
        draw(g, glyph, x, y, CELL, argb);
    }

    public static Glyph hazard(Hazard hazard) {
        return switch (hazard) {
            case HEAT -> Glyph.HEAT;
            case COLD -> Glyph.COLD;
            case RADIATION -> Glyph.RADIATION;
            default -> Glyph.NONE;
        };
    }

    public static Glyph weather(WeatherEvent event) {
        return switch (event) {
            case DUST_STORM -> Glyph.DUST_STORM;
            case ASH_FALL -> Glyph.ASH_FALL;
            case DIAMOND_DUST -> Glyph.DIAMOND_DUST;
            case SPORE_BLOOM -> Glyph.SPORE_BLOOM;
            case FLARE -> Glyph.FLARE;
            default -> Glyph.W_NONE;
        };
    }

    public static Glyph gravity(double g) {
        if (g <= 0.2) {
            return Glyph.GRAVITY_1;
        }
        if (g <= 0.5) {
            return Glyph.GRAVITY_2;
        }
        return g <= 0.9 ? Glyph.GRAVITY_3 : Glyph.GRAVITY_4;
    }

    /** The module glyph by Station Console tile index (0 = core); out-of-range tiles get the core glyph. */
    public static Glyph module(int tile) {
        return tile >= 0 && tile < MODULES.length ? MODULES[tile] : Glyph.MOD_CORE;
    }

    public static Glyph module(ModuleKind kind) {
        return kind == ModuleKind.CORE || kind == ModuleKind.CORE_LEGACY ? Glyph.MOD_CORE : module(kind.tile());
    }
}
