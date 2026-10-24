package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.rules.SolarSinterRules;
import za.co.neroland.nerospace.world.planet.PlanetClock;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Lunar Basalt (002 §1.1): a player-placed block in Luna sunlight sinters into its polished form, 1-in-6 per random
 * tick. Stage 3 passes the polished block and must give {@code Properties.randomTicks()}.
 */
public class SolarSinterBlock extends PlacedFlagBlock {

    private final Supplier<BlockState> sintered;

    public SolarSinterBlock(Properties properties, Supplier<BlockState> sintered) {
        super(properties);
        this.sintered = sintered;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean onLuna = level.dimension().equals(Planets.LUNA.dimension());
        if (SolarSinterRules.canSinter(playerPlaced(state), level.canSeeSky(pos.above()), PlanetClock.daylight(level), onLuna)
                && SolarSinterRules.rolled(random.nextInt(SolarSinterRules.ONE_IN))) {
            level.setBlockAndUpdate(pos, this.sintered.get());
        }
    }
}
