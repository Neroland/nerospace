package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lunar Basalt sinter. */
@SuppressWarnings("unused")
class SolarSinterRulesTest {

    @Test
    @DisplayName("only placed basalt, sky-exposed, daylight >= 0.5, on Luna; 1 in 6")
    void sinter() {
        assertTrue(SolarSinterRules.canSinter(true, true, 0.5, true));
        assertFalse(SolarSinterRules.canSinter(false, true, 1.0, true), "worldgen maria never sinter");
        assertFalse(SolarSinterRules.canSinter(true, false, 1.0, true));
        assertFalse(SolarSinterRules.canSinter(true, true, 0.49, true));
        assertFalse(SolarSinterRules.canSinter(true, true, 1.0, false));
        assertEquals(6, SolarSinterRules.ONE_IN);
        assertTrue(SolarSinterRules.rolled(0));
        assertFalse(SolarSinterRules.rolled(5));
    }

    @Test
    @DisplayName("the placed flag gates random ticking itself: worldgen basalt (Luna's default block) never ticks")
    void placedFlagGatesTicking() throws Exception {
        assertTrue(SolarSinterRules.ticks(true));
        assertFalse(SolarSinterRules.ticks(false));
        String s = source("block/SolarSinterBlock.java");
        org.junit.jupiter.api.Assumptions.assumeTrue(s != null, "common sources not found");
        int at = s.indexOf("boolean isRandomlyTicking(BlockState state)");
        assertTrue(at >= 0, "SolarSinterBlock must override isRandomlyTicking");
        assertTrue(s.substring(at, s.indexOf('}', at)).contains("SolarSinterRules.ticks(playerPlaced(state))"));
    }

    @Test
    @DisplayName("Dry Ice sublimates on block light only (sky light at Mars' fixed noon must not erase the polar cap)")
    void dryIceReadsBlockLight() throws Exception {
        String s = source("block/DryIceBlock.java");
        org.junit.jupiter.api.Assumptions.assumeTrue(s != null, "common sources not found");
        assertTrue(s.contains("getBrightness(LightLayer.BLOCK, pos)"));
        assertFalse(s.contains("getMaxLocalRawBrightness"), "raw brightness includes sky light");
        assertFalse(DryIceRules.sublimates(0), "a dark cap under a 15 sky stays");
        assertTrue(DryIceRules.sublimates(14), "a torch beside it fumes it");
    }

    static String source(String rel) throws java.io.IOException {
        for (java.nio.file.Path p = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
                p != null; p = p.getParent()) {
            java.nio.file.Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace/" + rel);
            if (java.nio.file.Files.isRegularFile(c)) {
                return java.nio.file.Files.readString(c);
            }
        }
        return null;
    }
}
