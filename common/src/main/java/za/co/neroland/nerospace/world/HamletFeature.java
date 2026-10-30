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
        AlienBuild.Site site = SettlementPlacement.begin(level, o, StructureSpacing.Roi.HAMLET);
        if (site == null) {
            return false;
        }
        int baseY = o.getY();

        for (int dx = -PLAZA; dx <= PLAZA; dx++) {
            for (int dz = -PLAZA; dz <= PLAZA; dz++) {
                boolean edge = Math.abs(dx) == PLAZA || Math.abs(dz) == PLAZA;
                site.set(o.getX() + dx, baseY - 1, o.getZ() + dz,
                        edge && (dx + dz) % 2 == 0 ? site.lamp() : site.tile());
                for (int dy = 0; dy < 4; dy++) {
                    site.set(o.getX() + dx, baseY + dy, o.getZ() + dz, AlienBuild.air());
                }
            }
        }

        AlienBuild.tower(site, o.getX() - 4, baseY, o.getZ() - 4, 2, 4, false, rand);
        AlienBuild.tower(site, o.getX() + 4, baseY, o.getZ() + 4, 2, 4, false, rand);

        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        site.set(o.getX(), baseY - 1, o.getZ(), site.crystal());
        site.set(o.getX(), baseY, o.getZ(), core);
        for (int[] off : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            site.set(o.getX() + off[0], baseY, o.getZ() + off[1], site.lamp());
        }
        return true;
    }
}
