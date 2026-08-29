package za.co.neroland.nerospace.item;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutral, side-bound view of an item inventory the pipe network can move stacks through.
 *
 * <p>This is the item analogue of {@link za.co.neroland.nerospace.fluid.NerospaceFluidStorage}: it lets
 * the network treat a vanilla {@link net.minecraft.world.Container} and a <em>foreign</em> mod's
 * platform-standard item handler as the same thing. Vanilla containers are wrapped by
 * {@link ContainerItemStore}; anything else is resolved per loader through
 * {@link za.co.neroland.nerospace.platform.ItemLookup} — which is what lets a Universal Pipe insert into
 * a machine that exposes only NeoForge's {@code Capabilities.Item.BLOCK}, Fabric's
 * {@code ItemStorage.SIDED} or Forge's {@code ITEM_HANDLER} and never implements {@code Container}.</p>
 *
 * <p>An instance is bound to one position and one face and is valid for the tick it was resolved in;
 * never cache one across ticks.</p>
 */
public interface NerospaceItemStore {

    /**
     * Insert as much of {@code stack} as the target accepts.
     *
     * @return the remainder that could NOT be inserted (empty when everything moved). Implementations
     *         may return the same instance, shrunk — callers must use the returned value and must not
     *         assume the argument is untouched.
     */
    ItemStack insert(ItemStack stack, boolean simulate);

    /**
     * Extract up to {@code maxCount} items matching {@code filter} from a single logical slot.
     *
     * @return the extracted stack, or {@link ItemStack#EMPTY} if nothing matched.
     */
    ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate);
}
