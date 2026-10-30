package za.co.neroland.nerospace.rocket;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.Services;

/**
 * Development-only trace of the destination click → menu button → rocket path. Logs only in a dev
 * environment (never in a shipped build) and only ints: container id, button id, index, masks and outcome.
 */
public final class DestinationTrace {

    private static final boolean ENABLED = enabled();

    private DestinationTrace() {
    }

    public static void log(String message, Object... args) {
        if (ENABLED) {
            NerospaceCommon.LOGGER.info("[Nerospace/dest] " + message, args);
        }
    }

    private static boolean enabled() {
        try {
            return Services.PLATFORM.isDevelopmentEnvironment();
        } catch (RuntimeException | LinkageError e) {
            return false; // unit tests have no platform service
        }
    }
}
