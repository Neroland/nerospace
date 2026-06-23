package za.co.neroland.nerospace.client;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import za.co.neroland.nerospace.network.OxygenFieldSyncPayload;

/**
 * Client-side mirror of the nearby oxygen field (terraform design §1.7). Fed by
 * {@link OxygenFieldSyncPayload} (the clientbound handler registered in {@code ModNetwork.init()});
 * read by {@link ClientOxygenVisuals}. A plain data holder (no client-only imports) so it is safe to
 * reference from common network code.
 */
public final class ClientOxygenField {

    private static volatile Long2ByteOpenHashMap field = newMap();

    private ClientOxygenField() {
    }

    private static Long2ByteOpenHashMap newMap() {
        Long2ByteOpenHashMap m = new Long2ByteOpenHashMap();
        m.defaultReturnValue((byte) 0);
        return m;
    }

    public static void accept(OxygenFieldSyncPayload payload) {
        Long2ByteOpenHashMap m = newMap();
        Long2ByteMap incoming = payload.toMap();
        for (Long2ByteMap.Entry e : incoming.long2ByteEntrySet()) {
            m.put(e.getLongKey(), e.getByteValue());
        }
        field = m;
    }

    public static void clear() {
        field = newMap();
    }

    /** @return concentration {@code 0..MAX} at {@code pos} (0 if unknown). */
    public static int concentrationAt(BlockPos pos) {
        return field.get(pos.asLong()) & 0xFF;
    }

    public static boolean isEmpty() {
        return field.isEmpty();
    }

    public static Long2ByteMap view() {
        return field;
    }

    /** @return true if {@code pos} is breathable but borders a vacuum cell (a "membrane" boundary). */
    public static boolean isMembrane(BlockPos pos, int threshold) {
        if (concentrationAt(pos) < threshold) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            if (concentrationAt(pos.relative(dir)) < threshold) {
                return true;
            }
        }
        return false;
    }
}
