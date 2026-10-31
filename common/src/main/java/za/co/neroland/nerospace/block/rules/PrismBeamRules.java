package za.co.neroland.nerospace.block.rules;

/**
 * Nyx Prism: idle light 6, 12 while a flare runs. A flaring prism casts a 1x1x6 beam toward +X (away from the pinned
 * sun); an exposed position inside the beam takes the flare's radiation bump as +2 instead of +1. The flare flip
 * itself runs through the shared weather-flip scan, capped like Dawnband Stone (256 per chunk, lowered by config).
 */
public final class PrismBeamRules {

    public static final int RANGE = 6;
    public static final int LIGHT_IDLE = 6;
    public static final int LIGHT_FLARING = 12;
    /** Client ray particles per animate tick while flaring. */
    public static final int RAY_PARTICLES = 2;

    private PrismBeamRules() {
    }

    /** Offset from the prism to the position: inside the beam when dx is 1..6 and dy, dz are 0. */
    public static boolean inBeam(int dx, int dy, int dz) {
        return dx >= 1 && dx <= RANGE && dy == 0 && dz == 0;
    }

    /** The flare's radiation bump: 0 outside a flare, 1 normally, 2 inside a flaring prism's beam. */
    public static int flareRadiation(boolean flaring, boolean inBeam) {
        if (!flaring) {
            return 0;
        }
        return inBeam ? 2 : 1;
    }

    public static int light(boolean flaring) {
        return flaring ? LIGHT_FLARING : LIGHT_IDLE;
    }
}
