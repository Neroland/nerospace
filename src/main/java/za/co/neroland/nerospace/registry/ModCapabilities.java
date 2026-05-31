package za.co.neroland.nerospace.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import za.co.neroland.nerospace.Nerospace;

/**
 * Capability providers (Phase 2). Exposes the grinder's internal energy buffer via the NeoForge
 * energy capability, so external power sources can push energy in once they port to 26.1.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class ModCapabilities {

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
    }
}
