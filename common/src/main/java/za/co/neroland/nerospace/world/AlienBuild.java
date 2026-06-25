package za.co.neroland.nerospace.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Shared procedural pieces for the alien structures — a small kit that makes the hamlet, ruin and
 * mega-city read as futuristic alien architecture: tile podiums, brick walls with glowing crystal
 * window bands, taller lit corner pillars, tapered roofs and crystal spires. Built entirely from the
 * alien decoration block set.
 */
public final class AlienBuild {

    private AlienBuild() {
    }

    public static @NonNull BlockState bricks() {
        return NerospaceCommon.requireNonNull(ModBlocks.ALIEN_BRICKS.get().defaultBlockState());
    }

    public static @NonNull BlockState cracked() {
        return NerospaceCommon.requireNonNull(ModBlocks.CRACKED_ALIEN_BRICKS.get().defaultBlockState());
    }

    public static @NonNull BlockState tile() {
        return NerospaceCommon.requireNonNull(ModBlocks.ALIEN_TILE.get().defaultBlockState());
    }

    public static @NonNull BlockState pillar() {
        return NerospaceCommon.requireNonNull(ModBlocks.ALIEN_PILLAR.get().defaultBlockState());
    }

    public static @NonNull BlockState lamp() {
        return NerospaceCommon.requireNonNull(ModBlocks.ALIEN_LAMP.get().defaultBlockState());
    }

    public static @NonNull BlockState crystal() {
        return NerospaceCommon.requireNonNull(ModBlocks.ALIEN_CRYSTAL_BLOCK.get().defaultBlockState());
    }

    public static @NonNull BlockState air() {
        return NerospaceCommon.requireNonNull(Blocks.AIR.defaultBlockState());
    }

    private static void set(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z,
            @NonNull BlockState s) {
        m.set(x, y, z);
        level.setBlock(m, s, 2);
    }

    /**
     * A futuristic alien building. Tile podium, brick walls with a glowing crystal window band, taller
     * lit corner pillars, a tapered roof and a crystal spire. {@code weathered} → ruined variant
     * (cracked bricks, broken walls, no spire). {@code r} = half-footprint, {@code height} = wall height.
     * Origin is the building centre at ground level {@code baseY}.
     */
    public static void tower(WorldGenLevel level, int cx, int baseY, int cz, int r, int height,
            boolean weathered, RandomSource rand, BlockPos.MutableBlockPos m) {
        BlockState wall = weathered ? cracked() : bricks();
        int band = Math.max(1, height / 2);

        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                set(level, m, cx + dx, baseY - 1, cz + dz, tile());
            }
        }
        for (int dy = 0; dy <= height + 2; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    set(level, m, cx + dx, baseY + dy, cz + dz, air());
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean perimeter = Math.abs(dx) == r || Math.abs(dz) == r;
                    if (!perimeter) {
                        continue;
                    }
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (corner) {
                        continue;
                    }
                    boolean door = dz == -r && dx == 0 && dy <= 1;
                    if (door) {
                        continue;
                    }
                    if (weathered && rand.nextFloat() < 0.28F) {
                        continue;
                    }
                    boolean window = dy == band;
                    set(level, m, cx + dx, baseY + dy, cz + dz, window ? crystal() : wall);
                }
            }
        }

        int pillarTop = weathered ? height - 1 : height + 1;
        for (int sx : new int[] {-r, r}) {
            for (int sz : new int[] {-r, r}) {
                for (int dy = 0; dy <= pillarTop; dy++) {
                    set(level, m, cx + sx, baseY + dy, cz + sz, pillar());
                }
                if (!weathered) {
                    set(level, m, cx + sx, baseY + pillarTop + 1, cz + sz, lamp());
                }
            }
        }

        for (int dx = -(r - 1); dx <= r - 1; dx++) {
            for (int dz = -(r - 1); dz <= r - 1; dz++) {
                if (weathered && rand.nextFloat() < 0.35F) {
                    continue;
                }
                set(level, m, cx + dx, baseY + height, cz + dz, dx == 0 && dz == 0 ? crystal() : tile());
            }
        }
        set(level, m, cx, baseY + 1, cz, lamp());

        if (!weathered) {
            for (int dy = 1; dy <= 3; dy++) {
                set(level, m, cx, baseY + height + dy, cz, crystal());
            }
        }
    }
}
