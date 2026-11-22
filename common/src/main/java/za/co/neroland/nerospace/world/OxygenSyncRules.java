package za.co.neroland.nerospace.world;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;

/**
 * When an oxygen-field snapshot is worth building or sending. A client that already has this view is not sent
 * another copy; the first view for a player (or a dimension change, which clears {@code known}) is always sent.
 */
public final class OxygenSyncRules {

    private OxygenSyncRules() {
    }

    /** Standing still on an unchanged field: the previous snapshot is still exact, so skip the walk. */
    public static boolean skipBuild(boolean known, int lastRevision, long lastPos, int revision, long pos) {
        return known && lastRevision == revision && lastPos == pos;
    }

    /** The walk ran, but the cells in range match what this player was already sent. */
    public static boolean skipSend(boolean known, long lastHash, long hash) {
        return known && lastHash == hash;
    }

    /**
     * Sum of per-cell hashes, so two maps with the same cells and values match whatever order they iterate.
     * A collision of two different views would skip one send; the cells are a 64-bit mix of position and value.
     */
    public static long fingerprint(Long2ByteMap field) {
        long h = 0x9E3779B97F4A7C15L ^ field.size();
        for (Long2ByteMap.Entry e : field.long2ByteEntrySet()) {
            long cell = e.getLongKey() ^ ((long) (e.getByteValue() & 0xFF) << 56);
            h += Long.rotateLeft(cell, 17) * 0xC2B2AE3D27D4EB4FL;
        }
        return h;
    }
}
