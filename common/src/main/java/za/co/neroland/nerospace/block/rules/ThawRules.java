package za.co.neroland.nerospace.block.rules;

/** Permafrost (002 §1.4): immune to the Hydration Module; the Terraformer must reach stage 3. */
public final class ThawRules {

    public static final int MIN_TERRAFORM_STAGE = 3;

    private ThawRules() {
    }

    /** How deep below the surface the stage-3 Terraformer thaws Permafrost. */
    public static final int THAW_DEPTH = 4;

    /** What a cell becomes at the stage-3 thaw. */
    public enum Thaw { NONE, GRASS, DIRT }

    public static boolean canThaw(boolean viaHydration, int terraformStage) {
        return !viaHydration && terraformStage >= MIN_TERRAFORM_STAGE;
    }

    /**
     * Stage-2 hydration on a hydration-immune basin floor (Permafrost): the stage still advances but no meltwater is
     * placed. Any other floor fills exactly as before.
     */
    public static boolean hydrationSkipsFill(boolean floorHydrationImmune) {
        return floorHydrationImmune && !canThaw(true, MIN_TERRAFORM_STAGE);
    }

    /**
     * Stage-3 thaw of the cell {@code depth} blocks below the surface (1 = the top ground block): Permafrost becomes
     * grass on top and dirt below, down to {@link #THAW_DEPTH}. Every other block is left alone ({@link Thaw#NONE}).
     */
    public static Thaw thawAt(boolean hydrationImmune, int depth, int terraformStage) {
        if (!hydrationImmune || depth < 1 || depth > THAW_DEPTH || !canThaw(false, terraformStage)) {
            return Thaw.NONE;
        }
        return depth == 1 ? Thaw.GRASS : Thaw.DIRT;
    }
}
