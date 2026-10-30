package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.station.ModuleKind;

/** Every enum constant maps to a glyph, every glyph has its own painted cell on the 128x32 sheet. */
class GlyphsTest {

    private static BufferedImage sheet() throws IOException {
        try (InputStream in = GlyphsTest.class.getResourceAsStream("/assets/nerospace/textures/gui/hud_glyphs.png")) {
            assertNotNull(in, "hud_glyphs.png missing");
            return ImageIO.read(in);
        }
    }

    @Test
    @DisplayName("every glyph is a distinct, painted 8x8 cell inside the sheet")
    void cellsArePaintedAndDistinct() throws IOException {
        BufferedImage img = sheet();
        assertEquals(Glyphs.SHEET_W, img.getWidth());
        assertEquals(Glyphs.SHEET_H, img.getHeight());
        Set<Integer> cells = new HashSet<>();
        for (Glyphs.Glyph g : Glyphs.Glyph.values()) {
            assertTrue(g.u() + Glyphs.CELL <= Glyphs.SHEET_W && g.v() + Glyphs.CELL <= Glyphs.SHEET_H, g.name());
            assertTrue(cells.add(g.u() * 1000 + g.v()), "duplicate cell " + g);
            if (g == Glyphs.Glyph.W_NONE) {
                continue; // deliberately empty: "no weather"
            }
            boolean painted = false;
            for (int y = 0; y < Glyphs.CELL && !painted; y++) {
                for (int x = 0; x < Glyphs.CELL && !painted; x++) {
                    painted = (img.getRGB(g.u() + x, g.v() + y) >>> 24) != 0;
                }
            }
            assertTrue(painted, "empty glyph cell " + g);
        }
    }

    @Test
    @DisplayName("hazards, weather, modules and gravity all map; hazard glyphs differ in silhouette")
    void enumsMap() throws IOException {
        Set<Glyphs.Glyph> hazards = new HashSet<>();
        for (Hazard h : Hazard.values()) {
            assertNotNull(Glyphs.hazard(h));
            hazards.add(Glyphs.hazard(h));
        }
        assertEquals(Hazard.values().length, hazards.size(), "each hazard has its own glyph");
        for (WeatherEvent e : WeatherEvent.values()) {
            assertNotNull(Glyphs.weather(e));
        }
        for (ModuleKind k : ModuleKind.values()) {
            assertNotNull(Glyphs.module(k));
        }
        assertEquals(Glyphs.Glyph.MOD_OBSERVATORY, Glyphs.module(ModuleKind.OBSERVATORY));
        assertEquals(Glyphs.Glyph.MOD_CORE, Glyphs.module(ModuleKind.CORE_LEGACY));
        assertEquals(Glyphs.Glyph.GRAVITY_1, Glyphs.gravity(0.1));
        assertEquals(Glyphs.Glyph.GRAVITY_2, Glyphs.gravity(0.38));
        assertEquals(Glyphs.Glyph.GRAVITY_3, Glyphs.gravity(0.8));
        assertEquals(Glyphs.Glyph.GRAVITY_4, Glyphs.gravity(1.0));
        // Silhouettes, not hue: the hazard cells' alpha masks are pairwise different.
        BufferedImage img = sheet();
        Glyphs.Glyph[] list = hazards.toArray(new Glyphs.Glyph[0]);
        for (int i = 0; i < list.length; i++) {
            for (int j = i + 1; j < list.length; j++) {
                assertTrue(!sameMask(img, list[i], list[j]), list[i] + " vs " + list[j]);
            }
        }
    }

    private static boolean sameMask(BufferedImage img, Glyphs.Glyph a, Glyphs.Glyph b) {
        for (int y = 0; y < Glyphs.CELL; y++) {
            for (int x = 0; x < Glyphs.CELL; x++) {
                boolean pa = (img.getRGB(a.u() + x, a.v() + y) >>> 24) != 0;
                boolean pb = (img.getRGB(b.u() + x, b.v() + y) >>> 24) != 0;
                if (pa != pb) {
                    return false;
                }
            }
        }
        return true;
    }
}
