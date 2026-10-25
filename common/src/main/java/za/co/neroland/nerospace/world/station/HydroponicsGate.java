package za.co.neroland.nerospace.world.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import za.co.neroland.nerospace.block.rules.HydroRules;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Level side of the Hydroponics Tray rule ({@link HydroRules}), called from the crop random tick. Crops not on a tray
 * pass straight through.
 */
public final class HydroponicsGate {

    private HydroponicsGate() {
    }

    /** Whether the crop at {@code cropPos} may take this random tick. */
    public static boolean allowsGrowth(ServerLevel level, BlockPos cropPos, RandomSource random) {
        BlockPos tray = cropPos.below();
        if (!level.getBlockState(tray).is(StationContent.HYDROPONICS_TRAY.get())) {
            return true;
        }
        boolean window = false;
        for (int dy = 1; dy <= HydroRules.WINDOW_RANGE; dy++) {
            if (level.getBlockState(tray.above(dy)).is(StationContent.HULL_WINDOW.get())) {
                window = true;
                break;
            }
        }
        if (!HydroRules.canGrow(window, OxygenManager.isBreathableAt(level, cropPos))) {
            return false;
        }
        StationRegistry.StationEntry entry = StationRegistry.get(level.getServer()).entryAt(level.dimension(), tray);
        double multiplier = entry != null && StationRegistry.inClaimBox(entry, tray)
                ? entry.stationClass().traits().hydroponicsMultiplier() : 1.0;
        return HydroRules.classAllows(multiplier, random.nextDouble());
    }
}
