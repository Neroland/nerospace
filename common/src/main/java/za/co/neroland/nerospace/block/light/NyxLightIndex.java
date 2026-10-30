package za.co.neroland.nerospace.block.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Per-light-engine index of light-absorbing blocks (Nyx Stone). It keeps, per 16³ section, a
 * byte per cell counting the absorbers within its 3×3×3 neighbourhood, so the hot path ("is this cell within 1 block
 * of a Nyx Stone?") is one hash lookup plus one array read — and a single field read when the index is empty.
 *
 * <p>One instance belongs to one light engine and is only touched from that engine's thread (the server light
 * worker, or the client main thread), so it is not synchronised. Plain Java (fastutil only) so it is unit-tested.</p>
 */
public final class NyxLightIndex {

    private static final int SECTION_CELLS = 4096;

    /** Absorber positions (packed), so add/remove are idempotent. */
    private final LongOpenHashSet absorbers = new LongOpenHashSet();
    /** section key → neighbourhood counts (index = (y&15)<<8 | (z&15)<<4 | (x&15)). */
    private final Long2ObjectOpenHashMap<byte[]> near = new Long2ObjectOpenHashMap<>();

    private long lastKey = Long.MIN_VALUE;
    private byte[] lastArray;

    public boolean isEmpty() {
        return this.absorbers.isEmpty();
    }

    public int size() {
        return this.absorbers.size();
    }

    /** 26-bit x/z, 12-bit y — matches the vanilla block range without depending on game classes. */
    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static int unpackX(long p) {
        return (int) (p >> 38);
    }

    static int unpackY(long p) {
        return (int) (p << 52 >> 52);
    }

    static int unpackZ(long p) {
        return (int) (p << 26 >> 38);
    }

    static long sectionKey(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sz & 0x3FFFFF) << 20) | (sy & 0xFFFFF);
    }

    private static int cell(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    /** Registers an absorber; returns true when it was not already indexed. */
    public boolean add(int x, int y, int z) {
        if (!this.absorbers.add(pack(x, y, z))) {
            return false;
        }
        bump(x, y, z, 1);
        return true;
    }

    /** Removes an absorber; returns true when it was indexed. */
    public boolean remove(int x, int y, int z) {
        if (!this.absorbers.remove(pack(x, y, z))) {
            return false;
        }
        bump(x, y, z, -1);
        return true;
    }

    public boolean contains(int x, int y, int z) {
        return this.absorbers.contains(pack(x, y, z));
    }

    private void bump(int x, int y, int z, int delta) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = x + dx;
                    int cy = y + dy;
                    int cz = z + dz;
                    long key = sectionKey(cx >> 4, cy >> 4, cz >> 4);
                    byte[] arr = this.near.get(key);
                    if (arr == null) {
                        if (delta < 0) {
                            continue;
                        }
                        arr = new byte[SECTION_CELLS];
                        this.near.put(key, arr);
                    }
                    int i = cell(cx, cy, cz);
                    arr[i] = (byte) Math.max(0, arr[i] + delta);
                }
            }
        }
        this.lastKey = Long.MIN_VALUE;
        this.lastArray = null;
    }

    /** Whether a cell lies within 1 block (3×3×3) of an indexed absorber. */
    public boolean near(int x, int y, int z) {
        if (this.absorbers.isEmpty()) {
            return false;
        }
        long key = sectionKey(x >> 4, y >> 4, z >> 4);
        byte[] arr;
        if (key == this.lastKey) {
            arr = this.lastArray;
        } else {
            arr = this.near.get(key);
            this.lastKey = key;
            this.lastArray = arr;
        }
        return arr != null && arr[cell(x, y, z)] > 0;
    }

    /** Drops every absorber in a chunk column (chunk unloaded / light disabled). */
    public void clearChunk(int chunkX, int chunkZ) {
        if (this.absorbers.isEmpty()) {
            return;
        }
        LongOpenHashSet drop = null;
        for (LongIterator it = this.absorbers.iterator(); it.hasNext();) {
            long p = it.nextLong();
            if ((unpackX(p) >> 4) == chunkX && (unpackZ(p) >> 4) == chunkZ) {
                if (drop == null) {
                    drop = new LongOpenHashSet();
                }
                drop.add(p);
            }
        }
        if (drop != null) {
            for (LongIterator it = drop.iterator(); it.hasNext();) {
                long p = it.nextLong();
                remove(unpackX(p), unpackY(p), unpackZ(p));
            }
        }
        // Remove now-empty count arrays so memory follows the absorbers.
        this.near.long2ObjectEntrySet().removeIf(e -> isZero(e.getValue()));
        this.lastKey = Long.MIN_VALUE;
        this.lastArray = null;
    }

    private static boolean isZero(byte[] arr) {
        for (byte b : arr) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /** Count arrays held (for tests / the dev benchmark report). */
    public int sectionsHeld() {
        return this.near.size();
    }
}
