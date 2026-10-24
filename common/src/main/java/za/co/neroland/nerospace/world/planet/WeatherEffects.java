package za.co.neroland.nerospace.world.planet;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.server.level.ServerLevel;

/**
 * The per-event effect hook. The weather state machine ({@link PlanetWeather}) never applies effects itself;
 * it calls the handler registered for the active {@link WeatherEvent} — on start, once per second while active,
 * and on end. Effects that need new blocks (Cindara's Ash Layer, Mars regolith drift, Frost Crystal growth,
 * Dawnband flaring) register their handler from the block code that owns them; until then only the block-free
 * effects in {@link PlanetWeather} run.
 *
 * <p>Handlers run on the server thread and must respect the decision-003 budgets (one heightmap column per
 * chunk per cadence, transition scans only on start/end). Never key anything by player.</p>
 */
public interface WeatherEffects {

    /** Called once when the event starts. */
    default void onStart(ServerLevel level, WeatherMachine weather) {
    }

    /** Called once per second (every {@link WeatherMachine#STEP_TICKS} ticks) while the event is active. */
    default void onSecond(ServerLevel level, WeatherMachine weather) {
    }

    /** Called once when the event ends (intensity faded to zero, or forced clear). */
    default void onEnd(ServerLevel level, WeatherEvent ended) {
    }

    /** Registry of handlers, one per event. Later registrations for the same event chain after earlier ones. */
    final class Registry {

        private static final Map<WeatherEvent, WeatherEffects> HANDLERS = new EnumMap<>(WeatherEvent.class);

        private Registry() {
        }

        public static synchronized void register(WeatherEvent event, WeatherEffects handler) {
            WeatherEffects existing = HANDLERS.get(event);
            if (existing == null) {
                HANDLERS.put(event, handler);
                return;
            }
            HANDLERS.put(event, new WeatherEffects() {
                @Override
                public void onStart(ServerLevel level, WeatherMachine weather) {
                    existing.onStart(level, weather);
                    handler.onStart(level, weather);
                }

                @Override
                public void onSecond(ServerLevel level, WeatherMachine weather) {
                    existing.onSecond(level, weather);
                    handler.onSecond(level, weather);
                }

                @Override
                public void onEnd(ServerLevel level, WeatherEvent ended) {
                    existing.onEnd(level, ended);
                    handler.onEnd(level, ended);
                }
            });
        }

        static WeatherEffects get(WeatherEvent event) {
            WeatherEffects h = HANDLERS.get(event);
            return h != null ? h : NONE;
        }

        private static final WeatherEffects NONE = new WeatherEffects() {
        };
    }
}
