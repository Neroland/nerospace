package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.VentWarmthRules;

/** The reach of Europa's cold shield: the scan sphere and the basalt radius. */
class EuropaWarmthTest {

    @Test
    @DisplayName("warm basalt shields within 2 blocks (squared distance 4), not beyond")
    void basaltReach() {
        assertEquals(2, EuropaWarmth.BASALT_RADIUS);
        assertTrue(EuropaWarmth.basaltReaches(0));
        assertTrue(EuropaWarmth.basaltReaches(4));
        assertFalse(EuropaWarmth.basaltReaches(5));
    }

    @Test
    @DisplayName("the scan covers the radius-5 sphere: 515 cells, and the basalt reach lies inside it")
    void sphere() {
        int r = (int) VentWarmthRules.RADIUS;
        int cells = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r * r) {
                        cells++;
                    }
                }
            }
        }
        assertEquals(515, cells);
        assertTrue(EuropaWarmth.BASALT_RADIUS < r);
    }

    @Test
    @DisplayName("the shield is looked for on Europa only")
    void europaOnly() throws java.io.IOException {
        assertTrue(EuropaWarmth.appliesTo(id("nerospace", "europa")));
        assertFalse(EuropaWarmth.appliesTo(id("nerospace", "glacira")));
        assertFalse(EuropaWarmth.appliesTo(id("nerospace", "nyxara")));
        assertFalse(EuropaWarmth.appliesTo(id("minecraft", "overworld")));
        // The constant names the same dimension ModDimensions registers (read from the source, no bootstrap).
        assertTrue(java.nio.file.Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/registry/ModDimensions.java")).contains("EUROPA_LEVEL = level(\"europa\")"));
    }

    private static net.minecraft.resources.Identifier id(String ns, String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(ns, path);
    }

    @Test
    @DisplayName("an active vent shields within 5, an inactive one never; warm basalt (distance <= 3) within 2")
    void cellPredicate() {
        EuropaWarmth.Cell vent = EuropaWarmth.Cell.VENT;
        EuropaWarmth.Cell basalt = EuropaWarmth.Cell.BASALT;
        assertTrue(EuropaWarmth.cellShields(vent, true, 0, 0));
        assertTrue(EuropaWarmth.cellShields(vent, true, 0, 25));
        assertFalse(EuropaWarmth.cellShields(vent, true, 0, 26));
        assertFalse(EuropaWarmth.cellShields(vent, false, 0, 0));
        assertTrue(EuropaWarmth.cellShields(basalt, false, 1, 4));
        assertTrue(EuropaWarmth.cellShields(basalt, false, 3, 1));
        assertFalse(EuropaWarmth.cellShields(basalt, false, 4, 1), "cold basalt (distance 4) does not shield");
        assertFalse(EuropaWarmth.cellShields(basalt, false, 1, 5), "warm basalt beyond 2 blocks does not shield");
        assertFalse(EuropaWarmth.cellShields(EuropaWarmth.Cell.OTHER, true, 1, 0));
    }

    @Test
    @DisplayName("client cache: hit on same level, position and bucket; miss on a new level, a move or the next bucket")
    void clientCache() {
        Object level = new Object();
        Object other = new Object();
        long pos = 42L;
        long bucket = za.co.neroland.nerospace.world.planet.ColdShelterRules.bucket(100L);
        assertTrue(EuropaWarmth.clientCacheHit(level, level, pos, bucket, pos, bucket));
        assertFalse(EuropaWarmth.clientCacheHit(level, other, pos, bucket, pos, bucket), "level changed");
        assertFalse(EuropaWarmth.clientCacheHit(null, level, pos, bucket, pos, bucket), "no entry yet");
        assertFalse(EuropaWarmth.clientCacheHit(null, null, pos, bucket, pos, bucket), "unloaded level");
        assertFalse(EuropaWarmth.clientCacheHit(level, level, pos, bucket, pos + 1, bucket), "moved");
        long next = za.co.neroland.nerospace.world.planet.ColdShelterRules.bucket(110L);
        assertFalse(EuropaWarmth.clientCacheHit(level, level, pos, bucket, pos, next), "expired after 10 ticks");
        assertTrue(EuropaWarmth.clientCacheHit(level, level, pos, bucket, pos,
                za.co.neroland.nerospace.world.planet.ColdShelterRules.bucket(109L)), "same bucket");
    }
}
