package za.co.neroland.nerospace.neoforge;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.rocket.RocketPadFluidProxy;
import za.co.neroland.nerospace.rocket.RocketPadItemContainer;

/**
 * NeoForge side of the capability seams:
 * <ul>
 *   <li>item storage via the standard {@code Capabilities.Item.BLOCK} (26.x transfer API);</li>
 *   <li>energy via a mod-owned {@link #ENERGY} {@link BlockCapability} over
 *       {@link NerospaceEnergyStorage} (the Fabric side uses a matching {@code BlockApiLookup}) —
 *       self-contained until the platforms' energy libraries port to 26.x.</li>
 * </ul>
 */
public final class NeoForgeCapabilities {

    /** Mod-owned energy capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final @NonNull BlockCapability<NerospaceEnergyStorage, @Nullable Direction> ENERGY =
            NerospaceCommon.requireNonNull(BlockCapability.createSided(
                    NerospaceCommon.id("energy"),
                    NerospaceEnergyStorage.class));

    /** Mod-owned fluid capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final @NonNull BlockCapability<NerospaceFluidStorage, @Nullable Direction> FLUID =
            NerospaceCommon.requireNonNull(BlockCapability.createSided(
                    NerospaceCommon.id("fluid"),
                    NerospaceFluidStorage.class));

    /** Mod-owned gas capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final @NonNull BlockCapability<NerospaceGasStorage, @Nullable Direction> GAS =
            NerospaceCommon.requireNonNull(BlockCapability.createSided(
                    NerospaceCommon.id("gas"),
                    NerospaceGasStorage.class));

    private NeoForgeCapabilities() {
    }

    public static void register(@NonNull IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.ITEM_STORE.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.FLUID_TANK.get(),
                (be, side) -> be.getTank());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.PASSIVE_GENERATOR.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.PASSIVE_GENERATOR.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(
                GAS,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (be, side) -> be.getGas());
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (be, side) -> be.getFluidTank());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.UNIVERSAL_PIPE.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        event.registerBlockEntity(
                GAS,
                ModBlockEntities.GAS_TANK.get(),
                (be, side) -> be.getTank());

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(
                GAS,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (be, side) -> be.getGas());

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.SOLAR_PANEL.get(),
                (be, side) -> be.getEnergy());

        // Terraformer: grid power in, upgrade slot in.
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.TERRAFORMER.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.TERRAFORMER.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        // Hydration Module: glacite in (no energy of its own).
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.HYDRATION_MODULE.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> be.getFluid());
        event.registerBlockEntity(
                GAS,
                ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> be.getGas());

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.CREATIVE_BATTERY.get(),
                (be, side) -> be.getEnergy());

        // Creative storage: endless sources/sinks for testing logistics.
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.CREATIVE_FLUID_TANK.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(
                GAS,
                ModBlockEntities.CREATIVE_GAS_TANK.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.CREATIVE_ITEM_STORE.get(),
                (be, side) -> VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        // Fuel Tank: fluid out (pipes), canister in (hoppers/pipes).
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.FUEL_TANK.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        // Fuel Refinery: grid power in, refined fuel out, coal + blaze powder in.
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        // Quarry controller: grid power in, mined output + sucked fluid out, frame casings in.
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.QUARRY_CONTROLLER.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(NerospaceCommon.requireNonNull(be), side)
                        : VanillaContainerWrapper.of(NerospaceCommon.requireNonNull(be)));

        // Rocket launch pad: a fluid sink that forwards rocket_fuel into a docked rocket (pipe / Fuel Tank
        // refuelling automation), plus an item sink that forwards fuel canisters into the rocket's intake.
        // Registered on the BLOCK (no block entity) — the proxies find the docked rocket.
        event.registerBlock(
                FLUID,
                (level, pos, state, blockEntity, side) -> new RocketPadFluidProxy(level, pos),
                ModBlocks.ROCKET_LAUNCH_PAD.get());
        event.registerBlock(
                Capabilities.Item.BLOCK,
                (level, pos, state, blockEntity, side) -> side != null
                        ? new WorldlyContainerWrapper(new RocketPadItemContainer(level, pos), side)
                        : VanillaContainerWrapper.of(new RocketPadItemContainer(level, pos)),
                ModBlocks.ROCKET_LAUNCH_PAD.get());
    }
}
