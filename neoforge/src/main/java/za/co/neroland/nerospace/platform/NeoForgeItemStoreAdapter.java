package za.co.neroland.nerospace.platform;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.NerospaceItemStore;

/**
 * Presents a FOREIGN mod's platform-standard {@code ResourceHandler<ItemResource>} to Nerospace as a
 * {@link NerospaceItemStore}, so the Universal Pipe can push into and pull out of a machine that never
 * implements vanilla {@link net.minecraft.world.Container} and is therefore invisible to the pipe's
 * container path. Resolved by {@link NeoForgeItemLookup}.
 *
 * <p><b>Stack vs resource.</b> The transfer API splits a stack into a resource (item + components) and an
 * {@code int} amount; {@link NerospaceItemStore} speaks {@link ItemStack}. {@code ItemResource.of(stack)}
 * drops the count on the way in and {@code ItemResource#toStack(int)} restores it on the way out, so the
 * count travels as the transfer API's amount and the components survive the round trip untouched.</p>
 *
 * <p><b>Filters.</b> {@link #extract} takes a stack predicate but the handler filters by resource, so the
 * candidate resource is materialised as a single-item stack for the predicate to inspect. Every filter
 * Nerospace passes tests item identity and components — never the count — so a count of one is enough;
 * a count-sensitive predicate would misjudge here, which is why the pipe does not use one.</p>
 *
 * <p><b>Transactions.</b> {@code simulate} is implemented as a root transaction that is committed only
 * when {@code simulate} is false — closing without committing IS the rollback. Opening a ROOT transaction
 * is safe because this adapter is only reached from Nerospace's transaction-unaware common code.</p>
 *
 * <p>An instance is bound to one position and one face for the tick it was resolved in; never cache one.</p>
 */
public final class NeoForgeItemStoreAdapter implements NerospaceItemStore {

    private final ResourceHandler<ItemResource> handler;

    private NeoForgeItemStoreAdapter(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    /** Wrap {@code handler}, propagating {@code null} so the lookup stays a one-line fallback. */
    @Nullable
    public static NeoForgeItemStoreAdapter of(@Nullable ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new NeoForgeItemStoreAdapter(handler);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemResource resource = ItemResource.of(stack);
        int count = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            // insertStacking fills matching indices before empty ones, which is what a hopper/pipe expects.
            int inserted = ResourceHandlerUtil.insertStacking(this.handler, resource, count, transaction);
            if (inserted <= 0) {
                return stack;
            }
            if (!simulate) {
                transaction.commit();
            }
            // The contract is REMAINDER, and a fresh stack keeps the caller's argument intact either way.
            return inserted >= count ? ItemStack.EMPTY : resource.toStack(count - inserted);
        }
    }

    @Override
    public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
        if (maxCount <= 0) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            // NOTE: extractFirst returns null (not an empty stack) when nothing is extractable.
            ResourceStack<ItemResource> got = ResourceHandlerUtil.extractFirst(this.handler,
                    resource -> !resource.isEmpty() && filter.test(resource.toStack(1)), maxCount, transaction);
            if (got == null || got.amount() <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = got.resource().toStack(got.amount());
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
    }
}
