package za.co.neroland.nerospace.block.rules;

/** Martian Sandstone stonecutter strata (002 §1.2): exactly four rotation-aware variants. */
public final class StrataRules {

    public static final int COUNT = 4;

    private StrataRules() {
    }

    /** {@code index} 0..3 → {@code martian_sandstone_strata_1..4}; anything else throws. */
    public static String variantFor(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("strata index " + index);
        }
        return "martian_sandstone_strata_" + (index + 1);
    }
}
