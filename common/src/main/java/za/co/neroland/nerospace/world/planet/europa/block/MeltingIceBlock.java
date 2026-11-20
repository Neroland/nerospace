package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.MeltRules;

/**
 * Europa Ice: the surface ice. A heat source placed beside it, or the ice placed beside one, melts it to water, once.
 * Light alone never melts it, and worldgen never triggers it. Cost: 6 block reads per placement or neighbour change,
 * no scheduled or random tick.
 */
public class MeltingIceBlock extends Block {

    public static final MapCodec<MeltingIceBlock> CODEC = BlockCodecs.simple(MeltingIceBlock::new);

    public MeltingIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends MeltingIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        tryMelt(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        tryMelt(level, pos);
    }

    private static void tryMelt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (MeltRules.melts(true, MeltRules.anyHeat(EuropaHeat.heatAround(level, pos)))) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    MeltRules.STEAM_PARTICLES, 0.3, 0.2, 0.3, 0.01);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }
}
