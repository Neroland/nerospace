package za.co.neroland.nerospace.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import org.jetbrains.annotations.Nullable;

/**
 * The authoritative item store for a machine: a transfer-API handler (hopper/pipe capability) that
 * ALSO backs the machine's vanilla {@code Container} methods, GUI slots and tick logic through the
 * {@code *Stack} accessors below.
 *
 * <p>WHY: {@code StacksResourceHandler} <b>copies</b> any {@code NonNullList} passed to its
 * constructor ({@code mutableCopyOf}) — it never shares a Container's backing list. The Phase 9
 * machines were built on the shared-list assumption, so items inserted through
 * {@code Capabilities.Item.BLOCK} landed in the handler's private copy and the machines never saw
 * them (caught by the {@code *_cap_feed} gametests). With this class the handler IS the single
 * source of truth and the Container side is a view, mirroring
 * {@code RocketEntity.IntakeContainer}.</p>
 */
public class MachineItemHandler extends ItemStacksResourceHandler {

    /** Per-slot insert filter for the capability surface (see {@link #isValid}). */
    @FunctionalInterface
    public interface SlotValidator {
        boolean isValid(int index, ItemResource resource);
    }

    private final Runnable changeCallback;
    @Nullable
    private final SlotValidator validator;

    /** @param changeCallback typically the owner's {@code setChanged} (chunk dirty marking). */
    public MachineItemHandler(int size, Runnable changeCallback) {
        this(size, changeCallback, null);
    }

    public MachineItemHandler(int size, Runnable changeCallback, @Nullable SlotValidator validator) {
        super(size);
        this.changeCallback = changeCallback;
        this.validator = validator;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        SlotValidator local = this.validator; // local copy so the null check holds for the analyzer
        return local == null || local.isValid(index, resource);
    }

    @Override
    protected void onContentsChanged(int index, ItemStack oldStack) {
        this.changeCallback.run();
    }

    // --- Container-side accessors (live references, vanilla slot semantics) ----

    /** The LIVE stack in {@code index} (menus/ticks may mutate it in place, like vanilla slots). */
    public ItemStack getStack(int index) {
        return this.stacks.get(index);
    }

    public void setStack(int index, ItemStack stack) {
        this.stacks.set(index, stack);
        this.changeCallback.run();
    }

    /** {@code Container.removeItem} semantics: split {@code amount} off the stored stack. */
    public ItemStack removeStack(int index, int amount) {
        ItemStack removed = this.stacks.get(index).split(amount);
        if (!removed.isEmpty()) {
            this.changeCallback.run();
        }
        return removed;
    }

    /** {@code Container.removeItemNoUpdate} semantics: take the whole stack out. */
    public ItemStack takeStack(int index) {
        ItemStack stack = this.stacks.get(index);
        this.stacks.set(index, ItemStack.EMPTY);
        return stack;
    }

    public boolean isStoreEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void clearStore() {
        for (int i = 0; i < this.stacks.size(); i++) {
            this.stacks.set(i, ItemStack.EMPTY);
        }
        this.changeCallback.run();
    }
}
