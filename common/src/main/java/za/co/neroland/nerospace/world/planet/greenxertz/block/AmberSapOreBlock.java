package za.co.neroland.nerospace.world.planet.greenxertz.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/** Amber Sap Ore: decorative drip only — the animated texture plus one honey-drip particle per ~100 ticks. */
public class AmberSapOreBlock extends Block {

    public static final MapCodec<AmberSapOreBlock> CODEC = BlockCodecs.simple(AmberSapOreBlock::new);

    public static final int DRIP_ONE_IN = 100;

    public AmberSapOreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends AmberSapOreBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(DRIP_ONE_IN) != 0) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isAir()) {
            return;
        }
        level.addParticle(ParticleTypes.DRIPPING_HONEY, pos.getX() + random.nextDouble(), pos.getY() - 0.05,
                pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
    }
}
