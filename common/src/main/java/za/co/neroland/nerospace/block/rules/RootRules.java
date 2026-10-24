package za.co.neroland.nerospace.block.rules;

/** Xertz Soil (002 §1.5): the only substrate for Xertz crops and Spore Moss. */
public final class RootRules {

    private RootRules() {
    }

    public static boolean canRoot(boolean onXertzSoil) {
        return onXertzSoil;
    }
}
