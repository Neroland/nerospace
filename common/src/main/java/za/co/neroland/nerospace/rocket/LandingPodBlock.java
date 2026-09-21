package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/** Planet/Home return-site crate with an inflated arrival shell. */
public class LandingPodBlock extends ReturnSiteBlock {

    public static final MapCodec<LandingPodBlock> CODEC = BlockCodecs.simple(LandingPodBlock::new);

    public LandingPodBlock(Properties properties) {
        super(properties, true);
    }

    protected MapCodec<LandingPodBlock> codec() {
        return CODEC;
    }
}
