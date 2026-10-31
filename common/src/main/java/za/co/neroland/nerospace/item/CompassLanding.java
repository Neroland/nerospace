package za.co.neroland.nerospace.item;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/**
 * Where a destination compass sets its holder down, as pure arithmetic: on Nyxara the column is pulled into the
 * twilight band (clear of the ragged terminator), and a column with no ground gets a platform instead of a fall.
 */
public final class CompassLanding {

    /** Y of the platform laid under the holder when the column has no ground (the station, or a void column). */
    public static final int PLATFORM_Y = 64;
    /** Extra blocks kept between the holder and the band's ragged edge. */
    static final int EDGE_CLEARANCE = 16;

    private CompassLanding() {
    }

    /**
     * {@code x} moved into Nyxara's twilight band ({@code width} full width centred on {@code originX}), at least
     * the edge noise plus {@link #EDGE_CLEARANCE} inside each terminator; unchanged when it is already there.
     */
    public static int intoBand(int x, int width, int originX, int edgeNoise) {
        int half = FixedXBands.clampWidth(width) / 2;
        int noise = Math.max(0, Math.min(FixedXBands.MAX_EDGE_NOISE, edgeNoise));
        int inner = Math.max(0, half - noise - EDGE_CLEARANCE);
        long dx = (long) x - originX;
        long clamped = Math.max(-inner, Math.min(inner, dx));
        return (int) (originX + clamped);
    }

    /**
     * The feet Y above a column whose heightmap top (first free block) is {@code top}, or {@code -1} when the
     * column has no ground at all ({@code top} at or below the world's floor) and a platform is needed.
     */
    public static int feetY(int top, int minBuildY) {
        return top > minBuildY ? top + 1 : -1;
    }
}
