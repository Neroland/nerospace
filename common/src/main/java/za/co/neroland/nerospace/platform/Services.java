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

    /**
     * Resolves the remaining seams now, during mod construction, instead of leaving each to resolve on
     * first use.
     *
     * <p>{@link #PLATFORM} and {@link #NETWORK} load with this class, but the other seams hang their
     * instance off their own interface ({@code EnergyLookup.INSTANCE} and friends), so each one stayed
     * unresolved until the first time gameplay touched it — for the energy seam, the first pipe tick,
     * potentially hours into a session. {@link ServiceLoader} reads {@code META-INF/services} from the
     * mod jar at that moment, so if the jar is no longer readable by then (swapped or moved while the
     * game is running, or reached through a path that has stopped resolving) the read throws
     * {@code ServiceConfigurationError} out of a block-entity tick and crashes the world — Sentry
     * {@code MC-NEROSPACE-F}, where the jar behind a running session had gone missing.</p>
     *
     * <p>Loading everything up front does not make a genuinely absent jar work, but it moves the failure
     * to startup where it is diagnosable, and in the ordinary case the jar is open at init so the failure
     * never arises at all. Call this <em>after</em> registration, so the registry-facing seams keep their
     * existing load order.</p>
     */
    public static void preload() {
        final Object[] resolved = {
            EnergyLookup.INSTANCE,
            FluidLookup.INSTANCE,
            GasLookup.INSTANCE,
            ItemLookup.INSTANCE,
            FluidFactory.INSTANCE,
        };
        NerospaceCommon.LOGGER.debug("[Nerospace] {} platform services resolved during init",
                resolved.length + 2);
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
