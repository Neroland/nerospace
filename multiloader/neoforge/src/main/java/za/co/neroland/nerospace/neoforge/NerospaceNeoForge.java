package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import za.co.neroland.nerospace.NerospaceCommon;

/**
 * NeoForge entry point. Mirrors the root project's {@code Nerospace} class
 * but does nothing loader-specific beyond construction — all shared setup
 * is delegated to {@link NerospaceCommon#init()}.
 *
 * <p>As the migration proceeds, the per-loader registration wiring (binding
 * Architectury DeferredRegisters, capability providers via
 * {@code RegisterCapabilitiesEvent}, payload registration, etc.) lives here
 * and in sibling classes in this module.
 */
@Mod(NerospaceCommon.MOD_ID)
public final class NerospaceNeoForge {

    public NerospaceNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NerospaceCommon.LOGGER.info("[Nerospace] NeoForge bootstrap");
        // Shared, loader-agnostic init.
        NerospaceCommon.init();

        // TODO (migration): wire NeoForge-specific listeners on modEventBus,
        // e.g. RegisterCapabilitiesEvent, RegisterPayloadHandlersEvent,
        // EntityAttributeCreationEvent, GatherDataEvent, client renderer
        // registration (under a Dist.CLIENT guard).
    }
}
