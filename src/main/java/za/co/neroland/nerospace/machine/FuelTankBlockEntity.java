package za.co.neroland.nerospace.machine;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.RocketFuelTank;
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
public class FuelTankBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 32_000;
    /** Fuel moved per tick into a rocket on a partial pad cluster. */
    public static final int PUMP_RATE = 40;
    /** Faster feed once the canonical full 3x3 pad is formed. */
    public static final int PUMP_RATE_FULL_PAD = 160;
    /** One bucket / canister of fuel, in millibuckets. */
    public static final int CONTAINER_MB = 1_000;

    @SuppressWarnings("this-escape") // change-callback wiring, used only after construction
    private final RocketFuelTank tank = new RocketFuelTank(CAPACITY, this::setChanged);

    /** Synced to the open menu: [0]=fuel, [1]=capacity. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? tank.getAmount() : tank.getCapacity();
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.fuel_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuelTankMenu(containerId, playerInventory, this.dataAccess);
    }

    // --- Fuel access (used by the block's item interaction) -----------------

    /** The tank as a transfer-API handler, for {@code Capabilities.Fluid.BLOCK} (pipe filling). */
    public RocketFuelTank getTank() {
        return this.tank;
    }

    public int getFluidAmount() {
        return this.tank.getAmount();
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
        return this.tank.fill(CONTAINER_MB) == CONTAINER_MB;
    }

    /**
     * Tries to draw one bucket of fuel out of the tank (for refilling an empty bucket).
     *
     * @return {@code true} if a full bucket was removed.
     */
    public boolean tryDrainBucket() {
        return this.tank.drain(CONTAINER_MB) == CONTAINER_MB;
    }

    /** Human-readable fuel readout for the right-click status message. */
    public Component statusMessage() {
        return Component.translatable("block.nerospace.fuel_tank.status",
                this.tank.getAmount(), this.tank.getCapacity());
    }

    /** Comparator output: 0 (empty) .. 15 (full), scaled by fill fraction. */
    public int comparatorSignal() {
        if (this.tank.getAmount() <= 0) {
            return 0;
        }
        return 1 + (int) (this.tank.getAmount() / (double) this.tank.getCapacity() * 14.0D);
    }

    // --- Ticking ------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || this.tank.isEmpty()) {
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
        int toPump = Math.min(rate, this.tank.getAmount());
        if (toPump <= 0) {
            return;
        }

        int drained = this.tank.drain(toPump);
        int overflow = rocket.addFuel(drained);
        if (overflow > 0) {
            // The rocket was already topped up; return the unused fuel to the tank.
            this.tank.fill(overflow);
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
