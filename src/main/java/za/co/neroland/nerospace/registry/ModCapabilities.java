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

        // Fuel Refinery (BALANCE_COMPAT_AUDIT.md §3): grid power in, coal/blaze in, liquid fuel out —
        // a fully pipeable fuel production chain.
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.FUEL_REFINERY.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_REFINERY.get(),
                (blockEntity, side) -> blockEntity.getInputHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FUEL_REFINERY.get(),
                (blockEntity, side) -> blockEntity.getTank());

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

        // Hydration Module input (DEEPER_TERRAFORM_DESIGN.md §3.1): hopper/pipe-feedable glacite.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.HYDRATION_MODULE.get(),
                (blockEntity, side) -> blockEntity.getInputHandler());

        // Fluid: expose the rocket's tank and the Fuel Tank machine so pipes/automation can fill them.
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                ModEntities.ROCKET.get(),
                (rocket, side) -> rocket.getFuelTank());

        // Items: the rocket's fuel-intake slot, for hoppers/pipes aimed at the entity directly.
        event.registerEntity(
                Capabilities.Item.ENTITY,
                ModEntities.ROCKET.get(),
                (rocket, side) -> rocket.getIntakeHandler());

        // Pad proxy (launch-prep automation): a launch pad block forwards the item capability to the
        // rocket deployed above its connected pad cluster, putting the entity's intake slot into the
        // block-capability graph — so plain hoppers and the pipe item layer feed fuel containers in
        // without special-casing entities. No rocket on the pad = no capability.
        event.registerBlock(
                Capabilities.Item.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    java.util.Set<net.minecraft.core.BlockPos> pads =
                            za.co.neroland.nerospace.rocket.LaunchPadMultiblock.connectedPads(level, pos);
                    za.co.neroland.nerospace.rocket.RocketEntity rocket =
                            za.co.neroland.nerospace.rocket.LaunchPadMultiblock.rocketAbove(level, pads);
                    return rocket != null ? rocket.getIntakeHandler() : null;
                },
                ModBlocks.ROCKET_LAUNCH_PAD.get());

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (blockEntity, side) -> blockEntity.getTank());

        // Fuel Tank canister intake (BALANCE_COMPAT_AUDIT.md §3): hoppers/pipes feed canisters that
        // the tank converts to liquid fuel, making rocket fuelling automatable.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (blockEntity, side) -> blockEntity.getCanisterHandler());

        // Oxygen Generator (gas rework) — pipes can carry its oxygen away (or bottle it in Gas Tanks).
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getGasHandler());

        // Quarry Controller (MINER_DESIGN): grid power in. ONLY the mined OUTPUTS are exposed for
        // export — the item-output buffer (extract-only; it rejects insertion) on every side, and the
        // sucked-up fluid buffer. The configuration slots (frame casing + upgrade modules) are NOT
        // exposed to automation, so pipes/hoppers can never pull them out.
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (blockEntity, side) -> blockEntity.getOutputHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());

        // Trash Can: a void sink on every layer — accepts items, fluids and gas from any side and
        // discards them (no extraction surface, so nothing comes back out).
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.TRASH_CAN.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.TRASH_CAN.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());
        event.registerBlockEntity(
                za.co.neroland.nerospace.gas.GasCapability.BLOCK,
                ModBlockEntities.TRASH_CAN.get(),
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
