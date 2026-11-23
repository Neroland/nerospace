package za.co.neroland.nerospace.suit;

/**
 * Registry ids for suit modules, weaves and linings. Strings, not holders, so the rules and the saved loadout
 * can name an id this build does not register (it stays inert).
 */
public final class SuitIds {

    public static final String THERMAL_WEAVE = "nerospace:thermal_weave";
    public static final String CRYO_WEAVE = "nerospace:cryo_weave";
    public static final String ADAPTIVE_WEAVE = "nerospace:adaptive_weave";
    public static final String RAD_LINING = "nerospace:rad_lining";
    /** Reserved for Milestone C. Not registered as an item in this release. */
    public static final String PRESSURE_LINING = "nerospace:pressure_lining";

    public static final String LIFE_SUPPORT_TANK = "nerospace:life_support_tank";
    public static final String O2_RECLAIMER = "nerospace:o2_reclaimer";
    public static final String POWER_CELL = "nerospace:power_cell";
    public static final String SOLAR_SKIN = "nerospace:solar_skin";
    public static final String KINETIC_SHIELD = "nerospace:kinetic_shield";
    public static final String JUMP_JET = "nerospace:jump_jet";
    public static final String FLIGHT_HARNESS = "nerospace:flight_harness";
    /** Milestone B. Registered as data only; no effect in this release. */
    public static final String EVA_THRUSTERS = "nerospace:eva_thrusters";
    /** Inert hook. Registered, no recipe, hidden from the creative tab. */
    public static final String HARDPOINT = "nerospace:hardpoint";
    /** Milestone B. Data only. */
    public static final String TOOL_LINK = "nerospace:tool_link";
    public static final String NIGHT_VISOR = "nerospace:night_visor";
    public static final String SURVEY_SCANNER = "nerospace:survey_scanner";
    public static final String SPORE_FILTER = "nerospace:spore_filter";
    /** Milestone A. Data only until satellite forecasts exist. */
    public static final String UPLINK_VISOR = "nerospace:uplink_visor";
    public static final String STRIDE_SERVOS = "nerospace:stride_servos";
    public static final String LEAP_SERVOS = "nerospace:leap_servos";
    public static final String SHOCK_ABSORBERS = "nerospace:shock_absorbers";
    /** Milestone B. Data only. */
    public static final String MAG_CLAMPS = "nerospace:mag_clamps";
    public static final String GLIDE_FINS = "nerospace:glide_fins";

    public static final String PLATING_MK2 = "nerospace:frame_plating_mk2";
    public static final String PLATING_MK3 = "nerospace:frame_plating_mk3";
    public static final String PLATING_MK4 = "nerospace:frame_plating_mk4";

    private SuitIds() {
    }

    public static boolean isWeave(String id) {
        return THERMAL_WEAVE.equals(id) || CRYO_WEAVE.equals(id) || ADAPTIVE_WEAVE.equals(id);
    }
}
