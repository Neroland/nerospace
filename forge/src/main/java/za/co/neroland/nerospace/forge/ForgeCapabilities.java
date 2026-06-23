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
import za.co.neroland.nerospace.storage.BatteryBlockEntity;
import za.co.neroland.nerospace.storage.CreativeBatteryBlockEntity;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeGasTankBlockEntity;
import za.co.neroland.nerospace.storage.FluidTankBlockEntity;
import za.co.neroland.nerospace.storage.GasTankBlockEntity;
import za.co.neroland.nerospace.storage.TrashCanBlockEntity;

/** Forge capability providers for the existing loader-neutral storage seams. */
@SuppressWarnings("null")
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

        if (be instanceof BatteryBlockEntity battery) {
            energy = battery::getEnergy;
        } else if (be instanceof CreativeBatteryBlockEntity battery) {
            energy = battery::getEnergy;
        } else if (be instanceof FluidTankBlockEntity tank) {
            fluid = tank::getTank;
        } else if (be instanceof CreativeFluidTankBlockEntity tank) {
            fluid = tank::getTank;
        } else if (be instanceof GasTankBlockEntity tank) {
            gas = tank::getTank;
        } else if (be instanceof CreativeGasTankBlockEntity tank) {
            gas = tank::getTank;
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
        } else if (be instanceof TrashCanBlockEntity trash) {
            fluid = trash::getFluid;
            gas = trash::getGas;
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
        }

        Container container = be instanceof Container c ? c : null;
        if (energy == null && fluid == null && gas == null && container == null) {
            return null;
        }
        return new MachineCaps(energy, fluid, gas, container);
    }

    private static IItemHandler itemHandler(Container container, @Nullable Direction side) {
        if (container instanceof WorldlyContainer worldly && side != null) {
            return new SidedInvWrapper(worldly, side);
        }
        return new InvWrapper(container);
    }

    private static final class MachineCaps implements ICapabilityProvider {

        private final LazyOptional<ForgeEnergyStorageCapability> energy;
        private final LazyOptional<ForgeFluidStorageCapability> fluid;
        private final LazyOptional<ForgeGasStorageCapability> gas;
        @Nullable
        private final Container container;
        @Nullable
        private final LazyOptional<IItemHandler> itemUnsided;
        private final EnumMap<Direction, LazyOptional<IItemHandler>> itemBySide = new EnumMap<>(Direction.class);

        MachineCaps(@Nullable Supplier<NerospaceEnergyStorage> energy,
                @Nullable Supplier<NerospaceFluidStorage> fluid,
                @Nullable Supplier<NerospaceGasStorage> gas, @Nullable Container container) {
            this.energy = energy == null ? LazyOptional.empty() : LazyOptional.of(() -> new EnergyAdapter(energy));
            this.fluid = fluid == null ? LazyOptional.empty() : LazyOptional.of(() -> new FluidAdapter(fluid));
            this.gas = gas == null ? LazyOptional.empty() : LazyOptional.of(() -> new GasAdapter(gas));
            this.container = container;
            this.itemUnsided = container == null ? null : LazyOptional.of(() -> itemHandler(container, null));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            if (cap == ENERGY) {
                return energy.cast();
            }
            if (cap == FLUID) {
                return fluid.cast();
            }
            if (cap == GAS) {
                return gas.cast();
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
