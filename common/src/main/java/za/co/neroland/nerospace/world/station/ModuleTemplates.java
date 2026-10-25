package za.co.neroland.nerospace.world.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * Pure module geometry: for a module kind, its per-kind index and the station centre
 * {@code C = (cx, PLATFORM_Y, cz)}, the exact cells the console places. No {@code Level}, no block states — a
 * {@link Cell} names what goes where and {@code StationModules} maps it to a block. The legacy room occupies
 * {@code x,z ∈ [−3, +3]} (walls {@code +1..+4}, roof {@code +5}), the airlock {@code x ∈ [+4, +7]} and the pad
 * {@code x ∈ [+8, +10]}; modules therefore grow west, north, south and up. Every module box is disjoint from every
 * other; doorways are cut deliberately into a neighbour's wall and are listed as {@link Cell#AIR} cells.
 */
public final class ModuleTemplates {

    /** What a template cell holds. {@code AIR} = a doorway cut (the cell is cleared, never filled). */
    public enum Cell {
        FLOOR, STATION_WALL, GLASS, LAMP, PLATING, WINDOW, TRUSS_X, TRUSS_Y, TRUSS_Z, SOLAR_WING, DOCK_RING, TRAY,
        PLATE, TELESCOPE, CORE, AIR
    }

    /** One template cell at an absolute position. */
    public record Placement(BlockPos pos, Cell cell) {
    }

    /** An inclusive axis-aligned box in absolute coordinates. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public boolean contains(BlockPos p) {
            return p.getX() >= this.minX && p.getX() <= this.maxX && p.getY() >= this.minY && p.getY() <= this.maxY
                    && p.getZ() >= this.minZ && p.getZ() <= this.maxZ;
        }

        public boolean intersects(Box o) {
            return this.minX <= o.maxX && this.maxX >= o.minX && this.minY <= o.maxY && this.maxY >= o.minY
                    && this.minZ <= o.maxZ && this.maxZ >= o.minZ;
        }

        static Box rel(BlockPos c, int x0, int y0, int z0, int x1, int y1, int z1) {
            return new Box(c.getX() + Math.min(x0, x1), c.getY() + Math.min(y0, y1), c.getZ() + Math.min(z0, z1),
                    c.getX() + Math.max(x0, x1), c.getY() + Math.max(y0, y1), c.getZ() + Math.max(z0, z1));
        }
    }

    /** Legacy room radius, wall height and airlock length (mirrors {@code StationStructure}). */
    public static final int ROOM_R = 3;
    public static final int WALL_H = 4;
    public static final int AIRLOCK_LEN = 4;

    private ModuleTemplates() {
    }

    /** The module's anchor block — its idempotence key. */
    public static BlockPos anchor(ModuleKind kind, int index, BlockPos c) {
        return switch (kind) {
            case CORE_LEGACY, CORE -> c;
            case DOCKING_RING -> c.offset(9, 0, 0);
            case SOLAR_WING -> c.offset(0, 2, index == 0 ? -4 : 4);
            case HABITAT_RING -> c.offset(-10, 0, 0);
            case HYDROPONICS_BAY -> c.offset(-10, 0, -8);
            case GRAVITY_DECK -> c.offset(-10, 0, 8);
            case OBSERVATORY -> c.offset(0, 6, 0);
        };
    }

    /** The air cell that receives the module's oxygen source: anchor + 1 up (Observatory: anchor). */
    public static BlockPos oxygenCell(ModuleKind kind, int index, BlockPos c) {
        BlockPos a = anchor(kind, index, c);
        return kind == ModuleKind.OBSERVATORY ? a : a.above();
    }

    /** The legacy room + airlock + pad box (x −3..+10, y 0..+5, z −3..+3). */
    public static Box legacyBox(BlockPos c) {
        return Box.rel(c, -ROOM_R, 0, -ROOM_R, ROOM_R + AIRLOCK_LEN + 3, WALL_H + 1, ROOM_R);
    }

    /** The module's bounding box. */
    public static Box box(ModuleKind kind, int index, BlockPos c) {
        int s = index == 0 ? -1 : 1;
        return switch (kind) {
            case CORE_LEGACY, CORE -> Box.rel(c, -ROOM_R, 0, -ROOM_R, ROOM_R + AIRLOCK_LEN, WALL_H + 1, ROOM_R);
            case DOCKING_RING -> Box.rel(c, 8, 0, -1, 10, 0, 1);
            case SOLAR_WING -> Box.rel(c, -1, 2, 4 * s, 1, 2, 11 * s);
            case HABITAT_RING -> Box.rel(c, -14, 0, -4, -4, 5, 4);
            case HYDROPONICS_BAY -> Box.rel(c, -13, 0, -11, -7, 3, -5);
            case GRAVITY_DECK -> Box.rel(c, -12, 0, 5, -8, 5, 10);
            case OBSERVATORY -> Box.rel(c, -2, 5, -2, 2, 9, 2);
        };
    }

