package za.co.neroland.nerospace.neoforge;

//? if >=1.20.3 {
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.platform.NeoForgeFluidResourceHandler;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.rocket.RocketPadFluidProxy;
import za.co.neroland.nerospace.rocket.RocketPadGasProxy;
import za.co.neroland.nerospace.rocket.RocketPadItemContainer;

/**
 * NeoForge side of the capability seams:
 * <ul>
 *   <li>item storage via the standard {@code Capabilities.Item.BLOCK} (26.x transfer API);</li>
 *   <li>energy via a mod-owned {@link #ENERGY} {@link BlockCapability} over
 *       {@link NerospaceEnergyStorage} (the Fabric side uses a matching {@code BlockApiLookup}) —
 *       self-contained until the platforms' energy libraries port to 26.x;</li>
 *   <li>fluid on BOTH the mod-owned {@link #FLUID} capability and the standard
 *       {@code Capabilities.Fluid.BLOCK} — see {@link #registerStandardFluid}.</li>
 * </ul>
 */
public final class NeoForgeCapabilities {

    /** Mod-owned energy capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final BlockCapability<NerospaceEnergyStorage, Direction> ENERGY =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "energy"),
                    NerospaceEnergyStorage.class);

    /** Mod-owned fluid capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final BlockCapability<NerospaceFluidStorage, Direction> FLUID =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "fluid"),
                    NerospaceFluidStorage.class);

    /** Mod-owned gas capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final BlockCapability<NerospaceGasStorage, Direction> GAS =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "gas"),
                    NerospaceGasStorage.class);

    private NeoForgeCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // The Battery / Fluid Tank / Gas Tank / Item Store endpoints now live in Neroland Core and expose
        // Core's own capabilities. Energy crosses for free via Core's energy lookup and the Item Store is a
        // vanilla Container; only fluid/gas need re-exposing on Nerospace's lookups for the Universal Pipe —
        // see the CoreTankBridge registrations below.

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.NEROSIUM_GRINDER.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.PASSIVE_GENERATOR.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));
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
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (be, side) -> be.getEnergy());
        // Gas is gated by the side config: the gated Core gas view (null on DISABLED/INPUT faces) mapped
        // back onto Nerospace's gas capability so the Universal Pipe pulls oxygen only from OUTPUT faces.
        event.registerBlockEntity(
                GAS,
                ModBlockEntities.OXYGEN_GENERATOR.get(),
                (be, side) -> za.co.neroland.nerospace.machine.MachineSideConfig.gasView(be.sideConfig(), side));

        // Launch Controller resource hub: fuel in, oxygen in, power in (then pumped into the rocket).
        event.registerBlockEntity(FLUID, ModBlockEntities.LAUNCH_CONTROLLER.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(GAS, ModBlockEntities.LAUNCH_CONTROLLER.get(), (be, side) -> be.getGas());
        event.registerBlockEntity(ENERGY, ModBlockEntities.LAUNCH_CONTROLLER.get(), (be, side) -> be.getEnergy());

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
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        // Hydration Module: glacite in (no energy of its own).
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.HYDRATION_MODULE.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        // Trash Can now lives in Neroland Core (vanilla Container for items found by item adjacency); only
        // its fluid/gas surfaces need bridging onto Nerospace's caps so the Universal Pipe still voids into it.
        event.registerBlockEntity(FLUID, za.co.neroland.nerolandcore.registry.ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getFluid()));
        event.registerBlockEntity(GAS, za.co.neroland.nerolandcore.registry.ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(be.getGas()));

        // Re-expose Core's tank block-entities on Nerospace's own fluid/gas capabilities so the Universal
        // Pipe still connects to the (now Core-owned) Fluid Tank / Gas Tank and their creative variants.
        event.registerBlockEntity(FLUID, za.co.neroland.nerolandcore.registry.ModBlockEntities.FLUID_TANK.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getTank()));
        event.registerBlockEntity(FLUID, za.co.neroland.nerolandcore.registry.ModBlockEntities.CREATIVE_FLUID_TANK.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getTank()));
        event.registerBlockEntity(GAS, za.co.neroland.nerolandcore.registry.ModBlockEntities.GAS_TANK.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(be.getTank()));
        event.registerBlockEntity(GAS, za.co.neroland.nerolandcore.registry.ModBlockEntities.CREATIVE_GAS_TANK.get(),
                (be, side) -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(be.getTank()));

        // Cargo Pad: rocket fuel in via the side config (fuel-only buffer), cargo in/out on every face.
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.CARGO_PAD.get(),
                (be, side) -> za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.CARGO_PAD.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        // Fuel Tank: fluid in/out via the side config (STORAGE preset, default IO), canister in.
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.FUEL_TANK.get(),
                (be, side) -> za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_TANK.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        // Fuel Refinery: grid power in, refined fuel out, coal + blaze powder in.
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> be.getEnergy());
        // Refined fuel out via the side config (gated; null on non-OUTPUT/IO faces).
        event.registerBlockEntity(
                FLUID,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

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
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));

        // Rocket launch pad: a fluid sink that forwards rocket_fuel into a docked rocket (pipe / Fuel Tank
        // refuelling automation), plus an item sink that forwards fuel canisters into the rocket's intake.
        // Registered on the BLOCK (no block entity) — the proxies find the docked rocket.
        event.registerBlock(
                FLUID,
                (level, pos, state, blockEntity, side) -> new RocketPadFluidProxy(level, pos),
                ModBlocks.ROCKET_LAUNCH_PAD.get());
        // Gas sink: forwards oxygen into a docked rocket's onboard life-support tank (Oxygen Generator /
        // gas pipe → pad → rocket), the gas analogue of the fuel proxy above.
        event.registerBlock(
                GAS,
                (level, pos, state, blockEntity, side) -> new RocketPadGasProxy(level, pos),
                ModBlocks.ROCKET_LAUNCH_PAD.get());
        event.registerBlock(
                Capabilities.Item.BLOCK,
                (level, pos, state, blockEntity, side) -> side != null
                        ? new WorldlyContainerWrapper(new RocketPadItemContainer(level, pos), side)
                        : VanillaContainerWrapper.of(new RocketPadItemContainer(level, pos)),
                ModBlocks.ROCKET_LAUNCH_PAD.get());

        registerCoreEnergy(event);
        registerCoreGas(event);
        registerStandardFluid(event);
    }

    /**
     * Cross-mod gas (Neroland Core): expose the Universal Pipe's gas buffer on Core's shared
     * {@code nerolandcore:gas} capability too, so a Nero machine that pushes/pulls through Core's gas
     * lookup (NeroTech's Electrolyzer, Chemical Processor, Gas Turbine) reaches the pipe directly and any
     * gas id — not just oxygen — can enter it. Ungated per face, exactly like the pipe's
     * {@link #GAS} registration above. The pipe's {@code nerospace:gas} registration stays for
     * Nerospace's own {@code GasResource} consumers.
     *
     * <p>Nerospace's side-configured machines are deliberately NOT registered here: the pipe's
     * neighbour lookup ({@code PipeGas.find}) asks Core's capability first and falls back to
     * {@link #GAS}, where their gated view lives. Registering one here would need its gated
     * {@code sideConfig().gasView(side)}, or the gate could be walked around.</p>
     */
    private static void registerCoreGas(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(za.co.neroland.nerolandcore.platform.NeoForgeGasLookup.GAS,
                ModBlockEntities.UNIVERSAL_PIPE.get(), (be, side) -> be.getCoreGas());
    }

