package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.DryIceRules;

/**
 * Dry Ice: on placement and neighbour change, each adjacent <b>source</b> water becomes ice (≤ 6, one-shot, never
 * spreads, never touches flowing water). Random tick: in <b>block</b> light above 11 it fumes and vanishes 1-in-12,
 * dropping nothing. Sky light never counts, so the open polar cap under Mars' fixed noon persists; a torch or lamp
 * beside it makes it fume. Register with {@code randomTicks()} and {@code noOcclusion()}.
 *
 * <p>Like vanilla ice it is half-transparent: its texture has translucent pixels, so 26.x puts it in the translucent
 * chunk layer, and faces between two Dry Ice blocks are skipped.</p>
 */
public class DryIceBlock extends HalfTransparentBlock {

    public static final MapCodec<DryIceBlock> CODEC = BlockCodecs.simple(DryIceBlock::new);

    public DryIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends DryIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        freeze(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        freeze(level, pos);
    }

    private static void freeze(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        boolean[] source = new boolean[6];
        for (Direction d : Direction.values()) {
            source[d.get3DDataValue()] = level.getFluidState(pos.relative(d)).isSourceOfType(Fluids.WATER)
                    && level.getBlockState(pos.relative(d)).is(Blocks.WATER);
        }
        int mask = DryIceRules.freezeTargets(source);
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.get3DDataValue())) != 0) {
                level.setBlockAndUpdate(pos.relative(d), Blocks.ICE.defaultBlockState());
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Block light only: Mars' fixed noon puts sky light at 15 on the open polar cap, which must persist.
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        if (!DryIceRules.sublimates(light)) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                DryIceRules.CLOUD_PARTICLES, 0.3, 0.1, 0.3, 0.01);
        if (DryIceRules.vanishes(light, random.nextInt(DryIceRules.VANISH_ONE_IN))) {
            level.removeBlock(pos, false);
        }
    }
}
