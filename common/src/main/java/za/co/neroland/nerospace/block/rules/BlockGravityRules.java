package za.co.neroland.nerospace.block.rules;

import java.util.List;

/**
 * Block-level gravity overrides (002 §1.6/§1.7, §10.4). A Gravity Shard sets −0.15 over the 3×5×3 above it; a
 * Gravity Plate sets 1.0 over the 1×6×1 above it. Where several cover a cell: the highest-priority kind wins
 * (PLATE beats SHARD), then the lowest factor. No override in range → the dimension factor.
 */
public final class BlockGravityRules {

    public static final double SHARD_FACTOR = -0.15;
    public static final double PLATE_FACTOR = 1.0;
    /** Overrides per chunk (config may lower). */
    public static final int PER_CHUNK_CAP = 64;

    private BlockGravityRules() {
    }

    /** Override kinds in ascending priority: a later constant beats an earlier one. Persisted by name. */
    public enum Kind {
        SHARD(1, 5, 1, SHARD_FACTOR),
        PLATE(0, 6, 0, PLATE_FACTOR);

        private final int halfWidth;
        private final int height;
        private final int halfDepth;
        private final double factor;

        Kind(int halfWidth, int height, int halfDepth, double factor) {
            this.halfWidth = halfWidth;
            this.height = height;
            this.halfDepth = halfDepth;
            this.factor = factor;
        }

        public double factor() {
            return this.factor;
        }

        public int height() {
            return this.height;
        }

        public int halfWidth() {
            return this.halfWidth;
        }

        public static Kind byName(String name) {
            for (Kind k : values()) {
                if (k.name().equals(name)) {
                    return k;
                }
            }
            return null;
        }
    }

    /** One override that covers the queried cell. */
    public record Override(Kind kind, double factor) {
    }

    /** Whether the offset (cell − source block) lies in the kind's column above the block. */
    public static boolean inColumn(int dx, int dy, int dz, Kind kind) {
        return dy >= 1 && dy <= kind.height
                && Math.abs(dx) <= kind.halfWidth && Math.abs(dz) <= kind.halfDepth;
    }

    /** Shard-shaped column (3×5×3): kept for the matrix's 3-arg signature. */
    public static boolean inColumn(int dx, int dy, int dz) {
        return inColumn(dx, dy, dz, Kind.SHARD);
    }

    public static double factor(double dimensionFactor, List<Override> overridesInRange) {
        if (overridesInRange == null || overridesInRange.isEmpty()) {
            return dimensionFactor;
        }
        Override best = overridesInRange.get(0);
        for (Override o : overridesInRange) {
            if (o.kind().ordinal() > best.kind().ordinal()
                    || (o.kind() == best.kind() && o.factor() < best.factor())) {
                best = o;
            }
        }
        return best.factor();
    }

    /** Whether another override may be registered in a chunk already holding {@code count}. */
    public static boolean canRegister(int count, int configuredCap) {
        return count < Caps.lower(PER_CHUNK_CAP, configuredCap);
    }
}
