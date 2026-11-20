package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.ConductRules;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Vent Basalt: the sea-floor rock, which carries a vent's warmth. {@code DISTANCE} is 1 beside an active Hydrothermal
 * Vent, otherwise one more than the warmest basalt neighbour, capped at 4 (cold); 3 or less is warm. A neighbour
 * change compares the stored value with the computed one (6 block reads) and, only when they differ, schedules a tick
 * that writes it, so a vent switching on or off sends a wave that dies three blocks out.
 *
 * <p>Generated basalt is at distance 4 until something next to it changes. That is accepted: an active vent already
 * shields everything within 5 blocks, which covers every block the wave would have warmed.</p>
 */
public class WarmBasaltBlock extends Block {

    public static final MapCodec<WarmBasaltBlock> CODEC = BlockCodecs.simple(WarmBasaltBlock::new);

    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 1, ConductRules.COLD);

    @SuppressWarnings("this-escape")
    public WarmBasaltBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, ConductRules.COLD));
    }

    protected MapCodec<? extends WarmBasaltBlock> codec() {
        return CODEC;
    }

    /** Whether a state is warm vent basalt. */
    public static boolean warm(BlockState state) {
        return state.hasProperty(DISTANCE) && ConductRules.warm(state.getValue(DISTANCE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        check(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        check(state, level, pos);
    }

    private void check(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide() && computed(level, pos) != state.getValue(DISTANCE)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int next = computed(level, pos);
        if (next != state.getValue(DISTANCE)) {
            level.setBlock(pos, state.setValue(DISTANCE, next), Block.UPDATE_ALL);
        }
    }

    private int computed(Level level, BlockPos pos) {
        int[] distances = new int[6];
        boolean vent = false;
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            distances[d.get3DDataValue()] = n.is(this) ? n.getValue(DISTANCE) : ConductRules.COLD;
            vent |= n.is(EuropaContent.HYDROTHERMAL_VENT.get()) && n.getValue(HydrothermalVentBlock.ACTIVE);
        }
        return ConductRules.distance(distances, vent);
    }
}
