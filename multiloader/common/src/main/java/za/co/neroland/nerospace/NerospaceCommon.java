package za.co.neroland.nerospace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModRegistries;

/**
 * Loader-agnostic entry point. Both {@code NerospaceFabric} and
 * {@code NerospaceNeoForge} call {@link #init()} during mod construction.
 * Loader-specific behaviour is reached only through {@link Services}, keeping
 * this module free of {@code net.neoforged.*} / {@code net.fabricmc.*} imports.
 */
public final class NerospaceCommon {

    public static final String MOD_ID = "nerospace";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nerospace");

    private NerospaceCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[Nerospace] common init on platform: {} (dev={})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment());

        // Shared content registration via the RegistrationProvider seam. On
        // NeoForge this builds DeferredRegisters (the loader entry point then
        // attaches them to the mod bus); on Fabric it registers eagerly.
        ModRegistries.init();
    }
}
