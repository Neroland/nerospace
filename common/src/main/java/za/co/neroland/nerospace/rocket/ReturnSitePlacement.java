package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;

/** Builds or reuses return sites at rocket arrival points. */
public final class ReturnSitePlacement {

    private static final int SEARCH_RADIUS = 6;
    private static final int PLATFORM_RADIUS = 3;

    public record Arrival(BlockPos site, double x, double y, double z) {
    }

    private ReturnSitePlacement() {
    }

    public static Arrival place(ServerLevel level, net.minecraft.resources.ResourceKey<Level> target,
            BlockPos preferred, RocketTier tier, int carriedFuelMb) {
        if (target.equals(ModDimensions.STATION_LEVEL)) {
            return placeStationDock(level, preferred, tier, carriedFuelMb);
        }
        return placeLandingPod(level, preferred.getX(), preferred.getZ(), tier, carriedFuelMb);
    }

    private static Arrival placeStationDock(ServerLevel level, BlockPos centre, RocketTier tier, int carriedFuelMb) {
        level.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        buildStationPlatform(level, centre);
        BlockPos site = findOrPlace(level, ModBlocks.DOCKING_PORT.get(), centre.above(), true);
        seed(level, site, tier, carriedFuelMb);
        return arrival(site);
    }

    private static Arrival placeLandingPod(ServerLevel level, int x, int z, RocketTier tier, int carriedFuelMb) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos site = null;
        for (int radius = 0; radius <= SEARCH_RADIUS && site == null; radius++) {
            for (int dx = -radius; dx <= radius && site == null; dx++) {
                for (int dz = -radius; dz <= radius && site == null; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int px = x + dx;
                    int pz = z + dz;
                    level.getChunk(px >> 4, pz >> 4);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                    BlockPos candidate = new BlockPos(px, y, pz);
                    site = existingOrPlaceable(level, candidate, ModBlocks.LANDING_POD.get());
                }
            }
        }
        if (site == null) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 8;
            site = new BlockPos(x, y, z);
            for (int attempts = 0; attempts < 32 && !canPlaceSite(level, site); attempts++) {
                site = site.above();
            }
        }
        placeSite(level, site, ModBlocks.LANDING_POD.get(), false);
        ensureTinyFoundation(level, site);
        seed(level, site, tier, carriedFuelMb);
        return arrival(site);
    }

    private static BlockPos findOrPlace(ServerLevel level, Block block, BlockPos preferred, boolean station) {
        BlockPos site = null;
        for (int radius = 0; radius <= SEARCH_RADIUS && site == null; radius++) {
            for (int dx = -radius; dx <= radius && site == null; dx++) {
                for (int dz = -radius; dz <= radius && site == null; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    site = existingOrPlaceable(level, preferred.offset(dx, 0, dz), block);
                }
            }
        }
        if (site == null) {
            site = preferred.above(station ? 1 : 8);
        }
        placeSite(level, site, block, station);
        return site;
    }

    private static BlockPos existingOrPlaceable(ServerLevel level, BlockPos pos, Block block) {
        BlockState state = level.getBlockState(pos);
        if (state.is(block) && level.getBlockEntity(pos) instanceof ReturnSiteBlockEntity site && site.isEmpty()) {
            return pos;
        }
        return canPlaceSite(level, pos) ? pos : null;
    }

    private static boolean canPlaceSite(ServerLevel level, BlockPos pos) {
        return replaceable(level, pos) && replaceable(level, pos.above()) && replaceable(level, pos.above(2));
    }

    private static boolean replaceable(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static void placeSite(ServerLevel level, BlockPos pos, Block block, boolean station) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(block)) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
        clearHeadroom(level, pos);
        if (!station) {
            ensureTinyFoundation(level, pos);
        }
    }

    private static void clearHeadroom(ServerLevel level, BlockPos pos) {
        clearIfReplaceable(level, pos.above());
        clearIfReplaceable(level, pos.above(2));
    }

    private static void clearIfReplaceable(ServerLevel level, BlockPos pos) {
        if (replaceable(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void ensureTinyFoundation(ServerLevel level, BlockPos site) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        int y = site.getY() - 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(site.getX() + dx, y, site.getZ() + dz);
                if (replaceable(level, pos)) {
                    level.setBlockAndUpdate(pos, floor);
                }
            }
        }
    }

    private static void buildStationPlatform(ServerLevel level, BlockPos centre) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                BlockPos pos = new BlockPos(centre.getX() + dx, centre.getY(), centre.getZ() + dz);
                if (level.getBlockEntity(pos) == null && !level.getBlockState(pos).is(ModBlocks.STATION_CORE.get())) {
                    level.setBlockAndUpdate(pos, floor);
                }
            }
        }
    }

    private static void seed(ServerLevel level, BlockPos pos, RocketTier tier, int carriedFuelMb) {
        if (level.getBlockEntity(pos) instanceof ReturnSiteBlockEntity site) {
            site.seed(tier, carriedFuelMb);
        }
    }

    private static Arrival arrival(BlockPos site) {
        return new Arrival(site, site.getX() + 0.5D, site.getY() + 1.1D, site.getZ() + 0.5D);
    }
}
