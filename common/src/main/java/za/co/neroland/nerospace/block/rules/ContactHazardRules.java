package za.co.neroland.nerospace.block.rules;

/** Perchlorate Crust: Poison I 4 s on step without sealed boots. */
public final class ContactHazardRules {

    public static final int POISON_TICKS = 80;
    public static final int POISON_AMPLIFIER = 0;

    private ContactHazardRules() {
    }

    public static boolean poison(boolean livingEntity, boolean sealedBoots) {
        return livingEntity && !sealedBoots;
    }
}
