package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

/** Planet/Home return-site crate with an inflated arrival shell. */
public class LandingPodBlock extends ReturnSiteBlock {

    public static final MapCodec<LandingPodBlock> CODEC = simpleCodec(LandingPodBlock::new);

    public LandingPodBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected MapCodec<LandingPodBlock> codec() {
        return CODEC;
    }
}
