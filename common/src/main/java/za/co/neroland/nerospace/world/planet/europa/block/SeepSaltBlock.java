package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SeepRules;

/**
 * Lineae Salt: the rust-coloured salt of Europa's ridges. Where a cavity or brine lies within 12 blocks below, a faint
 * mist seeps from the top, so a ridge shows what is under it. Client display only: one 12-cell column scan on one
 * display call in 40, one particle at most, no state and no server cost.
 */
public class SeepSaltBlock extends Block {

    public static final MapCodec<SeepSaltBlock> CODEC = BlockCodecs.simple(SeepSaltBlock::new);

    public SeepSaltBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SeepSaltBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!SeepRules.scans(random.nextInt(SeepRules.GATE))) {
            return;
        }
        boolean[] open = new boolean[SeepRules.MAX_DEPTH];
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < open.length; i++) {
            p.move(0, -1, 0);
            BlockState below = level.getBlockState(p);
            open[i] = below.isAir() || EuropaHeat.isLiquid(below);
        }
        if (SeepRules.seeps(SeepRules.depthToCavity(open))) {
            for (int i = 0; i < SeepRules.PARTICLES_PER_CALL; i++) {
                level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                        pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
            }
        }
    }
}
