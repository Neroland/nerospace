package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The structural frame the quarry materialises around its claimed region. Built by the controller (one
 * {@code frame_casing} per block) or placed by hand from a Frame Casing (players can outline a closed
 * rectangle themselves and the controller adopts it — no landmarks needed). Player-broken frames drop
 * a Frame Casing; the controller pauses ("frame incomplete") until the ring is whole again.
 *
 * <p>When the controlling quarry is broken while the frame stands, the controller flags every frame
 * block {@link #ORPHANED} and schedules staggered block ticks: each orphaned frame then crumbles
 * (break FX, no drop) one by one until the ring is gone. A new controller adopting the ring clears the
 * flag, which cancels any pending decay. A dedicated class so the mining loop can recognise and skip
 * frames.</p>
 */
public class QuarryFrameBlock extends Block {

    public static final MapCodec<QuarryFrameBlock> CODEC = simpleCodec(QuarryFrameBlock::new);

    /** True once the owning controller was removed — the frame is decaying and will crumble without drops. */
    public static final BooleanProperty ORPHANED = BooleanProperty.create("orphaned");

    /** Minimum scheduled-tick delay before an orphaned frame crumbles. */
    public static final int DECAY_MIN_DELAY = 20;
    /** Random extra delay bound, so orphaned frames visibly break one by one rather than all at once. */
    public static final int DECAY_RANDOM_DELAY = 200;

    @SuppressWarnings("this-escape") // registerDefaultState only touches the state definition
    public QuarryFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(ORPHANED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<QuarryFrameBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORPHANED);
    }

    /** Orphan-decay: an orphaned frame crumbles (break particles + sound, no drop) on its scheduled tick. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ORPHANED)) {
            level.destroyBlock(pos, false);
        }
    }

    /** Flags the frame at {@code pos} as orphaned and schedules its staggered crumble tick. */
    public static void startDecay(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getBlock() instanceof QuarryFrameBlock frame && !state.getValue(ORPHANED)) {
            level.setBlock(pos, state.setValue(ORPHANED, Boolean.TRUE), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, frame, DECAY_MIN_DELAY + random.nextInt(DECAY_RANDOM_DELAY));
        }
    }
}
