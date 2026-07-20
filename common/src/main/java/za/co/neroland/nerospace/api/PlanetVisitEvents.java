package za.co.neroland.nerospace.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;

/** Server-thread event fired once when a player first reaches a Nerospace planet. */
public final class PlanetVisitEvents {

    public record Visit(ServerPlayer player, PlanetId planet) {
    }

    private static final List<Consumer<Visit>> LISTENERS = new CopyOnWriteArrayList<>();

    private PlanetVisitEvents() {
    }

    public static void onVisit(Consumer<Visit> listener) {
        LISTENERS.add(listener);
    }

    static void fire(Visit visit) {
        LISTENERS.forEach(listener -> listener.accept(visit));
    }
}
