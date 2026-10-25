package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * A pod landing produces exactly one rocket and no return kit: in 1.6.0 the kit WAS the arriving rocket; in 2.0
 * the arriving rocket and its fuel persist on the pod, so filling the pod would duplicate both (farmable).
 */
class PodArrivalTest {

    static String source(String rel) throws Exception {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace/rocket").resolve(rel);
            if (Files.isRegularFile(c)) {
                return Files.readString(c).replace("\r\n", "\n");
            }
        }
        throw new IllegalStateException("source not found: " + rel);
    }

    static String body(String src, String signature) {
        int start = src.indexOf(signature);
        int open = src.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            char ch = src.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}' && --depth == 0) {
                return src.substring(open, i + 1);
            }
        }
        throw new IllegalStateException("unbalanced " + signature);
    }

    static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    @Test
    void podIsPlacedEmpty() throws Exception {
        String pod = body(source("ReturnSitePlacement.java"), "public static void placePod(");
        assertFalse(pod.contains("seed("), "a new Landing Pod must not be filled with a return kit");
        String prepare = body(source("LandingResolverLevelAdapter.java"), "public static void prepare(");
        assertFalse(prepare.contains("seed("));
    }

    @Test
    void arrivalSpawnsExactlyOneRocket() throws Exception {
        String launch = body(source("RocketEntity.java"), "private void completeLaunch0()");
        assertEquals(1, count(launch, "new RocketEntity("), "one arriving rocket per crewed landing");
        assertFalse(launch.contains("seed("));
    }
}
