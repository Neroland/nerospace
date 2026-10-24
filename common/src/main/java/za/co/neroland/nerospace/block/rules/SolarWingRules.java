package za.co.neroland.nerospace.block.rules;

/**
 * Solar Wing (002 §1.7; 005 §7.2): {@code enabled && exposed ? round(basePeak × 2 × classMultiplier) : 0}, no
 * weather term, never stacks with Anorthosite.
 */
public final class SolarWingRules {

    public static final double WING_MULTIPLIER = 2.0;

    private SolarWingRules() {
    }

    public static int output(int basePeak, boolean exposed) {
        return output(basePeak, exposed, 1.0, true);
    }

    public static int output(int basePeak, boolean exposed, double classMultiplier, boolean enabled) {
        if (!enabled || !exposed || basePeak <= 0 || !(classMultiplier > 0.0)) {
            return 0;
        }
        return (int) Math.round(basePeak * WING_MULTIPLIER * classMultiplier);
    }
}
