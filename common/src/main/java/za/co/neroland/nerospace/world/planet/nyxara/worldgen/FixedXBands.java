package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

/**
 * Pure maths of Nyxara's fixed-X biome source ({@code width} is the full band width): the planet is split by block
 * X alone into a sunward dayside (west, −X), a habitable twilight band centred on {@code originX}, and a nightside
 * (east, +X). With no edge noise: {@code x − originX < −w/2} is {@link Side#WEST}, {@code |x − originX| ≤ w/2} is
 * {@link Side#BAND}, {@code x − originX > w/2} is {@link Side#EAST}. The edge noise moves each edge by a deterministic
 * 2-D value noise of amplitude {@code edgeNoise} blocks so the terminator is ragged rather than a straight wall; beyond
 * {@code w/2 + 4·edgeNoise} the side is returned without any noise lookup. No wrap and no second band. Arithmetic is
 * done in {@code long}/{@code double}, so ±30 000 000 is safe. No game classes.
 */
public final class FixedXBands {

    public enum Side { WEST, BAND, EAST }

    /** Config range and default of {@code nyxara.twilightBandWidth} (the full band width). */
    public static final int MIN_WIDTH = 512;
    public static final int MAX_WIDTH = 16384;
    public static final int DEFAULT_WIDTH = 2048;
    /** Default edge raggedness (blocks) and its codec range. */
    public static final int DEFAULT_EDGE_NOISE = 96;
    public static final int MAX_EDGE_NOISE = 256;
    /** Lattice spacing (blocks) of the edge noise. */
    static final int LATTICE = 48;

    private static final long SALT_WEST = 0x5745535444415953L;
    private static final long SALT_EAST = 0x454153544E494748L;

    private FixedXBands() {
    }

    public static int clampWidth(int width) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }

    /** The side at block ({@code x}, {@code z}); {@code width} is clamped to the config range. Unseeded edges. */
    public static Side sideAt(int x, int z, int width, int originX, int edgeNoise) {
        return sideAt(x, z, width, originX, edgeNoise, 0L);
    }

    /**
     * {@link #sideAt(int, int, int, int, int)} with the terminator's raggedness mixed with a world seed, so every world
     * gets its own edge shape. {@code seed = 0} is the unseeded (test) form. Only the edge offset depends on the seed:
     * beyond {@code w/2 + 4·edgeNoise} the side never changes.
     */
    public static Side sideAt(int x, int z, int width, int originX, int edgeNoise, long seed) {
        double half = clampWidth(width) / 2.0;
        int noise = Math.max(0, Math.min(MAX_EDGE_NOISE, edgeNoise));
        double dx = (double) ((long) x - (long) originX);
        double far = half + 4.0 * noise;
        if (dx < -far) {
            return Side.WEST;
        }
        if (dx > far) {
            return Side.EAST;
        }
        double west = -half + noise * edgeOffset(x, z, salt(SALT_WEST, seed));
        double east = half + noise * edgeOffset(x, z, salt(SALT_EAST, seed));
        if (dx < west) {
            return Side.WEST;
        }
        if (dx > east) {
            return Side.EAST;
        }
        return Side.BAND;
    }

    /** Mixes a world seed into an edge salt; {@code seed = 0} leaves the salt unchanged. Package-private for tests. */
    static long salt(long base, long seed) {
        if (seed == 0L) {
            return base;
        }
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        return base ^ h;
    }

    /**
     * Smooth value noise in [−1, 1] over a {@value #LATTICE}-block lattice (bilinear with a smoothstep), fixed per salt.
     * Package-private for tests.
     */
    static double edgeOffset(int x, int z, long salt) {
        int gx = Math.floorDiv(x, LATTICE);
        int gz = Math.floorDiv(z, LATTICE);
        double fx = smooth((x - (double) gx * LATTICE) / LATTICE);
        double fz = smooth((z - (double) gz * LATTICE) / LATTICE);
        double a = lattice(gx, gz, salt);
        double b = lattice(gx + 1, gz, salt);
        double c = lattice(gx, gz + 1, salt);
        double d = lattice(gx + 1, gz + 1, salt);
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /** Deterministic lattice value in [−1, 1]. */
    private static double lattice(int gx, int gz, long salt) {
        long h = salt ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }
}
