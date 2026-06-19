package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Fabric entry point. Shared init registers content eagerly (Fabric needs no
 * deferred bus). Creative-tab insertion (which needs the Fabric API
 * item-group module) is wired in the next step alongside the Fabric API setup.
 */
public final class NerospaceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();
    }
}
