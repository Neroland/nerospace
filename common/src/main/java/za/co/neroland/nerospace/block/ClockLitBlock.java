package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.AuroraRules;
import za.co.neroland.nerospace.world.planet.PlanetClock;

/**
 * Clock-gated emitter (Aurora Stone, 002 §1.4, §10.3): a self-rescheduling tick every 200 ticks sets {@code LIT} from
 * the planet daylight — a block-state flip, not a light-engine hook. Register with
 * {@code lightLevel(s -> s.getValue(LIT) ? AuroraRules.LIGHT : 0)}.
 */
public class ClockLitBlock extends Block {

    public static final MapCodec<ClockLitBlock> CODEC = BlockCodecs.simple(ClockLitBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @SuppressWarnings("this-escape")
    public ClockLitBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    protected MapCodec<? extends ClockLitBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1 + level.getRandom().nextInt(AuroraRules.INTERVAL_TICKS));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = AuroraRules.lit(PlanetClock.daylight(level));
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, AuroraRules.INTERVAL_TICKS);
    }
}
