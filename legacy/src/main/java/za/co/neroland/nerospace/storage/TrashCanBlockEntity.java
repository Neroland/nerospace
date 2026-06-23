package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasStacksResourceHandler;
import za.co.neroland.nerospace.machine.MachineItemHandler;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Trash Can: a bottomless sink for every transferable layer. It exposes item, fluid and gas
 * capabilities that accept anything inserted (by hopper or pipe) and then <b>void it</b>. Each layer
 * is a high-capacity buffer that is emptied every server tick, so it always has room and never spills
 * anything back out — there is no extraction surface, so nothing can be pulled out of it.
 */
public class TrashCanBlockEntity extends BlockEntity {

    private static final int ITEM_SLOTS = 9;
    /** Per-tick headroom for fluids/gas (mB) — far above any pipe's throughput. */
    private static final int VOID_CAPACITY = 1_000_000;

    private final MachineItemHandler items = new MachineItemHandler(ITEM_SLOTS, () -> { });
    private final VoidFluid fluid = new VoidFluid();
    private final VoidGas gas = new VoidGas();

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        return this.items;
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluid;
    }

    public ResourceHandler<GasResource> getGasHandler() {
        return this.gas;
    }

    /** Empty every sink each tick so it always accepts more and stores nothing. */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        if (!this.items.isStoreEmpty()) {
            this.items.clearStore();
        }
        this.fluid.clear();
        this.gas.clear();
    }

    private static final class VoidFluid extends FluidStacksResourceHandler {
        private VoidFluid() {
            super(1, VOID_CAPACITY);
        }

        void clear() {
            if (getAmountAsInt(0) > 0) {
                set(0, FluidResource.EMPTY, 0);
            }
        }
    }

    private static final class VoidGas extends GasStacksResourceHandler {
        private VoidGas() {
            super(1, VOID_CAPACITY);
        }

        void clear() {
            if (getAmountAsInt(0) > 0) {
                set(0, GasResource.EMPTY, 0);
            }
        }
    }
}
