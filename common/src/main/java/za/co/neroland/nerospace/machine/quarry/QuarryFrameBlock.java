package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * The structural frame the quarry materialises around its claimed region. Built by the controller (one
 * {@code frame_casing} per block) or placed by hand from a Frame Casing (players can outline a closed
 * rectangle themselves and the controller adopts it — no landmarks needed). Player-broken frames drop
 * a Frame Casing; the controller pauses ("frame incomplete") until the ring is whole again.
 *
 * <p>When the controlling quarry is broken while the frame stands, the controller flags every frame
 * block {@link #ORPHANED} and schedules staggered block ticks: each orphaned frame then slowly crumbles
 * (break FX, dropping its Frame Casing) at its own random moment — the ring visibly decays block by
 * block over minutes. The delay window is tuned by the {@code quarryFrameDecayTicks} config. A new
 * controller adopting the ring clears the flag, which cancels any pending decay. A dedicated class so
 * the mining loop can recognise and skip frames.</p>
 */
public class QuarryFrameBlock extends Block {

    public static final MapCodec<QuarryFrameBlock> CODEC = simpleCodec(QuarryFrameBlock::new);

    /** True once the owning controller was removed — the frame is decaying and will slowly crumble, dropping its casing. */
    public static final BooleanProperty ORPHANED = BooleanProperty.create("orphaned");

    /**
     * Random extra delay bound as a multiple of the configured base delay: each orphaned frame waits
     * {@code base + rand(base * 7)} ticks, so at the default base of 600 a block crumbles after
     * ~30 s – 4 min and the ring visibly decays one block at a time.
     */
    public static final int DECAY_RANDOM_SPREAD = 7;

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

    /**
     * Orphan-decay: an orphaned frame crumbles (break particles + sound) on its scheduled tick,
     * dropping via its loot table — the same Frame Casing a player-mined frame drops.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ORPHANED)) {
            level.destroyBlock(pos, true);
        }
    }

    /** Flags the frame at {@code pos} as orphaned and schedules its staggered slow-crumble tick. */
    public static void startDecay(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getBlock() instanceof QuarryFrameBlock frame && !state.getValue(ORPHANED)) {
            level.setBlock(pos, state.setValue(ORPHANED, Boolean.TRUE), Block.UPDATE_CLIENTS);
            int base = NerospaceConfig.quarryFrameDecayTicks();
            level.scheduleTick(pos, frame, base + random.nextInt(base * DECAY_RANDOM_SPREAD));
        }
    }
}
