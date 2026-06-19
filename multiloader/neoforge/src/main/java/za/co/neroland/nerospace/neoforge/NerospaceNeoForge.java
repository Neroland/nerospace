package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.NeoForgeRegistrationFactory;

/**
 * NeoForge entry point. Runs shared init (which builds the DeferredRegisters via
 * the RegistrationProvider seam), then attaches those registers to the mod bus.
 * Creative-tab insertion is added in the next step (with the Fabric side, for
 * symmetry).
 */
@Mod(NerospaceCommon.MOD_ID)
public final class NerospaceNeoForge {

    public NerospaceNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NerospaceCommon.LOGGER.info("[Nerospace] NeoForge bootstrap");
        NerospaceCommon.init();
        NeoForgeRegistrationFactory.registerAll(modEventBus);
    }
}
