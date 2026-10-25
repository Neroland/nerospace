package za.co.neroland.nerospace.world.planet.glacira.block;

import com.mojang.serialization.MapCodec;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.ClockLitBlock;

/**
 * Aurora Stone: the shared clock-lit base. The base bootstraps the 200-tick schedule on a random tick for blocks
 * placed by worldgen, so this needs {@code randomTicks()} and nothing else.
 */
public class AuroraStoneBlock extends ClockLitBlock {

    public static final MapCodec<AuroraStoneBlock> CODEC = BlockCodecs.simple(AuroraStoneBlock::new);

    public AuroraStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AuroraStoneBlock> codec() {
        return CODEC;
    }
}
