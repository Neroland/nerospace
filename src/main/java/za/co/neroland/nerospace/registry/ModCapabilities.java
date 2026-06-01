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

        // Fluid: expose the rocket's tank and the Fuel Tank machine so pipes/automation can fill them.
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                ModEntities.ROCKET.get(),
                (rocket, side) -> rocket.getFuelTank());

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (blockEntity, side) -> blockEntity.getTank());
    }
}
