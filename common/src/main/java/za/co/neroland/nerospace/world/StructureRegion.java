package za.co.neroland.nerospace.world;

/**
 * The block area a world-gen feature may write: the 3×3 chunks centred on the chunk being decorated. Vanilla
 * drops (and logs "Detected setBlock in a far chunk") any write outside it, so the alien structures centre
 * themselves inside it and guard every write with {@link #contains(int, int)}.
 *
 * @param minX inclusive
 * @param minZ inclusive
 * @param maxX inclusive
 * @param maxZ inclusive
 */
public record StructureRegion(int minX, int minZ, int maxX, int maxZ) {

    /** The writable region for a feature whose origin is at block {@code (x, z)}. */
    public static StructureRegion around(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        return new StructureRegion((cx - 1) << 4, (cz - 1) << 4, ((cx + 1) << 4) + 15, ((cz + 1) << 4) + 15);
    }

    /** The centre block of the chunk that contains block coordinate {@code v} (x or z). */
    public static int chunkCentre(int v) {
        return ((v >> 4) << 4) + 8;
    }

    public boolean contains(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    /** Whether a square footprint of half-size {@code r} centred at {@code (x, z)} fits entirely. */
    public boolean containsSquare(int x, int z, int r) {
        return contains(x - r, z - r) && contains(x + r, z + r);
    }
}
