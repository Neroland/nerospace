package za.co.neroland.nerospace.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A mod may refuse suit flight at a position. The callback receives a level, a position and an entity type.
 * It never receives a player id.
 */
public final class NerospaceSuitPolicy {

    @FunctionalInterface
    public interface FlightVeto {
        boolean veto(Level level, Vec3 position, EntityType<?> type);
    }

    private static final List<FlightVeto> VETOES = new CopyOnWriteArrayList<>();

    private NerospaceSuitPolicy() {
    }

    public static void registerFlightVeto(FlightVeto veto) {
        if (veto != null) {
            VETOES.add(veto);
        }
    }

    public static boolean vetoed(Level level, Vec3 position, EntityType<?> type) {
        for (FlightVeto veto : VETOES) {
            try {
                if (veto.veto(level, position, type)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // A veto that throws does not grant flight.
                return true;
            }
        }
        return false;
    }
}
