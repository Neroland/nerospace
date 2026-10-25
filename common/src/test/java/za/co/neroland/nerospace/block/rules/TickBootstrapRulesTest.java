package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickBootstrapRulesTest {

    @Test
    void bootstrapsOnlyWithoutPendingTick() {
        assertTrue(TickBootstrapRules.shouldBootstrap(false));
        assertFalse(TickBootstrapRules.shouldBootstrap(true));
    }

    @Test
    void clockLitStartsNextTick() {
        assertEquals(1, TickBootstrapRules.clockLitDelay());
    }

    @Test
    void geyserDelayStaysWithinHalfInterval() {
        int half = GeyserRules.INTERVAL_TICKS / 2;
        assertEquals(1, TickBootstrapRules.geyserDelay(0));
        assertEquals(half, TickBootstrapRules.geyserDelay(half - 1));
        for (int r = -500; r < 500; r++) {
            int d = TickBootstrapRules.geyserDelay(r);
            assertTrue(d >= 1 && d <= half, "delay " + d);
        }
    }

    @Test
    void gravityOverrideRegistersOnlyWhenMissing() {
        assertTrue(TickBootstrapRules.shouldRegisterOverride(false));
        assertFalse(TickBootstrapRules.shouldRegisterOverride(true));
    }

    @Test
    void gravityShardBlockRandomTicks() throws Exception {
        java.nio.file.Path src = null;
        for (java.nio.file.Path p = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
                p != null && src == null; p = p.getParent()) {
            java.nio.file.Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace/world/planet/nyxara/NyxaraContent.java");
            if (java.nio.file.Files.isRegularFile(c)) {
                src = c;
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(src != null, "common sources not found");
        String s = java.nio.file.Files.readString(src);
        int start = s.indexOf("\"gravity_shard_block\"");
        int end = s.indexOf(";", start);
        assertTrue(start >= 0 && end > start, "gravity_shard_block registration not found");
        assertTrue(s.substring(start, end).contains(".randomTicks()"),
                "a worldgen-placed Gravity Shard Block needs randomTicks() to register its override");
    }

    @Test
    void gravityPlateRandomTicks() throws Exception {
        java.nio.file.Path src = null;
        for (java.nio.file.Path p = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
                p != null && src == null; p = p.getParent()) {
            java.nio.file.Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace/world/station/StationContent.java");
            if (java.nio.file.Files.isRegularFile(c)) {
                src = c;
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(src != null, "common sources not found");
        String s = java.nio.file.Files.readString(src);
        int start = s.indexOf("\"gravity_plate\"");
        int end = s.indexOf(";", start);
        assertTrue(start >= 0 && end > start, "gravity_plate registration not found");
        assertTrue(s.substring(start, end).contains(".randomTicks()"),
                "a Gravity Plate placed without onPlace needs randomTicks() to register its override");
    }

    @Test
    void basesDeclareTheRandomTickBootstrap() throws Exception {
        for (Class<?> base : new Class<?>[] {
                za.co.neroland.nerospace.block.ClockLitBlock.class, za.co.neroland.nerospace.block.GeyserBlock.class,
                za.co.neroland.nerospace.block.GravityOverrideBlock.class }) {
            boolean found = false;
            for (java.lang.reflect.Method m : base.getDeclaredMethods()) {
                found |= m.getName().equals("randomTick");
            }
            assertTrue(found, base.getSimpleName() + " must bootstrap its schedule on a random tick");
        }
    }
}
