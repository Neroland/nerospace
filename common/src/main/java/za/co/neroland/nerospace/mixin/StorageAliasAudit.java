package za.co.neroland.nerospace.mixin;

/**
 * Whether the storage-alias injections landed on {@code MappedRegistry}. A miss is a warning, not a crash:
 * the world still loads, and old {@code nerospace:} storage blocks fall back to being replaced by hand.
 */
public final class StorageAliasAudit {

    private StorageAliasAudit() {
    }

    public static boolean complete(boolean holderByKey, boolean holderById, boolean valueById) {
        return holderByKey && holderById && valueById;
    }

    /** A log line when an injection is missing, or null when all three landed. No world data. */
    public static String warning(boolean holderByKey, boolean holderById, boolean valueById) {
        if (complete(holderByKey, holderById, valueById)) {
            return null;
        }
        return "[Nerospace] Storage-alias remap did not fully apply (holder-by-key=" + holderByKey
                + ", holder-by-id=" + holderById + ", value-by-id=" + valueById
                + "). Old nerospace storage blocks will not load as nerolandcore blocks until they are replaced.";
    }
}
