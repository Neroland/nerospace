package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Fabric common-side entry point. Delegates all shared setup to
 * {@link NerospaceCommon#init()}.
 *
 * <p>As the migration proceeds, Fabric-specific wiring goes here:
 * registering networking via {@code PayloadTypeRegistry} +
 * {@code ServerPlayNetworking}, event callbacks ({@code ServerTickEvents},
 * {@code ServerPlayConnectionEvents}, ...), capability/storage providers via
 * the Fabric Transfer API, and Fabric data generation.
 */
public final class NerospaceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();
    }
}
