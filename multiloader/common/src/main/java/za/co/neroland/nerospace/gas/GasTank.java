package za.co.neroland.nerospace.gas;

/** Single-gas bounded tank (millibuckets) backing a block entity. Mirrors {@code FluidTank}. */
public final class GasTank implements NerospaceGasStorage {

    private GasResource gas = GasResource.EMPTY;
    private long amount;
    private final long capacity;
    private final Runnable onChanged;

    public GasTank(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    @Override
    public GasResource getGas() {
        return this.gas;
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
    public long fill(GasResource gas, long amount, boolean simulate) {
        if (amount <= 0 || gas.isEmpty()) {
            return 0;
        }
        if (!this.gas.isEmpty() && this.gas != gas) {
            return 0;
        }
        long filled = Math.min(amount, this.capacity - this.amount);
        if (filled > 0 && !simulate) {
            if (this.gas.isEmpty()) {
                this.gas = gas;
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
                this.gas = GasResource.EMPTY;
            }
            this.onChanged.run();
        }
        return drained;
    }

    // Raw accessors for NBT save/load.
    public GasResource getRawGas() {
        return this.gas;
    }

    public int getRawAmount() {
        return (int) this.amount;
    }

    public void setRaw(GasResource gas, int amount) {
        this.gas = gas;
        this.amount = Math.max(0, Math.min((int) this.capacity, amount));
        if (this.amount == 0) {
            this.gas = GasResource.EMPTY;
        }
    }
}
