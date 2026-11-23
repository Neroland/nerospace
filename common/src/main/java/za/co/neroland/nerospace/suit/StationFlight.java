package za.co.neroland.nerospace.suit;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.rocket.StationRegistry;

/** The station "no suit flight" flag. Default off, so an older station entry still allows flight. */
public final class StationFlight {

    private StationFlight() {
    }

    public static boolean denied(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return false;
        }
        StationRegistry registry = StationRegistry.get(player.level().getServer());
        StationRegistry.StationEntry entry = registry.entryAt(player.level().dimension(), player.blockPosition());
        return entry != null && entry.suitFlightDenied();
    }
}
