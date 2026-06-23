package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.world.item.ItemStack;

/**
 * The seam for the quarry output filter. Every mined drop passes through {@link #keep(ItemStack)}
 * before it is buffered; the only implementation is {@link #KEEP_ALL}, so nothing is voided.
 */
@FunctionalInterface
public interface OutputFilter {

    OutputFilter KEEP_ALL = stack -> true;

    /** @return whether {@code drop} should be kept (buffered/output) rather than voided. */
    boolean keep(ItemStack drop);
}
