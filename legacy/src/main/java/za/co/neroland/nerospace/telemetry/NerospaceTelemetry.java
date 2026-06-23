package za.co.neroland.nerospace.telemetry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;

/**
 * Crash/error reporting for Nerospace via Sentry (EU ingest), built to satisfy both the
 * CurseForge moderation rule (external analytics must be disclosed and opt-out-able) and
 * POPIA/GDPR data-minimisation:
 *
 * <ul>
 *   <li><b>Opt-out:</b> gated on {@code telemetryEnabled} in the common config; toggling it
 *       off stops reporting immediately (config reload), and nothing initialises before the
 *       config has loaded.</li>
 *   <li><b>Nerospace errors only:</b> {@code beforeSend} drops any event whose stack trace
 *       does not touch {@code za.co.neroland.nerospace}.</li>
 *   <li><b>No personal data:</b> {@code sendDefaultPii=false} (no IP stored), no server/host
 *       name, no user identity of any kind, and OS-account names are scrubbed out of file
 *       paths in messages. Remaining payload: stack trace, mod/MC/NeoForge versions, OS name
 *       and Java version.</li>
 *   <li><b>Bounded volume:</b> per-session de-duplication plus a hard cap of
 *       {@value #MAX_EVENTS_PER_SESSION} events per game session.</li>
 * </ul>
 *
 * <p>Full disclosure text lives in {@code PRIVACY.md}; the CurseForge project description must
 * carry the short blurb from that file.</p>
 */
public final class NerospaceTelemetry {

    /** Sentry DSN — a public client key (write-only ingest), safe to ship in the jar. */
    private static final String DSN =
            "https://05493ad141e6ed1526488f84618ce63d@o4511183823241216.ingest.de.sentry.io/4511509478834256";

    /** Stack traces must contain this package for an event to be sent. */
    private static final String PACKAGE_MARKER = "za.co.neroland.nerospace";

    /** Hard cap on events per game session (data minimisation + noise control). */
    private static final int MAX_EVENTS_PER_SESSION = 10;

    /** Masks OS-account names in Windows/macOS/Linux home-directory paths. */
    private static final Pattern USER_PATH =
            Pattern.compile("(?i)(?:[A-Z]:)?[/\\\\](?:Users|home)[/\\\\][^/\\\\\\s:;,'\"]+");

    private static volatile boolean active;
    private static final AtomicInteger eventsSent = new AtomicInteger();
    private static final Set<String> seenFingerprints = ConcurrentHashMap.newKeySet();
    private static SentryLogAppender appender;

    private NerospaceTelemetry() {}

    /**
     * Called from {@code ModConfigEvent.Loading} and {@code .Reloading} for the common config.
     * Starts or stops reporting to match {@link Config#TELEMETRY_ENABLED}, so an opt-out takes
     * effect without a restart and nothing is ever sent before the player's choice is known.
     */
    public static void onConfigChanged(ModContainer modContainer) {
        boolean enabled;
        try {
            enabled = Config.TELEMETRY_ENABLED.get();
        } catch (IllegalStateException e) {
            return; // config not loaded yet; a later event will re-invoke us
        }
        if (enabled && !active) {
            start(modContainer);
        } else if (!enabled && active) {
            stop();
        }
    }

    private static synchronized void start(ModContainer modContainer) {
        if (active) {
            return;
        }
        String modVersion = modContainer.getModInfo().getVersion().toString();
        Sentry.init(options -> {
            options.setDsn(DSN);
            options.setRelease("nerospace@" + modVersion);
            // Environment follows the release channel in the version string
            // (0.0.7-beta -> "beta"); dev vs shipped is the "runtime" tag below.
            options.setEnvironment(environmentOf(modVersion));
            // POPIA/GDPR: never store the sender's IP address or identity.
            options.setSendDefaultPii(false);
            // The machine's hostname is identifying; never attach it.
            options.setAttachServerName(false);
            options.setEnableUncaughtExceptionHandler(true);
            options.setBeforeSend((event, hint) -> filterAndScrub(event));
        });
        Sentry.configureScope(scope -> {
            scope.setTag("dist", FMLEnvironment.getDist().name().toLowerCase(java.util.Locale.ROOT));
            scope.setTag("neoforge", net.neoforged.neoforge.common.NeoForgeVersion.getVersion());
            scope.setTag("runtime", FMLEnvironment.isProduction() ? "production" : "dev");
        });
        if (appender == null) {
            appender = new SentryLogAppender();
            appender.start();
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(appender);
        }
        active = true;
        Nerospace.LOGGER.info(
                "Nerospace telemetry enabled (anonymous error reports, EU servers; opt out via telemetryEnabled=false).");

        // Local verification hook: launch with -Dnerospace.telemetry.test=true to send one
        // synthetic event (it originates in this class, so it passes the nerospace-only
        // filter). Never enabled by default; dev-run instructions in PRIVACY.md.
        if (Boolean.getBoolean("nerospace.telemetry.test")) {
            Nerospace.LOGGER.warn("Telemetry self-test: sending a test event to Sentry.");
            capture(new IllegalStateException("Nerospace telemetry self-test — safe to ignore"));
        }
    }

    /** Maps the mod version's release channel to a Sentry environment. */
    private static String environmentOf(String version) {
        String v = version.toLowerCase(java.util.Locale.ROOT);
        if (v.contains("-alpha")) {
            return "alpha";
        }
        if (v.contains("-beta")) {
            return "beta";
        }
        return "production";
    }

