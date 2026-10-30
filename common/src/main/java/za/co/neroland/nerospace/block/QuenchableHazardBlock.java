package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerospace.block.rules.QuenchRules;

/**
 * Heat hazard that water quenches (Ember-Cracked Ashstone): on placement or a neighbour change, water in any
 * of the 6 neighbours turns it into {@code quenched} with smoke and the extinguish sound.
 */
public class QuenchableHazardBlock extends ContactHazardBlock {

    private final Supplier<BlockState> quenched;

    public QuenchableHazardBlock(Properties properties, Supplier<BlockState> quenched) {
        super(properties, Hazard.HEAT);
        this.quenched = quenched;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        tryQuench(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        tryQuench(level, pos);
    }

    private void tryQuench(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean[] water = new boolean[6];
        for (Direction d : Direction.values()) {
            water[d.get3DDataValue()] = level.getFluidState(pos.relative(d)).is(FluidTags.WATER);
        }
        if (QuenchRules.quenches(water)) {
            level.setBlockAndUpdate(pos, this.quenched.get());
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    QuenchRules.SMOKE_PARTICLES, 0.3, 0.2, 0.3, 0.01);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }
}
