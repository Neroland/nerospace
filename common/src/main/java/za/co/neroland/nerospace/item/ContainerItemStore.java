package za.co.neroland.nerospace.item;

import java.util.function.Predicate;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

/**
 * {@link NerospaceItemStore} over a vanilla {@link Container}, honouring {@link WorldlyContainer} face
 * rules. This is the path every Nerospace machine and every vanilla chest takes; the logic is the
 * pipe network's original sided insert/extract, lifted here unchanged so the network can speak one
 * interface to vanilla containers and to foreign mods' item handlers alike.
 */
public final class ContainerItemStore implements NerospaceItemStore {

    private final Container container;
    private final Direction side;

    public ContainerItemStore(Container container, Direction side) {
        this.container = container;
        this.side = side;
    }

    public Container container() {
        return this.container;
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (simulate) {
            return simulateInsert(stack.copy());
        }
        return insert(this.container, this.side, stack);
    }

    @Override
    public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
        int[] slots = slotsFor(this.container, this.side);
        for (int slot : slots) {
            ItemStack inSlot = this.container.getItem(slot);
            if (inSlot.isEmpty() || !filter.test(inSlot)) {
                continue;
            }
            if (this.container instanceof WorldlyContainer w
                    && !w.canTakeItemThroughFace(slot, inSlot, this.side)) {
                continue;
            }
            int take = Math.min(maxCount, inSlot.getCount());
            if (take <= 0) {
                continue;
            }
            return simulate ? inSlot.copyWithCount(take) : this.container.removeItem(slot, take);
        }
        return ItemStack.EMPTY;
    }

    /** Room-only pass used by {@link #insert(ItemStack, boolean)} when simulating; mutates nothing. */
    private ItemStack simulateInsert(ItemStack stack) {
        for (int slot : slotsFor(this.container, this.side)) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (!canPlace(this.container, slot, stack, this.side)) {
                continue;
            }
            ItemStack inSlot = this.container.getItem(slot);
            int max = Math.min(this.container.getMaxStackSize(), stack.getMaxStackSize());
            if (inSlot.isEmpty()) {
                stack.shrink(Math.min(max, stack.getCount()));
            } else if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                stack.shrink(Math.max(0, Math.min(max - inSlot.getCount(), stack.getCount())));
            }
        }
        return stack;
    }

    /** Standard sided insertion into a container; returns the un-inserted remainder. */
    private static ItemStack insert(Container dst, Direction side, ItemStack stack) {
        int[] slots = slotsFor(dst, side);
        // Pass 1: merge into matching stacks.
        for (int slot : slots) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (!canPlace(dst, slot, stack, side)) {
                continue;
            }
            ItemStack inSlot = dst.getItem(slot);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int max = Math.min(dst.getMaxStackSize(), inSlot.getMaxStackSize());
                int move = Math.min(max - inSlot.getCount(), stack.getCount());
                if (move > 0) {
                    inSlot.grow(move);
                    stack.shrink(move);
                    dst.setChanged();
                }
            }
        }
        // Pass 2: fill empty slots.
        for (int slot : slots) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (!canPlace(dst, slot, stack, side) || !dst.getItem(slot).isEmpty()) {
                continue;
            }
            int max = Math.min(dst.getMaxStackSize(), stack.getMaxStackSize());
            ItemStack put = stack.copyWithCount(Math.min(max, stack.getCount()));
            dst.setItem(slot, put);
            stack.shrink(put.getCount());
            dst.setChanged();
        }
        return stack;
    }

    private static boolean canPlace(Container dst, int slot, ItemStack stack, Direction side) {
        if (dst instanceof WorldlyContainer w) {
            return w.canPlaceItem(slot, stack) && w.canPlaceItemThroughFace(slot, stack, side);
        }
        return dst.canPlaceItem(slot, stack);
    }

    private static int[] slotsFor(Container container, Direction side) {
        if (container instanceof WorldlyContainer w) {
            return w.getSlotsForFace(side);
        }
        int[] slots = new int[container.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
