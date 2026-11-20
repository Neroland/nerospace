package za.co.neroland.nerospace.world.planet;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.world.planet.europa.EuropaWarmth;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraWarmth;

/**
 * The one entry point for positional cold shields: a flaring Dawnband Stone on Nyxara, an active vent or warm basalt
 * on Europa, nothing anywhere else. The last answer is kept per level for {@link ColdShelterRules#BUCKET_TICKS}
 * ticks, so the repeated calls of one hazard check scan once. Server thread only.
 */
public final class ColdShelter {

    private record Entry(long posKey, long bucket, boolean value) {
    }

    private static final Map<ServerLevel, Entry> CACHE = new WeakHashMap<>();

    private ColdShelter() {
    }

    /** Whether {@code pos} is shielded from cold by something nearby. False off a planet. */
    public static boolean shielded(ServerLevel level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return false;
        }
        ColdShelterRules.Source source = ColdShelterRules.source(planet.id().asString());
        if (source == ColdShelterRules.Source.NONE) {
            return false;
        }
        long key = pos.asLong();
        long bucket = ColdShelterRules.bucket(level.getGameTime());
        Entry entry = CACHE.get(level);
        if (entry != null && ColdShelterRules.cacheHit(true, entry.posKey(), entry.bucket(), key, bucket)) {
            return entry.value();
        }
        boolean value = source == ColdShelterRules.Source.NYXARA
                ? NyxaraWarmth.shieldedFromCold(level, pos)
                : EuropaWarmth.shieldedFromCold(level, pos);
        CACHE.put(level, new Entry(key, bucket, value));
        return value;
    }
}