    private static synchronized void stop() {
        if (!active) {
            return;
        }
        active = false;
        if (appender != null) {
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).removeAppender(appender);
            appender.stop();
            appender = null;
        }
        Sentry.close();
        Nerospace.LOGGER.info("Nerospace telemetry disabled by config.");
    }

    static boolean isActive() {
        return active;
    }

    /** True if any frame of the throwable (or its causes/suppressed) is Nerospace code. */
    static boolean touchesNerospace(Throwable t) {
        int depth = 0;
        while (t != null && depth++ < 16) {
            for (StackTraceElement el : t.getStackTrace()) {
                if (el.getClassName().startsWith(PACKAGE_MARKER)) {
                    return true;
                }
            }
            for (Throwable s : t.getSuppressed()) {
                if (touchesNerospace(s)) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    /** Capture an exception already known to touch Nerospace code. */
    static void capture(Throwable t) {
        if (!active || t == null) {
            return;
        }
        Sentry.captureException(t);
    }

    /**
     * Diagnostic hook for the in-game {@code sentry_test} block (a hidden, creative-menu-excluded
     * block obtained with {@code /give}). Captures one synthetic, clearly-labelled exception so a
     * developer can confirm the whole telemetry pipeline reaches the Sentry dashboard. The exception
     * is created here, so its stack trace touches {@code za.co.neroland.nerospace} and passes the
     * package-only {@code beforeSend} filter.
     *
     * @param origin a short human label for where the test fired (woven into the event message only;
     *               the per-session de-duplication keys on the exception type + Nerospace stack, so
     *               repeat placements in one session collapse to a single Sentry event — that is the
     *               normal de-dup behaviour and itself part of what this confirms).
     * @return {@code true} if telemetry is active and the event was dispatched; {@code false} if the
     *         player has opted out ({@code telemetryEnabled=false}), in which case nothing was sent.
     */
    public static boolean sendTestEvent(String origin) {
        if (!active) {
            return false;
        }
        capture(new IllegalStateException(
                "Nerospace Sentry test block (" + origin + ") — synthetic event, safe to ignore"));
        return true;
    }

    /** Capture a (scrubbed, truncated) FATAL log line that names Nerospace without a throwable. */
    static void captureMessage(String message) {
        if (!active) {
            return;
        }
        String scrubbed = scrub(message);
        if (scrubbed.length() > 4000) {
            scrubbed = scrubbed.substring(0, 4000) + "…[truncated]";
        }
        final String text = scrubbed;
        SentryEvent event = new SentryEvent();
        event.setLevel(SentryLevel.FATAL);
        Message msg = new Message();
        msg.setFormatted(text);
        event.setMessage(msg);
        Sentry.captureEvent(event);
    }

    /**
     * The single privacy/noise gate every outgoing event passes through:
     * keep only Nerospace-related events, de-duplicate, rate-limit, and scrub PII.
     * Returning {@code null} drops the event.
     */
    private static SentryEvent filterAndScrub(SentryEvent event) {
        if (!isNerospaceRelated(event)) {
            return null;
        }
        String fingerprint = fingerprintOf(event);
        if (!seenFingerprints.add(fingerprint)) {
            return null; // already reported this session
        }
        if (eventsSent.incrementAndGet() > MAX_EVENTS_PER_SESSION) {
            return null;
        }
        // POPIA/GDPR scrubbing: no user identity, no hostname, no OS-account names in paths.
        event.setUser(null);
        event.setServerName(null);
        List<SentryException> scrubExceptions = event.getExceptions();
        if (scrubExceptions != null) {
            for (SentryException ex : scrubExceptions) {
                String value = ex.getValue();
                if (value != null) {
                    ex.setValue(scrub(value));
                }
                SentryStackTrace st = ex.getStacktrace();
                List<SentryStackFrame> frames = st == null ? null : st.getFrames();
                if (frames != null) {
                    for (SentryStackFrame frame : frames) {
                        frame.setAbsPath(null);
                    }
                }
            }
        }
        Message message = event.getMessage();
        if (message != null && message.getFormatted() != null) {
            message.setFormatted(scrub(message.getFormatted()));
        }
        return event;
    }

    private static boolean isNerospaceRelated(SentryEvent event) {
        Throwable t = event.getThrowable();
        if (t != null && touchesNerospace(t)) {
            return true;
        }
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            for (SentryException ex : exceptions) {
                SentryStackTrace st = ex.getStacktrace();
                List<SentryStackFrame> frames = st == null ? null : st.getFrames();
                if (frames == null) {
                    continue;
                }
                for (SentryStackFrame frame : frames) {
                    String module = frame.getModule();
                    if (module != null && module.startsWith(PACKAGE_MARKER)) {
                        return true;
                    }
                }
            }
        }
        Message message = event.getMessage();
        String formatted = message == null ? null : message.getFormatted();
        return formatted != null && formatted.contains(PACKAGE_MARKER);
    }

    private static String fingerprintOf(SentryEvent event) {
        StringBuilder sb = new StringBuilder();
        List<SentryException> exceptions = event.getExceptions();
        Message message = event.getMessage();
        if (exceptions != null) {
            for (SentryException ex : exceptions) {
                sb.append(ex.getType()).append('|');
                SentryStackTrace st = ex.getStacktrace();
                List<SentryStackFrame> frames = st == null ? null : st.getFrames();
                if (frames != null) {
                    for (SentryStackFrame frame : frames) {
                        String module = frame.getModule();
                        if (module != null && module.startsWith(PACKAGE_MARKER)) {
                            sb.append(module).append(':').append(frame.getLineno()).append('|');
                        }
                    }
                }
            }
        } else if (message != null) {
            String formatted = message.getFormatted();
            if (formatted != null) {
                sb.append(formatted, 0, Math.min(200, formatted.length()));
            }
        }
        return sb.toString();
    }

    /** Replaces home-directory paths (which contain the OS account name) with a neutral marker. */
    static String scrub(String text) {
        return USER_PATH.matcher(text).replaceAll("/~");
    }
}
