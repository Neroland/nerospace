package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.StarlitRules;
import za.co.neroland.nerospace.world.planet.RadiationRules;

/**
 * Mirror Crystal (Nyxara nightside lakes): glassy and translucent, and {@code STARLIT} (light 5) only while it sees
 * the sky in the nightside biome: it reflects the galaxy side. Covered, or carried elsewhere, it goes dark. Worldgen
 * lakes are placed {@code starlit=true}; a placed block re-reads two ticks after placement or a neighbour change.
 * Half-transparent like ice, so faces between two mirror blocks are skipped. Register with {@code noOcclusion()}.
 */
public class MirrorCrystalBlock extends HalfTransparentBlock {

    public static final MapCodec<MirrorCrystalBlock> CODEC = BlockCodecs.simple(MirrorCrystalBlock::new);

    public static final BooleanProperty STARLIT = BooleanProperty.create("starlit");

    static final int RECHECK_TICKS = 2;

    @SuppressWarnings("this-escape")
    public MirrorCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STARLIT, Boolean.FALSE));
    }

    protected MapCodec<? extends MirrorCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STARLIT);
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
        boolean nightside = level.getBiome(pos).unwrapKey()
                .map(k -> RadiationRules.NYXARA_NIGHTSIDE.equals(k.identifier().toString())).orElse(false);
        boolean starlit = StarlitRules.starlit(level.canSeeSky(pos.above()), nightside);
        if (starlit != state.getValue(STARLIT)) {
            level.setBlock(pos, state.setValue(STARLIT, starlit), Block.UPDATE_CLIENTS);
        }
    }
}
