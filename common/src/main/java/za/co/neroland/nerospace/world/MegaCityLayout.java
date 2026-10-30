package za.co.neroland.nerospace.world;

/**
 * Mega City geometry, kept apart from the feature class so tests can check it without a game bootstrap.
 *
 * <p>1.6.0 centred the 41×41 city on the {@code in_square} origin, which can sit anywhere in its chunk, so up to 20
 * blocks spilled past the 3×3-chunk region vanilla lets a feature write; those blocks were dropped with a
 * "Detected setBlock in a far chunk" error. 2.0 centres the city on its chunk's centre: the footprint then spans
 * -12..+28 from the chunk's minimum corner, always inside the writable -16..+31. New chunks only; generated cities
 * are untouched.</p>
 */
public final class MegaCityLayout {

    /** Curtain-wall half-size (41×41). The gate lamps sit on the wall line, so this is also the footprint. */
    public static final int WALL_R = 20;
    /** Corner towers stand at ± this on both axes. */
    public static final int CORNER_OFFSET = 11;
    /** Corner tower half-size (its podium is one wider). */
    public static final int CORNER_R = 3;
    /** Central keep half-size (its podium is one wider). */
    public static final int KEEP_R = 5;

    private MegaCityLayout() {
    }

    /** The city's centre coordinate (x or z) for a feature origin coordinate: the centre of the origin's chunk. */
    public static int centreFor(int originCoord) {
        return StructureRegion.chunkCentre(originCoord);
    }

    /** Half-size of everything the city writes, measured from its centre. */
    public static int footprintRadius() {
        int corner = CORNER_OFFSET + CORNER_R + 1;
        int keep = KEEP_R + 1;
        return Math.max(WALL_R, Math.max(corner, keep));
    }
}
