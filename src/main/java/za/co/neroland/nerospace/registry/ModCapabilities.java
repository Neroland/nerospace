package za.co.neroland.nerospace.registry;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.RangedResourceHandler;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;

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

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.TERRAFORMER.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());

        // Power grid: the pipe (network buffer) + both generators expose the energy capability so the
        // network, machines and other mods' cables interoperate.
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (blockEntity, side) -> blockEntity.getGasHandler());

        // Storage endpoints + creative sources.
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.BATTERY.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.CREATIVE_BATTERY.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FLUID_TANK.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.CREATIVE_FLUID_TANK.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.GAS_TANK.get(),
                (blockEntity, side) -> blockEntity.getGasHandler());
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.CREATIVE_GAS_TANK.get(),
                (blockEntity, side) -> blockEntity.getGasHandler());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.ITEM_STORE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.CREATIVE_ITEM_STORE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.PASSIVE_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());

        // Generators expose their fuel/core slot so hoppers and pipes can feed them.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getFuelHandler());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.PASSIVE_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getCoreHandler());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.TERRAFORMER.get(),
                (blockEntity, side) -> blockEntity.getUpgradeHandler());

        // Fluid: expose the rocket's tank and the Fuel Tank machine so pipes/automation can fill them.
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                ModEntities.ROCKET.get(),
                (rocket, side) -> rocket.getFuelTank());

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (blockEntity, side) -> blockEntity.getTank());

        // Oxygen Generator (gas rework) — pipes can carry its oxygen away (or bottle it in Gas Tanks).
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getGasHandler());

        // Nerosium Grinder — sided: insert into the input from the top/sides, extract output below.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (blockEntity, side) -> side == Direction.DOWN
                        ? RangedResourceHandler.ofSingleIndex(blockEntity.getItemHandler(), NerosiumGrinderBlockEntity.OUTPUT_SLOT)
                        : RangedResourceHandler.ofSingleIndex(blockEntity.getItemHandler(), NerosiumGrinderBlockEntity.INPUT_SLOT));
    }
}
