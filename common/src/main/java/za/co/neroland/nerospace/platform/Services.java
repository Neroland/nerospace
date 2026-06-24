package za.co.neroland.nerospace.platform;

import java.util.ServiceLoader;
import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Loads loader-specific {@link IPlatformHelper} (and future service)
 * implementations via {@link ServiceLoader}.
 *
 * <p>This is the lightweight, dependency-free alternative to Architectury's
 * {@code @ExpectPlatform}. Common code calls {@code Services.PLATFORM.xxx()};
 * the correct Fabric or NeoForge implementation is resolved at runtime from
 * the {@code META-INF/services} entry in each loader module.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static final NetworkPlatform NETWORK = load(NetworkPlatform.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        final T loaded = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "No implementation found for service " + clazz.getName()));
        NerospaceCommon.LOGGER.debug("Loaded service {} -> {}",
                clazz.getSimpleName(), loaded.getClass().getName());
        return loaded;
    }
}
