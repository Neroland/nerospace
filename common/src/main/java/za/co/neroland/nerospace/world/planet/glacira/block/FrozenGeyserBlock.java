package za.co.neroland.nerospace.world.planet.glacira.block;

import com.mojang.serialization.MapCodec;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.GeyserBlock;

/**
 * Frozen Geyser: the shared vent base. The base bootstraps the 160-tick cycle on a random tick for geysers placed by
 * worldgen, so this needs {@code randomTicks()} and nothing else.
 */
public class FrozenGeyserBlock extends GeyserBlock {

    public static final MapCodec<FrozenGeyserBlock> CODEC = BlockCodecs.simple(FrozenGeyserBlock::new);

    public FrozenGeyserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FrozenGeyserBlock> codec() {
        return CODEC;
    }
}