    /**
     * Cross-mod energy network (Neroland Core): expose every Nerospace energy block-entity on Core's
     * shared {@code nerolandcore:energy} capability too, so machines from any Nero mod interoperate on
     * one power network. {@link NerospaceEnergyStorage} extends {@code NeroEnergyStorage}, so the
     * existing {@code getEnergy()} suppliers satisfy Core's capability unchanged. The mod's own
     * {@link #ENERGY} stays registered above for back-compat.
     */
    private static void registerCoreEnergy(RegisterCapabilitiesEvent event) {
        BlockCapability<NeroEnergyStorage, Direction> core = NeoForgeEnergyLookup.ENERGY;
        // Side-config-integrated machines expose their energy through the gated side-config view so a
        // face set to DISABLED returns null (the Universal Pipe queries this Core capability via
        // Nerospace's delegating energy lookup). Machines not yet integrated keep the raw energy store.
        event.registerBlockEntity(core, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.NEROSIUM_GRINDER.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.PASSIVE_GENERATOR.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.UNIVERSAL_PIPE.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(core, ModBlockEntities.OXYGEN_GENERATOR.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.LAUNCH_CONTROLLER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(core, ModBlockEntities.SOLAR_PANEL.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.TERRAFORMER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(core, ModBlockEntities.FUEL_REFINERY.get(), (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(core, ModBlockEntities.QUARRY_CONTROLLER.get(), (be, side) -> be.getEnergy());
    }

    /**
     * Cross-mod fluid, EXPORT half: expose every block entity that already provides the mod-private
     * {@link #FLUID} capability on the platform-standard {@code Capabilities.Fluid.BLOCK} as well, wrapped
     * in {@link NeoForgeFluidResourceHandler}.
     *
     * <p>WHY: a foreign pipe, pump or tank only ever asks the standard capability, so before this every
     * Nerospace tank was invisible to the rest of the ecosystem — the reason players reported the Fluid
     * Tank as useless because they could not pipe into a bigger tank from another mod. This is purely
     * additive: the mod-private registrations above are untouched and are still what
     * {@code NeoForgeFluidLookup} asks first, so Nerospace's own blocks behave exactly as they did.</p>
     *
     * <p>Each line mirrors its mod-private twin one-for-one, INCLUDING the gated side-config views —
     * {@link NeoForgeFluidResourceHandler#of} propagates {@code null}, so a face the side config has
     * disabled stays invisible on the standard capability too and the gate cannot be walked around. The
     * Rocket Launch Pad is the ONE deliberate exception; see the comment at the end of this method.</p>
     *
     * <p>The three Neroland Core block-entities are listed because Nerospace re-exposes them on its own
     * fluid capability (see {@code CoreTankBridge} above). Core loads first, so if Core ever registers its
     * own provider for the standard capability, Core's wins and these are dead weight rather than a
     * conflict — NeoForge queries providers in registration order and takes the first non-null.</p>
     */
    private static void registerStandardFluid(RegisterCapabilitiesEvent event) {
        BlockCapability<ResourceHandler<FluidResource>, Direction> standard = Capabilities.Fluid.BLOCK;

        event.registerBlockEntity(standard, ModBlockEntities.UNIVERSAL_PIPE.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(be.getFluidTank()));
        event.registerBlockEntity(standard, ModBlockEntities.LAUNCH_CONTROLLER.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(be.getTank()));

        event.registerBlockEntity(standard, za.co.neroland.nerolandcore.registry.ModBlockEntities.TRASH_CAN.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getFluid())));
        event.registerBlockEntity(standard, za.co.neroland.nerolandcore.registry.ModBlockEntities.FLUID_TANK.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getTank())));
        event.registerBlockEntity(standard,
                za.co.neroland.nerolandcore.registry.ModBlockEntities.CREATIVE_FLUID_TANK.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.storage.CoreTankBridge.fluid(be.getTank())));

        event.registerBlockEntity(standard, ModBlockEntities.FUEL_TANK.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side)));
        event.registerBlockEntity(standard, ModBlockEntities.CARGO_PAD.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side)));
        event.registerBlockEntity(standard, ModBlockEntities.FUEL_REFINERY.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(
                        za.co.neroland.nerospace.machine.MachineSideConfig.fluidView(be.sideConfig(), side)));
        event.registerBlockEntity(standard, ModBlockEntities.QUARRY_CONTROLLER.get(),
                (be, side) -> NeoForgeFluidResourceHandler.of(be.getTank()));

        // DELIBERATELY NOT REGISTERED: ModBlocks.ROCKET_LAUNCH_PAD / RocketPadFluidProxy. The standard
        // handler implements `simulate` as "mutate, then undo if the transaction aborts", which is only
        // sound for an INVERTIBLE storage. The pad proxy is a write-only sink — its drain() always returns
        // 0 while its fill() permanently fuels the docked rocket — so it cannot be undone, and a foreign
        // mod's routine insert-then-abort probe would gift the rocket free fuel on every tick that probes.
        // Nothing is lost: the pad keeps its mod-private FLUID registration above, so Nerospace's own pipes
        // and the Fuel Tank refuel rockets exactly as before, and a foreign mod can still fuel a rocket by
        // piping into a Nerospace Fuel Tank that feeds the pad.
    }
}
//?} else {
/*import java.util.EnumMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.capabilities.Capabilities;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.NeoForgeGasLookup;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.FuelTankBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.SolarPanelBlockEntity;
import za.co.neroland.nerospace.machine.TerraformerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.platform.NeoForgeFluidResourceHandler;
import za.co.neroland.nerospace.storage.CoreTankBridge;

// NeoForge 20.2 still has Forge's capability system (no BlockCapability, no RegisterCapabilitiesEvent providers):
// every Nerospace block entity gets one attached provider, the same set and face gating as the Forge cells.
// The Rocket Launch Pad has no block entity, so the fluid / gas lookups reach its proxies directly instead.
public final class NeoForgeCapabilities {

    public static final Capability<NerospaceEnergyStorage> ENERGY = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<NerospaceFluidStorage> FLUID = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<NerospaceGasStorage> GAS = CapabilityManager.get(new CapabilityToken<>() { });

    private static final ResourceLocation MACHINE_CAPS = new ResourceLocation("nerospace", "machine_caps");

    private NeoForgeCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.register(NerospaceEnergyStorage.class);
            event.register(NerospaceFluidStorage.class);
            event.register(NerospaceGasStorage.class);
        });
        NeoForge.EVENT_BUS.addGenericListener(BlockEntity.class, NeoForgeCapabilities::onAttachBlockEntity);
    }

    // One capability of one block entity, or null.
    @Nullable
    public static <T> T find(Level level, BlockPos pos, Capability<T> capability, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(capability, side).orElse(null);
    }

    private static void onAttachBlockEntity(AttachCapabilitiesEvent<BlockEntity> event) {
        MachineCaps caps = createCaps(event.getObject());
        if (caps != null) {
            event.addCapability(MACHINE_CAPS, caps);
            event.addListener(caps::invalidate);
        }
    }

    @Nullable
    private static MachineCaps createCaps(BlockEntity be) {
        Supplier<NerospaceEnergyStorage> energy = null;
        Supplier<NerospaceFluidStorage> fluid = null;
        Supplier<NerospaceGasStorage> gas = null;
        Supplier<NeroGasStorage> coreGas = null;
        SideConfigComponent sideConfig = be instanceof SideConfigured sc ? sc.sideConfig() : null;

        if (be instanceof za.co.neroland.nerolandcore.storage.FluidTankBlockEntity t) {
            fluid = () -> CoreTankBridge.fluid(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.CreativeFluidTankBlockEntity t) {
            fluid = () -> CoreTankBridge.fluid(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.GasTankBlockEntity t) {
            gas = () -> CoreTankBridge.gas(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.CreativeGasTankBlockEntity t) {
            gas = () -> CoreTankBridge.gas(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.TrashCanBlockEntity trash) {
            fluid = () -> CoreTankBridge.fluid(trash.getFluid());
            gas = () -> CoreTankBridge.gas(trash.getGas());
        } else if (be instanceof CombustionGeneratorBlockEntity machine) {
            energy = machine::getEnergy;
        } else if (be instanceof NerosiumGrinderBlockEntity machine) {
            energy = machine::getEnergy;
        } else if (be instanceof PassiveGeneratorBlockEntity machine) {
            energy = machine::getEnergy;
        } else if (be instanceof UniversalPipeBlockEntity pipe) {
            energy = pipe::getEnergy;
            fluid = pipe::getFluidTank;
            gas = pipe::getGas;
            coreGas = pipe::getCoreGas;
        } else if (be instanceof OxygenGeneratorBlockEntity machine) {
            energy = machine::getEnergy;
            gas = machine::getGas;
        } else if (be instanceof SolarPanelBlockEntity panel) {
            energy = panel::getEnergy;
        } else if (be instanceof TerraformerBlockEntity machine) {
            energy = machine::getEnergy;
        } else if (be instanceof za.co.neroland.nerospace.route.CargoPadBlockEntity cargoPad) {
            fluid = cargoPad::getTank;
        } else if (be instanceof FuelTankBlockEntity tank) {
            fluid = tank::getTank;
        } else if (be instanceof FuelRefineryBlockEntity machine) {
            energy = machine::getEnergy;
            fluid = machine::getTank;
        } else if (be instanceof QuarryControllerBlockEntity machine) {
            energy = machine::getEnergy;
            fluid = machine::getTank;
        } else if (be instanceof za.co.neroland.nerospace.rocket.LaunchControllerBlockEntity controller) {
            energy = controller::getEnergy;
            fluid = controller::getTank;
            gas = controller::getGas;
        }

        // Item handlers only for Nerospace's own containers: vanilla and other mods expose their own.
        Container container = be instanceof Container c && be.getClass().getName().startsWith("za.co.neroland.nerospace.")
                ? c : null;
        if (energy == null && fluid == null && gas == null && container == null) {
            return null;
        }
        return new MachineCaps(energy, fluid, gas, coreGas, container, sideConfig);
    }

    private static IItemHandler itemHandler(Container container, @Nullable Direction side) {
        if (container instanceof WorldlyContainer worldly && side != null) {
            return new SidedInvWrapper(worldly, side);
        }
        return new InvWrapper(container);
    }

    private static final class MachineCaps implements ICapabilityProvider {

        private final LazyOptional<NerospaceEnergyStorage> energy;
        private final LazyOptional<NeroEnergyStorage> coreEnergy;
        private final LazyOptional<NerospaceFluidStorage> fluid;
        private final LazyOptional<IFluidHandler> standardFluid;
        private final LazyOptional<NerospaceGasStorage> gas;
        private final LazyOptional<NeroGasStorage> coreGas;
        @Nullable
        private final Container container;
        @Nullable
        private final LazyOptional<IItemHandler> itemUnsided;
        private final EnumMap<Direction, LazyOptional<IItemHandler>> itemBySide = new EnumMap<>(Direction.class);
        @Nullable
        private final SideConfigComponent sideConfig;

        MachineCaps(@Nullable Supplier<NerospaceEnergyStorage> energy, @Nullable Supplier<NerospaceFluidStorage> fluid,
                @Nullable Supplier<NerospaceGasStorage> gas, @Nullable Supplier<NeroGasStorage> coreGas,
                @Nullable Container container, @Nullable SideConfigComponent sideConfig) {
            this.energy = energy == null ? LazyOptional.empty() : LazyOptional.<NerospaceEnergyStorage>of(energy::get);
            this.coreEnergy = energy == null ? LazyOptional.empty() : LazyOptional.<NeroEnergyStorage>of(energy::get);
            this.fluid = fluid == null ? LazyOptional.empty() : LazyOptional.<NerospaceFluidStorage>of(fluid::get);
            this.standardFluid = fluid == null ? LazyOptional.empty()
                    : LazyOptional.<IFluidHandler>of(() -> NeoForgeFluidResourceHandler.of(fluid.get()));
            this.gas = gas == null ? LazyOptional.empty() : LazyOptional.<NerospaceGasStorage>of(gas::get);
            this.coreGas = coreGas == null ? LazyOptional.empty() : LazyOptional.<NeroGasStorage>of(coreGas::get);
            this.container = container;
            this.sideConfig = sideConfig;
            this.itemUnsided = container == null ? null : LazyOptional.of(() -> itemHandler(container, null));
        }

        // Whether the side config (if any) gates this channel to DISABLED on this face.
        private boolean gatedOff(Channel channel, @Nullable Direction side) {
            if (sideConfig == null || side == null) {
                return false;
            }
            return switch (channel) {
                case ENERGY -> sideConfig.energyView(side) == null;
                case FLUID -> sideConfig.fluidView(side) == null;
                case GAS -> sideConfig.gasView(side) == null;
                case ITEM -> false;
            };
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            if (cap == ENERGY) {
                return gatedOff(Channel.ENERGY, side) ? LazyOptional.empty() : energy.cast();
            }
            if (cap == NeoForgeEnergyLookup.ENERGY) {
                return gatedOff(Channel.ENERGY, side) ? LazyOptional.empty() : coreEnergy.cast();
            }
            if (cap == FLUID) {
                return gatedOff(Channel.FLUID, side) ? LazyOptional.empty() : fluid.cast();
            }
            if (cap == Capabilities.FLUID_HANDLER) {
                return gatedOff(Channel.FLUID, side) ? LazyOptional.empty() : standardFluid.cast();
            }
            if (cap == GAS) {
                return gatedOff(Channel.GAS, side) ? LazyOptional.empty() : gas.cast();
            }
            if (cap == NeoForgeGasLookup.GAS) {
                return gatedOff(Channel.GAS, side) ? LazyOptional.empty() : coreGas.cast();
            }
            if (cap == Capabilities.ITEM_HANDLER && container != null) {
                return item(side).cast();
            }
            return LazyOptional.empty();
        }

        private LazyOptional<IItemHandler> item(@Nullable Direction side) {
            if (side == null) {
                return itemUnsided == null ? LazyOptional.empty() : itemUnsided;
            }
            return itemBySide.computeIfAbsent(side, d -> LazyOptional.of(() -> itemHandler(container, d)));
        }

        void invalidate() {
            energy.invalidate();
            coreEnergy.invalidate();
            fluid.invalidate();
            standardFluid.invalidate();
            gas.invalidate();
            coreGas.invalidate();
            if (itemUnsided != null) {
                itemUnsided.invalidate();
            }
            itemBySide.values().forEach(LazyOptional::invalidate);
        }
    }
}
*///?}
