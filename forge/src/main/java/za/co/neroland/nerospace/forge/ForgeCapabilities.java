package za.co.neroland.nerospace.forge;

import java.util.EnumMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
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

/** Forge capability providers for the existing loader-neutral storage seams. */
public final class ForgeCapabilities {

    public static final Capability<ForgeEnergyStorageCapability> ENERGY =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<ForgeFluidStorageCapability> FLUID =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<ForgeGasStorageCapability> GAS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final Identifier MACHINE_CAPS =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "machine_caps");

    private ForgeCapabilities() {
    }

    public static void register() {
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeCapabilities::onAttachBlockEntity);
    }

    private static void onAttachBlockEntity(AttachCapabilitiesEvent.BlockEntities event) {
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
        // When present, gates the per-face energy/fluid/gas capability through Neroland Core's side config
        // (a face set to DISABLED exposes no capability). Resolved below for side-config-integrated machines.
        za.co.neroland.nerolandcore.sideconfig.SideConfigComponent sideConfig =
                be instanceof za.co.neroland.nerolandcore.sideconfig.SideConfigured sc ? sc.sideConfig() : null;

        // The Battery / Fluid Tank / Gas Tank / Item Store endpoints now live in Neroland Core. Battery
        // energy crosses via Core's own Forge capability wiring and the Item Store is a vanilla Container;
        // only the Core fluid/gas tanks need bridging back onto Nerospace's fluid/gas caps so the Universal
        // Pipe still connects to them.
        if (be instanceof za.co.neroland.nerolandcore.storage.FluidTankBlockEntity t) {
            fluid = () -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.CreativeFluidTankBlockEntity t) {
            fluid = () -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.GasTankBlockEntity t) {
            gas = () -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(t.getTank());
        } else if (be instanceof za.co.neroland.nerolandcore.storage.CreativeGasTankBlockEntity t) {
            gas = () -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(t.getTank());
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
        } else if (be instanceof za.co.neroland.nerolandcore.storage.TrashCanBlockEntity trash) {
            fluid = () -> za.co.neroland.nerospace.storage.CoreTankBridge.fluid(trash.getFluid());
            gas = () -> za.co.neroland.nerospace.storage.CoreTankBridge.gas(trash.getGas());
        } else if (be instanceof OxygenGeneratorBlockEntity machine) {
            energy = machine::getEnergy;
            gas = machine::getGas;
        } else if (be instanceof SolarPanelBlockEntity panel) {
            energy = panel::getEnergy;
        } else if (be instanceof TerraformerBlockEntity machine) {
            energy = machine::getEnergy;
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

        Container container = be instanceof Container c ? c : null;
        if (energy == null && fluid == null && gas == null && container == null) {
            return null;
        }
        return new MachineCaps(energy, fluid, gas, container, sideConfig);
    }

    private static IItemHandler itemHandler(Container container, @Nullable Direction side) {
        if (container instanceof WorldlyContainer worldly && side != null) {
            return new SidedInvWrapper(worldly, side);
        }
        return new InvWrapper(container);
    }

    private static final class MachineCaps implements ICapabilityProvider {

        private final LazyOptional<ForgeEnergyStorageCapability> energy;
        // Same storage exposed on Neroland Core's shared nerolandcore:energy capability (cross-mod power
        // network). NerospaceEnergyStorage IS a NeroEnergyStorage, so no adapter is needed.
        private final LazyOptional<NeroEnergyStorage> coreEnergy;
        private final LazyOptional<ForgeFluidStorageCapability> fluid;
        private final LazyOptional<ForgeGasStorageCapability> gas;
        @Nullable
        private final Container container;
        @Nullable
        private final LazyOptional<IItemHandler> itemUnsided;
        private final EnumMap<Direction, LazyOptional<IItemHandler>> itemBySide = new EnumMap<>(Direction.class);
        /** When non-null, gates the energy/fluid/gas caps per face through Core's side config. */
        @Nullable
        private final za.co.neroland.nerolandcore.sideconfig.SideConfigComponent sideConfig;

        MachineCaps(@Nullable Supplier<NerospaceEnergyStorage> energy,
                @Nullable Supplier<NerospaceFluidStorage> fluid,
                @Nullable Supplier<NerospaceGasStorage> gas, @Nullable Container container,
                @Nullable za.co.neroland.nerolandcore.sideconfig.SideConfigComponent sideConfig) {
            this.energy = energy == null ? LazyOptional.empty() : LazyOptional.of(() -> new EnergyAdapter(energy));
            this.coreEnergy = energy == null ? LazyOptional.empty() : LazyOptional.<NeroEnergyStorage>of(energy::get);
            this.fluid = fluid == null ? LazyOptional.empty() : LazyOptional.of(() -> new FluidAdapter(fluid));
            this.gas = gas == null ? LazyOptional.empty() : LazyOptional.of(() -> new GasAdapter(gas));
            this.container = container;
            this.sideConfig = sideConfig;
            this.itemUnsided = container == null ? null : LazyOptional.of(() -> itemHandler(container, null));
        }

        /** Whether the side config (if any) gates {@code channel} to DISABLED on {@code side}. */
        private boolean gatedOff(za.co.neroland.nerolandcore.sideconfig.Channel channel, @Nullable Direction side) {
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
                return gatedOff(za.co.neroland.nerolandcore.sideconfig.Channel.ENERGY, side)
                        ? LazyOptional.empty() : energy.cast();
            }
            if (cap == ForgeEnergyLookup.ENERGY) {
                return gatedOff(za.co.neroland.nerolandcore.sideconfig.Channel.ENERGY, side)
                        ? LazyOptional.empty() : coreEnergy.cast();
            }
            if (cap == FLUID) {
                return gatedOff(za.co.neroland.nerolandcore.sideconfig.Channel.FLUID, side)
                        ? LazyOptional.empty() : fluid.cast();
            }
            if (cap == GAS) {
                return gatedOff(za.co.neroland.nerolandcore.sideconfig.Channel.GAS, side)
                        ? LazyOptional.empty() : gas.cast();
            }
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && container != null) {
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
            gas.invalidate();
            if (itemUnsided != null) {
                itemUnsided.invalidate();
            }
            itemBySide.values().forEach(LazyOptional::invalidate);
        }
    }

    private record EnergyAdapter(Supplier<NerospaceEnergyStorage> delegate) implements ForgeEnergyStorageCapability {
        @Override
        public long getAmount() {
            return delegate.get().getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.get().getCapacity();
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            return delegate.get().insert(maxAmount, simulate);
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return delegate.get().extract(maxAmount, simulate);
        }
    }

    private record FluidAdapter(Supplier<NerospaceFluidStorage> delegate) implements ForgeFluidStorageCapability {
        @Override
        public net.minecraft.world.level.material.Fluid getFluid() {
            return delegate.get().getFluid();
        }

        @Override
        public long getAmount() {
            return delegate.get().getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.get().getCapacity();
        }

        @Override
        public long fill(net.minecraft.world.level.material.Fluid fluid, long amount, boolean simulate) {
            return delegate.get().fill(fluid, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return delegate.get().drain(amount, simulate);
        }
    }

    private record GasAdapter(Supplier<NerospaceGasStorage> delegate) implements ForgeGasStorageCapability {
        @Override
        public GasResource getGas() {
            return delegate.get().getGas();
        }

        @Override
        public long getAmount() {
            return delegate.get().getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.get().getCapacity();
        }

        @Override
        public long fill(GasResource gas, long amount, boolean simulate) {
            return delegate.get().fill(gas, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return delegate.get().drain(amount, simulate);
        }
    }
}
