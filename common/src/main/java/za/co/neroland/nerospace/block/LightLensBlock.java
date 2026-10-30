package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.LightLensRules;

/**
 * Light lens glass (Crater Glass): emits {@code LENS} = brightest adjacent non-lens emitter + 1,
 * recomputed on placement and neighbour change only — no ticks, no chains. Register it with
 * {@code lightLevel(s -> s.getValue(LightLensBlock.LENS))}. Sparkles client-side at {@code LENS ≥ 8}.
 */
public class LightLensBlock extends TransparentBlock {

    public static final MapCodec<LightLensBlock> CODEC = BlockCodecs.simple(LightLensBlock::new);

    public static final IntegerProperty LENS = IntegerProperty.create("lens", 0, LightLensRules.MAX);

    @SuppressWarnings("this-escape")
    public LightLensBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LENS, 0));
    }

    protected MapCodec<? extends LightLensBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LENS);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        recompute(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        recompute(state, level, pos);
    }

    private void recompute(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        int[] light = new int[6];
        boolean[] lens = new boolean[6];
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            int i = d.get3DDataValue();
            lens[i] = ns.getBlock() instanceof LightLensBlock;
            // Adjacent emitters only: reading the neighbours' propagated light would read the lens's own output
            // back (a self-latching loop that never dims when the source is removed).
            light[i] = ns.getLightEmission();
        }
        int next = LightLensRules.lensLevel(light, lens);
        if (next != state.getValue(LENS)) {
            level.setBlock(pos, state.setValue(LENS, next), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (LightLensRules.sparkles(state.getValue(LENS)) && random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
