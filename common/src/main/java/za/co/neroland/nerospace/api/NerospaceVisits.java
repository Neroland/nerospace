package za.co.neroland.nerospace.api;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.progression.PlanetVisitState;

/** Public historical planet-visit query and observation facade. */
public final class NerospaceVisits {

    private NerospaceVisits() {
    }

    /** Called from the server player tick; records only first visit, never a timestamp or route history. */
    public static void observeCurrentPlanet(ServerPlayer player) {
        NerospacePlanets.currentPlanet(player).ifPresent(planet -> record(player, planet));
    }

    public static boolean hasVisited(ServerPlayer player, PlanetId planet) {
        MinecraftServer server = player.level().getServer();
        return server != null && PlanetVisitState.get(server).hasVisited(player.getUUID(), planet);
    }

    public static Set<PlanetId> visitedPlanets(MinecraftServer server, UUID player) {
        Set<PlanetId> result = new LinkedHashSet<>();
        PlanetVisitState.get(server).export(player).forEach(id -> NerospacePlanets.byId(id).ifPresent(result::add));
        return Set.copyOf(result);
    }

    private static void record(ServerPlayer player, PlanetId planet) {
        MinecraftServer server = player.level().getServer();
        if (server != null && PlanetVisitState.get(server).record(player.getUUID(), planet)) {
            PlanetVisitEvents.fire(new PlanetVisitEvents.Visit(player, planet));
        }
    }
}
