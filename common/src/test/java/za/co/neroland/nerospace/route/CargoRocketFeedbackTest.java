package za.co.neroland.nerospace.route;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** A cargo rocket with no pad still consumes the click, and now says why. */
class CargoRocketFeedbackTest {

    @Test
    void noPadIsExplained() throws Exception {
        String src = source("route/CargoRocketEntity.java");
        int pad = src.indexOf("CargoPadBlockEntity pad = padBelow()");
        int message = src.indexOf("entity.nerospace.cargo_rocket.no_pad", pad);
        int consume = src.indexOf("InteractionResult.CONSUME", message);
        int open = src.indexOf("MenuOpener.openOrConsume", consume);
        assertTrue(pad >= 0 && message > pad && consume > message && open > consume, src.substring(pad, pad + 600));
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
