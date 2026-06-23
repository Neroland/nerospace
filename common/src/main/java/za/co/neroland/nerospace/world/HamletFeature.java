package za.co.neroland.nerospace.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Hamlet — a small alien outpost: a glowing tile plaza, a central {@code VillageCore} on a lit podium,
 * and two futuristic towers. Placement is gated by {@link StructureSpacing} for spacing + density cap.
 */
public class HamletFeature extends Feature<NoneFeatureConfiguration> {

    private static final int PLAZA = 6; // 13x13 plaza

    public HamletFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        BlockPos o = ctx.origin();
        if (!StructureSpacing.shouldPlace(o, StructureSpacing.Roi.HAMLET)) {
            return false;
        }
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        int baseY = o.getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int dx = -PLAZA; dx <= PLAZA; dx++) {
            for (int dz = -PLAZA; dz <= PLAZA; dz++) {
                m.set(o.getX() + dx, baseY - 1, o.getZ() + dz);
                boolean edge = Math.abs(dx) == PLAZA || Math.abs(dz) == PLAZA;
                level.setBlock(m, edge && (dx + dz) % 2 == 0 ? AlienBuild.lamp() : AlienBuild.tile(), 2);
                for (int dy = 0; dy < 4; dy++) {
                    m.set(o.getX() + dx, baseY + dy, o.getZ() + dz);
                    level.setBlock(m, AlienBuild.air(), 2);
                }
            }
        }

        AlienBuild.tower(level, o.getX() - 4, baseY, o.getZ() - 4, 2, 4, false, rand, m);
        AlienBuild.tower(level, o.getX() + 4, baseY, o.getZ() + 4, 2, 4, false, rand, m);

        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        m.set(o.getX(), baseY - 1, o.getZ());
        level.setBlock(m, AlienBuild.crystal(), 2);
        m.set(o.getX(), baseY, o.getZ());
        level.setBlock(m, core, 2);
        for (int[] off : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            m.set(o.getX() + off[0], baseY, o.getZ() + off[1]);
            level.setBlock(m, AlienBuild.lamp(), 2);
        }
        return true;
    }
}
