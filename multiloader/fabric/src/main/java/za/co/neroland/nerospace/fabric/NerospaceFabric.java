package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Fabric entry point. Shared init registers content eagerly, then fills creative
 * tabs from the common grouping via the Fabric API creative-tab module.
 */
public final class NerospaceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();
        ModItems.creativeTabItems().forEach((tab, items) ->
                CreativeModeTabEvents.modifyOutputEvent(tab)
                        .register(output -> items.forEach(output::accept)));
    }
}
