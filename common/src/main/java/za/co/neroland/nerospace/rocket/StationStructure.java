package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Builds the enclosed station in the void: a 7×7 station-floor observation room (station-wall corner
 * pillars, two-high glass window bands, a station-floor roof, a lit ceiling), plus an <b>airlock corridor
 * on the east side</b> that runs out to an exposed <b>3×3 Tier-2 launch pad</b>. The pad is registered as
 * a travel node so a rocket flown to the station lands on it (no docking port). Shared by founding (the
 * charter) and rocket arrival, and idempotent — re-running never clobbers the Station Core or block
 * entities, and re-registers the same pad rather than duplicating it.
 */
public final class StationStructure {

    /** 7×7 room footprint (radius 3). */
    public static final int RADIUS = 3;
    /** Room wall height: walls at +1..+WALL_H, roof at +WALL_H+1. */
    public static final int WALL_H = 4;
    /** Length of the airlock corridor (blocks east of the room wall). */
    private static final int AIRLOCK_LEN = 4;

    private StationStructure() {
    }

    /** The Tier-2 landing-pad centre for a station at {@code centre} (deterministic from the layout). */
    public static BlockPos padCenter(BlockPos centre) {
        return new BlockPos(centre.getX() + RADIUS + AIRLOCK_LEN + 2, centre.getY(), centre.getZ());
    }

    /**
     * Generates the station around {@code centre} <b>once</b>, returning the centre of its Tier-2 landing
     * pad. If a Station Core already stands at {@code centre} the station is left completely untouched —
     * player-placed/-removed blocks are preserved — and only the travel-pad node is kept registered.
     */
    public static BlockPos build(ServerLevel level, BlockPos centre) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        BlockState wall = ModBlocks.STATION_WALL.get().defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        int cx = centre.getX();
        int cy = centre.getY();
        int cz = centre.getZ();
        int r = RADIUS;

        // The structure straddles up to four chunks — load the whole footprint (room + airlock) first.
        for (int xx = cx - r; xx <= cx + r + AIRLOCK_LEN + 3; xx += r) {
            for (int zz = cz - r; zz <= cz + r; zz += r) {
                level.getChunk(xx >> 4, zz >> 4);
            }
        }

        // Already generated — never rebuild (preserve the player's changes); just keep the pad node live.
        if (level.getBlockState(centre).is(ModBlocks.STATION_CORE.get())) {
            BlockPos existing = padCenter(centre);
            registerPad(level, centre, existing);
            return existing;
        }

        // --- Room: floor, lit ceiling, walls + window bands -----------------------------------
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos fp = new BlockPos(cx + dx, cy, cz + dz);
                if (!level.getBlockState(fp).is(ModBlocks.STATION_CORE.get()) && level.getBlockEntity(fp) == null) {
                    level.setBlockAndUpdate(fp, floor);
                }
                boolean ceilingLight = (dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2);
                level.setBlockAndUpdate(new BlockPos(cx + dx, cy + WALL_H + 1, cz + dz),
                        ceilingLight ? lamp : floor);

                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                if (!edge) {
                    continue;
                }
                boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                for (int h = 1; h <= WALL_H; h++) {
                    BlockState block = (corner || h == 1 || h == WALL_H) ? wall : glass;
                    level.setBlockAndUpdate(new BlockPos(cx + dx, cy + h, cz + dz), block);
                }
            }
        }

        // --- Doorway in the east wall (x = cx+r), two high at the centre row -------------------
        level.setBlockAndUpdate(new BlockPos(cx + r, cy + 1, cz), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(cx + r, cy + 2, cz), Blocks.AIR.defaultBlockState());

        // --- Airlock corridor: floor + roof, glazed side walls, open far end ------------------
        int startX = cx + r + 1;            // first corridor column, just outside the room wall
        int endX = startX + AIRLOCK_LEN - 1;
        for (int x = startX; x <= endX; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(x, cy, cz + dz), floor);          // floor
                level.setBlockAndUpdate(new BlockPos(x, cy + 3, cz + dz), floor);      // roof
            }
            // Side walls (kneewall + glass window).
            level.setBlockAndUpdate(new BlockPos(x, cy + 1, cz - 1), wall);
            level.setBlockAndUpdate(new BlockPos(x, cy + 1, cz + 1), wall);
            level.setBlockAndUpdate(new BlockPos(x, cy + 2, cz - 1), glass);
            level.setBlockAndUpdate(new BlockPos(x, cy + 2, cz + 1), glass);
        }

        // --- 3×3 Tier-2 launch pad beyond the airlock (open sky above for the rocket) ----------
        BlockState pad = ModBlocks.ROCKET_LAUNCH_PAD.get().defaultBlockState();
        int padCx = endX + 2; // pad centre column
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(padCx + dx, cy, cz + dz), pad);
            }
        }

        BlockPos padCentre = new BlockPos(padCx, cy, cz);

        // Anchor the founded station: place + bind the (unbreakable) Station Core.
        StationRegistry.StationEntry entry = entryAt(level, centre);
        if (entry != null) {
            level.setBlockAndUpdate(centre, ModBlocks.STATION_CORE.get().defaultBlockState());
            if (level.getBlockEntity(centre) instanceof StationCoreBlockEntity core) {
                core.bindStation(entry.slot(), entry.name());
            }
        }
        registerPad(level, centre, padCentre);
        return padCentre;
    }

    @org.jetbrains.annotations.Nullable
    private static StationRegistry.StationEntry entryAt(ServerLevel level, BlockPos centre) {
        for (StationRegistry.StationEntry e : StationRegistry.get(level.getServer()).all()) {
            if (e.center().equals(centre)) {
                return e;
            }
        }
        return null;
    }

    /** Keeps the station's landing pad registered as a travel node, named after the station. */
    private static void registerPad(ServerLevel level, BlockPos centre, BlockPos padCentre) {
        StationRegistry.StationEntry entry = entryAt(level, centre);
        String label = entry != null ? entry.name() : "Station";
        PadRegistry.get(level.getServer()).register(label + " Landing", level.dimension(), padCentre);
    }
}
