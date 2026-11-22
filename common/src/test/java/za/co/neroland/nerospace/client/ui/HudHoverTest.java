package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Oxygen and weather hover amounts, without a client. */
class HudHoverTest {

    @Test
    @DisplayName("the oxygen readout covers the bar, and the weather cells are the four-cell strip")
    void geometry() {
        int[] oxygen = HudHover.oxygenReadout(100, 200);
        int[] bar = new int[] { HudLayout.barX(100), HudLayout.barY(200), HudLayout.BAR_W, HudLayout.BAR_H };
        assertTrue(HudHover.contains(oxygen, bar[0] + 1, bar[1] + 1));
        assertFalse(HudHover.contains(oxygen, bar[0] - 20, bar[1]));

        int[] cells = HudHover.weatherCells(40, 10);
        assertEquals(HudHover.WEATHER_CELLS_W, cells[2]);
        assertTrue(HudHover.contains(cells, 40, 12));
        assertFalse(HudHover.contains(cells, 40 + HudHover.WEATHER_CELLS_W, 12));
    }

    @Test
    @DisplayName("hover amounts are whole percents, clamped")
    void amounts() {
        assertEquals(41, HudHover.percent(123, 300));
        assertEquals(0, HudHover.percent(5, 0));
        assertEquals(100, HudHover.percent(400, 300));
        assertEquals(75, HudHover.weatherPercent(0.75F));
        assertEquals(0, HudHover.weatherPercent(Float.NaN));
        assertEquals(100, HudHover.weatherPercent(4.0F));
        assertTrue(Double.isNaN(HudHover.toGui(10, 0, 100)));
        assertEquals(50.0D, HudHover.toGui(100, 200, 100), 0.001D);
    }
}
