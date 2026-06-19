package za.co.neroland.nerospace.fluid;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Single-fluid bounded tank (millibuckets) backing a block entity. */
public final class FluidTank implements NerospaceFluidStorage {

    private Fluid fluid = Fluids.EMPTY;
    private long amount;
    private final long capacity;
    private final Runnable onChanged;

    public FluidTank(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    @Override
    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    public long getAmount() {
        return this.amount;
    }

    @Override
    public long getCapacity() {
        return this.capacity;
    }

    @Override
    public long fill(Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0 || fluid == Fluids.EMPTY) {
            return 0;
        }
        if (this.fluid != Fluids.EMPTY && this.fluid != fluid) {
            return 0;
        }
        long filled = Math.min(amount, this.capacity - this.amount);
        if (filled > 0 && !simulate) {
            if (this.fluid == Fluids.EMPTY) {
                this.fluid = fluid;
            }
            this.amount += filled;
            this.onChanged.run();
        }
        return filled;
    }

    @Override
    public long drain(long amount, boolean simulate) {
        if (amount <= 0 || this.amount == 0) {
            return 0;
        }
        long drained = Math.min(amount, this.amount);
        if (!simulate) {
            this.amount -= drained;
            if (this.amount == 0) {
                this.fluid = Fluids.EMPTY;
            }
            this.onChanged.run();
        }
        return drained;
    }

    // Raw accessors for NBT save/load.
    public Fluid getRawFluid() {
        return this.fluid;
    }

    public int getRawAmount() {
        return (int) this.amount;
    }

    public void setRaw(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = Math.max(0, Math.min((int) this.capacity, amount));
        if (this.amount == 0) {
            this.fluid = Fluids.EMPTY;
        }
    }
}
