package za.co.neroland.nerospace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.neroland.nerospace.platform.Services;

/**
 * Loader-agnostic entry point.
 *
 * <p>Both {@code NerospaceFabric} and {@code NerospaceNeoForge} call
 * {@link #init()} from their own loader entry points. All shared setup
 * (registration, config wiring, common event hooks) belongs here or in
 * the packages it touches — never in a loader module.
 *
 * <p>Anything that must reach loader-specific behaviour goes through
 * {@link Services} (a Java {@link java.util.ServiceLoader} abstraction),
 * keeping this module free of {@code net.neoforged.*} and
 * {@code net.fabricmc.*} imports.
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

        // Content registration is wired per loader (NeoForge DeferredRegister /
        // Fabric Registry.register) from each module's entry point. Without
        // Architectury API there is no shared DeferredRegister; the migration
        // (docs/MULTILOADER.md §2) introduces a small registration service on
        // top of the platform Services seam when content is ported.
    }
}
