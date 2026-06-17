package za.co.neroland.nerospace.platform;

/**
 * The loader-specific behaviour the common module is allowed to depend on.
 *
 * <p>Each loader module ships exactly one implementation, registered via a
 * {@code META-INF/services} file so {@link Services} can load it with
 * {@link java.util.ServiceLoader}.
 *
 * <p>Grow this interface as the migration proceeds — it is the seam where
 * NeoForge capabilities, networking, config, attachments, etc. get their
 * cross-loader abstractions. See {@code docs/MULTILOADER.md} for the full
 * subsystem map.
 */
public interface IPlatformHelper {

    /** Human-readable platform name ("Fabric" / "NeoForge"). */
    String getPlatformName();

    /** True when running in a development (dev/data/test) environment. */
    boolean isDevelopmentEnvironment();

    /** True when the named mod is loaded. */
    boolean isModLoaded(String modId);

    /** True on the physical client (renderers, screens, HUD available). */
    boolean isClient();
}
