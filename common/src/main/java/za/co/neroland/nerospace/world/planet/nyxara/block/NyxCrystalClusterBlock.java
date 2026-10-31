package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.ClusterLootRules;

/**
 * Nyx Crystal Cluster (Nyxara geodes and band floor): an amethyst-style cluster, {@code STAGE} 0-3 and
 * {@code FACING} (the face it grows from). Needs a sturdy face behind it; no collision; cross model (cutout layer).
 * The loot table gives a full cluster 2 shards with a pickaxe and 1 otherwise ({@link ClusterLootRules}). Register
 * with {@code lightLevel(s -> ClusterLootRules.light(s.getValue(STAGE)))} and {@code noCollision()}.
 */
public class NyxCrystalClusterBlock extends Block {

    public static final MapCodec<NyxCrystalClusterBlock> CODEC = BlockCodecs.simple(NyxCrystalClusterBlock::new);

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, ClusterLootRules.MAX_STAGE);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final VoxelShape[][] SHAPES = new VoxelShape[ClusterLootRules.MAX_STAGE + 1][6];

    static {
        for (int stage = 0; stage <= ClusterLootRules.MAX_STAGE; stage++) {
            double h = ClusterLootRules.height(stage);
            double o = ClusterLootRules.inset(stage);
            SHAPES[stage][Direction.UP.get3DDataValue()] = Block.box(o, 0, o, 16 - o, h, 16 - o);
            SHAPES[stage][Direction.DOWN.get3DDataValue()] = Block.box(o, 16 - h, o, 16 - o, 16, 16 - o);
            SHAPES[stage][Direction.NORTH.get3DDataValue()] = Block.box(o, o, 16 - h, 16 - o, 16 - o, 16);
            SHAPES[stage][Direction.SOUTH.get3DDataValue()] = Block.box(o, o, 0, 16 - o, 16 - o, h);
            SHAPES[stage][Direction.EAST.get3DDataValue()] = Block.box(0, o, o, h, 16 - o, 16 - o);
            SHAPES[stage][Direction.WEST.get3DDataValue()] = Block.box(16 - h, o, o, 16, 16 - o, 16 - o);
        }
    }

    @SuppressWarnings("this-escape")
    public NyxCrystalClusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, ClusterLootRules.MAX_STAGE)
                .setValue(FACING, Direction.UP));
    }

    protected MapCodec<? extends NyxCrystalClusterBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)][state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
}
