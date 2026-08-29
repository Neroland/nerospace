package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.forge.ForgeCapabilities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.rocket.RocketPadFluidProxy;

/** Forge query of the mod's fluid capability, falling back to Forge's standard fluid handler. */
public final class ForgeFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            // Mod-private capability first: Nerospace's own tanks must keep their exact behaviour and
            // never be seen through the lossy int-millibucket standard view.
            NerospaceFluidStorage cap = be.getCapability(ForgeCapabilities.FLUID, side).orElse(null);
            if (cap != null) {
                return cap;
            }
            // Foreign machines speak only Forge's standard capability; adapt one so the Universal Pipe
            // can trade fluid with them.
            IFluidHandler handler = be
                    .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side)
                    .orElse(null);
            if (handler != null) {
                return new HandlerStorage(handler);
            }
        }
        // The launch pad has no block entity: expose the rocket fuel sink directly. This must stay
        // reachable even when a block entity IS present but exposes no fluid capability at all.
        if (level.getBlockState(pos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
            return new RocketPadFluidProxy(level, pos);
        }
        return null;
    }

    /**
     * {@link NerospaceFluidStorage} over a Forge {@link IFluidHandler}. Nerospace's contract is a SINGLE
     * tank, so a multi-tank handler is projected onto one "view tank" — the first tank holding something,
     * or tank 0 when the handler is empty. Amount and capacity are both read from that one tank so a
     * caller's fill-level maths stays self-consistent; fills and drains are handed to the handler whole
     * and may still spread across its other tanks, which is why callers must trust the returned amount.
     * Both sides count millibuckets, so there is no unit conversion — only a long/int clamp.
     */
    private record HandlerStorage(IFluidHandler handler) implements NerospaceFluidStorage {

        /** The projected tank, or {@code -1} when the handler declares no tanks at all. */
        private int viewTank() {
            int tanks = this.handler.getTanks();
            if (tanks == 0) {
                return -1;
            }
            for (int tank = 0; tank < tanks; tank++) {
                if (!this.handler.getFluidInTank(tank).isEmpty()) {
                    return tank;
                }
            }
            return 0;
        }

        private FluidStack viewStack() {
            int tank = viewTank();
            return tank < 0 ? FluidStack.EMPTY : this.handler.getFluidInTank(tank);
        }

        @Override
        public Fluid getFluid() {
            return viewStack().getFluid();
        }

        @Override
        public long getAmount() {
            return viewStack().getAmount();
        }

        @Override
        public long getCapacity() {
            int tank = viewTank();
            return tank < 0 ? 0L : this.handler.getTankCapacity(tank);
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            if (fluid == Fluids.EMPTY || amount <= 0L) {
                return 0L;
            }
            FluidStack offered = new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
            return this.handler.fill(offered, action(simulate));
        }

        @Override
        public long drain(long amount, boolean simulate) {
            if (amount <= 0L) {
                return 0L;
            }
            FluidStack held = viewStack();
            if (held.isEmpty()) {
                return 0L;
            }
            // Drain by stack rather than by the amount-only overload: it pins the drain to the fluid
            // getFluid() just advertised, and the copy constructor carries the stack's NBT across so a
            // tagged fluid still matches the handler's own equality check.
            FluidStack requested = new FluidStack(held, (int) Math.min(amount, Integer.MAX_VALUE));
            return this.handler.drain(requested, action(simulate)).getAmount();
        }

        private static IFluidHandler.FluidAction action(boolean simulate) {
            return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        }
    }
}
