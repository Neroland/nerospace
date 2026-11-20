package za.co.neroland.nerospace.world.planet.europa;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Europa's positional cold shield for the hazard system: an active hydrothermal vent within 5 blocks, or warm vent
 * basalt within 2. The vent and basalt blocks gain their state in a later build step; until they do nothing on
 * Europa is warm, so both lookups answer false.
 */
public final class EuropaWarmth {

    private EuropaWarmth() {
    }

    /** Server lookup, called through {@code ColdShelter} only when cold would otherwise bite. */
    public static boolean shieldedFromCold(ServerLevel level, BlockPos pos) {
        return false;
    }

    /** Client mirror for the HUD (reads block states only). */
    public static boolean shieldedClient(Level level, BlockPos pos) {
        return false;
    }
}
