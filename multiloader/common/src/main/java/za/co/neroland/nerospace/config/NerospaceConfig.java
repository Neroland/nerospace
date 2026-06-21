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
    private static final String KEY_ENERGY_RATE = "energyRateMultiplier";
    private static final String KEY_OXYGEN_DRAIN = "oxygenDrainMultiplier";
    private static final String KEY_OXYGEN_CAPACITY = "oxygenCapacityMultiplier";

    /** Multiplier range (mirrors the root config spec): 0.1× .. 10×. */
    private static final double MULT_MIN = 0.1D;
    private static final double MULT_MAX = 10.0D;

    /** Anonymous crash reporting (Sentry, EU) is ON by default; players opt out by setting this false. */
    private static volatile boolean telemetryEnabled = true;
    /** Scales the FE/tick of every generator (combustion, passive, solar). Clamped 0.1×..10×. */
    private static volatile double energyRateMultiplier = 1.0D;
    /** Scales how fast oxygen drains off a safe zone (bare + suited). Clamped 0.1×..10×. */
    private static volatile double oxygenDrainMultiplier = 1.0D;
    /** Scales player + suit air capacity. Clamped 0.1×..10×. */
    private static volatile double oxygenCapacityMultiplier = 1.0D;
    private static volatile boolean loaded;

    private NerospaceConfig() {
    }

    public static boolean isTelemetryEnabled() {
        return telemetryEnabled;
    }

    public static double energyRateMultiplier() {
        return energyRateMultiplier;
    }

    public static double oxygenDrainMultiplier() {
        return oxygenDrainMultiplier;
    }

    public static double oxygenCapacityMultiplier() {
        return oxygenCapacityMultiplier;
    }

    /**
     * Applies a balance multiplier to a base integer rate, clamped to a minimum of 1 so an extreme low
     * multiplier (0.1×) can never zero a rate (mirrors the root {@code Tuning} clamping contract).
     */
    public static int scale(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    private static double clampMultiplier(double value) {
        return Math.max(MULT_MIN, Math.min(MULT_MAX, value));
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
                energyRateMultiplier = clampMultiplier(parseDouble(
                        props.getProperty(KEY_ENERGY_RATE), energyRateMultiplier));
                oxygenDrainMultiplier = clampMultiplier(parseDouble(
                        props.getProperty(KEY_OXYGEN_DRAIN), oxygenDrainMultiplier));
                oxygenCapacityMultiplier = clampMultiplier(parseDouble(
                        props.getProperty(KEY_OXYGEN_CAPACITY), oxygenCapacityMultiplier));
            } catch (IOException e) {
                NerospaceCommon.LOGGER.warn("[Nerospace] Could not read {}; using defaults.", FILE_NAME, e);
            }
        } else {
            write(file);
        }
    }

    /** Lenient double parse — falls back to {@code fallback} on null/blank/invalid input. */
    private static double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Writes the default config file with an explanatory comment (best-effort). */
    private static void write(Path file) {
        Properties props = new Properties();
        props.setProperty(KEY_TELEMETRY, Boolean.toString(telemetryEnabled));
        props.setProperty(KEY_ENERGY_RATE, Double.toString(energyRateMultiplier));
        props.setProperty(KEY_OXYGEN_DRAIN, Double.toString(oxygenDrainMultiplier));
        props.setProperty(KEY_OXYGEN_CAPACITY, Double.toString(oxygenCapacityMultiplier));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Nerospace config. telemetryEnabled: send anonymous, Nerospace-only "
                        + "crash reports (Sentry, EU servers) — stack trace + mod/MC/loader/OS/Java "
                        + "versions only; no IP, username, UUID, world data or chat; file paths are "
                        + "scrubbed of your account name. Set to false to opt out. See PRIVACY.md. "
                        + "energyRateMultiplier: scales FE/tick of all generators. oxygenDrainMultiplier: "
                        + "scales how fast air drains. oxygenCapacityMultiplier: scales air capacity. "
                        + "All multipliers 0.1..10, default 1.");
            }
        } catch (IOException e) {
            NerospaceCommon.LOGGER.warn("[Nerospace] Could not write {}; using defaults.", FILE_NAME, e);
        }
    }
}
