package za.co.neroland.nerospace.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * POPIA/GDPR guard over the main sources: no log line, telemetry call, dev trace or admin-broadcast command feedback may
 * carry a player name, a UUID, a station name or per-player planet data.
 *
 * <p>Pattern-based: every call to a logger, {@code NerospaceTelemetry}, {@code DestinationTrace.log},
 * {@code System.out/err} or a broadcasting {@code sendSuccess(..., true)} is extracted (parenthesis-balanced), its
 * string literals are dropped, and what is left (the arguments) is matched against identity patterns. A hit that is
 * provably harmless goes on {@link #ALLOWED} with a reason; anything else fails the build.</p>
 */
class PersonalDataLogScanTest {

    private static final Pattern CALL = Pattern.compile(
            "(LOGGER\\s*\\.\\s*(?:trace|debug|info|warn|error)|NerospaceTelemetry\\s*\\.\\s*\\w+|DestinationTrace\\s*\\.\\s*log"
                    + "|System\\s*\\.\\s*(?:out|err)\\s*\\.\\s*print\\w*|Sentry\\s*\\.\\s*\\w+|\\bsendSuccess)\\s*\\(");

    /** Identity sources: accessors and variable names that carry a player, a UUID, a station name or visit data. */
    private static final Pattern IDENTITY = Pattern.compile(
            "getUUID|getStringUUID|getGameProfile|getScoreboardName|getDisplayName|getPlainTextName"
                    + "|\\bgetName\\s*\\(\\s*\\)|\\buuid\\b|\\bUUID\\b|\\bplayerId\\b|\\bplayer\\b|\\bpilot\\w*\\b|\\brider\\b"
                    + "|\\bowner\\w*\\b|\\bmembers?\\b|\\bstationName\\b|\\bentry\\s*\\.\\s*name\\s*\\(|ownerName"
                    + "|PlanetVisitState|\\bvisits\\b|\\.export\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    /** Harmless constructs removed before matching. */
    private static final Pattern NEUTRAL = Pattern.compile("getClass\\s*\\(\\s*\\)\\s*\\.\\s*getName\\s*\\(\\s*\\)");

    /**
     * Explicit allow-list: {@code "<file name>|<normalised argument text fragment>"} with the reason beside it.
     */
    private static final Set<String> ALLOWED = Set.of(
            // Dev-only trace (isDevelopmentEnvironment): gateMask(player) is an int bit mask of Core gates, not identity.
            "RocketMenu.java|RocketEntity.gateMask(player)");

    @Test
    void noPersonalDataInLogsTelemetryOrBroadcasts() throws IOException {
        Path main = sourceRoot();
        List<String> hits = new ArrayList<>();
        int calls;
        try (Stream<Path> files = Files.walk(main)) {
            List<Path> java = files.filter(p -> p.toString().endsWith(".java")).toList();
            calls = 0;
            for (Path p : java) {
                String src = stripComments(Files.readString(p, StandardCharsets.UTF_8));
                Matcher m = CALL.matcher(src);
                while (m.find()) {
                    int open = m.end() - 1;
                    int close = matching(src, open);
                    if (close < 0) {
                        continue;
                    }
                    String args = src.substring(open + 1, close);
                    if (m.group(1).equals("sendSuccess") && !args.replaceAll("\\s", "").endsWith(",true")) {
                        continue; // feedback to the invoker only: not logged, not broadcast
                    }
                    calls++;
                    String code = NEUTRAL.matcher(stripLiterals(args)).replaceAll("_");
                    Matcher id = IDENTITY.matcher(code);
                    if (!id.find()) {
                        continue;
                    }
                    String norm = code.replaceAll("\\s+", "");
                    String file = p.getFileName().toString();
                    boolean allowed = ALLOWED.stream().anyMatch(a -> a.startsWith(file + "|")
                            && norm.contains(a.substring(a.indexOf('|') + 1)));
                    if (!allowed) {
                        int line = 1 + (int) src.substring(0, m.start()).chars().filter(c -> c == '\n').count();
                        hits.add(main.relativize(p) + ":" + line + " [" + id.group() + "] " + norm);
                    }
                }
            }
        }
        assertTrue(calls > 50, "the scan found only " + calls + " calls - the source root or patterns are wrong");
        assertTrue(hits.isEmpty(), "personal data may reach a log/telemetry/broadcast:\n  " + String.join("\n  ", hits));
    }

    @Test
    void scannerCatchesTheShapesItGuardsAgainst() {
        String[] bad = {
            "LOGGER.info(\"x {}\", player.getName())",
            "NerospaceTelemetry.breadcrumb(\"station\", \"rename \" + entry.name())",
            "LOGGER.warn(\"owner {}\", owner)",
            "source.sendSuccess(() -> Component.literal(name), true)".replace("name", "stationName"),
        };
        for (String s : bad) {
            Matcher m = CALL.matcher(s);
            assertTrue(m.find(), s);
            String args = s.substring(m.end(), matching(s, m.end() - 1));
            assertTrue(IDENTITY.matcher(stripLiterals(args)).find(), "not caught: " + s);
        }
        assertTrue(!IDENTITY.matcher(stripLiterals("\"[Nerospace] player data erased\", count")).find(),
                "words inside a literal are fine");
    }

    // --- helpers --------------------------------------------------------------------------------------------------

    static Path sourceRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (Path d = dir; d != null; d = d.getParent()) {
            Path candidate = d.resolve("common/src/main/java/za/co/neroland/nerospace");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("common/src/main/java not found above " + dir);
    }

    /** Index of the ')' matching the '(' at {@code open}, skipping string and char literals; -1 if unbalanced. */
    static int matching(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipLiteral(s, i);
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int skipLiteral(String s, int start) {
        char q = s.charAt(start);
        if (q == '"' && s.startsWith("\"\"\"", start)) {
            int end = s.indexOf("\"\"\"", start + 3);
            return end < 0 ? s.length() : end + 2;
        }
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == q) {
                return i;
            }
        }
        return s.length();
    }

    static String stripLiterals(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipLiteral(s, i);
                out.append("\"\"");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static String stripComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                int end = skipLiteral(s, i);
                out.append(s, i, Math.min(end + 1, s.length()));
                i = end;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                int nl = s.indexOf('\n', i);
                i = (nl < 0 ? s.length() : nl) - 1;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                String block = s.substring(i, end < 0 ? s.length() : end + 2);
                out.append(block.replaceAll("[^\n]", " "));
                i = (end < 0 ? s.length() : end + 2) - 1;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
