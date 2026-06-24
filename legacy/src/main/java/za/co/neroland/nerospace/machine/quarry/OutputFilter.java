package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.world.item.ItemStack;

/**
 * The seam for the future quarry filter feature (MINER_DESIGN — "whitelist-keep, void-rest"). Every
 * mined drop passes through {@link #keep(ItemStack)} before it is buffered; today the only
 * implementation is {@link #KEEP_ALL}, so nothing is voided. A later filter card will supply a
 * whitelist implementation and the mining loop needs no further change — items that fail the filter
 * are simply not buffered (voided).
 */
@FunctionalInterface
public interface OutputFilter {

    OutputFilter KEEP_ALL = stack -> true;

    /** @return whether {@code drop} should be kept (buffered/output) rather than voided. */
    boolean keep(ItemStack drop);
}
