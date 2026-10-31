package za.co.neroland.nerospace.world.planet.nyxara;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.block.rules.PrismBeamRules;
import za.co.neroland.nerospace.block.rules.RadShadeRules;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Position lookups the radiation level reads for the Nyxara crystal: the Sunglass Crystal shade (a
 * {@code #nerospace:radiation_shade} block in the three cells above the head) and the Nyx Prism beam (a flaring prism
 * 1..6 blocks toward -X at feet or head height, the beam running toward +X). Both read loaded chunks only and are
 * called on the radiation damage interval, not every tick.
 */
public final class NyxaraCrystalHazards {

    private NyxaraCrystalHazards() {
    }

    /** {@code feet} is the player's block position; the head is one above it. */
    public static boolean shaded(ServerLevel level, BlockPos feet) {
        boolean[] above = new boolean[RadShadeRules.CELLS];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < RadShadeRules.CELLS; i++) {
            cursor.set(feet.getX(), feet.getY() + 2 + i, feet.getZ());
            above[i] = level.isLoaded(cursor) && level.getBlockState(cursor).is(ModTags.Blocks.RADIATION_SHADE);
        }
        return RadShadeRules.shaded(above);
    }

    public static boolean inPrismBeam(ServerLevel level, BlockPos feet) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = 1; dx <= PrismBeamRules.RANGE; dx++) {
                cursor.set(feet.getX() - dx, feet.getY() + dy, feet.getZ());
                if (!level.isLoaded(cursor)) {
                    continue;
                }
                BlockState s = level.getBlockState(cursor);
                if (s.is(NyxaraContent.NYX_PRISM.get()) && s.getValue(WeatherFlipBlock.FLARING)
                        && PrismBeamRules.inBeam(dx, 0, 0)) {
                    return true;
                }
            }
        }
        return false;
    }
}
