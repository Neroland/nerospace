package za.co.neroland.nerospace.world.planet.glacira.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.CrystalStageBlock;
import za.co.neroland.nerospace.block.rules.CrystalStageRules;
import za.co.neroland.nerospace.world.planet.glacira.GlaciraNumbers;

/**
 * Frost Crystal Cluster: the shared staged-growth base with a cluster outline that widens and rises per stage
 * (small bud at 0, full cluster at 3). Growth itself is the diamond-dust hook in {@link CrystalStageBlock}.
 */
public class FrostCrystalClusterBlock extends CrystalStageBlock {

    public static final MapCodec<FrostCrystalClusterBlock> CODEC = BlockCodecs.simple(FrostCrystalClusterBlock::new);

    private static final VoxelShape[] SHAPES = new VoxelShape[CrystalStageRules.MAX_STAGE + 1];

    static {
        for (int s = 0; s <= CrystalStageRules.MAX_STAGE; s++) {
            double inset = GlaciraNumbers.clusterInset(s);
            SHAPES[s] = Block.box(inset, 0.0, inset, 16.0 - inset, GlaciraNumbers.clusterHeight(s), 16.0 - inset);
        }
    }

    public FrostCrystalClusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FrostCrystalClusterBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)];
    }
}
