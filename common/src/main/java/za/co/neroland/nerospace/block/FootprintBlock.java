package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.FootprintRules;

/**
 * Ground that keeps a {@code FOOTPRINT} (Regolith, 002 §1.1). Set by a living, non-spectator walker; cleared by a
 * neighbour update from above, a piston move (pushed blocks are re-placed with the default state), or a re-step
 * from the opposite facing. No ticks. The landing puff is a server particle burst only when the fall was ≥ 1.
 */
public class FootprintBlock extends Block {

    public static final MapCodec<FootprintBlock> CODEC = BlockCodecs.simple(FootprintBlock::new);

    public static final BooleanProperty FOOTPRINT = BooleanProperty.create("footprint");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    @SuppressWarnings("this-escape")
    public FootprintBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FOOTPRINT, Boolean.FALSE).setValue(FACING, Direction.NORTH));
    }

    protected MapCodec<? extends FootprintBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FOOTPRINT, FACING);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()) {
            double move = entity.getDeltaMovement().horizontalDistance();
            boolean imprinted = state.getValue(FOOTPRINT);
            Direction facing = entity.getDirection().getAxis().isHorizontal() ? entity.getDirection() : Direction.NORTH;
            if (imprinted && facing == state.getValue(FACING).getOpposite()
                    && FootprintRules.shouldClear(false, false, true)) {
                level.setBlock(pos, state.setValue(FOOTPRINT, Boolean.FALSE), Block.UPDATE_CLIENTS);
            } else if (FootprintRules.shouldImprint(entity instanceof LivingEntity, entity.isSpectator(),
                    entity.fallDistance, move, imprinted)) {
                level.setBlock(pos, state.setValue(FOOTPRINT, Boolean.TRUE).setValue(FACING, facing), Block.UPDATE_CLIENTS);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level instanceof ServerLevel server && FootprintRules.showPuff(fallDistance)) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), entity.getX(), pos.getY() + 1.0,
                    entity.getZ(), FootprintRules.PUFF_PARTICLES, 0.3, 0.05, 0.3, 0.05);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(FOOTPRINT) && FootprintRules.shouldClear(direction == Direction.UP, false)) {
            return state.setValue(FOOTPRINT, Boolean.FALSE);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }
}
