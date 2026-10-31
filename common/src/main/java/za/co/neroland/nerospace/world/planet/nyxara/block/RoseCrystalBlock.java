package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.FootglowRules;

/**
 * Rose Crystal (Nyxara band): lights up where a player walks. A player (not a mob, not a spectator) stepping on an
 * unlit block sets {@code LIT} (light 9) and schedules one tick 60 ticks later that clears it; an already lit block is
 * left alone, so each block entered costs at most one state change. Idle light 2. Register with
 * {@code lightLevel(s -> FootglowRules.light(s.getValue(LIT)))}.
 */
public class RoseCrystalBlock extends HalfTransparentBlock {

    public static final MapCodec<RoseCrystalBlock> CODEC = BlockCodecs.simple(RoseCrystalBlock::new);

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    @SuppressWarnings("this-escape")
    public RoseCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    protected MapCodec<? extends RoseCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()
                && FootglowRules.lightUp(entity instanceof Player, entity.isSpectator(), state.getValue(LIT))) {
            level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, FootglowRules.LIT_TICKS);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), Block.UPDATE_CLIENTS);
        }
    }
}
