package za.co.neroland.nerospace.world.planet.greenxertz.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.SporeBurstBlock;

/**
 * Spore Pod: the shared bursting base ({@link SporeBurstBlock}: walk into it or punch it and it bursts into the
 * gated nausea cloud; shears or Silk Touch mine it whole) with a pod-sized outline and ground support.
 */
public class SporePodBlock extends SporeBurstBlock {

    public static final MapCodec<SporePodBlock> CODEC = BlockCodecs.simple(SporePodBlock::new);

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public SporePodBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SporePodBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }
}