    /** Every box Nerospace's own templates may occupy for this centre. */
    public static List<Box> union(BlockPos c) {
        List<Box> out = new ArrayList<>();
        out.add(legacyBox(c));
        for (ModuleKind k : ModuleKind.values()) {
            if (k == ModuleKind.CORE_LEGACY || k == ModuleKind.CORE) {
                continue;
            }
            for (int i = 0; i < k.maxCount(); i++) {
                out.add(box(k, i, c));
            }
        }
        return out;
    }

    /** Whether {@code p} lies in this centre's template union. */
    public static boolean inUnion(BlockPos c, BlockPos p) {
        for (Box b : union(c)) {
            if (b.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /** The module's cells. {@code CORE_LEGACY} has none (it is never built). */
    public static List<Placement> template(ModuleKind kind, int index, BlockPos c) {
        List<Placement> out = new ArrayList<>();
        switch (kind) {
            case CORE_LEGACY -> {
                return Collections.emptyList();
            }
            case CORE -> core(out, c);
            case DOCKING_RING -> {
                for (int dx = 8; dx <= 10; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        put(out, c, dx, 0, dz, Cell.DOCK_RING);
                    }
                }
            }
            case SOLAR_WING -> {
                int s = index == 0 ? -1 : 1;
                for (int k = 4; k <= 11; k++) {
                    put(out, c, 0, 2, k * s, Cell.TRUSS_Z);
                }
                for (int k = 5; k <= 10; k++) {
                    put(out, c, -1, 2, k * s, Cell.SOLAR_WING);
                    put(out, c, 1, 2, k * s, Cell.SOLAR_WING);
                }
            }
            case HABITAT_RING -> habitat(out, c);
            case HYDROPONICS_BAY -> hydroponics(out, c);
            case GRAVITY_DECK -> gravityDeck(out, c);
            case OBSERVATORY -> observatory(out, c);
        }
        return out;
    }

    private static void put(List<Placement> out, BlockPos c, int dx, int dy, int dz, Cell cell) {
        out.add(new Placement(c.offset(dx, dy, dz), cell));
    }

    /** The 1.x room + airlock (the 3×3 pad is not part of {@code CORE}; new stations get a Docking Ring). */
    private static void core(List<Placement> out, BlockPos c) {
        int r = ROOM_R;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                put(out, c, dx, 0, dz, dx == 0 && dz == 0 ? Cell.CORE : Cell.FLOOR);
                boolean light = (dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2);
                put(out, c, dx, WALL_H + 1, dz, light ? Cell.LAMP : Cell.FLOOR);
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                if (!edge) {
                    continue;
                }
                boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                for (int h = 1; h <= WALL_H; h++) {
                    boolean door = dx == r && dz == 0 && h <= 2;
                    put(out, c, dx, h, dz, door ? Cell.AIR
                            : (corner || h == 1 || h == WALL_H) ? Cell.STATION_WALL : Cell.GLASS);
                }
            }
        }
        for (int dx = r + 1; dx <= r + AIRLOCK_LEN; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put(out, c, dx, 0, dz, Cell.FLOOR);
                put(out, c, dx, 3, dz, Cell.FLOOR);
            }
            put(out, c, dx, 1, -1, Cell.STATION_WALL);
            put(out, c, dx, 1, 1, Cell.STATION_WALL);
            put(out, c, dx, 2, -1, Cell.GLASS);
            put(out, c, dx, 2, 1, Cell.GLASS);
        }
    }

    private static void habitat(List<Placement> out, BlockPos c) {
        // Doorway cut in the legacy room's west wall.
        put(out, c, -3, 1, 0, Cell.AIR);
        put(out, c, -3, 2, 0, Cell.AIR);
        // Corridor x −4..−5.
        for (int dx = -5; dx <= -4; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put(out, c, dx, 0, dz, Cell.FLOOR);
                put(out, c, dx, 3, dz, Cell.PLATING);
            }
            put(out, c, dx, 1, -1, Cell.PLATING);
            put(out, c, dx, 1, 1, Cell.PLATING);
            put(out, c, dx, 2, -1, Cell.WINDOW);
            put(out, c, dx, 2, 1, Cell.WINDOW);
        }
        // Room x −14..−6, z −4..+4.
        for (int dx = -14; dx <= -6; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                put(out, c, dx, 0, dz, Cell.FLOOR);
                boolean lamp = (dx == -10 && dz == 0) || (Math.abs(dx + 10) == 2 && Math.abs(dz) == 2);
                put(out, c, dx, 5, dz, lamp ? Cell.LAMP : Cell.PLATING);
                boolean edge = dx == -14 || dx == -6 || Math.abs(dz) == 4;
                if (!edge) {
                    continue;
                }
                boolean corner = (dx == -14 || dx == -6) && Math.abs(dz) == 4;
                for (int h = 1; h <= 4; h++) {
                    Cell cell = (corner || h == 1 || h == 4) ? Cell.PLATING : Cell.WINDOW;
                    if (dx == -6 && dz == 0 && h <= 2) {
                        cell = Cell.AIR; // east doorway to the corridor
                    } else if (dx == -10 && Math.abs(dz) == 4 && h <= 2) {
                        cell = Cell.PLATING; // closed doorways the dependent modules cut
                    }
                    put(out, c, dx, h, dz, cell);
                }
            }
        }
    }

    private static void connector(List<Placement> out, BlockPos c, int dz) {
        put(out, c, -10, 0, dz, Cell.FLOOR);
        put(out, c, -10, 3, dz, Cell.PLATING);
        for (int h = 1; h <= 2; h++) {
            put(out, c, -11, h, dz, Cell.PLATING);
            put(out, c, -9, h, dz, Cell.PLATING);
        }
    }

    private static void hydroponics(List<Placement> out, BlockPos c) {
        put(out, c, -10, 1, -4, Cell.AIR);
        put(out, c, -10, 2, -4, Cell.AIR);
        connector(out, c, -5);
        for (int dx = -13; dx <= -8; dx++) {
            for (int dz = -11; dz <= -6; dz++) {
                boolean tray = dx >= -12 && dx <= -9 && dz >= -10 && dz <= -8;
                put(out, c, dx, 0, dz, tray ? Cell.TRAY : Cell.FLOOR);
                put(out, c, dx, 3, dz, Cell.WINDOW);
                boolean edge = dx == -13 || dx == -8 || dz == -11 || dz == -6;
                if (!edge) {
                    continue;
                }
                for (int h = 1; h <= 2; h++) {
                    Cell cell = h == 1 ? Cell.PLATING : Cell.WINDOW;
                    if (dx == -10 && dz == -6) {
                        cell = Cell.AIR; // doorway from the connector
                    }
                    put(out, c, dx, h, dz, cell);
                }
            }
        }
    }

    private static void gravityDeck(List<Placement> out, BlockPos c) {
        put(out, c, -10, 1, 4, Cell.AIR);
        put(out, c, -10, 2, 4, Cell.AIR);
        connector(out, c, 5);
        for (int dx = -12; dx <= -8; dx++) {
            for (int dz = 6; dz <= 10; dz++) {
                boolean plate = dx >= -11 && dx <= -9 && dz >= 7 && dz <= 9;
                put(out, c, dx, 0, dz, plate ? Cell.PLATE : Cell.FLOOR);
                put(out, c, dx, 5, dz, dx == -10 && dz == 8 ? Cell.LAMP : Cell.PLATING);
                boolean edge = dx == -12 || dx == -8 || dz == 6 || dz == 10;
                if (!edge) {
                    continue;
                }
                boolean corner = (dx == -12 || dx == -8) && (dz == 6 || dz == 10);
                for (int h = 1; h <= 4; h++) {
                    Cell cell = (corner || h == 1 || h == 4) ? Cell.PLATING : Cell.WINDOW;
                    if (dx == -10 && dz == 6 && h <= 2) {
                        cell = Cell.AIR; // doorway from the connector
                    }
                    put(out, c, dx, h, dz, cell);
                }
            }
        }
    }

    private static void observatory(List<Placement> out, BlockPos c) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                put(out, c, dx, 9, dz, dx == 0 && dz == 0 ? Cell.TELESCOPE : Cell.PLATING);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    for (int h = 6; h <= 8; h++) {
                        put(out, c, dx, h, dz, Cell.WINDOW);
                    }
                }
            }
        }
        // Access: a climbable truss column inside the core room up through the roof, one cell in from the corner
        // so it opens into the cupola's interior rather than its wall.
        for (int h = 1; h <= 5; h++) {
            put(out, c, 1, h, 1, Cell.TRUSS_Y);
        }
    }

    /** Number of cells of {@code cell} in a template (tests and the console's cost hints). */
    public static int count(List<Placement> template, Cell cell) {
        int n = 0;
        for (Placement p : template) {
            if (p.cell() == cell) {
                n++;
            }
        }
        return n;
    }
}
