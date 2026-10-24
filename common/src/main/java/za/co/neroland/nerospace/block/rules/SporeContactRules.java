package za.co.neroland.nerospace.block.rules;

/** Spore Moss point nausea (002 §1.5, §2.1.1): Nausea I 5 s to the stepping entity without a sealed helmet. */
public final class SporeContactRules {

    public static final int MOSS_NAUSEA_TICKS = 100;

    private SporeContactRules() {
    }

    public static boolean nausea(boolean sealedHelmet) {
        return !sealedHelmet;
    }
}
