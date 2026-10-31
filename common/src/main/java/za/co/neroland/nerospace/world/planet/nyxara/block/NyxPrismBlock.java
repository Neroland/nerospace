package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.block.rules.PrismBeamRules;
import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * Nyx Prism (Nyxara spire tips, craftable): the second flare-flipped block. Idle light 6; while {@code FLARING}
 * (set by its own {@code WeatherFlipBlock} registration and scan) it glows 12 and throws a 6-block ray of violet-to-gold
 * particles toward +X, away from the pinned sun. The hazard side (the beam doubles the flare's radiation bump) is read
 * by the hazard system. In {@code #nerospace:beam_refractors}. Register with
 * {@code lightLevel(s -> PrismBeamRules.light(s.getValue(FLARING)))} and {@code noOcclusion()}.
 */
public class NyxPrismBlock extends WeatherFlipBlock {

    public static final MapCodec<NyxPrismBlock> CODEC = BlockCodecs.simple(NyxPrismBlock::new);

    private static final DustColorTransitionOptions RAY = new DustColorTransitionOptions(0x9B5DE5, 0xFFB86B, 0.8F);

    public NyxPrismBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NyxPrismBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FLARING) || !NerospaceConfig.particlesEnabled()) {
            return;
        }
        for (int i = 0; i < PrismBeamRules.RAY_PARTICLES; i++) {
            double along = 1.0 + random.nextDouble() * (PrismBeamRules.RANGE - 1);
            level.addParticle(RAY, pos.getX() + 0.5 + along, pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.2,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0.02, 0.0, 0.0);
        }
    }
}
