package za.co.neroland.nerospace.world.planet.cindara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.LavaImmuneRules;

/**
 * Magma Glass (Cindara): translucent orange glass that glows (light 7, set on the block properties) and is the only
 * transparent block lava can never replace. No fluid may replace it ({@link LavaImmuneRules}); it is also in
 * {@code #nerospace:lava_immune} for any custom lava interaction. Client-side it sheds about two flame particles per
 * 60 ticks. It has no sealing and no light lens.
 */
public class MagmaGlassBlock extends TransparentBlock {

    public static final MapCodec<MagmaGlassBlock> CODEC = BlockCodecs.simple(MagmaGlassBlock::new);

    /** One flame per this many animate ticks on average (≈ 2 per 60 ticks). */
    static final int FLAME_ONE_IN = 30;

    public MagmaGlassBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends MagmaGlassBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return LavaImmuneRules.replaceable(fluid.defaultFluidState().is(FluidTags.LAVA));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(FLAME_ONE_IN) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.02,
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
