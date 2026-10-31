package za.co.neroland.nerospace.block.rules;

/**
 * Smoke Crystal: nothing moving on it makes a step or fall sound (both are vanilla's {@code intentionally_empty}
 * event; break and place sound as normal), and it is in {@code #minecraft:occludes_vibration_signals} and
 * {@code #minecraft:dampens_vibrations}. Light 1.
 */
public final class HushRules {

    /** The silent sound event used for step and fall (present on 26.1.2, 26.2 and 26.3). */
    public static final String SILENT_EVENT = "minecraft:intentionally_empty";
    public static final int LIGHT = 1;
    public static final String[] VIBRATION_TAGS = {"minecraft:occludes_vibration_signals",
            "minecraft:dampens_vibrations"};

    private HushRules() {
    }

    /** Which sound a movement plays: steps and falls are silent, breaking and placing are not. */
    public static boolean silent(boolean stepOrFall) {
        return stepOrFall;
    }
}
