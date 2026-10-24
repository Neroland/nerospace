package za.co.neroland.nerospace.block.rules;

/** Magma Glass (002 §1.3): the only transparent block lava cannot replace. */
public final class LavaImmuneRules {

    private LavaImmuneRules() {
    }

    /** Whether a fluid may replace the block: never lava. Magma Glass is not replaceable by any fluid either. */
    public static boolean replaceable(boolean isLava) {
        return false;
    }
}
