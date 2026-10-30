package za.co.neroland.nerospace.world.planet;

/**
 * Why a terraformer will not run where it stands. Synced to the Terraformer and Terraform Monitor
 * screens by ordinal; append only. Lang key: {@code gui.nerospace.terraform.refused.<lowercase name>}.
 */
public enum TerraformRefusal {
    NONE,
    /** The station (or any world without a table): there is no ground to terraform. */
    NO_SURFACE,
    /** Nyxara: the twilight grove only roots inside the twilight band. */
    OUT_OF_BAND;

    /** Pure rule: no table → no surface; a band-only table outside the band → out of band; else none. */
    public static TerraformRefusal evaluate(boolean hasTable, boolean bandOnly, boolean inBand) {
        if (!hasTable) {
            return NO_SURFACE;
        }
        if (bandOnly && !inBand) {
            return OUT_OF_BAND;
        }
        return NONE;
    }

    public String langKey() {
        return "gui.nerospace.terraform.refused." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static TerraformRefusal byOrdinal(int ordinal) {
        TerraformRefusal[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
