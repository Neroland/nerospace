package za.co.neroland.nerospace.world;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Hamlet — a small alien outpost: a glowing tile plaza, a central {@code VillageCore} on a lit podium,
 * and two futuristic towers. Placement is gated by {@link StructureSpacing} for spacing + density cap.
 */
//? if >=26.3 {
/*public class HamletFeature implements Feature {
*///?} else {
public class HamletFeature extends Feature<NoneFeatureConfiguration> {
//?}

    private static final int PLAZA = 6; // 13x13 plaza

    //? if >=26.3 {
    /*// Minecraft 26.3+: features are data-driven values; this configuration-free type has one instance.
    public static final HamletFeature INSTANCE = new HamletFeature();
    public static final com.mojang.serialization.MapCodec<HamletFeature> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }
    *///?}
    //? if <26.3 {
    public HamletFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    //?}

    //? if >=26.3 {
    /*@Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource rand,
            BlockPos o) {
        return generate(level, rand, o);
    }
    *///?} else {
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return generate(ctx.level(), ctx.random(), ctx.origin());
    }
    //?}

    /** Version-neutral body shared by both feature APIs. */
    private boolean generate(WorldGenLevel level, RandomSource rand, BlockPos o) {
        if (!StructureSpacing.shouldPlace(o, StructureSpacing.Roi.HAMLET)) {
            return false;
        }
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
