package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Void Crystal (Nyxara, 002 §1.6 / §11.2): a purple crystal that emits light 10 and refracts beams. The refraction is
 * data, not code here: the block is in {@code #nerospace:beam_refractors}, which {@code api.optics.BeamRefraction}
 * reads (any beam mod may call it) and which the Observatory telescope counts along its sightline
 * ({@code TelescopeSightline}). Client-side it sheds about one portal particle per 60 ticks.
 */
public class VoidCrystalBlock extends Block {

    public static final MapCodec<VoidCrystalBlock> CODEC = BlockCodecs.simple(VoidCrystalBlock::new);

    /** Light emitted (002 §1.6). */
    public static final int LIGHT = 10;

    /** One portal particle per this many animate ticks on average (≈ 1 per 60 ticks). */
    static final int PARTICLE_ONE_IN = 60;

    public VoidCrystalBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends VoidCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(PARTICLE_ONE_IN) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.5, -random.nextDouble() * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);
        }
    }
}
