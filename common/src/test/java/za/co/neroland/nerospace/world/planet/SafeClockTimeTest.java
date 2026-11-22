package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The throwing clock is tried once, then every caller stays on game time. */
class SafeClockTimeTest {

    @AfterEach
    void reset() {
        SafeClockTime.resetForTest();
    }

    @Test
    @DisplayName("a working clock is used, and the game time is not")
    void usesTheClockWhileItWorks() {
        assertEquals(1200L, SafeClockTime.read(() -> 1200L, () -> 9L));
        assertEquals(1300L, SafeClockTime.read(() -> 1300L, () -> 9L));
    }

    @Test
    @DisplayName("the first throw sticks, and later calls do not touch the clock again")
    void fallsBackForTheRestOfTheProcess() {
        assertEquals(50L, SafeClockTime.read(() -> {
            throw new IllegalStateException("Time marker sunrise does not exist");
        }, () -> 50L));
        assertEquals(60L, SafeClockTime.read(() -> {
            throw new AssertionError("clock was called after it had failed");
        }, () -> 60L));
    }

    @Test
    @DisplayName("only SafeClockTime calls getDefaultClockTime")
    void singleCallSite() throws Exception {
        Path root = repo();
        try (Stream<Path> files = Files.walk(root.resolve("common/src/main/java"))) {
            long callers = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("getDefaultClockTime()");
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .filter(p -> !p.getFileName().toString().equals("SafeClockTime.java"))
                    .count();
            assertEquals(0L, callers);
        }
        String clock = Files.readString(root.resolve(
                "common/src/main/java/za/co/neroland/nerospace/world/planet/PlanetClock.java"));
        String client = Files.readString(root.resolve(
                "common/src/main/java/za/co/neroland/nerospace/client/ClientPlanetClock.java"));
        assertTrue(clock.contains("SafeClockTime.of(level)"));
        assertTrue(client.contains("SafeClockTime.of(level)"));
    }

    private static Path repo() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("common/src/main/java"))) {
                return p;
            }
        }
        throw new IllegalStateException("repo root not found from " + System.getProperty("user.dir"));
    }
}
