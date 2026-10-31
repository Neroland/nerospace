package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SkySoakRules;

/**
 * Twilight Crystal (Nyxara band): glows with its own sky light. {@code GLOW} 0-3 (light 0/3/5/7) follows the sky light
 * just above the block. Worldgen writes {@code glow=3} on the floor and {@code glow=0} below, so nothing scans; a
 * placed or exposed block re-reads it two ticks after placement or a neighbour change (one state change at most, and
 * only when the value differs). Register with {@code lightLevel(s -> SkySoakRules.light(s.getValue(GLOW)))}.
 */
public class TwilightCrystalBlock extends Block {

    public static final MapCodec<TwilightCrystalBlock> CODEC = BlockCodecs.simple(TwilightCrystalBlock::new);

    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, SkySoakRules.MAX_GLOW);

    /** Ticks after a change before the sky light is read (the light engine settles first). */
    static final int RECHECK_TICKS = 2;

    @SuppressWarnings("this-escape")
    public TwilightCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(GLOW, 0));
    }

    protected MapCodec<? extends TwilightCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            recheck(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        recheck(level, pos);
    }

    private void recheck(Level level, BlockPos pos) {
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, RECHECK_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int glow = SkySoakRules.glow(level.getBrightness(LightLayer.SKY, pos.above()));
        if (glow != state.getValue(GLOW)) {
            level.setBlock(pos, state.setValue(GLOW, glow), Block.UPDATE_CLIENTS);
        }
    }
}
