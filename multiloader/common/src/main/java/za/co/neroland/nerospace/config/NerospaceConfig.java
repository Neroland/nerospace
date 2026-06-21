package za.co.neroland.nerospace.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.Services;

/**
 * Minimal cross-loader config — a single {@code config/nerospace.properties} file read once at mod
 * init. The full root config (NeoForge {@code ModConfigSpec}, ~50 keys) is deferred; this exists so
 * the disclosed telemetry has a real, user-editable <b>opt-out</b> toggle (CurseForge moderation +
 * POPIA/GDPR). The file is created with documented defaults on first run.
 *
 * <p>Loader-agnostic: the config directory comes through the {@link Services#PLATFORM} seam.</p>
 */
public final class NerospaceConfig {

    private static final String FILE_NAME = "nerospace.properties";
    private static final String KEY_TELEMETRY = "telemetryEnabled";

    /** Anonymous crash reporting (Sentry, EU) is ON by default; players opt out by setting this false. */
    private static volatile boolean telemetryEnabled = true;
    private static volatile boolean loaded;

    private NerospaceConfig() {
    }

    public static boolean isTelemetryEnabled() {
        return telemetryEnabled;
    }

    /** Reads (creating with defaults if absent) the config file. Safe to call once at mod init. */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file;
        try {
            file = Services.PLATFORM.getConfigDir().resolve(FILE_NAME);
        } catch (RuntimeException e) {
            return; // no config dir available — keep defaults
        }

        Properties props = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
                telemetryEnabled = Boolean.parseBoolean(
                        props.getProperty(KEY_TELEMETRY, Boolean.toString(telemetryEnabled)).trim());
            } catch (IOException e) {
                NerospaceCommon.LOGGER.warn("[Nerospace] Could not read {}; using defaults.", FILE_NAME, e);
            }
        } else {
            write(file);
        }
    }

    /** Writes the default config file with an explanatory comment (best-effort). */
    private static void write(Path file) {
        Properties props = new Properties();
        props.setProperty(KEY_TELEMETRY, Boolean.toString(telemetryEnabled));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Nerospace config. telemetryEnabled: send anonymous, Nerospace-only "
                        + "crash reports (Sentry, EU servers) — stack trace + mod/MC/loader/OS/Java "
                        + "versions only; no IP, username, UUID, world data or chat; file paths are "
                        + "scrubbed of your account name. Set to false to opt out. See PRIVACY.md.");
            }
        } catch (IOException e) {
            NerospaceCommon.LOGGER.warn("[Nerospace] Could not write {}; using defaults.", FILE_NAME, e);
        }
    }
}
