package za.co.neroland.nerospace.block.rules;

/**
 * Capped spreading: random-tick spread onto a valid target, refused once the chunk holds {@code cap}.
 * {@code roll} is uniform in [0, oneIn); a {@code boostFactor} of k succeeds on rolls below k (so ×3 in a bloom).
 */
public final class SpreadRules {

    private SpreadRules() {
    }

    public static boolean canSpread(boolean onSubstrate, int countInChunk, int cap, int boostFactor, int roll) {
        if (!onSubstrate || countInChunk >= cap || roll < 0) {
            return false;
        }
        return roll < Math.max(1, boostFactor);
    }
}
