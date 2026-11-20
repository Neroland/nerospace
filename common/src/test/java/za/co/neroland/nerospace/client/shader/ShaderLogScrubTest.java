package za.co.neroland.nerospace.client.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Privacy scrub over the log lines 2.4.0 added (shader compatibility, the shared sky path, the sky hook): each
 * carries a fixed message and at most a fixed reason string or the exception. None may format in a player
 * name, UUID, position, shader-pack name or file path.
 */
class ShaderLogScrubTest {

    private static final Pattern LOG_CALL = Pattern.compile("LOGGER\\.(?:info|warn|error|debug)\\(([^;]*);");
    private static final List<String> FORBIDDEN = List.of("getName", "getUUID", "getGameProfile", "blockPosition",
            "position()", "getX()", "getZ()", "getCurrentPackName", "packName", "getPath", "toAbsolutePath",
            "getDirectory", "getScoreboardName", "getStringUUID");

    private static String source(String relative) throws Exception {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace").resolve(relative);
            if (Files.isRegularFile(c)) {
                return Files.readString(c).replace("\r\n", "\n");
            }
        }
        throw new IllegalStateException("source not found: " + relative);
    }

    private static List<String> logCalls(String relative) throws Exception {
        List<String> calls = new ArrayList<>();
        Matcher m = LOG_CALL.matcher(source(relative));
        while (m.find()) {
            calls.add(m.group(1));
        }
        return calls;
    }

    @Test
    @DisplayName("the shader bridge logs one fixed line with a fixed reason")
    void bridge() throws Exception {
        List<String> calls = logCalls("client/shader/IrisBridge.java");
        assertEquals(1, calls.size());
        assertTrue(calls.get(0).contains("shader compatibility inactive"));
        String bridge = source("client/shader/IrisBridge.java");
        // Every reason passed to fail(...) is a string literal.
        Matcher m = Pattern.compile("fail\\(([^)]*)\\)").matcher(bridge);
        int reasons = 0;
        while (m.find()) {
            String arg = m.group(1).trim();
            if (arg.startsWith("String ")) {
                continue; // the declaration
            }
            assertTrue(arg.startsWith("\"") && arg.endsWith("\""), "fail(" + arg + ") must pass a literal");
            reasons++;
        }
        assertTrue(reasons >= 1);
    }

    @Test
    @DisplayName("no new log line formats in an identifier, a position, a pack name or a path")
    void noIdentifiers() throws Exception {
        for (String file : List.of("client/shader/IrisBridge.java", "client/shader/ShaderCompat.java",
                "client/shader/ShaderCompatRules.java", "client/sky/PlanetSkyDraw.java",
                "mixin/shader/IrisDimensionMixin.java", "mixin/shader/ShaderCompatMixinPlugin.java",
                "mixin/sky/SkyRendererPlanetMixin.java", "rocket/AscentMath.java", "rocket/IgnitionBurst.java")) {
            for (String call : logCalls(file)) {
                for (String bad : FORBIDDEN) {
                    assertFalse(call.contains(bad), file + " logs " + bad + ": " + call);
                }
            }
        }
    }
}
