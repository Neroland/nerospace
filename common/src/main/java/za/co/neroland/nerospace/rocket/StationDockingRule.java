package za.co.neroland.nerospace.rocket;

import za.co.neroland.nerospace.world.station.DockingPolicy;

/**
 * Pure station docking-policy rule: an OPEN station may be chosen by anyone; a MEMBERS station only by a
 * player who passes {@code canUse} (owner, member or op). Applied to the Rocket screen's station cycler and
 * re-checked at launch; a rocket already in flight still lands.
 */
public final class StationDockingRule {

    private StationDockingRule() {
    }

    public static boolean mayChoose(DockingPolicy policy, boolean canUse) {
        return policy != DockingPolicy.MEMBERS || canUse;
    }
}
