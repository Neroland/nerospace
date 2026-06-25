package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.menu.TrashCanMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Trash Can — a bottomless sink. Exposes item and fluid surfaces that accept anything (from hoppers
 * or pipes) and void it: the item container always reads empty and discards on insert; the fluid
 * and gas sinks accept any amount and store nothing. Nothing can be pulled back out.
 */
public class TrashCanBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    private static final int SIZE = 1;
    private static final int @org.jspecify.annotations.NonNull[] SLOTS = {0};

    /** Last item dropped in through the GUI; voided only when the NEXT stack is inserted (so closing the
     *  screen never trashes it). Held purely as a reference — it is never shown or retrievable. */
    @SuppressWarnings("unused") // intentionally write-only: holding the ref is what defers the void
    private ItemStack guiHeld = ItemStack.EMPTY;

    /** GUI drop buffer: reports the slot empty so every insert is accepted, then holds the inserted stack,
     *  discarding the previously-held one. Separate from the automation void-sink (hoppers / pipes) above. */
    private final Container guiBuffer = new Container() {
        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty()) {
                guiHeld = stack; // hold the new stack; the previously-held one is dropped = voided
            }
        }

        @Override
        public void setChanged() {
            // nothing persisted
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            guiHeld = ItemStack.EMPTY;
        }
    };

    private final NerospaceFluidStorage voidFluid = new NerospaceFluidStorage() {
        @Override
        public Fluid getFluid() {
            return Fluids.EMPTY;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return 1_000_000;
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return Math.max(0, amount);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return 0;
        }
    };

    private final NerospaceGasStorage voidGas = new NerospaceGasStorage() {
        @Override
        public GasResource getGas() {
            return GasResource.EMPTY;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return 1_000_000;
        }

        @Override
        public long fill(GasResource gas, long amount, boolean simulate) {
            return Math.max(0, amount);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return 0;
        }
    };

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN.get(), pos, state);
    }

    public NerospaceFluidStorage getFluid() {
        return this.voidFluid;
    }

    public NerospaceGasStorage getGas() {
        return this.voidGas;
    }

    // --- MenuProvider (the manual trash GUI; trashes on the NEXT insert) -------
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.trash_can");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new TrashCanMenu(containerId, playerInventory, this.guiBuffer);
    }

    // --- WorldlyContainer (void item sink) ------------------------------------
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // void: store nothing
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // nothing stored
    }
}
