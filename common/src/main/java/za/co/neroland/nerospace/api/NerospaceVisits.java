package za.co.neroland.nerospace.api;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.data.NerospaceErasure;
import za.co.neroland.nerospace.progression.PlanetVisitState;

/**
 * Public historical planet-visit query and observation facade.
 *
 * <p><b>Public API — semver-stable.</b> This is what lets another mod ask "has this player ever been to
 * Cindara?" — a question no amount of live event listening can answer for visits that happened before that
 * mod was installed. {@link PlanetVisitEvents} covers the live half.</p>
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * The backing store records only the set of planet ids a UUID has reached: no timestamps, no coordinates,
 * no route or location trail, and nothing that could reconstruct where a player has been within a planet.
 * It is registered with Core's shared {@code PlayerDataErasure} hook through
 * {@code NerospaceErasure}, so one erasure request purges it along with every other Nerospace store, and
 * the erasure is pushed into the saved-data backup file immediately rather than at the next periodic pass.
 */
public final class NerospaceVisits {

    private NerospaceVisits() {
    }

    /**
     * Records the player's current planet if it is not already recorded, firing {@link PlanetVisitEvents}
     * on a genuine first visit. Called from the Nerospace server player tick.
     *
     * <p>Returns immediately for players on Earth or any non-Nerospace dimension — that check is a single
     * map lookup and touches no saved data, so the cost off-world is negligible.</p>
     */
    public static void observeCurrentPlanet(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // Explicit branch rather than ifPresent: the lambda would capture `player` and so allocate on
        // every call, including the overwhelmingly common off-world one.
        Optional<PlanetId> planet = NerospacePlanets.currentPlanet(player);
        if (planet.isPresent()) {
            record(player, planet.get());
        }
    }

    /** Whether this player has ever been to {@code planet}. */
    public static boolean hasVisited(ServerPlayer player, PlanetId planet) {
        if (player == null || planet == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        return server != null && PlanetVisitState.get(server).hasVisited(player.getUUID(), planet);
    }

    /**
     * Every planet this player has reached. Works for an offline player too, which is why it takes a UUID
     * rather than a {@link ServerPlayer}. The returned set is immutable.
     */
    public static Set<PlanetId> visitedPlanets(MinecraftServer server, UUID player) {
        if (server == null || player == null) {
            return Set.of();
        }
        Set<PlanetId> result = new LinkedHashSet<>();
        PlanetVisitState.get(server).export(player)
                .forEach(id -> NerospacePlanets.byId(id).ifPresent(result::add));
        return Set.copyOf(result);
    }

    private static void record(ServerPlayer player, PlanetId planet) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        // A player who has exercised erasure while standing on a planet must not have that exact fact
        // written straight back on the next tick — which would also re-fire a "first visit" to every
        // third-party listener for someone who just asked to be forgotten. Same session latch the alien
        // villager sweep uses; it lapses when the server restarts.
        if (NerospaceErasure.erasedThisSession(player.getUUID())) {
            return;
        }
        if (PlanetVisitState.get(server).record(player.getUUID(), planet)) {
            PlanetVisitEvents.fire(new PlanetVisitEvents.Visit(player, planet));
        }
    }
}
