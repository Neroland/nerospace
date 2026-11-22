package za.co.neroland.nerospace.telemetry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** The give-only test block's event text must not carry a block position. */
class SentryTestBlockPrivacyTest {

    @Test
    void eventTextHasNoPosition() throws Exception {
        String src = source("telemetry/SentryTestBlock.java");
        assertTrue(src.contains("sentry_test block placed"));
        assertFalse(src.contains("toShortString"));
        assertFalse(src.contains("getX("));
        assertFalse(src.contains("pos.to"));
    }

    private static String source(String rel) throws Exception {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace").resolve(rel);
            if (Files.isRegularFile(c)) {
                return Files.readString(c);
            }
        }
        throw new IllegalStateException(rel);
    }
}
