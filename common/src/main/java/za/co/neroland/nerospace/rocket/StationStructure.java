package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Builds the enclosed station room in the void — a 7×7 station-floor deck, station-wall corner pillars
 * with a two-high glass window band on each side, a station-floor roof, and a lit ceiling (sea lanterns
 * at the centre and inner corners). Shared by both ways a station comes into being so it is always a
 * sealed, lit room: founding via the {@code StationCharterItem} and arriving by rocket via
 * {@link ReturnSitePlacement#place}. Idempotent — re-running never clobbers the Station Core or any
 * other block entity in the footprint.
 */
public final class StationStructure {

    /** 7×7 footprint (radius 3). */
    public static final int RADIUS = 3;
    /** Wall height: walls at +1..+WALL_H, roof at +WALL_H+1. */
    public static final int WALL_H = 4;

    private StationStructure() {
    }

    public static void build(ServerLevel level, BlockPos centre) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        BlockState wall = ModBlocks.STATION_WALL.get().defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        int cx = centre.getX();
        int cy = centre.getY();
        int cz = centre.getZ();
        int r = RADIUS;

        // The centre lands on a chunk corner, so the footprint straddles up to four chunks — load them
        // all first, otherwise writes into the unloaded chunks are dropped and the room builds partial.
        for (int xx = cx - r; xx <= cx + r; xx += r) {
            for (int zz = cz - r; zz <= cz + r; zz += r) {
                level.getChunk(xx >> 4, zz >> 4);
            }
        }

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                // Deck floor — never overwrite the Station Core or any other block entity.
                BlockPos fp = new BlockPos(cx + dx, cy, cz + dz);
                if (!level.getBlockState(fp).is(ModBlocks.STATION_CORE.get()) && level.getBlockEntity(fp) == null) {
                    level.setBlockAndUpdate(fp, floor);
                }

                // Lit ceiling: sea lanterns at the centre + the four inner corners, station floor elsewhere.
                boolean ceilingLight = (dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2);
                level.setBlockAndUpdate(new BlockPos(cx + dx, cy + WALL_H + 1, cz + dz),
                        ceilingLight ? lamp : floor);

                // Perimeter walls + window band.
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
    }
}
