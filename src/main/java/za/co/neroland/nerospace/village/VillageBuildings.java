package za.co.neroland.nerospace.village;

import java.util.ArrayList;
import java.util.List;

/**
 * The Village Core's building catalogue (ALIEN_VILLAGERS_DESIGN.md §4) for Phase 4. Buildings are
 * taught in order; each gates on a reputation tier and a nerosteel cost, and is generated as a simple
 * box structure (walls + roof + a door gap + an interior light) the core raises block-by-block.
 *
 * <p>Phase 5 turns these into <i>functional</i> buildings (farms, workshops, labs…) with block
 * entities and per-profession trade unlocks; Phase 4 proves the teach-and-grow loop with the shells.
 */
public final class VillageBuildings {

    public enum Kind { WALL, ROOF, LIGHT, AIR }

    public record Placement(int dx, int dy, int dz, Kind kind) {
    }

    /** A teachable building: required reputation tier, nerosteel cost, footprint size and wall height. */
    public enum Type {
        HUT(2, 32, 5, 4),
        WORKSHOP(3, 48, 7, 5);

        public final int reqTier;
        public final int cost;
        public final int size;
        public final int height;

        Type(int reqTier, int cost, int size, int height) {
            this.reqTier = reqTier;
            this.cost = cost;
            this.size = size;
            this.height = height;
        }

        public static Type byOrdinalOrNull(int i) {
            Type[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : null;
        }
    }

    /** Buildings are constructed in this order as the village grows. */
    public static Type[] order() {
        return new Type[] {Type.HUT, Type.WORKSHOP};
    }

    private VillageBuildings() {
    }

    /**
     * The ordered block placements (relative to the plot origin) for a building, bottom layer first so
     * it visibly rises. WALL/ROOF map to nerosteel, LIGHT to a glow source, AIR clears headroom.
     */
    public static List<Placement> build(Type t) {
        List<Placement> out = new ArrayList<>();
        int r = t.size / 2;
        for (int dy = 0; dy <= t.height; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean perimeter = Math.abs(dx) == r || Math.abs(dz) == r;
                    if (dy == t.height) {
                        out.add(new Placement(dx, dy, dz, Kind.ROOF));
                    } else if (perimeter) {
                        boolean door = dz == -r && dx == 0 && dy <= 1;
                        if (!door) {
                            out.add(new Placement(dx, dy, dz, Kind.WALL));
                        }
                    } else if (dy == 0 && dx == 0 && dz == 0) {
                        out.add(new Placement(0, 0, 0, Kind.LIGHT));
                    } else {
                        out.add(new Placement(dx, dy, dz, Kind.AIR));
                    }
                }
            }
        }
        return out;
    }
}
