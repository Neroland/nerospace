package za.co.neroland.nerospace.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Hamlet feature (ALIEN_VILLAGERS_DESIGN.md §5.2, Phase 3) — a small Greenxertz alien outpost: a
 * levelled nerosteel platform with a low wall ring, four lit corner pillars, and a claimable
 * {@link za.co.neroland.nerospace.village.VillageCoreBlock} at its heart. Placed rarely on the
 * surface; the player stumbles on it, claims the core, and the alien villagers that wander the biome
 * give it life. (The jigsaw/megastructure work is Phase 7; this self-contained Feature is the robust
 * "find a small village" first slice.)
 */
public class HamletFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 3; // 7x7 footprint

    public HamletFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        BlockState floor = ModBlocks.NEROSTEEL_BLOCK.get().defaultBlockState();
        BlockState wall = ModBlocks.NEROSTEEL_BLOCK.get().defaultBlockState();
        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        BlockState light = Blocks.GLOWSTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int baseY = origin.getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // Levelled platform: nerosteel floor, cleared headroom, a low wall around the edge.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                m.set(x, baseY - 1, z);
                level.setBlock(m, floor, 2);
                for (int dy = 0; dy < 4; dy++) {
                    m.set(x, baseY + dy, z);
                    level.setBlock(m, air, 2);
                }
                if (Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS) {
                    m.set(x, baseY, z);
                    level.setBlock(m, wall, 2);
                }
            }
        }

        // Four lit corner pillars.
        for (int sx : new int[] {-RADIUS, RADIUS}) {
            for (int sz : new int[] {-RADIUS, RADIUS}) {
                for (int dy = 0; dy < 3; dy++) {
                    m.set(origin.getX() + sx, baseY + dy, origin.getZ() + sz);
                    level.setBlock(m, wall, 2);
                }
                m.set(origin.getX() + sx, baseY + 3, origin.getZ() + sz);
                level.setBlock(m, light, 2);
            }
        }

        // The Village Core at the centre.
        m.set(origin.getX(), baseY, origin.getZ());
        level.setBlock(m, core, 2);
        return true;
    }
}
