package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.BladeRules;
import za.co.neroland.nerospace.world.planet.PlanetHazards;

/**
 * Penitente Blade: a thin blade of sun-carved ice standing on the surface, aligned east-west ({@code AXIS} x) or
 * north-south (z). Landing on one from 2 blocks or more deals one and a half times the fall damage and snaps the blade
 * (it drops itself). Native fauna are not hurt. It needs a solid top below it. Work happens in {@code fallOn} only.
 */
public class PenitenteBladeBlock extends Block {

    public static final MapCodec<PenitenteBladeBlock> CODEC = BlockCodecs.simple(PenitenteBladeBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape SHAPE_X = Block.box(1.0, 0.0, 5.0, 15.0, 15.0, 11.0);
    private static final VoxelShape SHAPE_Z = Block.box(5.0, 0.0, 1.0, 11.0, 15.0, 15.0);

    @SuppressWarnings("this-escape")
    public PenitenteBladeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    protected MapCodec<? extends PenitenteBladeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        return defaultBlockState().setValue(AXIS, axis == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
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
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!(level instanceof ServerLevel server) || !BladeRules.breaks(fallDistance)) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }
        entity.causeFallDamage(fallDistance, BladeRules.damageFactor(fallDistance, PlanetHazards.isNative(entity)),
                entity.damageSources().fall());
        server.destroyBlock(pos, true, entity);
    }
}
