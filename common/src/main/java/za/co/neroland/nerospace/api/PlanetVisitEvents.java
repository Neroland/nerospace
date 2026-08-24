package za.co.neroland.nerospace.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server-thread event fired exactly once, the first time a player reaches a given Nerospace planet.
 *
 * <p><b>Public API — semver-stable.</b> Listeners are invoked synchronously on the server thread inside the
 * player tick, so they must be cheap and must not block. A listener that throws is logged and skipped
 * rather than allowed to break the tick loop for every other listener.</p>
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the event hands over a live {@link ServerPlayer} because listeners
 * generally need to grant something to that player. Nerospace itself records only the UUID-to-planet fact
 * (see {@code PlanetVisitState}); it never logs who visited what.</p>
 */
public final class PlanetVisitEvents {

    /** A player's first arrival at {@code planet}. */
    public record Visit(ServerPlayer player, PlanetId planet) {
    }

    private static final List<Consumer<Visit>> LISTENERS = new CopyOnWriteArrayList<>();

    private PlanetVisitEvents() {
    }

    /** Registers a first-visit listener. Call during mod init; there is no unregister. */
    public static void onVisit(Consumer<Visit> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Visit listener must not be null");
        }
        LISTENERS.add(listener);
    }

    static void fire(Visit visit) {
        for (Consumer<Visit> listener : LISTENERS) {
            try {
                listener.accept(visit);
            } catch (Exception e) {
                // One bad integration must not break the player tick, nor the other listeners.
                // The planet id is public world data; the player is deliberately not logged.
                NerospaceCommon.LOGGER.error(
                        "[Nerospace] A planet-visit listener threw for planet {}; skipping it.",
                        visit.planet().asString(), e);
            }
        }
    }
}
