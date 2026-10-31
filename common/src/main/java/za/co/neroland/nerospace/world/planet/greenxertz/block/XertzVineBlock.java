package za.co.neroland.nerospace.world.planet.greenxertz.block;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.VineRules;

/**
 * Xertz Vine: weeping-vine style. Hangs from a sturdy ceiling or another vine, is climbable (tag
 * {@code minecraft:climbable}), and the lowest segment ({@link #TIP}) grows one block down on 1 in 10 random ticks
 * while the strand is shorter than 12 and the cell below is air ({@link VineRules}). Losing its support breaks the
 * vine (and so everything below it); shears drop Xertz Fibre (loot table).
 */
public class XertzVineBlock extends Block {

    public static final MapCodec<XertzVineBlock> CODEC = BlockCodecs.simple(XertzVineBlock::new);

    public static final BooleanProperty TIP = BooleanProperty.create("tip");

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public XertzVineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true));
    }

    protected MapCodec<? extends XertzVineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState().setValue(TIP, !below.is(this));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState up = level.getBlockState(above);
        return up.is(this) || up.isFaceSturdy(level, above, Direction.DOWN);
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    *///?}
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            //? if >=1.21.2 {
            ticks.scheduleTick(pos, this, 1);
            //?} else {
            /*level.scheduleTick(pos, this, 1);
            *///?}
        }
        if (direction == Direction.DOWN) {
            return state.setValue(TIP, !neighborState.is(this));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TIP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TIP) || random.nextInt(VineRules.ONE_IN) != 0) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.isInWorldBounds(below)) {
            return;
        }
        if (VineRules.canGrowDown(length(level, pos), VineRules.MAX_LENGTH, level.getBlockState(below).isAir())) {
            level.setBlockAndUpdate(below, defaultBlockState().setValue(TIP, true));
        }
    }

    /** Segments in this strand from {@code pos} upward (capped just past the maximum). */
    private int length(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        int n = 0;
        while (n <= VineRules.MAX_LENGTH && level.getBlockState(m).is(this)) {
            n++;
            m.move(Direction.UP);
        }
        return n;
    }
}
