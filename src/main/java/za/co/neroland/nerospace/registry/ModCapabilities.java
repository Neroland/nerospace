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

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());

        // NOTE: the rocket's fuel store is now a transfer-API ResourceHandler<FluidResource>
        // (RocketFuelTank), so exposing it via Capabilities.Fluid.ENTITY for pipe automation is a
        // small follow-up (register the entity capability returning RocketEntity#getFuelTank). The
        // fuel tank machine's RocketFuelTank could likewise be exposed via Capabilities.Fluid.BLOCK.
    }
}
