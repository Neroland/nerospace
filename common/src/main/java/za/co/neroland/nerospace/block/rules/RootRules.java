package za.co.neroland.nerospace.block.rules;

/** Xertz Soil: the only substrate for Xertz crops and Spore Moss. */
public final class RootRules {

    private RootRules() {
    }

    public static boolean canRoot(boolean onXertzSoil) {
        return onXertzSoil;
    }
}
