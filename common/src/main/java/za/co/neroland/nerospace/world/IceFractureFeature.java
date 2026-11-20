package za.co.neroland.nerospace.world;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.registry.ModTags;

/**
 * Ice fracture: a narrow straight crack through Europa's shell, from the surface to the ocean (see
 * {@link FractureRules}). It only replaces blocks in {@code #nerospace:europa_fracture_replaceable}, writes without
 * neighbour updates, and stays inside the 3 x 3 chunk region a feature may write to.
 */
//? if >=26.3 {
/*public class IceFractureFeature implements Feature {
*///?} else {
public class IceFractureFeature extends Feature<NoneFeatureConfiguration> {
//?}

    //? if >=26.3 {
    /*// Minecraft 26.3+: features are data-driven values; this configuration-free type has one instance.
    public static final IceFractureFeature INSTANCE = new IceFractureFeature();
    public static final com.mojang.serialization.MapCodec<IceFractureFeature> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }
    *///?}
    //? if <26.3 {
    public IceFractureFeature(Codec<NoneFeatureConfiguration> codec) {
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
        boolean alongX = rand.nextBoolean();
        int length = FractureRules.length(rand.nextInt(FractureRules.LENGTH_MAX - FractureRules.LENGTH_MIN + 1));
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (FractureRules.Column c : FractureRules.plan(o.getX(), o.getZ(), alongX, length)) {
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, c.x(), c.z()) - 1;
            for (int y = top; y >= FractureRules.BOTTOM_Y; y--) {
                cursor.set(c.x(), y, c.z());
                if (!level.getBlockState(cursor).is(ModTags.Blocks.EUROPA_FRACTURE_REPLACEABLE)) {
                    continue;
                }
                level.setBlock(cursor, FractureRules.WATER.equals(FractureRules.fill(y)) ? water : air, 2);
                any = true;
            }
        }
        return any;
    }
}
