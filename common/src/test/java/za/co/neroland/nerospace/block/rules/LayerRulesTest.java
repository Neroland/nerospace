package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.3 Ash Layer, §3 layer accumulation. */
@SuppressWarnings("unused")
class LayerRulesTest {

    @Test
    @DisplayName("weather adds one layer up to 4; players stack to 8")
    void accumulate() {
        assertEquals(1, LayerRules.nextLayers(0, 4, true));
        assertEquals(4, LayerRules.nextLayers(3, 4, true));
        assertEquals(4, LayerRules.nextLayers(4, 4, true), "weather cap");
        assertEquals(5, LayerRules.nextLayers(4, 4, false), "players stack");
        assertEquals(8, LayerRules.nextLayers(8, 4, false));
        assertEquals(6, LayerRules.nextLayers(6, 4, true), "never shrinks");
    }

    @Test
    @DisplayName("config may lower the weather cap, never raise it")
    void cap() {
        assertEquals(4, LayerRules.nextLayers(4, 8, true));
        assertEquals(2, LayerRules.nextLayers(2, 2, true));
        assertEquals(0, LayerRules.nextLayers(0, 0, true));
        assertEquals(40, LayerRules.INTERVAL_TICKS);
    }

    @Test
    @DisplayName("slows at 3+ layers to 0.7; melts only beside fire/lava")
    void slowAndMelt() {
        assertEquals(1.0F, LayerRules.slowFactor(2));
        assertEquals(0.7F, LayerRules.slowFactor(3));
        assertEquals(0.7F, LayerRules.slowFactor(8));
        assertEquals(3, LayerRules.melt(4, true));
        assertEquals(4, LayerRules.melt(4, false));
        assertEquals(0, LayerRules.melt(1, true));
        assertEquals(0, LayerRules.melt(0, true));
    }
}
