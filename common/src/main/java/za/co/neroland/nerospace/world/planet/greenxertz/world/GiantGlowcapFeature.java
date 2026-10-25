package za.co.neroland.nerospace.world.planet.greenxertz.world;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

/**
 * Worldgen wrapper for {@link GiantGlowcap#grow}: placed on a surface air cell above Xertz Soil
 * ({@code nerospace:greenxertz_giant_glowcap}). Configuration-free.
 */
//? if >=26.3 {
/*public class GiantGlowcapFeature implements Feature {
*///?} else {
public class GiantGlowcapFeature extends Feature<NoneFeatureConfiguration> {
//?}

    //? if >=26.3 {
    /*public static final GiantGlowcapFeature INSTANCE = new GiantGlowcapFeature();
    public static final com.mojang.serialization.MapCodec<GiantGlowcapFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource rand,
            BlockPos o) {
        return generate(level, rand, o);
    }
    *///?} else {
    public GiantGlowcapFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return generate(ctx.level(), ctx.random(), ctx.origin());
    }
    //?}

    private static boolean generate(WorldGenLevel level, RandomSource rand, BlockPos o) {
        return GiantGlowcap.grow(level, o, rand, true);
    }
}
