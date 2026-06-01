package za.co.neroland.nerospace.machine;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Block entity for the {@link FuelTankBlock} (Phase 8a). It stores a large buffer of
 * {@code rocket_fuel} and, each server tick, automatically feeds a rocket standing on an adjacent
 * launch pad — the first piece of the "massive launch pad" machinery from the roadmap.
 *
 * <p>Fuelling is done by calling the rocket's own {@code addFuel}, so the tier fuel cap and overflow
 * handling stay in one place. A complete 3x3 pad footprint pumps faster than a partial cluster,
 * giving players a reason to build the full multiblock.</p>
 */
public class FuelTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 32_000;
    /** Fuel moved per tick into a rocket on a partial pad cluster. */
    public static final int PUMP_RATE = 40;
    /** Faster feed once the canonical full 3x3 pad is formed. */
    public static final int PUMP_RATE_FULL_PAD = 160;
    /** One bucket / canister of fuel, in millibuckets. */
    public static final int CONTAINER_MB = 1_000;

    private final FluidTank tank = new FluidTank(CAPACITY,
            stack -> stack.getFluid() == ModFluids.ROCKET_FUEL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK.get(), pos, state);
    }

    // --- Fuel access (used by the block's item interaction) -----------------

    public int getFluidAmount() {
        return this.tank.getFluidAmount();
    }

    public int getCapacity() {
        return this.tank.getCapacity();
    }

    /**
     * Tries to add one container (bucket/canister) of fuel.
     *
     * @return {@code true} if the whole container fit (so the caller may consume the item).
     */
    public boolean tryFillContainer() {
        int filled = this.tank.fill(
                new FluidStack(ModFluids.ROCKET_FUEL.get(), CONTAINER_MB), IFluidHandler.FluidAction.EXECUTE);
        return filled == CONTAINER_MB;
    }

    /**
     * Tries to draw one bucket of fuel out of the tank (for refilling an empty bucket).
     *
     * @return {@code true} if a full bucket was removed.
     */
    public boolean tryDrainBucket() {
        FluidStack drained = this.tank.drain(CONTAINER_MB, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount() == CONTAINER_MB;
    }

    /** Human-readable fuel readout for the right-click status message. */
    public Component statusMessage() {
        return Component.translatable("block.nerospace.fuel_tank.status",
                this.tank.getFluidAmount(), this.tank.getCapacity());
    }

    /** Comparator output: 0 (empty) .. 15 (full), scaled by fill fraction. */
    public int comparatorSignal() {
        if (this.tank.getFluidAmount() <= 0) {
            return 0;
        }
        return 1 + (int) (this.tank.getFluidAmount() / (double) this.tank.getCapacity() * 14.0D);
    }

    // --- Ticking ------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || this.tank.getFluidAmount() <= 0) {
            return;
        }

        BlockPos padPos = LaunchPadMultiblock.adjacentPad(level, pos);
        if (padPos == null) {
            return;
        }

        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, padPos);
        RocketEntity rocket = LaunchPadMultiblock.rocketAbove(level, pads);
        if (rocket == null) {
            return;
        }

        int rate = LaunchPadMultiblock.isFullThreeByThree(pads) ? PUMP_RATE_FULL_PAD : PUMP_RATE;
        int toPump = Math.min(rate, this.tank.getFluidAmount());
        if (toPump <= 0) {
            return;
        }

        FluidStack drained = this.tank.drain(toPump, IFluidHandler.FluidAction.EXECUTE);
        int overflow = rocket.addFuel(drained.getAmount());
        if (overflow > 0) {
            // The rocket was already topped up; return the unused fuel to the tank.
            this.tank.fill(new FluidStack(ModFluids.ROCKET_FUEL.get(), overflow), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("FuelTank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("FuelTank"));
    }
}
