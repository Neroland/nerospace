package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.VentBloomRules;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Tube Mat: a glowing mat of vent life, one pixel tall, on the sea floor around hydrothermal vents. Under water and
 * with an active vent within 6 blocks it spreads to a neighbouring water cell that sits on Vent Basalt, until 24 mats
 * stand in the scanned box. Out of water, or away from a vent, it stays as it is.
 *
 * <p>Cost: three random ticks in four, and every tick of a dry mat, return at once. The fourth scans a
 * 13 x 4 x 13 band (676 block reads) once for both the vent and the mat count, and places one block at most.</p>
 */
public class TubeMatBlock extends Block implements SimpleWaterloggedBlock {

    public static final MapCodec<TubeMatBlock> CODEC = BlockCodecs.simple(TubeMatBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    @SuppressWarnings("this-escape")
    public TubeMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    protected MapCodec<? extends TubeMatBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, water);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
    *///?}
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            //? if >=1.21.2 {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            //?} else {
            /*level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            *///?}
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WATERLOGGED) || !VentBloomRules.attempts(random.nextFloat())) {
            return;
        }
        BlockPos target = pickTarget(level, pos, random);
        if (target == null) {
            return;
        }
        int mats = 0;
        boolean vent = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -VentBloomRules.BAND_DOWN; dy <= VentBloomRules.BAND_UP; dy++) {
            for (int dx = -VentBloomRules.RANGE; dx <= VentBloomRules.RANGE; dx++) {
                for (int dz = -VentBloomRules.RANGE; dz <= VentBloomRules.RANGE; dz++) {
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.isLoaded(p)) {
                        return;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.is(this)) {
                        mats++;
                    } else if (s.is(EuropaContent.HYDROTHERMAL_VENT.get()) && s.getValue(HydrothermalVentBlock.ACTIVE)) {
                        vent = true;
                    }
                }
            }
        }
        if (VentBloomRules.maySpread(true, vent, mats)) {
            level.setBlockAndUpdate(target, defaultBlockState().setValue(WATERLOGGED, true));
        }
    }

    /** A water cell on Vent Basalt beside the mat (one step in a random direction, same level, one up or one down). */
    private static BlockPos pickTarget(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos side = pos.relative(Direction.from2DDataValue(random.nextInt(4)));
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos at = dy == 0 ? side : side.below();
            if (fits(level, at)) {
                return at;
            }
        }
        BlockPos up = side.above();
        return fits(level, up) ? up : null;
    }

    private static boolean fits(ServerLevel level, BlockPos at) {
        if (!level.isLoaded(at)) {
            return false;
        }
        BlockState cell = level.getBlockState(at);
        return cell.is(Blocks.WATER) && cell.getFluidState().isSource()
                && level.getBlockState(at.below()).is(EuropaContent.VENT_BASALT.get());
    }
}
