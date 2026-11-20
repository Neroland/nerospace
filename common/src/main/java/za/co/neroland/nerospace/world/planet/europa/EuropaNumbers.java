package za.co.neroland.nerospace.world.planet.europa;

/**
 * Europa's fixed numbers. The worldgen constants are the exact Y intervals the density functions produce (three
 * clamped linear ramps plus clamped 2D noise); {@code EuropaDensityTest} proves the JSON against them.
 */
public final class EuropaNumbers {

    // --- world box -------------------------------------------------------------------------------------
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    /** Open cells below this are water (the top water block is at 87). */
    public static final int SEA_LEVEL = 88;

    // --- layer boundaries (inclusive ranges the boundary can fall in) ----------------------------------
    /** Sea floor: always solid at or below {@code FLOOR_MIN - 1}, always open at or above {@code FLOOR_MAX}. */
    public static final int FLOOR_MIN = -44;
    public static final int FLOOR_MAX = -20;
    /** Shell underside: always open at or below {@code ROOF_MIN}, always solid at or above {@code ROOF_MAX + 1}. */
    public static final int ROOF_MIN = 84;
    public static final int ROOF_MAX = 100;
    /** Shell top: always solid at or below {@code SURFACE_MIN - 1}, always air at or above {@code SURFACE_MAX}. */
    public static final int SURFACE_MIN = 176;
    public static final int SURFACE_MAX = 212;

    // --- biome bands (by the router's depth = (192 - y) / 128) ------------------------------------------
    /** Surface biomes at and above this Y. */
    public static final int BIOME_SURFACE_Y = 168;
    /** Ocean biomes below this Y; the shell biome sits between it and {@link #BIOME_SURFACE_Y}. */
    public static final int BIOME_OCEAN_Y = 80;
    /** The vent-field biome at and below this Y. */
    public static final int BIOME_VENT_Y = -8;
    /** Y at which the depth ramp is zero. */
    public static final int DEPTH_ZERO_Y = 192;
    /** Blocks per unit of depth. */
    public static final double DEPTH_SPAN = 128.0;

    // --- biome table thresholds shared with the density functions ---------------------------------------
    /** Erosion at and above which the surface is chaos terrain. */
    public static final double CHAOS_EROSION = 0.35;
    /** Half-width of the lineae band in the ridges noise. */
    public static final double LINEAE_HALF_WIDTH = 0.05;

    // --- blocks ----------------------------------------------------------------------------------------
    /** Walk-speed factor on Cut Europa Ice (vanilla default is 1.0). */
    public static final float CUT_ICE_SPEED = 1.15F;

    // --- tech chain ------------------------------------------------------------------------------------
    /** Burn time of one Deuterium Cell in a Combustion Generator, in ticks. */
    public static final int DEUTERIUM_CELL_BURN_TICKS = 8_000;
    /** Oxygen one Oxidant Crystal gives in a Fuel Refinery, in mB. */
    public static final int OXIDANT_OXYGEN_MB = 400;

    private EuropaNumbers() {
    }

    /** The router's depth value at a Y level. */
    public static double depthAt(int y) {
        return (DEPTH_ZERO_Y - y) / DEPTH_SPAN;
    }
}
