package za.co.neroland.nerospace.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Wind-drifted ground (Red Regolith): a {@link PlacedFlagBlock} the {@link DriftWeather} hook may move one
 * block downwind during a dust storm. Player-placed states never move.
 */
public class DriftingBlock extends PlacedFlagBlock {

    public DriftingBlock(Properties properties) {
        super(properties);
    }

    /** Worldgen state only (player-placed regolith is immovable). */
    public boolean drifts(BlockState state) {
        return state.is(this) && !playerPlaced(state);
    }
}
