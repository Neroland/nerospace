package za.co.neroland.nerospace.block.rules;

/** Permafrost (002 §1.4): immune to the Hydration Module; the Terraformer must reach stage 3. */
public final class ThawRules {

    public static final int MIN_TERRAFORM_STAGE = 3;

    private ThawRules() {
    }

    public static boolean canThaw(boolean viaHydration, int terraformStage) {
        return !viaHydration && terraformStage >= MIN_TERRAFORM_STAGE;
    }
}
