package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.ChimneyRules;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Smoker Chimney: a mineral chimney on a hydrothermal vent. On a random tick the top block of a stack that stands on
 * an active vent may add one block above itself, up to 5 tall, while the cell above is water or air. {@code CAPPED}
 * marks the top of a stack (it follows the block above); the block keeps the water it stands in.
 *
 * <p>Cost: only the top block does anything; it walks down at most 5 cells and places one block.</p>
 */
public class SmokerChimneyBlock extends Block implements SimpleWaterloggedBlock {

    public static final MapCodec<SmokerChimneyBlock> CODEC = BlockCodecs.simple(SmokerChimneyBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CAPPED = BooleanProperty.create("capped");

    @SuppressWarnings("this-escape")
    public SmokerChimneyBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(CAPPED, true));
    }

    protected MapCodec<? extends SmokerChimneyBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, CAPPED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(CAPPED, !level.getBlockState(pos.above()).is(this));
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
        if (state.getValue(WATERLOGGED)) {
            //? if >=1.21.2 {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            //?} else {
            /*level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            *///?}
        }
        if (direction == Direction.UP) {
            return state.setValue(CAPPED, !neighbourState.is(this));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(CAPPED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CAPPED) || !ChimneyRules.grows(random.nextFloat())) {
            return;
        }
        BlockPos above = pos.above();
        if (!level.isInsideBuildHeight(above.getY())) {
            return;
        }
        BlockState over = level.getBlockState(above);
        boolean water = over.is(Blocks.WATER) && over.getFluidState().isSource();
        if (ChimneyRules.mayGrow(height(level, pos), onActiveVent(level, pos), water || over.isAir())) {
            level.setBlockAndUpdate(above, defaultBlockState().setValue(WATERLOGGED, water));
        }
    }

    /** Stack height counted downward from {@code top}, stopping at the maximum. */
    private int height(BlockGetter level, BlockPos top) {
        int h = 0;
        BlockPos.MutableBlockPos p = top.mutable();
        while (h < ChimneyRules.MAX && level.getBlockState(p).is(this)) {
            h++;
            p.move(0, -1, 0);
        }
        return h;
    }

    /** Whether the block under the stack that {@code top} belongs to is an active vent (false for a taller stack). */
    private boolean onActiveVent(BlockGetter level, BlockPos top) {
        BlockPos.MutableBlockPos p = top.mutable();
        for (int i = 0; i < ChimneyRules.MAX && level.getBlockState(p).is(this); i++) {
            p.move(0, -1, 0);
        }
        BlockState base = level.getBlockState(p);
        return base.is(EuropaContent.HYDROTHERMAL_VENT.get()) && base.getValue(HydrothermalVentBlock.ACTIVE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CAPPED) && random.nextInt(6) == 0 && onActiveVent(level, pos)) {
            level.addParticle(state.getValue(WATERLOGGED) ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.06, 0.0);
        }
    }
}
